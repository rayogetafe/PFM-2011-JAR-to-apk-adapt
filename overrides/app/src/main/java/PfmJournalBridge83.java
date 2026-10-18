import java.lang.reflect.*;
import java.util.*;
import pfm.android.*;

/** Imports exact legacy fixture facts after the existing ledgers have completed. */
public final class PfmJournalBridge83 {
    private PfmJournalBridge83() {}
    private static Field field(Class<?> c,String name,Class<?> type)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}throw new NoSuchFieldException(name);}
    private static int id(dw t)throws Exception{return field(dw.class,"n",Byte.TYPE).getByte(t)&255;}
    private static int score(dw t,int round)throws Exception{return ((byte[])field(dw.class,"c",byte[].class).get(t))[round]&255;}
    public static ArrayList<MatchJournal.Match> matches(int through)throws Exception{
        dw[] teams=(dw[])field(cp.class,"a",dw[].class).get(null);int[][][] schedule=(int[][][])field(cp.class,"a",int[][][].class).get(null);
        String code=PfmSeasonBridge.leagueCode();ArrayList<MatchJournal.Match> out=new ArrayList<MatchJournal.Match>();
        for(int r=0;r<Math.min(through,schedule.length);r++)for(int[] fixture:schedule[r]){
            dw h=teams[fixture[0]],a=teams[fixture[1]];MatchJournal.Match m=new MatchJournal.Match();m.competition="league:"+code;m.round=r+1;m.home=code+":"+id(h);m.away=code+":"+id(a);m.id=NativeMatchJournal.key(m.competition,m.round,m.home,m.away);m.homeGoals=score(h,r);m.awayGoals=score(a,r);m.source="legacy-score";
            m.report=pfmLeagueMatchArchive34.report(r+1,h,a);
            MatchJournal.legacyDetails(m);
            out.add(m);
        }
        return out;
    }
    public static void sortTableAndRecord(){
        pfmAvailable70.sortTableAndRecord();
        try{if(pfm2.getMode()==1)NativeMatchJournal.advance(AndroidRuntime.context(),pfmMatchPost35.completedRoundOne());}
        catch(Throwable error){android.util.Log.e("PFM-Journal","Post-match archive failed",error);}
    }
}
