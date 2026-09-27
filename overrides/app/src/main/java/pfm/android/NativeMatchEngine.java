package pfm.android;

import java.util.ArrayList;

/** Shared data-only port of the legacy bb fast-match probability kernel. */
final class NativeMatchEngine {
    private NativeMatchEngine() {}
    private static final int[][] FORM={{55,45},{65,35},{50,50},{55,45},{70,30},{60,40},{50,50},{65,35},{60,40}};
    private static final int BASE=240000; // pfmPatch35.fastDen0/fastDen1

    static int strength(ArrayList<NativeWorldCenter.P> xi){
        int total=-3414;
        for(NativeWorldCenter.P p:xi)total+=(p.qua*125+p.spe*85+p.res*75+p.mor*30+2600)/67;
        return Math.max(1,total);
    }

    static int attackPct(NativeWorldCenter.T team){
        int form=Math.max(0,Math.min(FORM.length-1,team.form));
        int attack=FORM[form][0]*1000+(100-team.style)*1000+140*333;
        int defence=FORM[form][1]*1000+team.style*1000+160*333;
        return attack*100/Math.max(1,attack+defence);
    }

    /** Fourteen paired attacking checks, as in the main game's quick-match path. */
    static int[] score(int seed,NativeWorldCenter.T home,NativeWorldCenter.T away,
                       NativeWorldCenter.S homeSelection,NativeWorldCenter.S awaySelection){
        int homeStrength=strength(homeSelection.xi)+50,awayStrength=strength(awaySelection.xi);
        int homeAttack=attackPct(home),awayAttack=attackPct(away);
        int homeOffence=homeStrength*homeAttack,homeDefence=homeStrength*(100-homeAttack);
        int awayOffence=awayStrength*awayAttack,awayDefence=awayStrength*(100-awayAttack);
        int homeGoals=0,awayGoals=0;
        for(int segment=0;segment<14;segment++){
            int noise=(NativeWorldCenter.hash(seed+segment*1009)&0x7fffffff)%20000;
            int denominator=homeOffence+awayDefence+noise+BASE;
            if((NativeWorldCenter.hash(seed+segment*1543+71)&0x7fffffff)%Math.max(1,denominator)<homeOffence)homeGoals++;
            noise=(NativeWorldCenter.hash(seed+segment*2017+991)&0x7fffffff)%20000;
            denominator=awayOffence+homeDefence+noise+BASE;
            if((NativeWorldCenter.hash(seed+segment*2617+313)&0x7fffffff)%Math.max(1,denominator)<awayOffence)awayGoals++;
        }
        return new int[]{homeGoals,awayGoals};
    }
}
