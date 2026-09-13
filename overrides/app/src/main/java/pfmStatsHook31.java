/** Stock player-stat update plus goalkeeper ledger and roster integrity. */
public final class pfmStatsHook31 {
    private pfmStatsHook31() {}
    public static void onMatchdayCompleted(){
        try{
            pfmRosterIntegrity32.repairAll();
            pfmPlayerStats.onMatchdayCompleted();
        }catch(Throwable ignored){}
        try{pfmGoalkeeperStats31.onMatchdayCompleted();}catch(Throwable ignored){}
        try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}
    }
}
