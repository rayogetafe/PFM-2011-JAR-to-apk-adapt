import java.lang.reflect.*;
import java.util.*;
import pfm.android.*;
import android.content.*;

public class ArchiveHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        dw h=new dw(0),a=new dw(1);pfmPlayerHistoryV2.user=h;cp.round=0;
        Method prepare=PfmMatchEngineBridge.class.getDeclaredMethod("game",dw.class,dw.class,Boolean.TYPE);prepare.setAccessible(true);prepare.invoke(null,h,a,true);
        MatchEngine.Lineup[] sides=PfmMatchEngineBridge.currentLineups();sides[0].scripted(20,"legacy:1","RC");sides[0].scripted(30,"legacy:0","INJ");
        Vector[] goals=BridgeHarness.goals();PfmMatchEngineBridge.fast(h,a,goals,0);PfmMatchEngineBridge.fast(h,a,goals,45);
        Field context=AndroidRuntime.class.getDeclaredField("current");context.setAccessible(true);Context c=new Context("",new java.io.File(args[0]));context.set(null,c);
        Method key=pfmLeagueMatchArchive34.class.getDeclaredMethod("key",Integer.TYPE,dw.class,dw.class);key.setAccessible(true);String k=(String)key.invoke(null,1,h,a);
        StringBuilder text=new StringBuilder("H\tHome\tAway\t0\t0\nM\t50\t50\t10\t10\t3\t3\t4\t4\t12\t12\t1\t1\t0\t0\t0\t0\n");
        for(int side=0;side<2;side++)for(MatchEngine.Member p:sides[side].squad)if(p.minutes>0){int id=Integer.parseInt(p.uid.substring(7));text.append("P\t"+side+"\t"+id+"\tPlayer "+id+"\t"+(p.position==0?"GK":"DEF")+"\t"+(p.starter?1:0)+"\t90\t65\t\t\t"+id+"\t\t\n");}
        SharedPreferences prefs=c.getSharedPreferences("pfm_match_archive_v34",0);prefs.edit().putString(k,text.toString()).commit();byte[] empty=new byte[36];
        pfmLeagueMatchArchive34.applyCards(empty,empty,empty,empty,empty,empty,1);String first=prefs.getString(k,"");int adjusted=pfmRatingPost36.adjusted;
        pfmLeagueMatchArchive34.applyCards(empty,empty,empty,empty,empty,empty,1);String repeated=prefs.getString(k,"");if(!first.equals(repeated)||adjusted!=pfmRatingPost36.adjusted){System.out.println("FIRST "+first);System.out.println("REPEAT "+repeated);System.out.println("RATING "+adjusted+" -> "+pfmRatingPost36.adjusted);}check(first.equals(repeated)&&adjusted==pfmRatingPost36.adjusted,"repeated archive changed cards/ratings/stats");
        boolean found=false;for(String line:first.split("\n")){String[] row=line.split("\t",-1);if(row[0].equals("P")&&row[10].equals("1")){check(row[6].equals("20")&&row[12].equals("20"),"red owner/minute rewritten");found=true;}}check(found,"red participant absent");
        MatchJournal.Match m=new MatchJournal.Match();m.competition="league:en";m.home="en:0";m.away="en:1";m.round=1;m.report=first;MatchJournal.legacyDetails(m);PfmIncidentsBridge88.enrich(m,h,a);
        boolean injured=false;for(MatchJournal.PlayerFact p:m.players)if(p.uid.endsWith(":0")){check(p.minutes==30&&p.injury>0,"injury/minutes lost in journal import");injured=true;}
        check(injured&&m.effectsVersion==0&&m.report.contains("LIVE EVENTS"),"domestic journal duplicated overlay effects or lost timeline");
        System.out.println("PASS: production archive retains red owner/minute, repeated archive is idempotent, domestic injury/minutes enter journal without duplicate overlay effects");
    }
}
class pfmRatingPost36 {static int adjusted;public static void adjust(int id,int amount){adjusted+=amount;}}
