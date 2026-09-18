package pfm.android;

import android.content.Context;

/** One calendar shared by user and AI transfers. */
final class NativeTransferCalendar {
    private NativeTransferCalendar() {}
    private static int call(String name,int fallback){try{return ((Number)Class.forName("PfmSeasonBridge").getMethod(name).invoke(null)).intValue();}catch(Throwable t){return fallback;}}
    static int played(){return Math.max(0,call("playedMatches",0));}
    static int total(){return Math.max(30,call("totalRounds",38));}
    static boolean offseason(){return played()>=total();}
    static int phase(){int p=played(),half=total()/2;if(p<=3||offseason())return 0;if(p>=half-2&&p<=half+2)return 1;return -1;}
    static boolean open(){return phase()>=0;}
    static int effectiveSeason(int season){return offseason()?season+1:season;}
    static int windowId(int season){int phase=phase();return phase<0?-1:effectiveSeason(season)*10+phase;}
    static int tick(Context c,boolean advance){int p=played(),phase=phase();if(phase<0)return -1;if(!offseason())return phase==0?20+p:40+(p-(total()/2-2));String key="offseason_"+NativeWorldCenter.identity()+"_"+effectiveSeason(NativeWorldCenter.season());android.content.SharedPreferences prefs=c.getSharedPreferences("pfm_ai_market_clock",0);int n=prefs.getInt(key,0);if(advance){n=Math.min(19,n+1);prefs.edit().putInt(key,n).commit();}return Math.max(1,n);}
    static int target(int cap,int tick){if(cap<=0)return 0;if(tick<20)return Math.min(cap,(cap*Math.min(5,tick)+9)/10);if(tick<=23){int[] pct={60,75,90,100};return (cap*pct[tick-20]+99)/100;}if(tick>=40&&tick<=44)return (cap*(tick-39)+4)/5;return cap;}
    static String status(){int p=played(),half=total()/2,phase=phase();if(offseason())return "OPEN — summer market during the season transition";if(phase==0)return "OPEN — summer window (through round 3)";if(phase==1)return "OPEN — winter window (rounds "+(half-2)+"–"+(half+2)+")";if(p<half-2)return "CLOSED — winter window opens after round "+(half-3);return "CLOSED — next window opens before the new league season";}
}
