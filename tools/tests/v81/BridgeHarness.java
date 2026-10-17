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
    static void runLive(ca screen,int from,int to,Vector[] goals)throws Exception{
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
        dw home=new dw(0),away=new dw(1);pfmPlayerHistoryV2.user=home;ca screen=new ca(home,away);
        for(int round=0;round<1000;round++){
            cp.round=round;Vector[] quick=goals();PfmMatchEngineBridge.fast(home,away,quick,0);PfmMatchEngineBridge.fast(home,away,quick,45);
            Vector[] full=goals();runLive(screen,1,90,full);
            check(Arrays.equals(new int[]{quick[0].size(),quick[1].size()},new int[]{full[0].size(),full[1].size()}),"live/quick result differs at "+round);
            check(quick[0].equals(full[0])&&quick[1].equals(full[1]),"live/quick scorer minutes differ");
            // Reuse the same screen; switching at minute 37 must exactly preserve the event list.
            Vector[] skip=goals();runLive(screen,1,37,skip);screen.n=37;
            PfmMatchEngineBridge.fast(home,away,skip,37);PfmMatchEngineBridge.fast(home,away,skip,45);
            check(skip[0].equals(quick[0])&&skip[1].equals(quick[1]),"skip duplicate/missing event at "+round);
        }
        screen.m=100;screen.t=2;check(PfmMatchEngineBridge.clockTick(screen)==100,"celebration consumed clock");
        screen.t=0;check(PfmMatchEngineBridge.clockTick(screen)==101,"clock did not resume");
        System.out.println("Legacy adapter: 1000 quick/live/skip matches have identical goals, scorers and minutes; repeated UI frames and celebrations verified");
    }
}
class ci {byte b,c=75,d=75,e=75,h=55;ci(int p){b=(byte)p;}}
class dw {byte c=55,d=2,e=11,n;short[] a=new short[11];ci[] players=new ci[11];dw(int id){n=(byte)id;for(int i=0;i<11;i++){a[i]=(short)(id*11+i);players[i]=new ci(i==0?0:i<5?1:i<9?2:3);}}}
class cp {static byte[] a={0,0,0,0,0};static int round;static ci a(dw t,int i){return t.players[i];}static int pfmPlayedForTable(){return round;}}
class bb {static int[] b={50},c={50},d={50};}
class pfm2 {static int getSeason(){return 2010;}}
class pfmCondition60 {static byte[] fatigue=new byte[22];}
class pfmPlayerHistoryV2 {static dw user;static dw userTeam(){return user;}}
class pfmAvailable70 {static int minutesForRosterIndex(dw t,int i,int round){return i<11?90:0;}}
class pfmSubstitutions {static int currentEventMinute;static StringBuffer appendLiveMinute(StringBuffer b,int minute){currentEventMinute=minute;return b.append(minute);}static ci pfmScorer(dw t,int minute){PfmMatchEngineBridge.prepareEventAndRound(minute);return t.players[10];}}
class pfmMatchEvents {static ci liveScorer(dw t,int minute){return pfmSubstitutions.pfmScorer(t,minute);}}
class cb {int p,q;}
class aw {cb a=new cb();}
class af extends aw {bm b=new bm();}
class bm {}
class bt {int m=1;bm a(){return new bm();}}
class ca {dw a,b;byte c=1;int n,m,t;af scene=new af();bt homeDisplay=new bt(),awayDisplay=new bt();ca(dw h,dw aw){a=h;b=aw;}}
