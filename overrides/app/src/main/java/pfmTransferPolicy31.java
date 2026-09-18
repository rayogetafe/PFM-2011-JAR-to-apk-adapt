import android.content.Context;
import android.content.SharedPreferences;
import pfm.android.AndroidRuntime;

/** Runtime transfer-frequency policy used by the patched career market tick. */
public final class pfmTransferPolicy31 {
    private static final String PREFS="pfm_career_policy_v31";
    private static final String KEY="transfer_frequency";
    private static final int[] WINDOW_CAP={0,2,5,8,12};
    private pfmTransferPolicy31() {}

    private static SharedPreferences prefs(){return AndroidRuntime.context().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static int get(){try{return Math.max(0,Math.min(4,prefs().getInt(KEY,2)));}catch(Throwable t){return 2;}}
    public static boolean set(int value){try{int v=Math.max(0,Math.min(4,value));return prefs().edit().putInt(KEY,v).commit()&&get()==v;}catch(Throwable t){return false;}}
    public static int chance(){return WINDOW_CAP[get()];}
    public static int windowCap(){return WINDOW_CAP[get()];}

    /** Native career state is authoritative; the stock market tick must not create a second deal. */
    public static void runStockMarketTick(){
        try{
            pfmRosterIntegrity32.repairAll();
            /* Intentionally no bb.a(): native transfers now own rosters, finance and mail. */
            if(pfm2.getMode()==1&&get()>0)pfm.android.NativeMarketTick.run();
        }catch(Throwable ignored){}
        finally{try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}}
    }

    /** Return value is compared with 30 by the original pfmMarketTick code. */
    public static int rollForMarket(){
        try{
            if(pfm2.getMode()!=1)return 99; // Season Mode: always disabled.
            return 99;
        }catch(Throwable t){return 99;}
    }

    /** Compatibility hook retained for the patched call site; native AI owns all deals. */
    public static void runPoach(){
        try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}
    }
}
