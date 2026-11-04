import java.util.*;
import java.lang.reflect.*;

/** Runs the production legacy adapter with deterministic minimal legacy fixtures. */
public class BridgeHarness {
    static Field f(Object o,String name,Class<?> type)throws Exception{
        for(Field f:o.getClass().getDeclaredFields())if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}
        throw new AssertionError(name);
    }
    static Vector[] goals(){return new Vector[]{new Vector(),new Vector()};}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static String details(){StringBuilder b=new StringBuilder();for(pfm.android.MatchEngine.Lineup s:PfmMatchEngineBridge.currentLineups()){for(pfm.android.MatchEngine.Member p:s.squad)b.append(p.uid+":"+p.minutes+":"+p.yellow+":"+p.red+":"+p.injury+";");for(pfm.android.MatchEngine.Incident e:s.incidents)b.append(e.minute+":"+e.type+":"+e.uid+":"+e.other+";");}return b.toString();}
    static void runLive(ca screen,int from,int to,Vector[] goals)throws Exception{
        f(screen,"a",Vector[].class).set(screen,goals);
        for(int minute=from;minute<=to;minute++){
            screen.n=minute-1;screen.c=(byte)(minute<=45?1:3);
            if(PfmMatchEngineBridge.liveGoal(screen)){
                Object ball=((aw)f(screen,"a",af.class).get(screen)).a;
                int side=((cb)ball).p<0?0:1;
                goals[side].addElement(pfmMatchEvents.liveScorer(side==0?screen.a:screen.b,screen.n));
                goals[side].addElement(Integer.toString(screen.n));
            }
            // Hundreds of UI frames at the same football minute must never re-roll it.
            screen.n=minute-1;
            for(int repeat=0;repeat<5;repeat++)check(!PfmMatchEngineBridge.liveGoal(screen),"same minute scored twice");
        }
    }
    public static void main(String[] args)throws Exception{
        Method profile=PfmMatchEngineBridge.class.getDeclaredMethod("profile");profile.setAccessible(true);
        dw home=new dw(0),away=new dw(1);pfmPlayerHistoryV2.user=home;ca screen=new ca(home,away);
        for(int round=0;round<1000;round++){
            PfmSeasonBridge.code=new String[]{"en","es","it","de","fr"}[round/200];
            check(((pfm.android.MatchEngine.Profile)profile.invoke(null)).code.equals(PfmSeasonBridge.code),"legacy adapter lost league profile");
            cp.round=round;Vector[] quick=goals();PfmMatchEngineBridge.fast(home,away,quick,0);PfmMatchEngineBridge.fast(home,away,quick,45);
            String facts=details();
            Vector[] full=goals();runLive(screen,1,90,full);
            check(Arrays.equals(new int[]{quick[0].size(),quick[1].size()},new int[]{full[0].size(),full[1].size()}),"live/quick result differs at "+round);
            check(quick[0].equals(full[0])&&quick[1].equals(full[1]),"live/quick scorer minutes differ");
            check(facts.equals(details()),"live/quick cards, injuries or playing time differ");
            // Reuse the same screen; switching at minute 37 must exactly preserve the event list.
            Vector[] skip=goals();runLive(screen,1,37,skip);screen.n=37;
            PfmMatchEngineBridge.fast(home,away,skip,37);PfmMatchEngineBridge.fast(home,away,skip,45);
            check(skip[0].equals(quick[0])&&skip[1].equals(quick[1]),"skip duplicate/missing event at "+round);
            check(facts.equals(details()),"Skip lost or repeated incident facts");
        }
        screen.m=100;screen.t=2;check(PfmMatchEngineBridge.clockTick(screen)==100,"celebration consumed clock");
        screen.t=0;check(PfmMatchEngineBridge.clockTick(screen)==101,"clock did not resume");
        PfmIncidentsBridge88.condition();PfmIncidentsBridge88.discipline();
        byte[] fatigue=pfmCondition60.fatigue.clone(),injury=pfmCondition60.injury.clone(),yc=pfmDiscipline70.yellow.clone(),rc=pfmDiscipline70.red.clone(),ban=pfmDiscipline70.suspension.clone();
        PfmIncidentsBridge88.condition();PfmIncidentsBridge88.discipline();
        check(Arrays.equals(fatigue,pfmCondition60.fatigue)&&Arrays.equals(injury,pfmCondition60.injury)&&Arrays.equals(yc,pfmDiscipline70.yellow)&&Arrays.equals(rc,pfmDiscipline70.red)&&Arrays.equals(ban,pfmDiscipline70.suspension),"repeated domestic ledger commit");
        for(pfm.android.MatchEngine.Lineup s:PfmMatchEngineBridge.currentLineups())for(pfm.android.MatchEngine.Member p:s.squad){int id=Integer.parseInt(p.uid.substring(7));check((yc[id]&255)==p.yellow&&(rc[id]&255)==p.red&&(injury[id]&255)==p.injury,"domestic ledger differs from played incidents");}
        Arrays.fill(pfmDiscipline70.suspension,(byte)0);cp.round=1001;home=new dw(0);away=new dw(1);pfmPlayerHistoryV2.user=home;screen=new ca(home,away);Vector[] controlled=goals();runLive(screen,1,1,controlled);
        PfmMatchEngineBridge.currentLineups()[0].scripted(20,"legacy:1","RC");runLive(screen,2,25,controlled);
        short off=home.a[1];home.a[1]=home.a[13];home.a[13]=off;runLive(screen,26,26,controlled);
        check(home.a[1]==off&&PfmMatchEngineBridge.currentLineups()[0].find("legacy:1").minutes==20,"legacy manual UI replaced a dismissed slot");
        PfmMatchEngineBridge.currentLineups()[0].substitutions=3;short original=home.a[3];home.a[3]=home.a[17];home.a[17]=original;runLive(screen,27,27,controlled);check(home.a[3]==original,"legacy manual UI allowed fourth substitution");
        PfmMatchEngineBridge.fast(home,away,controlled,27);PfmMatchEngineBridge.fast(home,away,controlled,45);check(PfmMatchEngineBridge.currentLineups()[0].find("legacy:1").minutes==20,"Skip revived a red card after rejected manual replacement");
        PfmMatchEngineBridge.currentLineups()[0].find("legacy:1").yellow=1;pfmDiscipline70.yellow[1]=4;pfmDiscipline70.suspension[1]=0;PfmIncidentsBridge88.discipline();check((pfmDiscipline70.suspension[1]&255)==3,"fifth yellow plus straight red differs from shared native ban rule");
        System.out.println("Manual legacy UI: red-card replacement and fourth substitution rejected; Skip preserves dismissal");
        cp.round=1003;Arrays.fill(pfmDiscipline70.suspension,(byte)0);Vector[] reference=goals();PfmMatchEngineBridge.fast(home,away,reference,0);PfmMatchEngineBridge.fast(home,away,reference,45);String referenceFacts=details();
        screen=new ca(home,away);Vector[] delayed=goals();f(screen,"a",Vector[].class).set(screen,delayed);f(f(screen,"a",af.class).get(screen),"b",bm.class).set(f(screen,"a",af.class).get(screen),null);bt.unavailable=true;
        Field pending=PfmMatchEngineBridge.class.getDeclaredField("pendingMask");pending.setAccessible(true);int stalled=0;
        for(int m=1;m<=90;m++){screen.n=m-1;screen.c=(byte)(m<=45?1:3);check(!PfmMatchEngineBridge.liveGoal(screen),"missing actor unexpectedly rendered a goal");if(pending.getInt(null)!=0){stalled=m;break;}}
        check(stalled>0,"delayed-goal fixture has no goal");bt.unavailable=false;PfmMatchEngineBridge.fast(home,away,delayed,stalled-1);if(stalled<=45)PfmMatchEngineBridge.fast(home,away,delayed,45);
        check(delayed[0].equals(reference[0])&&delayed[1].equals(reference[1])&&referenceFacts.equals(details()),"Skip lost pending goal/incident minute when renderer had no actor");
        System.out.println("Pending render: Skip preserves a queued goal and completes its incident minute exactly once");
        System.out.println("Legacy adapter: 1000 quick/live/skip matches have identical goals, scorers and minutes; repeated UI frames and celebrations verified");
    }
}
class ci {byte b,c=75,d=75,e=75,h=55;ci(int p){b=(byte)p;}}
class dw {byte c=55,d=2,e,n;short[] a;ci[] players;dw(int id){this(id,18);}dw(int id,int count){n=(byte)id;e=(byte)count;a=new short[count];players=new ci[count];for(int i=0;i<count;i++){a[i]=(short)(id*25+i);players[i]=new ci(i==0||i==11?0:i<5||i==12||i==13?1:i<9||i<16?2:3);cp.pool[id*25+i]=players[i];}}}
class cp {static byte[] a={0,0,0,0,0};static ci[] pool=new ci[50];static int round;static ci a(dw t,int i){return pool[t.a[i]&65535];}static int pfmPlayedForTable(){return round;}}
class bb {static int[] b={50},c={50},d={50};}
class pfm2 {static int getSeason(){return 2010;}static int getSlot(){return 0;}}
class PfmSeasonBridge {static String code="en";public static String leagueCode(){return code;}}
class pfmCondition60 {static byte[] fatigue=new byte[50],injury=new byte[50];static int lastRound=-1;private static void ensure(){}private static void save(){}static int minutesForRosterIndex(dw t,int i,int r){return pfmAvailable70.minutesForRosterIndex(t,i,r);}}
class pfmDiscipline70 {static byte[] yellow=new byte[50],red=new byte[50],suspension=new byte[50];static int lastRound=-1;private static void ensure(){}private static void save(){}static boolean suspended(int id){return suspension[id]>0;}static int roundsOut(int id){return suspension[id];}}
class pfmRoundAccess60 {static int round(){return cp.round;}}
class pfmMatchPost35 {static int completedRoundOne(){return cp.round+1;}}
class pfmPlayerHistoryV2 {static dw user;static dw userTeam(){return user;}}
class pfmAvailable70 {static boolean outside;static int minutesForRosterIndex(dw t,int i,int round){return i<11?90:i==12?26:i==14?19:i==(outside?20:16)?12:0;}}
class pfmSubstitutions {static int currentEventMinute;static StringBuffer appendLiveMinute(StringBuffer b,int minute){currentEventMinute=minute;return b.append(minute);}static ci pfmScorer(dw t,int minute){PfmMatchEngineBridge.prepareEventAndRound(minute);return PfmMatchEngineBridge.scorer(t,minute);}}
class pfmMatchEvents {static ci liveScorer(dw t,int minute){return pfmSubstitutions.pfmScorer(t,minute);}}
class cb {int p,q;}
class aw {cb a=new cb();}
class af extends aw {bm b=new bm();}
class bm {}
class bt {static boolean unavailable;int m=1;bm a(){return unavailable?null:new bm();}}
class ca {dw a,b;byte c=1;int n,m,t;Vector[] records;af scene=new af();bt homeDisplay=new bt(),awayDisplay=new bt();ca(dw h,dw aw){a=h;b=aw;}}
