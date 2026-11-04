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
    static final class Result {int homeGoals,awayGoals;double expectedHome,expectedAway;MatchEngine.Lineup home,away;}
    static MatchEngine.Lineup lineup(int seed,NativeWorldCenter.T club,NativeWorldCenter.S selection){
        ArrayList<MatchEngine.Member> xi=new ArrayList<MatchEngine.Member>(),bench=new ArrayList<MatchEngine.Member>();
        ArrayList<NativeWorldCenter.P> pool=new ArrayList<NativeWorldCenter.P>(selection.xi);for(NativeWorldCenter.P p:selection.bench)if(!pool.contains(p))pool.add(p);for(NativeWorldCenter.P p:selection.used)if(!pool.contains(p))pool.add(p);
        for(NativeWorldCenter.P p:pool)(selection.xi.contains(p)?xi:bench).add(new MatchEngine.Member(p.uid==null||p.uid.length()==0?"id:"+p.id:p.uid,p.pos,p.qua,p.spe,p.res,p.mor,p.fat));
        MatchEngine.Lineup side=MatchEngine.lineup(seed,xi,bench);side.style=club.style;side.formation=club.form;
        return side;
    }
    static Result play(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        Result result=new Result();result.home=lineup(seed,home,hs);result.away=lineup(seed^0x5bd1e995,away,as);
        int homeGoals=0,awayGoals=0;
        for(int minute=1;minute<=90;minute++){
            result.home.begin(minute);result.away.begin(minute);
            MatchEngine.Team h=result.home.team(),a=result.away.team();
            result.expectedHome+=MatchEngine.expectedGoals(h,a,true,profile)/90.0;result.expectedAway+=MatchEngine.expectedGoals(a,h,false,profile)/90.0;
            int events=MatchEngine.events(seed,minute,h,a,profile,homeGoals,awayGoals);
            if((events&1)!=0){homeGoals++;result.home.goal(minute);result.away.concede();}
            if((events&2)!=0){awayGoals++;result.away.goal(minute);result.home.concede();}
            result.home.end(minute);result.away.end(minute);
        }
        result.homeGoals=homeGoals;result.awayGoals=awayGoals;return result;
    }
    /** Nominal goal intensity integrated over the same lineup/incident timeline, before score-correlation weighting. */
    static double[] expected(NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        return expected(home,away,hs,as,profile,0);
    }
    static double[] expected(NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile,int seed){Result r=play(seed,home,away,hs,as,profile);return new double[]{r.expectedHome,r.expectedAway};}
}
