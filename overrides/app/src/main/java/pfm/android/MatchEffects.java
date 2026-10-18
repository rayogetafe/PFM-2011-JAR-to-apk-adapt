package pfm.android;

import java.util.*;

/** Replayable consequences. The journal is the commit log; no second writable condition store. */
final class MatchEffects {
    static final class Health {int fatigue,injuryUntil;}
    static final class Discipline {int yellow,red,ban;}
    final HashMap<String,Health> health=new HashMap<String,Health>();
    final HashMap<String,Discipline> discipline=new HashMap<String,Discipline>();
    private final HashSet<String> applied=new HashSet<String>();
    int clock;
    Health health(String uid){Health h=health.get(uid);if(h==null){h=new Health();health.put(uid,h);}return h;}
    Discipline discipline(String competition,String uid){String key=competition+"|"+uid;Discipline d=discipline.get(key);if(d==null){d=new Discipline();discipline.put(key,d);}return d;}
    Discipline readDiscipline(String competition,String uid){Discipline d=discipline.get(competition+"|"+uid);return d==null?new Discipline():d;}
    void rest(int step){if(step<=clock)return;int elapsed=step-clock;for(Health h:health.values())h.fatigue=Math.max(0,h.fatigue-7*elapsed);clock=step;}
    int injury(String uid){Health h=health.get(uid);return h==null?0:Math.max(0,h.injuryUntil-clock);}
    int fatigue(String uid){Health h=health.get(uid);return h==null?0:h.fatigue;}
    boolean available(String uid,String competition){Health h=health.get(uid);return (h==null||h.injuryUntil==0||h.injuryUntil<clock)&&discipline(competition,uid).ban==0;}
    void apply(MatchJournal.Match m){
        if(!applied.add(m.id))return;
        for(MatchJournal.PlayerFact p:m.players){
            if(m.effectsVersion==0)continue; // No retroactive injuries, load, cards or money for v83/migration records.
            Health h=health(p.uid);h.fatigue=Math.min(100,Math.max(0,h.fatigue+p.fatigueDelta));if(p.injury>0)h.injuryUntil=Math.max(h.injuryUntil,m.clock+p.injury);
            Discipline d=discipline(m.competition,p.uid);if(d.ban>0&&p.stats[2]==0)d.ban--;d.yellow+=p.yellow;d.red+=p.red;
            if(p.red>0)d.ban+=p.yellow==2?1:2;
            // Three yellows in Europe, five domestically; a two-yellow dismissal has its own ban.
            int threshold=m.competition.startsWith("ucl:")?3:5;
            if(p.yellow<2&&p.yellow>0&&d.yellow/threshold>(d.yellow-p.yellow)/threshold)d.ban++;
        }
    }
    static int roll(int seed,String uid,int salt,int bound){return (MatchEngine.mix(seed^uid.hashCode()^salt)&0x7fffffff)%bound;}
    /** Post-match events, consistent with the legacy post-match discipline model (not live send-offs). */
    static void stamp(MatchJournal.Match m,Map<String,NativeWorldCenter.P> players){
        m.effectsVersion=1;
        for(MatchJournal.PlayerFact f:m.players){NativeWorldCenter.P p=players.get(f.uid);if(p==null||f.stats[2]==0)continue;
            f.fatigueDelta=Math.max(2,(f.minutes*(18-p.res/12)+89)/90);
            int yellow=p.pos==0?3:p.pos==1?21:p.pos==2?16:10;
            if(roll(m.seed,f.uid,271,100)<yellow)f.yellow=1;
            if(f.yellow>0&&roll(m.seed,f.uid,277,100)<4){f.yellow=2;f.red=1;}
            else if(roll(m.seed,f.uid,281,1000)<3)f.red=1;
            int risk=6+Math.max(0,p.fat-35)/3;
            if(roll(m.seed,f.uid,283,10000)<risk*f.minutes/9)f.injury=1+roll(m.seed,f.uid,293,4);
        }
    }
    /** Gameplay economy, not claimed historical prize money. League running costs use frozen payroll. */
    static int credit(boolean ucl,boolean home,int goals,int against,int level,long annualPayroll,int rounds){
        if(ucl)return (home?150000:0)+(goals>against?300000:goals==against?100000:0);
        long revenue=home?Math.max(100000,Math.min(600000,(level-40)*12000)):0;
        return (int)(revenue-annualPayroll/Math.max(30,rounds));
    }
}
