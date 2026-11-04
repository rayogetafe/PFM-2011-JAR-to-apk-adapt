import java.util.*;
import pfm.android.MatchEngine;
public class ParticipationLedgerHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        ArchiveHarness.main(args);PfmParticipationBridge90.record();Map<Integer,MatchEngine.Member> facts=PfmMatchEngineBridge.completedParticipation();
        for(Map.Entry<Integer,MatchEngine.Member> entry:facts.entrySet()){int id=entry.getKey();MatchEngine.Member p=entry.getValue();boolean played=p!=null&&p.minutes>0;check((pfmPlayerStats.appearances[id]&255)==(played?1:0),"phantom/missing appearance "+id);check((pfmPlayerStats.starts[id]&255)==(played&&p.starter?1:0),"wrong starter "+id);check((pfmPlayerStats.ratingMatches[id]&255)==(played?1:0),"phantom/missing rating sample "+id);if(played)check(pfmPlayerStats.ratingSum10[id]==PfmMatchEngineBridge.completedRating(id),"rating differs from report");}
        int saves=pfmPlayerStats.saves;PfmParticipationBridge90.record();check(saves==pfmPlayerStats.saves,"duplicate round writes stats");
        pfmGoalkeeperStats31.onMatchdayCompleted();for(MatchEngine.Member keeper:PfmMatchEngineBridge.currentLineups()[0].squad)if(keeper.position==0&&keeper.minutes>0){int id=Integer.parseInt(keeper.uid.substring(7));check(pfmGoalkeeperStats31.conceded(id)==keeper.conceded,"keeper charged goals after leaving pitch");check(pfmGoalkeeperStats31.starts(id)==(keeper.starter?1:0),"sub keeper counted as starter");}int ga=pfmGoalkeeperStats31.conceded(0);pfmGoalkeeperStats31.onMatchdayCompleted();check(ga==pfmGoalkeeperStats31.conceded(0),"keeper duplicate consequence");
        Map<Integer,int[]> repair=new HashMap<>();repair.put(0,new int[]{2,2,131,2});repair.put(20,new int[4]);PfmParticipationBridge90.reconcile(repair);check((pfmPlayerStats.appearances[0]&255)==2&&pfmPlayerStats.ratingSum10[0]==131&&pfmPlayerStats.appearances[20]==0,"complete journal did not restore counters");saves=pfmPlayerStats.saves;PfmParticipationBridge90.reconcile(repair);check(saves==pfmPlayerStats.saves,"rebuild not idempotent");
        System.out.println("PASS: actual appearances/starts/rating samples replace stock forecasts; phantom reserve excluded, keeper/substitute entries counted, duplicate round and journal rebuild idempotent");
    }
}
class pfmPlayerStats {
    static byte[] appearances=new byte[50],starts=new byte[50],ratingMatches=new byte[50];static int[] ratingSum10=new int[50];public static int lastRound=-1;static int saves;
    public static void ensureLoaded(){}private static void save(){saves++;}
    public static void onMatchdayCompleted(){if(cp.round<=lastRound)return;lastRound=cp.round;for(int id=0;id<50;id++)if(id%25<11||id%25==12||id%25==14||id%25==20){appearances[id]++;ratingMatches[id]++;ratingSum10[id]+=65;}}
}
