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
        int homeGoals=0,awayGoals=0;
        MatchEngine.Team h=snapshot(home,homeSelection,1),a=snapshot(away,awaySelection,1);
        for(int minute=1;minute<=90;minute++){
            if(minute==65||minute==72||minute==79){h=snapshot(home,homeSelection,minute);a=snapshot(away,awaySelection,minute);}
            if(MatchEngine.goal(seed,minute,0,h,a))homeGoals++;
            if(MatchEngine.goal(seed,minute,1,h,a))awayGoals++;
        }
        return new int[]{homeGoals,awayGoals};
    }
}
