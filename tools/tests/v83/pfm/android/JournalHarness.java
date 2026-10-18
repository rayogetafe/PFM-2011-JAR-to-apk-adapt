package pfm.android;
import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;

public class JournalHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void load(String path,NativeCareerStore.Career career)throws Exception{
        for(String line:Files.readAllLines(Paths.get(path))){String[] q=line.split("\t",-1);if(q[0].equals("type"))continue;String id=q[1]+":"+q[2];
            if(q[0].equals("T")){NativeCareerStore.Club c=new NativeCareerStore.Club();c.id=id;c.name=q[3];c.league=q[1];c.sourceId=Integer.parseInt(q[2]);career.clubs.add(c);
                NativeWorldCenter.T t=new NativeWorldCenter.T();t.id=c.sourceId;t.name=c.name;t.style=Integer.parseInt(q[14]);t.form=Integer.parseInt(q[15]);NativeWorldCenter.leagues.computeIfAbsent(c.league,k->new ArrayList<>()).add(t);}
            else{NativeCareerStore.Player p=new NativeCareerStore.Player();p.id=q[1].startsWith("eu_")?q[18]:id+":"+q[4];p.owner=id;p.name=q[5];p.nationality=q[9];p.sourceId=Integer.parseInt(q[4]);p.position=Integer.parseInt(q[6]);p.age=Integer.parseInt(q[7]);p.overall=Integer.parseInt(q[8]);p.spe=Integer.parseInt(q[10]);p.res=Integer.parseInt(q[11]);p.qua=Integer.parseInt(q[12]);p.morale=Integer.parseInt(q[13]);career.players.add(p);}
        }
    }
    static void world(String path,int nonce)throws Exception{
        NativeWorldCenter.nonce=nonce;NativeWorldCenter.year=2010;NativeWorldCenter.played=0;NativeWorldCenter.leagues.clear();
        NativeCareerStore.Career c=new NativeCareerStore.Career();c.activeLeague="en";c.season=2010;load(path,c);load("overrides/app/src/main/assets/europe_world_v78.tsv",c);NativeCareerStore.current=c;
        // The global UI model is deliberately separate from career ownership and attributes.
        HashMap<String,NativeWorldCenter.T> teams=NativeMatchJournal.teams(c);for(NativeCareerStore.Club club:c.clubs)NativeWorldCenter.leagues.get(club.league).set(club.sourceId,teams.get(club.id));
    }
    public static ArrayList<MatchJournal.Match> legacy(int upto){
        String code=NativeCareerStore.current.activeLeague;ArrayList<MatchJournal.Match> out=new ArrayList<>();
        for(NativeWorldCenter.M fixture:NativeWorldCenter.schedule(NativeWorldCenter.leagues.get(code).size()))if(fixture.r<=upto){
            MatchJournal.Match m=new MatchJournal.Match();m.competition="league:"+code;m.home=code+":"+fixture.h;m.away=code+":"+fixture.a;m.round=fixture.r;m.id=NativeMatchJournal.key(m.competition,m.round,m.home,m.away);m.homeGoals=1;m.awayGoals=0;m.source="legacy-exact";out.add(m);
        }return out;
    }
    static void restart()throws Exception{Field f=NativeMatchJournal.class.getDeclaredField("identity");f.setAccessible(true);f.setInt(null,Integer.MIN_VALUE);}
    static String frozen(MatchJournal.Match m){StringBuilder s=new StringBuilder(m.id+":"+m.homeGoals+":"+m.awayGoals+":"+m.report);for(MatchJournal.PlayerFact p:m.players)s.append(p.uid).append(p.name).append(p.club).append(Arrays.toString(p.stats));return s.toString();}
    static MatchJournal open(Context c,int nonce,int year)throws Exception{return new MatchJournal(new File(c.getFilesDir(),"match-journals"),nonce,year);}
    static void codec(File files)throws Exception{
        MatchJournal.Match raw=new MatchJournal.Match();raw.competition="league:en";raw.home="en:0";raw.away="en:1";raw.homeGoals=2;raw.report="H\tHome\tAway\t2\t0\nP\t0\t9\tScorer\tFW\t1\t90\t80\t10,70\t\t99\t\t\nP\t1\t1\tKeeper\tGK\t1\t90\t60\t\t\t100\t\t\n";
        MatchJournal.legacyDetails(raw);check(raw.players.size()==2&&raw.players.get(0).uid.equals("legacy:en:99")&&raw.players.get(0).stats[0]==2&&raw.players.get(1).stats[6]==2,"legacy report parsing");
        File dir=new File(files,"codec");MatchJournal j=new MatchJournal(dir,1,2010);MatchJournal.Match m=new MatchJournal.Match();m.id="x";m.competition="league:en";m.home="en:0";m.away="en:1";m.round=1;m.homeGoals=2;m.report="Рубин – København";
        MatchJournal.PlayerFact f=new MatchJournal.PlayerFact();f.uid="uid:one";f.name="Player";f.stats[0]=2;m.players.add(f);j.put(m);j.save();m.homeGoals=9;m.players.get(0).name="Changed";
        check(j.get("x").homeGoals==2&&j.get("x").players.get(0).name.equals("Player"),"input mutation rewrote facts");
        MatchJournal.Match exposed=j.get("x");exposed.homeGoals=8;check(j.get("x").homeGoals==2,"getter exposed facts");check(!j.put(m),"duplicate fixture replaced score");
        check(new MatchJournal(dir,1,2011).size()==0&&new MatchJournal(dir,2,2010).size()==0,"identity/season leak");
        MatchJournal.Match second=new MatchJournal.Match();second.id="y";second.competition=m.competition;second.home=m.home;second.away=m.away;second.round=2;j.put(second);j.save();
        File primary=new File(dir,"matches-1-2010.bin");Files.write(primary.toPath(),new byte[]{1,2,3});MatchJournal recovered=new MatchJournal(dir,1,2010);check(recovered.size()==1&&recovered.get("x").homeGoals==2,"backup recovery");
        recovered.put(second);recovered.save();check(new MatchJournal(dir,1,2010).size()==2,"recovery/save broke backup");
        Files.write(primary.toPath(),new byte[]{0});Files.write(new File(primary.getPath()+".backup").toPath(),new byte[]{0});boolean rejected=false;try{new MatchJournal(dir,1,2010);}catch(IOException expected){rejected=true;}check(rejected,"double corruption reset history silently");
        System.out.println("Journal codec: immutable copies, duplicate protection, UTF-8 names, identities, seasons, checksum and backup recovery verified");
    }
    public static void main(String[] args)throws Exception{
        File files=new File(args[1]);Context context=new Context("overrides/app/src/main/assets",files);AndroidRuntime.current=context;codec(files);world(args[0],101);
        NativeMatchJournal.advance(context,0);check(NativeCareerStore.ensures==0,"new-career bootstrap created career early");
        for(int r=1;r<=4;r++){NativeWorldCenter.played=r;NativeMatchJournal.advance(context,r);}
        MatchJournal j=open(context,101,2010);check(j.competition("ucl:2010").size()==16,"UCL calendar round 4");check(j.competition("league:en").size()==40,"active league import");
        for(MatchJournal.Match m:j.competition("league:en"))check(m.homeGoals==1&&m.awayGoals==0,"active score resimulated");
        MatchJournal.Match first=j.competition("league:es").get(0);String before=frozen(first);String uid=first.players.get(0).uid;
        NativeCareerStore.Player changed=null;for(NativeCareerStore.Player p:NativeCareerStore.current.players)if(p.id.equals(uid)){changed=p;break;}check(changed!=null,"snapshot identity");changed.owner="eu_portugal:0";changed.name="Transferred Player";changed.qua=99;
        byte[] disk=Files.readAllBytes(new File(files,"match-journals/matches-101-2010.bin").toPath());NativeMatchJournal.advance(context,4);check(Arrays.equals(disk,Files.readAllBytes(new File(files,"match-journals/matches-101-2010.bin").toPath())),"reopening wrote a second match");
        for(NativeWorldCenter.T t:NativeWorldCenter.leagues.get("es"))for(NativeWorldCenter.P p:t.ps)p.fat=100;
        NativeWorldCenter.played=8;NativeMatchJournal.advance(context,8);j=open(context,101,2010);check(j.competition("ucl:2010").size()==32,"UCL round 8");check(before.equals(frozen(j.get(first.id))),"transfer rewrote old score/players");
        ArrayList<NativeWorldCenter.M> projection=NativeMatchJournal.project("es",8,2010);int points=0;for(NativeWorldCenter.T t:NativeWorldCenter.leagues.get("es"))points+=t.pts;
        NativeMatchJournal.project("it",8,2010);NativeMatchJournal.project("es",8,2010);int again=0;for(NativeWorldCenter.T t:NativeWorldCenter.leagues.get("es"))again+=t.pts;check(points==again,"projections double-counted stats");
        int count=j.size();restart();NativeMatchJournal.advance(context,8);check(open(context,101,2010).size()==count,"restart duplicated matches");
        NativeWorldCenter.year=2011;NativeCareerStore.current.season=2011;NativeWorldCenter.played=1;NativeMatchJournal.advance(context,1);check(open(context,101,2011).size()>0,"season rollover");
        int currentCount=open(context,101,2011).size();NativeMatchJournal.project("es",38,2010);check(open(context,101,2010).size()==count&&open(context,101,2011).size()==currentCount,"history view simulated matches");
        world(args[0],202);NativeMatchJournal.advance(context,0);check(open(context,202,2010).size()==0,"new career inherited matches");
        world(args[0],303);context.getSharedPreferences("pfm_ucl_groups_v79",0).put("303_2010_0_0_0","7:6");NativeWorldCenter.played=4;NativeMatchJournal.advance(context,4);
        MatchJournal.Match old=open(context,303,2010).get(NativeMatchJournal.key("ucl:2010",1,"it:0","eu_netherlands:2"));check(old!=null&&old.homeGoals==7&&old.awayGoals==6&&old.players.isEmpty(),"v82 UCL result replaced or line-up invented");
        world(args[0],404);long start=System.nanoTime();for(int round=1;round<=38;round++){NativeWorldCenter.played=round;NativeMatchJournal.advance(context,round);}
        MatchJournal full=open(context,404,2010);check(full.size()==1922,"full calendar fixture count "+full.size());
        for(String code:NativeWorldCenter.C)check(full.competition("league:"+code).size()==(code.equals("de")?306:380),"incomplete league "+code);
        ArrayList<MatchJournal.Match> nativeMatches=full.competition("ucl:2010");for(String code:new String[]{"es","it","de","fr"})nativeMatches.addAll(full.competition("league:"+code));
        for(MatchJournal.Match m:nativeMatches){int hg=0,ag=0,hs=0,as=0;for(MatchJournal.PlayerFact p:m.players)if(p.club.equals(m.home)){hg+=p.stats[0];hs+=p.stats[3];}else{ag+=p.stats[0];as+=p.stats[3];}
            check(hg==m.homeGoals&&ag==m.awayGoals&&hs==11&&as==11,"score/participants mismatch "+m.id);}
        for(int step=0;step<=34;step++)check(NativeCompetitionCalendar.parallelRound(step,34,"en")>=NativeCompetitionCalendar.parallelRound(Math.max(0,step-1),34,"en"),"German source calendar reversed");
        check(NativeCompetitionCalendar.parallelRound(34,34,"en")==38,"German calendar lost final rounds");
        System.out.printf(Locale.US,"Full season: %d frozen fixtures, coherent scorers/XIs; %.2f MB journal, %.2f seconds for 38 persisted rounds%n",full.size(),new File(files,"match-journals/matches-404-2010.bin").length()/1048576.0,(System.nanoTime()-start)/1e9);
        System.out.println("Calendar: active scores, parallel/UCL schedule, transfers, projections, restart, rollover, history, fresh careers and old UCL scores verified");
    }
}
class NativeCareerStore {
    static class Club {String id,name,league;int sourceId;}
    static class Player {String id,name,owner,nationality;int sourceId,position,age,overall,spe,res,qua,morale;}
    static class Career {int season;String activeLeague;ArrayList<Club> clubs=new ArrayList<>();ArrayList<Player> players=new ArrayList<>();}
    static Career current;static int ensures;static Career ensure(Context c){ensures++;return current;}
}
class NativeTransferCalendar {static int total(){return NativeCareerStore.current.activeLeague.equals("de")?34:38;}static int played(){return NativeWorldCenter.played;}}
