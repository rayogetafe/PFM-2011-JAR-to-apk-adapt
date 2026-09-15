package pfm.android;

/** One calendar shared by user and AI transfers. */
final class NativeTransferCalendar {
    private NativeTransferCalendar() {}
    private static int call(String name,int fallback){try{return ((Number)Class.forName("PfmSeasonBridge").getMethod(name).invoke(null)).intValue();}catch(Throwable t){return fallback;}}
    static int played(){return Math.max(0,call("playedMatches",0));}
    static int total(){return Math.max(30,call("totalRounds",38));}
    static int phase(){int p=played(),half=total()/2;if(p<=3)return 0;if(p>=half-2&&p<=half+2)return 1;return -1;}
    static boolean open(){return phase()>=0;}
    static int windowId(int season){int phase=phase();return phase<0?-1:season*10+phase;}
    static String status(){int p=played(),half=total()/2,phase=phase();if(phase==0)return "OPEN — summer window (through round 3)";if(phase==1)return "OPEN — winter window (rounds "+(half-2)+"–"+(half+2)+")";if(p<half-2)return "CLOSED — winter window opens after round "+(half-3);return "CLOSED — next window opens before the new league season";}
}
