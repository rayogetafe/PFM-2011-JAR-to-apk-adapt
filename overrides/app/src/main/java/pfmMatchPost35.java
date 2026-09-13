/** Adds archive detail at the exact post-ledger phase. */
public final class pfmMatchPost35 {
    private pfmMatchPost35() {}
    public static int completedRoundOne(){try{return pfmRoundAccess60.round()+1;}catch(Throwable ignored){return 1;}}
    public static void runContrib(){byte[] b=pfmLeagueMatchArchive34.assistSnapshot();try{pfmContrib80.onMatchdayCompleted();}finally{try{pfmLeagueMatchArchive34.applyAssists(b,pfmLeagueMatchArchive34.assistSnapshot(),completedRoundOne());}catch(Throwable ignored){}}}
    public static void runDiscipline(){byte[] y=pfmLeagueMatchArchive34.yellowSnapshot(),r=pfmLeagueMatchArchive34.redSnapshot(),s=pfmLeagueMatchArchive34.suspensionSnapshot();try{pfmDiscipline70.onMatchdayCompleted();}finally{try{pfmLeagueMatchArchive34.applyCards(y,pfmLeagueMatchArchive34.yellowSnapshot(),r,pfmLeagueMatchArchive34.redSnapshot(),s,pfmLeagueMatchArchive34.suspensionSnapshot(),completedRoundOne());}catch(Throwable ignored){}}}
}
