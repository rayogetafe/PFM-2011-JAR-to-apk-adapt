/** Hook preserving the stock player-stat update and adding the v31 goalkeeper ledger. */
public final class pfmStatsHook31 {
    private pfmStatsHook31() {}
    public static void onMatchdayCompleted(){pfmPlayerStats.onMatchdayCompleted();try{pfmGoalkeeperStats31.onMatchdayCompleted();}catch(Throwable ignored){}}
}
