import java.lang.reflect.*;
import pfm.android.NativeMatchJournal;

/** Read-only European condition overlay; never mutates/re-saves domestic condition ledgers. */
public final class PfmEffectsBridge84 {
    private PfmEffectsBridge84(){}
    private static Field field(Class<?> type,String name,Class<?> value)throws Exception{for(Field f:type.getDeclaredFields())if(f.getName().equals(name)&&f.getType()==value){f.setAccessible(true);return f;}throw new NoSuchFieldException(name);}
    private static int value(Class<?> type,String name,int id)throws Exception{byte[] data=(byte[])field(type,name,byte[].class).get(null);return id>=0&&id<data.length?data[id]&255:0;}
    public static int[] condition(int id)throws Exception{Method ensure=pfmCondition60.class.getDeclaredMethod("ensure");ensure.setAccessible(true);ensure.invoke(null);return new int[]{value(pfmCondition60.class,"fatigue",id),value(pfmCondition60.class,"injury",id)};}
    public static int fatigue(int id,int domestic){return Math.min(100,domestic+NativeMatchJournal.legacyEffects(id)[0]);}
    public static int injury(int id,int domestic){return Math.max(domestic,NativeMatchJournal.legacyEffects(id)[1]);}
    private static int penalty(int fatigue){return fatigue<=25?0:Math.min(5,(fatigue-25+14)/15)*6;}
    public static int starts(int id){int base=pfmAvailable70.starts(id);int[] e=NativeMatchJournal.legacyEffects(id);if(e[1]>0)return base+5000;try{int fat=condition(id)[0];return base+penalty(Math.min(100,fat+e[0]))-penalty(fat);}catch(Exception error){throw new IllegalStateException(error);}}
    public static int minutesForRosterIndex(dw club,int index,int round){int id=pfmRosterId60.idAt(club,index);if(NativeMatchJournal.legacyEffects(id)[1]>0)return 0;return pfmAvailable70.minutesForRosterIndex(club,index,round);}
    public static int benchImpact(dw club){return pfmAvailable70.benchImpact(club);}
    public static void setBudget(int budget)throws Exception{field(dw.class,"g",Integer.TYPE).setInt(pfmPlayerHistoryV2.userTeam(),budget);}
}
