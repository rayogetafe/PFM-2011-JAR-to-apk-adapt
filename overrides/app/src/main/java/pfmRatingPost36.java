import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Applies event bonuses to the already-recorded rating for the current match. */
public final class pfmRatingPost36 {
    private pfmRatingPost36() {}
    private static Field field(Class<?> c,String n,Class<?> t)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(n)&&f.getType()==t){f.setAccessible(true);return f;}throw new NoSuchFieldException(n);}

    public static synchronized void adjust(int playerId,int delta10){
        if(delta10==0||playerId<0)return;
        try{
            // Loading through the public accessor keeps the private arrays/save slot coherent.
            pfmPlayerStats.averageRating10(playerId);
            if(pfmPlayerStats.lastRound!=pfmRoundAccess60.round())return;
            int[] sums=(int[])field(pfmPlayerStats.class,"ratingSum10",int[].class).get(null);
            byte[] matches=(byte[])field(pfmPlayerStats.class,"ratingMatches",byte[].class).get(null);
            if(sums==null||matches==null||playerId>=sums.length||playerId>=matches.length||(matches[playerId]&255)==0)return;
            sums[playerId]+=delta10;
            Method save=pfmPlayerStats.class.getDeclaredMethod("save");save.setAccessible(true);save.invoke(null);
        }catch(Throwable ignored){}
    }
}
