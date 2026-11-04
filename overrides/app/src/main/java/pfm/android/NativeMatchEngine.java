package pfm.android;

import java.util.ArrayList;

/** Native roster adapter to the competition-independent minute engine. */
final class NativeMatchEngine {
    private NativeMatchEngine() {}
    private static MatchEngine.Team snapshot(NativeWorldCenter.T club,NativeWorldCenter.S selection,int minute){
        ArrayList<NativeWorldCenter.P> xi=new ArrayList<NativeWorldCenter.P>(selection.xi);
        int sub=0;
        for(NativeWorldCenter.P reserve:selection.used)if(!selection.xi.contains(reserve)&&reserve.pos!=0){
            if(minute>=65+sub*7){for(int i=xi.size()-1;i>=0;i--)if(xi.get(i).pos==reserve.pos){xi.set(i,reserve);break;}}
            sub++;
        }
        MatchEngine.Team team=new MatchEngine.Team();team.style=club.style;team.formation=club.form;
        for(NativeWorldCenter.P p:xi)team.add(p.pos,p.qua,p.spe,p.res,p.mor,p.fat);
        return team;
    }

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
    static final class Result {int homeGoals,awayGoals;MatchEngine.Lineup home,away;}
    static MatchEngine.Lineup lineup(int seed,NativeWorldCenter.T club,NativeWorldCenter.S selection){
        MatchEngine.Lineup side=new MatchEngine.Lineup(seed);side.style=club.style;side.formation=club.form;
        ArrayList<NativeWorldCenter.P> squad=new ArrayList<NativeWorldCenter.P>(selection.xi);
        for(NativeWorldCenter.P p:selection.used)if(!squad.contains(p))squad.add(p);
        // Include a reserve keeper and up to seven substitutes, without giving them appearances in advance.
        for(NativeWorldCenter.P p:selection.bench)if(squad.size()<18&&!squad.contains(p))squad.add(p);
        for(NativeWorldCenter.P p:squad)side.add(new MatchEngine.Member(p.uid==null||p.uid.length()==0?"id:"+p.id:p.uid,p.pos,p.qua,p.spe,p.res,p.mor,p.fat),selection.xi.contains(p));
        int sub=0;for(NativeWorldCenter.P p:selection.used)if(!selection.xi.contains(p)&&p.pos!=0)side.plan(65+sub++*7,p.uid==null||p.uid.length()==0?"id:"+p.id:p.uid);
        return side;
    }
    static Result play(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        Result result=new Result();result.home=lineup(seed,home,hs);result.away=lineup(seed^0x5bd1e995,away,as);
        int homeGoals=0,awayGoals=0;
        for(int minute=1;minute<=90;minute++){
            result.home.begin(minute);result.away.begin(minute);
            MatchEngine.Team h=result.home.team(),a=result.away.team();
            int events=MatchEngine.events(seed,minute,h,a,profile,homeGoals,awayGoals);
            if((events&1)!=0){homeGoals++;result.home.goal(minute);result.away.concede();}
            if((events&2)!=0){awayGoals++;result.away.goal(minute);result.home.concede();}
            result.home.end(minute);result.away.end(minute);
        }
        result.homeGoals=homeGoals;result.awayGoals=awayGoals;return result;
    }
    /** Baseline intensity reference. Joint corrections preserve fixed-lineup marginals; substitutions are validated empirically. */
    static double[] expected(NativeWorldCenter.T home,NativeWorldCenter.T away,NativeWorldCenter.S hs,NativeWorldCenter.S as,MatchEngine.Profile profile){
        double[] out=new double[2];int[] at={1,65,72,79},duration={64,7,7,12};for(int i=0;i<at.length;i++){MatchEngine.Team h=snapshot(home,hs,at[i]),a=snapshot(away,as,at[i]);out[0]+=MatchEngine.expectedGoals(h,a,true,profile)*duration[i]/90.0;out[1]+=MatchEngine.expectedGoals(a,h,false,profile)*duration[i]/90.0;}return out;
    }
}
