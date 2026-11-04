import java.lang.reflect.*;
import java.util.*;
import pfm.android.MatchEngine;

/** Preserve stock round/goals snapshots, but commit participation from completed match facts. */
public final class PfmParticipationBridge90 {
    private PfmParticipationBridge90(){}
    public static void noSnapshotStarts(){} // The same starts are committed with appearances below, once per completed round.
    private static Field f(String name)throws Exception{Field f=pfmPlayerStats.class.getDeclaredField(name);f.setAccessible(true);return f;}
    public static void reconcile(Map<Integer,int[]> totals){try{pfmPlayerStats.ensureLoaded();byte[] apps=(byte[])f("appearances").get(null),starts=(byte[])f("starts").get(null),samples=(byte[])f("ratingMatches").get(null);int[] ratings=(int[])f("ratingSum10").get(null);boolean changed=false;
        for(Map.Entry<Integer,int[]> entry:totals.entrySet()){int id=entry.getKey();if(id<0||id>=apps.length)continue;int[] v=entry.getValue();int ap=Math.min(255,v[0]),st=Math.min(255,v[1]),count=Math.min(255,v[3]);if((apps[id]&255)!=ap||(starts[id]&255)!=st||(samples[id]&255)!=count||ratings[id]!=v[2])changed=true;apps[id]=(byte)ap;starts[id]=(byte)st;samples[id]=(byte)count;ratings[id]=v[2];}
        if(changed){Method save=pfmPlayerStats.class.getDeclaredMethod("save");save.setAccessible(true);save.invoke(null);}
    }catch(Exception error){throw new IllegalStateException("Participation rebuild failed",error);}}
    public static void record(){try{
        pfmPlayerStats.ensureLoaded();int round=pfmPlayerStats.lastRound;
        byte[] apps=(byte[])f("appearances").get(null),starts=(byte[])f("starts").get(null),samples=(byte[])f("ratingMatches").get(null);int[] ratings=(int[])f("ratingSum10").get(null);
        byte[] beforeApps=apps.clone(),beforeStarts=starts.clone(),beforeSamples=samples.clone();int[] beforeRatings=ratings.clone();
        pfmPlayerStats.onMatchdayCompleted();if(pfmPlayerStats.lastRound<=round)return;
        for(Map.Entry<Integer,MatchEngine.Member> entry:PfmMatchEngineBridge.completedParticipation().entrySet()){
            int id=entry.getKey();if(id<0||id>=apps.length)continue;MatchEngine.Member p=entry.getValue();boolean played=p!=null&&p.minutes>0;
            int rating=played?PfmMatchEngineBridge.completedRating(id):-1;if(played&&rating<0&&(apps[id]&255)>(beforeApps[id]&255))rating=ratings[id]-beforeRatings[id];
            apps[id]=(byte)Math.min(255,(beforeApps[id]&255)+(played?1:0));starts[id]=(byte)Math.min(255,(beforeStarts[id]&255)+(played&&p.starter?1:0));
            ratings[id]=beforeRatings[id]+(played&&rating>=0?rating:0);samples[id]=(byte)Math.min(255,(beforeSamples[id]&255)+(played&&rating>=0?1:0));
        }
        Method save=pfmPlayerStats.class.getDeclaredMethod("save");save.setAccessible(true);save.invoke(null);
    }catch(Exception error){throw new IllegalStateException("Played participation commit failed",error);}}
}
