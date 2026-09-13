import java.lang.reflect.Field;
import java.util.Arrays;

/** Enforces one authoritative team per player ID after stock transfer operations. */
public final class pfmRosterIntegrity32 {
    private static int lastRemoved;
    private pfmRosterIntegrity32() {}
    private static Field field(Class<?> c,String n,Class<?> t)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(n)&&f.getType()==t){f.setAccessible(true);return f;}throw new NoSuchFieldException(n);}
    private static int ub(Object o,String n){try{return field(o.getClass(),n,Byte.TYPE).getByte(o)&255;}catch(Throwable t){return -1;}}
    private static int count(dw t){try{short[] a=(short[])field(dw.class,"a",short[].class).get(t);return Math.min(Math.max(0,ub(t,"e")),a==null?0:a.length);}catch(Throwable x){return 0;}}
    private static boolean contains(short[] ids,int count,int id){for(int i=0;i<count&&i<ids.length;i++)if((ids[i]&65535)==id)return true;return false;}

    /** Returns the number of duplicate/stale roster and transfer-list entries removed. */
    public static synchronized int repairAll(){
        int removed=0;
        try{
            dw[] teams=(dw[])field(cp.class,"a",dw[].class).get(null);
            ci[] players=(ci[])field(cp.class,"a",ci[].class).get(null);
            if(teams==null||players==null)return 0;
            int[] owner=new int[players.length];Arrays.fill(owner,-1);

            // Prefer ci.j when that authoritative team actually contains the ID.
            for(int ti=0;ti<teams.length;ti++){
                dw t=teams[ti];if(t==null)continue;short[] ids=(short[])field(dw.class,"a",short[].class).get(t);int n=count(t),tid=ub(t,"n");
                for(int i=0;i<n;i++){int id=ids[i]&65535;if(id<0||id>=players.length||players[id]==null)continue;if(owner[id]<0||ub(players[id],"j")==tid)owner[id]=ti;}
            }

            for(int ti=0;ti<teams.length;ti++){
                dw t=teams[ti];if(t==null)continue;short[] ids=(short[])field(dw.class,"a",short[].class).get(t);int old=count(t),write=0;boolean[] seen=new boolean[players.length];
                for(int i=0;i<old;i++){
                    int id=ids[i]&65535;
                    if(id>=players.length||players[id]==null||owner[id]!=ti||seen[id]){removed++;continue;}
                    seen[id]=true;ids[write++]=(short)id;
                }
                for(int i=write;i<old;i++)ids[i]=0;
                field(dw.class,"e",Byte.TYPE).setByte(t,(byte)write);
                // A listed player must still belong to and occur in this roster.
                short[] listed=(short[])field(dw.class,"b",short[].class).get(t);int listedOld=Math.min(Math.max(0,ub(t,"f")),listed==null?0:listed.length),listedWrite=0;
                if(listed!=null){boolean[] listedSeen=new boolean[players.length];for(int i=0;i<listedOld;i++){int id=listed[i]&65535;if(id>=players.length||owner[id]!=ti||listedSeen[id]||!contains(ids,write,id)){removed++;continue;}listedSeen[id]=true;listed[listedWrite++]=(short)id;}for(int i=listedWrite;i<listedOld;i++)listed[i]=0;field(dw.class,"f",Byte.TYPE).setByte(t,(byte)listedWrite);}
            }
        }catch(Throwable ignored){}
        lastRemoved=removed;return removed;
    }
    public static int lastRemoved(){return lastRemoved;}
}
