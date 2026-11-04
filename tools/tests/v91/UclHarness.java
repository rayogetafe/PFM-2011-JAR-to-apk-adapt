package pfm.android;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import android.content.Context;

public class UclHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static MatchJournal.Match match(String h,String a,int hg,int ag){MatchJournal.Match m=new MatchJournal.Match();m.round=1;m.competition=UclKnockout.COMP;m.home=h;m.away=a;m.homeGoals=hg;m.awayGoals=ag;return m;}
    static void rules(){
        check(UclKnockout.decision(2,1,1,0)==1,"away goals second-leg home");
        check(UclKnockout.decision(1,0,2,1)==-1,"away goals second-leg away");
        check(UclKnockout.decision(1,1,1,1)==0,"ET required");
        check(UclKnockout.decision(1,1,2,2)==-1,"ET away goals apply");
        check(UclKnockout.decision(4,0,5,0)==1,"aggregate first");
        ArrayList<MatchJournal.Match> matches=new ArrayList<>();matches.add(match("a","b",1,0));matches.add(match("b","a",0,0));matches.add(match("a","c",0,9));matches.add(match("b","c",8,0));
        ArrayList<UclKnockout.Row> table=UclKnockout.table(Arrays.asList("a","b","c","d"),matches);check(table.get(0).id.equals("a"),"H2H must precede overall goal difference");
        MatchEngine.Team same=new MatchEngine.Team();for(int i=0;i<11;i++)same.add(i==0?0:i<5?1:i<9?2:3,70,70,70,55,0);
        check(Math.abs(MatchEngine.expectedGoals(same,same,true,MatchEngine.NEUTRAL_VENUE)-MatchEngine.expectedGoals(same,same,false,MatchEngine.NEUTRAL_VENUE))<1e-12,"neutral final has home advantage");
        System.out.println("Rules: aggregate, regulation/ET away goals, H2H and neutral venue verified");
    }
    static void codec(File root)throws Exception{
        File dir=new File(root,"v3");dir.mkdirs();ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x50464d53);out.writeInt(3);out.writeInt(909);out.writeInt(2010);out.writeInt(0);out.writeBoolean(true);out.writeInt(0);out.close();byte[] payload=bytes.toByteArray();CRC32 crc=new CRC32();crc.update(payload);
        try(DataOutputStream file=new DataOutputStream(new FileOutputStream(new File(dir,"matches-909-2010.bin")))){file.writeInt(payload.length);file.write(payload);file.writeLong(crc.getValue());}
        MatchJournal j=new MatchJournal(dir,909,2010);check(j.openingDefined(),"v3 read failed");j.define("draw","a,b;");MatchJournal.Match m=match("a","b",2,2);m.id="final";m.round=13;m.duration=120;m.homePenalties=5;m.awayPenalties=4;m.winner="a";j.put(m);j.save();j=new MatchJournal(dir,909,2010);check(j.get("final").duration==120&&j.get("final").homePenalties==5&&j.get("final").winner.equals("a")&&j.metadata("draw").equals("a,b;"),"v4 roundtrip");j.define("draw","c,d;");check(j.metadata("draw").equals("a,b;"),"draw overwritten");
        System.out.println("Codec: v3 upgrade and frozen v4 draw/extra-time/shootout facts verified");
    }
    static void penalties(HashMap<String,NativeWorldCenter.T> teams)throws Exception{
        NativeWorldCenter.T h=teams.get("en:0"),a=teams.get("en:1");int extra=0;
        for(int seed=0;seed<2000;seed++){NativeMatchEngine.Result r=NativeMatchEngine.play(seed,h,a,NativeWorldCenter.selection(h,seed),NativeWorldCenter.selection(a,seed+17),MatchEngine.NEUTRAL_VENUE);int hg=r.homeGoals,ag=r.awayGoals;NativeMatchEngine.extraTime(r,seed,MatchEngine.NEUTRAL_VENUE);check(r.duration==120&&r.homeGoals>=hg&&r.awayGoals>=ag,"ET restarted regulation");int goals=r.homeGoals+r.awayGoals;NativeMatchEngine.penalties(r,seed);check(r.homePenalties!=r.awayPenalties&&r.homeGoals+r.awayGoals==goals,"shootout polluted goals or tied");for(MatchEngine.Lineup side:new MatchEngine.Lineup[]{r.home,r.away}){check(side.substitutions<=3,"fourth extra-time substitution");for(MatchEngine.Member p:side.squad){check(p.minutes<=120,"minutes overflow");if(p.minutes>90)extra++;}}}
        check(extra>0,"ET never continued player minutes");System.out.println("Engine: 2,000 continued extra times and shootouts; three-sub cap and separate goal ledger verified");
    }
    public static void main(String[] args)throws Exception{
        rules();File root=new File(args[1]);root.mkdirs();codec(root);MatchJournal full=null;
        for(String active:new String[]{"en","de"}){
            int nonce=active.equals("en")?910:911;File dir=new File(root,active);dir.mkdirs();Context c=new Context("overrides/app/src/main/assets",dir);AndroidRuntime.current=c;JournalHarness.world(args[0],nonce);NativeCareerStore.current.activeLeague=active;int total=active.equals("de")?34:38;
            if(active.equals("en"))penalties(NativeMatchJournal.teams(NativeCareerStore.current));
            String prefix=null;
            for(int step=1;step<=total;step++){NativeWorldCenter.played=step;NativeMatchJournal.advance(c,step);MatchJournal j=JournalHarness.open(c,nonce,2010);
                if(step==20){check(j.competition(UclKnockout.COMP).size()==96,"premature knockout");check(NativeMatchJournal.uclDraw(c,0).size()==8,"last-16 draw missing");}
                if(step==25){prefix=j.competition(UclKnockout.COMP).toString();ArrayList<String> saved=new ArrayList<>();for(MatchJournal.Match m:j.all())saved.add(JournalHarness.frozen(m));JournalHarness.restart();NativeMatchJournal.advance(c,step);j=JournalHarness.open(c,nonce,2010);for(int i=0;i<saved.size();i++)check(saved.get(i).equals(JournalHarness.frozen(j.all().get(i))),"restart rewrote prefix");}
            }
            full=JournalHarness.open(c,nonce,2010);int[] counts=new int[14];for(MatchJournal.Match m:full.competition(UclKnockout.COMP)){counts[m.round]++;if(m.round>=7){check(m.clock>0&&m.effectsVersion>0,"new knockout missing effects");if(m.round%2==0||m.round==13)check(m.winner.equals(m.home)||m.winner.equals(m.away),"unresolved tie");int hg=0,ag=0;for(MatchJournal.PlayerFact p:m.players){if(p.club.equals(m.home))hg+=p.stats[0];else ag+=p.stats[0];check(p.minutes<=m.duration,"participant exceeds match duration");}check(hg==m.homeGoals&&ag==m.awayGoals,"goal ledger mismatch");}}
            check(full.size()==1951&&counts[7]==8&&counts[8]==8&&counts[9]==4&&counts[10]==4&&counts[11]==2&&counts[12]==2&&counts[13]==1,"knockout stage counts");
            HashMap<String,Integer> groupOf=new HashMap<>();ArrayList<ArrayList<String>> groups=NativeCompetitionCalendar.groups(c);for(int g=0;g<8;g++)for(String id:groups.get(g))groupOf.put(id,g);HashMap<String,String> association=new HashMap<>();for(NativeCareerStore.Club club:NativeCareerStore.current.clubs)association.put(club.id,club.league);
            for(String[] tie:NativeMatchJournal.uclDraw(c,0)){check(!groupOf.get(tie[0]).equals(groupOf.get(tie[1]))&&!association.get(tie[0]).equals(association.get(tie[1])),"illegal last-16 pairing");check(UclKnockout.table(groups.get(groupOf.get(tie[1])),full.competition(UclKnockout.COMP)).get(0).id.equals(tie[1]),"winner not second-leg home");}
            byte[] before=Files.readAllBytes(new File(dir,"match-journals/matches-"+nonce+"-2010.bin").toPath());for(int i=0;i<4;i++)NativeMatchJournal.uclDraw(c,i);NativeMatchJournal.ucl(c);check(Arrays.equals(before,Files.readAllBytes(new File(dir,"match-journals/matches-"+nonce+"-2010.bin").toPath())),"read projection wrote journal");System.out.println("Calendar "+active+": 125 UCL fixtures, seeded draw, stage progression, restart and read-only views verified");
        }
        File dir=new File(root,"migration");dir.mkdirs();Context c=new Context("overrides/app/src/main/assets",dir);AndroidRuntime.current=c;JournalHarness.world(args[0],912);NativeCareerStore.current.activeLeague="de";MatchJournal old=JournalHarness.open(c,912,2010);for(MatchJournal.Match m:full.all())if(!m.competition.equals(UclKnockout.COMP)||m.round<=6)old.put(m);old.save();HashMap<String,String> frozen=new HashMap<>();for(MatchJournal.Match m:old.all())frozen.put(m.id,JournalHarness.frozen(m));NativeWorldCenter.played=34;JournalHarness.restart();NativeMatchJournal.advance(c,34);MatchJournal upgraded=JournalHarness.open(c,912,2010);check(upgraded.size()==1951,"upgrade did not backfill knockout");for(MatchJournal.Match m:upgraded.all())if(frozen.containsKey(m.id))check(frozen.get(m.id).equals(JournalHarness.frozen(m)),"upgrade rewrote old fixture");else check(m.source.equals("reconstructed-v91")&&m.effectsVersion==0&&m.homeCredit==0&&m.awayCredit==0,"retroactive knockout consequences");
        System.out.println("PASS: old-season backfill preserves all 1,922 existing facts and applies no retrospective effects or money");
    }
}
