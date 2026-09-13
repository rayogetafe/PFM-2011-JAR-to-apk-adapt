import android.content.Context;
import android.content.SharedPreferences;
import pfm.android.AndroidRuntime;

/** Runtime transfer-frequency policy used by the patched career market tick. */
public final class pfmTransferPolicy31 {
    private static final String PREFS="pfm_career_policy_v31";
    private static final String KEY="transfer_frequency";
    private static final int[] CHANCE={0,12,30,50,70};
    private pfmTransferPolicy31() {}

    private static SharedPreferences prefs(){return AndroidRuntime.context().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static int get(){try{return Math.max(0,Math.min(4,prefs().getInt(KEY,2)));}catch(Throwable t){return 2;}}
    public static boolean set(int value){try{int v=Math.max(0,Math.min(4,value));return prefs().edit().putInt(KEY,v).commit()&&get()==v;}catch(Throwable t){return false;}}
    public static int chance(){return CHANCE[get()];}

    /** Gate the original transfer-list engine too; this ran before the v31 check. */
    public static void runStockMarketTick(){
        try{
            pfmRosterIntegrity32.repairAll();
            if(pfm2.getMode()!=1||get()==0)return;
            if(get()!=1||du.a(100)<35)bb.a();
            /* A matchday restores pfmLineup83's pre-match roster snapshot.  The
               winter market runs while that snapshot is still alive, so without
               refreshing it the mail/budget survive but the player move is
               silently rolled back.  Make the completed market the new source
               of truth before the end-of-round restore. */
            try{if(pfm2.getOffseasonPhase()==0)pfmLineup83.snapshotAfterLoad();}catch(Throwable ignored){}
        }catch(Throwable ignored){}
        finally{try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}}
    }

    /** Return value is compared with 30 by the original pfmMarketTick code. */
    public static int rollForMarket(){
        try{
            if(pfm2.getMode()!=1)return 99; // Season Mode: always disabled.
            int chance=chance();
            return chance>0&&du.a(100)<chance?0:99;
        }catch(Throwable t){return 99;}
    }

    /** Run the original private big-transfer branch and reconcile both rosters afterwards. */
    public static void runPoach(){
        try{
            if(pfm2.getMode()!=1||get()==0)return;
            java.lang.reflect.Method m=pfmMarketTick.class.getDeclaredMethod("tryPoach");m.setAccessible(true);m.invoke(null);
        }catch(Throwable ignored){}
        finally{try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}}
    }
}
