import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import pfm.android.compat.rms.RecordStore;

/** Native read model for the expanded matchday squad, auto subs and persisted event log. */
public final class PfmMatchdayBridge {
    private PfmMatchdayBridge() {}
    private static Field field(Class<?> c,String n,Class<?> t)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(n)&&f.getType()==t){f.setAccessible(true);return f;}throw new NoSuchFieldException(n);}
    private static dw team(){try{return pfmPlayerHistoryV2.userTeam();}catch(Throwable t){return null;}}
    private static int round(){try{return Math.max(0,cp.pfmPlayedForTable());}catch(Throwable t){return 0;}}
    private static ci[] players(){try{return (ci[])field(cp.class,"a",ci[].class).get(null);}catch(Throwable t){return null;}}
    private static short[] order(dw t){try{return (short[])field(dw.class,"a",short[].class).get(t);}catch(Throwable x){return null;}}
    private static int count(dw t){try{short[] a=order(t);return Math.min(field(dw.class,"e",Byte.TYPE).getByte(t)&255,a==null?0:a.length);}catch(Throwable x){return 0;}}
    private static int ub(ci p,String n){try{return field(ci.class,n,Byte.TYPE).getByte(p)&255;}catch(Throwable x){return 0;}}
    private static String name(ci p){try{return pfmPlayerName60.name(p).replace('\t',' ');}catch(Throwable x){return "Player";}}
    private static String pos(int p){return p==0?"GK":(p==1?"DEF":(p==2?"MID":(p==3?"FW":"?")));}
    private static ci at(dw t,int i){try{short[] o=order(t);ci[] p=players();int id=o[i]&65535;return p!=null&&id<p.length?p[id]:null;}catch(Throwable x){return null;}}
    private static int minutes(dw t,int i){try{return pfmAvailable70.minutesForRosterIndex(t,i,round());}catch(Throwable x){return i<11?90:0;}}

    public static boolean available(){return team()!=null&&count(team())>=11;}
    public static boolean live(){try{return PfmSquadBridge.liveMatchActive();}catch(Throwable t){return false;}}
    public static String opponent(){try{return PfmSeasonBridge.nextMatch();}catch(Throwable t){return "Next match";}}

    /** TSV: slot, playerId, name, position, expectedMinutes, speed, resistance, quality, fatigue, status. */
    public static String[] squadRows(){ArrayList<String> out=new ArrayList<String>();try{dw t=team();short[] o=order(t);int n=count(t);for(int i=0;i<n;i++){ci p=at(t,i);if(p==null)continue;int id=o[i]&65535;int fat=0,inj=0,susp=0;try{fat=field(pfmCondition60.class,"fatigue",byte[].class).get(null)==null?0:(((byte[])field(pfmCondition60.class,"fatigue",byte[].class).get(null))[id]&255);}catch(Throwable ignored){}try{inj=((byte[])field(pfmCondition60.class,"injury",byte[].class).get(null))[id]&255;}catch(Throwable ignored){}try{susp=pfmDiscipline70.roundsOut(id);}catch(Throwable ignored){}String status=inj>0?"INJ "+inj:(susp>0?"SUSP "+susp:"available");out.add(i+"\t"+id+"\t"+name(p)+"\t"+pos(ub(p,"b"))+"\t"+minutes(t,i)+"\t"+ub(p,"c")+"\t"+ub(p,"d")+"\t"+ub(p,"e")+"\t"+fat+"\t"+status);}}catch(Throwable ignored){}return out.toArray(new String[out.size()]);}

    /** TSV: minute, outgoing player, incoming player, outgoing slot, incoming slot. */
    public static String[] substitutionPlan(){ArrayList<String> out=new ArrayList<String>();try{dw t=team();int n=count(t);boolean[] used=new boolean[11];for(int i=11;i<n&&out.size()<3;i++){ci in=at(t,i);int inMinutes=minutes(t,i);if(in==null||ub(in,"b")==0||inMinutes<=0)continue;int minute=90-inMinutes,outSlot=-1;for(int j=0;j<11;j++){ci starter=at(t,j);if(!used[j]&&starter!=null&&ub(starter,"b")==ub(in,"b")&&minutes(t,j)==minute){outSlot=j;used[j]=true;break;}}out.add(minute+"\t"+(outSlot>=0?name(at(t,outSlot)):"Tactical change")+"\t"+name(in)+"\t"+outSlot+"\t"+i);}}catch(Throwable ignored){}return out.toArray(new String[out.size()]);}

    private static String eventStore(){int s=pfm2.getSlot();return s==1?"P2E002":(s==2?"P2E003":"P2E001");}
    /** Complete newest-first persisted match reports. */
    public static String[] eventRecords(){final class E{int key;String body;}ArrayList<E> all=new ArrayList<E>();RecordStore rs=null;try{rs=RecordStore.openRecordStore(eventStore(),false);for(int i=1;i<=rs.getNumRecords();i++){String raw=new String(rs.getRecord(i));int nl=raw.indexOf('\n');if(nl<0)continue;E e=new E();try{e.key=Integer.parseInt(raw.substring(0,nl).trim());}catch(Throwable x){e.key=0;}e.body=raw.substring(nl+1);all.add(e);}}catch(Throwable ignored){}finally{if(rs!=null)try{rs.closeRecordStore();}catch(Throwable ignored){}}Collections.sort(all,new Comparator<E>(){public int compare(E a,E b){return b.key-a.key;}});String[] out=new String[all.size()];for(int i=0;i<out.length;i++)out[i]=all.get(i).body;return out;}
}
