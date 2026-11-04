/** Stock player-stat update plus goalkeeper ledger and roster integrity. */
public final class pfmStatsHook31 {
    private pfmStatsHook31() {}
    public static void onMatchdayCompleted(){
        try{
            pfmRosterIntegrity32.repairAll();
            PfmParticipationBridge90.record();
        }catch(Throwable error){android.util.Log.e("PFM-Participation","Player ledger update failed",error);}
        try{pfmGoalkeeperStats31.onMatchdayCompleted();}catch(Throwable ignored){}
        try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}
    }
}
