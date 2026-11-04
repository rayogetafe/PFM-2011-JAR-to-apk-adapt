import java.lang.reflect.*;
import java.util.*;
import pfm.android.*;
import android.content.*;

public class ArchiveHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        dw h=new dw(0,25),a=new dw(1,25);pfmPlayerHistoryV2.user=h;cp.round=0;pfmAvailable70.outside=true;
        Method prepare=PfmMatchEngineBridge.class.getDeclaredMethod("game",dw.class,dw.class,Boolean.TYPE);prepare.setAccessible(true);prepare.invoke(null,h,a,true);
        MatchEngine.Lineup[] sides=PfmMatchEngineBridge.currentLineups();sides[0].scripted(20,"legacy:1","RC");sides[0].scripted(30,"legacy:0","INJ");sides[0].scripted(70,"legacy:12","INJ");
        Vector[] goals=BridgeHarness.goals();PfmMatchEngineBridge.fast(h,a,goals,0);PfmMatchEngineBridge.fast(h,a,goals,45);
        Method participants=pfmLeagueMatchArchive34.class.getDeclaredMethod("participants",dw.class,Integer.TYPE,Integer.TYPE);participants.setAccessible(true);
        int reportSubs=0;for(String[] row:(ArrayList<String[]>)participants.invoke(null,h,0,1))if(row[6].equals("0"))reportSubs++;
        check(sides[0].substitutions==3&&reportSubs==3,"report invents a fourth substitute: engine="+sides[0].substitutions+", report="+reportSubs);
        Method append=pfmLeagueMatchArchive34.class.getDeclaredMethod("appendSubs",StringBuilder.class,dw.class,Integer.TYPE,Integer.TYPE);append.setAccessible(true);StringBuilder subs=new StringBuilder();append.invoke(null,subs,h,0,1);
        ArrayList<MatchEngine.Incident> executed=new ArrayList<>();for(MatchEngine.Incident event:sides[0].incidents)if(event.type.equals("SUB"))executed.add(event);
        String[] archived=subs.toString().trim().split("\n");check(archived.length==executed.size(),"archive misses or invents substitution events");
        boolean chain=false;for(int i=0;i<archived.length;i++){String[] row=archived[i].split("\t",-1);MatchEngine.Incident event=executed.get(i);check(Integer.parseInt(row[2])==event.minute&&row[5].equals(event.uid.substring(7))&&row[6].equals(event.other.substring(7)),"archive changed substitution owner/minute");if(row[5].equals("12")&&row[6].equals("13"))chain=true;}check(chain,"substitute-for-substitute replacement absent");
        StringBuffer eventLog=new StringBuffer();PfmMatchEngineBridge.appendSubstitutions(eventLog,h,0);check(eventLog.toString().lines().filter(line->line.contains(" -> ")).count()==3,"text event log diverges from executed substitutions");
        StringBuffer missing=new StringBuffer();PfmMatchEngineBridge.appendSubstitutions(missing,h,5);check(missing.toString().equals("NONE\n"),"absent facts invented planned substitutions");
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
        System.out.println("PASS: 25-player roster + keeper injury + substitute injury: engine/used players/archive/text log all show three actual substitutions; unused planned player has no appearance");
    }
}
class pfmRatingPost36 {static int adjusted;public static void adjust(int id,int amount){adjusted+=amount;}}
