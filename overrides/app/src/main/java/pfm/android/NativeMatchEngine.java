package pfm.android;

import java.util.ArrayList;

/** Native roster adapter to the competition-independent minute engine. */
final class NativeMatchEngine {
    private NativeMatchEngine() {}
    static int[] score(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,
                       NativeWorldCenter.S homeSelection,NativeWorldCenter.S awaySelection){
        return score(seed,home,away,homeSelection,awaySelection,MatchEngine.DEFAULT);
    }
    static int[] score(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S homeSelection,NativeWorldCenter.S awaySelection,String competition){
        return score(seed,home,away,homeSelection,awaySelection,MatchEngine.profile(competition));
    }
    static int[] score(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S homeSelection,NativeWorldCenter.S awaySelection,MatchEngine.Profile profile){
        Result r=play(seed,home,away,homeSelection,awaySelection,profile);return new int[]{r.homeGoals,r.awayGoals};
    }
    static final class Result {int homeGoals,awayGoals,duration=90,homePenalties,awayPenalties;double expectedHome,expectedAway;MatchEngine.Lineup home,away;}
    static MatchEngine.Lineup lineup(int seed,NativeWorldCenter.T club,NativeWorldCenter.S selection){
        ArrayList<MatchEngine.Member> xi=new ArrayList<MatchEngine.Member>(),bench=new ArrayList<MatchEngine.Member>();
        ArrayList<NativeWorldCenter.P> pool=new ArrayList<NativeWorldCenter.P>(selection.xi);for(NativeWorldCenter.P p:selection.bench)if(!pool.contains(p))pool.add(p);for(NativeWorldCenter.P p:selection.used)if(!pool.contains(p))pool.add(p);
        for(NativeWorldCenter.P p:pool)(selection.xi.contains(p)?xi:bench).add(new MatchEngine.Member(p.uid==null||p.uid.length()==0?"id:"+p.id:p.uid,p.pos,p.qua,p.spe,p.res,p.mor,p.fat));
        MatchEngine.Lineup side=MatchEngine.lineup(seed,xi,bench);side.style=club.style;side.formation=club.form;
        return side;
    }
    static Result play(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        Result result=new Result();result.home=lineup(seed,home,hs);result.away=lineup(seed^0x5bd1e995,away,as);
        period(result,seed,profile,1,90);return result;
    }
    static void period(Result result,int seed,MatchEngine.Profile profile,int first,int last){
        int homeGoals=result.homeGoals,awayGoals=result.awayGoals;
        for(int minute=first;minute<=last;minute++){
            result.home.begin(minute);result.away.begin(minute);
            MatchEngine.Team h=result.home.team(),a=result.away.team();
            result.expectedHome+=MatchEngine.expectedGoals(h,a,true,profile)/90.0;result.expectedAway+=MatchEngine.expectedGoals(a,h,false,profile)/90.0;
            int events=MatchEngine.events(seed,minute,h,a,profile,homeGoals,awayGoals);
            if((events&1)!=0){homeGoals++;result.home.goal(minute);result.away.concede();}
            if((events&2)!=0){awayGoals++;result.away.goal(minute);result.home.concede();}
            result.home.end(minute);result.away.end(minute);
        }
        result.homeGoals=homeGoals;result.awayGoals=awayGoals;result.duration=last;
    }
    static void extraTime(Result result,int seed,MatchEngine.Profile profile){period(result,seed,profile,91,120);}
    /** Separate penalty kicks never enter the match goal/appearance ledger. */
    static void penalties(Result result,int seed){
        ArrayList<MatchEngine.Member> h=kickers(result.home),a=kickers(result.away);
        int n=Math.min(h.size(),a.size());if(n==0)throw new IllegalStateException("No eligible penalty taker");
        while(h.size()>n)h.remove(h.size()-1);while(a.size()>n)a.remove(a.size()-1);
        double hk=keeper(result.home),ak=keeper(result.away);int hg=0,ag=0;
        for(int kick=0;;kick++){
            if(scored(h.get(kick%n),ak,seed,kick,0))hg++;
            if(kick<5&&(hg>ag+5-kick||ag>hg+4-kick)){result.homePenalties=hg;result.awayPenalties=ag;return;}
            if(scored(a.get(kick%n),hk,seed,kick,1))ag++;
            if(kick<5&&Math.abs(hg-ag)>4-kick||kick>=4&&hg!=ag){result.homePenalties=hg;result.awayPenalties=ag;return;}
        }
    }
    private static ArrayList<MatchEngine.Member> kickers(MatchEngine.Lineup side){ArrayList<MatchEngine.Member> out=new ArrayList<MatchEngine.Member>();for(MatchEngine.Member p:side.squad)if(side.active(p.uid))out.add(p);java.util.Collections.sort(out,(a,b)->{int d=b.quality-a.quality;return d!=0?d:a.uid.compareTo(b.uid);});return out;}
    private static double keeper(MatchEngine.Lineup side){for(MatchEngine.Member p:side.squad)if(p.position==0&&side.active(p.uid))return p.quality;return 25;}
    private static boolean scored(MatchEngine.Member p,double keeper,int seed,int kick,int side){double probability=Math.max(.55,Math.min(.9,.76+(p.quality-keeper)*.002));return (MatchEngine.mix(seed^kick*0x9e3779b9^side*0x632be5ab^p.uid.hashCode())&0xffffffffL)/4294967296.0<probability;}
    /** Nominal goal intensity integrated over the same lineup/incident timeline, before score-correlation weighting. */
    static double[] expected(NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        return expected(home,away,hs,as,profile,0);
    }
    static double[] expected(NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile,int seed){Result r=play(seed,home,away,hs,as,profile);return new double[]{r.expectedHome,r.expectedAway};}
}
