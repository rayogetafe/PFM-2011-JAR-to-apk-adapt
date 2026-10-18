package pfm.android;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Immutable fixture facts, isolated by career and season; no Android or match simulation. */
public final class MatchJournal {
    private static final int MAGIC=0x50464d53,VERSION=1,MAX_BYTES=64*1024*1024;
    public static final class PlayerFact {
        public String uid="",club="",name="";
        // Goals, assists, appearances, starts, rating sum, clean sheets, conceded, saves, fatigue after.
        public int[] stats=new int[9];
    }
    public static final class Match {
        public String id="",competition="",home="",away="",source="",report="";
        public int round,homeGoals,awayGoals,seed;
        public ArrayList<PlayerFact> players=new ArrayList<PlayerFact>();
    }
    private final File file,backup;
    private final int career,season;
    private final LinkedHashMap<String,Match> matches=new LinkedHashMap<String,Match>();
    private final HashMap<String,String> strings=new HashMap<String,String>();
    private boolean dirty,primaryValid;
    public MatchJournal(File directory,int career,int season)throws IOException{
        this.career=career;this.season=season;
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create match journal directory");
        file=new File(directory,"matches-"+career+"-"+season+".bin");backup=new File(file.getPath()+".backup");
        if(file.exists()||backup.exists()){
            try{read(file);primaryValid=true;}catch(IOException primary){matches.clear();strings.clear();try{read(backup);}catch(IOException fallback){throw new IOException("Match journal and backup unavailable",primary);}}
        }
    }
    public Match get(String id){Match m=matches.get(id);return m==null?null:copy(m);}
    public ArrayList<Match> competition(String competition){ArrayList<Match> out=new ArrayList<Match>();for(Match m:matches.values())if(m.competition.equals(competition))out.add(copy(m));return out;}
    public int size(){return matches.size();}
    public boolean put(Match match){
        if(matches.containsKey(match.id))return false;
        if(match.id.length()==0||match.competition.length()==0||match.home.equals(match.away)||match.round<1||match.homeGoals<0||match.awayGoals<0)throw new IllegalArgumentException("Invalid fixture facts");
        for(PlayerFact p:match.players)if(p.uid==null||p.uid.length()==0||p.stats==null||p.stats.length!=9)throw new IllegalArgumentException("Invalid player facts");
        matches.put(match.id,canonical(copy(match)));dirty=true;return true;
    }
    private String canonical(String s){String old=strings.get(s);if(old!=null)return old;strings.put(s,s);return s;}
    private Match canonical(Match m){m.id=canonical(m.id);m.competition=canonical(m.competition);m.home=canonical(m.home);m.away=canonical(m.away);m.source=canonical(m.source);m.report=canonical(m.report);for(PlayerFact p:m.players){p.uid=canonical(p.uid);p.club=canonical(p.club);p.name=canonical(p.name);}return m;}
    /** Decode the original archived report without inventing absent participant data. */
    public static void legacyDetails(Match m){
        if(m.report==null)m.report="";if(!m.report.startsWith("H\t"))return;
        m.source="legacy-exact";
        for(String line:m.report.split("\n")){String[] p=line.split("\t",-1);if(p.length<13||!p[0].equals("P"))continue;
            try{if(Integer.parseInt(p[6])<=0)continue;int side=Integer.parseInt(p[1]);PlayerFact f=new PlayerFact();f.uid="legacy:"+m.competition.substring("league:".length())+":"+p[10];f.club=side==0?m.home:m.away;f.name=p[3];f.stats[0]=p[8].length()==0?0:p[8].split(",").length;f.stats[1]=p[9].length()==0?0:p[9].split(",").length;f.stats[2]=1;f.stats[3]=Integer.parseInt(p[5]);f.stats[4]=Integer.parseInt(p[7]);f.stats[8]=-1;
                if(p[4].equals("GK")){f.stats[6]=side==0?m.awayGoals:m.homeGoals;f.stats[5]=f.stats[6]==0?1:0;}m.players.add(f);
            }catch(NumberFormatException invalid){m.source="legacy-partial";}
        }
    }
    private static Match copy(Match m){Match n=new Match();n.id=m.id;n.competition=m.competition;n.home=m.home;n.away=m.away;n.source=m.source;n.report=m.report;n.round=m.round;n.homeGoals=m.homeGoals;n.awayGoals=m.awayGoals;n.seed=m.seed;
        for(PlayerFact p:m.players){PlayerFact q=new PlayerFact();q.uid=p.uid;q.club=p.club;q.name=p.name;q.stats=p.stats.clone();n.players.add(q);}return n;}
    private byte[] encode()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(career);out.writeInt(season);out.writeInt(matches.size());
        for(Match m:matches.values()){
            out.writeUTF(m.id);out.writeUTF(m.competition);out.writeUTF(m.home);out.writeUTF(m.away);out.writeUTF(m.source);out.writeUTF(m.report);
            out.writeInt(m.round);out.writeInt(m.homeGoals);out.writeInt(m.awayGoals);out.writeInt(m.seed);out.writeInt(m.players.size());
            for(PlayerFact p:m.players){out.writeUTF(p.uid);out.writeUTF(p.club);out.writeUTF(p.name);for(int n:p.stats)out.writeInt(n);}
        }
        out.flush();return bytes.toByteArray();
    }
    public void save()throws IOException{
        if(!dirty)return;byte[] bytes=encode();CRC32 crc=new CRC32();crc.update(bytes);File temporary=new File(file.getPath()+".tmp");
        try(FileOutputStream stream=new FileOutputStream(temporary);DataOutputStream out=new DataOutputStream(stream)){out.writeInt(bytes.length);out.write(bytes);out.writeLong(crc.getValue());out.flush();stream.getFD().sync();}
        if(file.exists()){
            if(!primaryValid){if(!file.delete())throw new IOException("Cannot replace damaged journal");}
            else {if(backup.exists()&&!backup.delete())throw new IOException("Cannot rotate journal backup");
                if(!file.renameTo(backup))throw new IOException("Cannot back up match journal");}
        }
        if(!temporary.renameTo(file)){if(backup.exists()&&!file.exists())backup.renameTo(file);throw new IOException("Cannot publish match journal");}
        dirty=false;primaryValid=true;
    }
    private static int count(DataInputStream in,int max)throws IOException{int n=in.readInt();if(n<0||n>max)throw new IOException("Invalid journal count");return n;}
    private void read(File source)throws IOException{
        if(!source.isFile())throw new FileNotFoundException(source.getName());
        byte[] bytes;try(DataInputStream in=new DataInputStream(new FileInputStream(source))){int length=count(in,MAX_BYTES);bytes=new byte[length];in.readFully(bytes);long expected=in.readLong();CRC32 crc=new CRC32();crc.update(bytes);if(crc.getValue()!=expected||in.read()!=-1)throw new IOException("Journal checksum mismatch");}
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION||in.readInt()!=career||in.readInt()!=season)throw new IOException("Wrong journal identity or version");
            int count=count(in,10000);for(int i=0;i<count;i++){
                Match m=new Match();m.id=in.readUTF();m.competition=in.readUTF();m.home=in.readUTF();m.away=in.readUTF();m.source=in.readUTF();m.report=in.readUTF();m.round=in.readInt();m.homeGoals=in.readInt();m.awayGoals=in.readInt();m.seed=in.readInt();
                int players=count(in,100);for(int p=0;p<players;p++){PlayerFact fact=new PlayerFact();fact.uid=in.readUTF();fact.club=in.readUTF();fact.name=in.readUTF();for(int s=0;s<9;s++)fact.stats[s]=in.readInt();m.players.add(fact);}
                if(matches.put(m.id,canonical(m))!=null)throw new IOException("Duplicate fixture in journal");
            }
            if(in.read()!=-1)throw new IOException("Trailing journal data");
        }
    }
}
