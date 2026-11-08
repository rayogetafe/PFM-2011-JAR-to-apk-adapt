import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.io.*;
import java.util.*;
import pfm.android.AndroidRuntime;

/** Per-save-slot goalkeeper ledger. CS/GA and saves agree with the archived match flow. */
public final class pfmGoalkeeperStats31 {
    private static final String PREFS="pfm_goalkeepers_v31";
    private pfmGoalkeeperStats31() {}
    private static SharedPreferences prefs(){return AndroidRuntime.context().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String prefix(){return "c"+pfm.android.NativeWorldCenter.identity()+"s"+pfm2.getSlot()+"y"+pfm2.getSeason()+"_";}
    private static String checkpointPrefix(){
        int slot = pfm2.getSlot();
        int career = AndroidRuntime.context().getSharedPreferences("pfm_world_careers", 0).getInt("slot_" + slot + "_nonce", 0);
        return "c"+career+"s"+slot+"y"+pfm2.getSeason()+"_";
    }
    static void writeCheckpoint(DataOutputStream out) throws IOException {
        String scope = checkpointPrefix();
        Map<String, Integer> values = new TreeMap<>();
        for (Map.Entry<String, ?> entry : prefs().getAll().entrySet()) {
            if (entry.getKey().startsWith(scope) && entry.getValue() instanceof Integer) {
                values.put(entry.getKey().substring(scope.length()), (Integer) entry.getValue());
            }
        }
        out.writeInt(values.size());
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }
    static Map<String, Integer> readCheckpoint(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 40000) throw new IOException("Invalid goalkeeper checkpoint size");
        Map<String, Integer> values = new HashMap<>();
        for (int i = 0; i < count; i++) values.put(in.readUTF(), in.readInt());
        return values;
    }
    static void restoreCheckpoint(Map<String, Integer> values) {
        String scope = checkpointPrefix();
        SharedPreferences p = prefs();
        SharedPreferences.Editor edit = p.edit();
        for (String key : p.getAll().keySet()) if (key.startsWith(scope)) edit.remove(key);
        for (Map.Entry<String, Integer> entry : values.entrySet()) edit.putInt(scope + entry.getKey(), entry.getValue());
        edit.commit();
    }
    private static Field field(Class<?> c,String n,Class<?> t)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(n)&&f.getType()==t){f.setAccessible(true);return f;}throw new NoSuchFieldException(n);}
    private static int teamId(dw t){try{return field(dw.class,"n",Byte.TYPE).getByte(t)&255;}catch(Throwable x){return -1;}}
    private static int pos(ci p){try{return field(ci.class,"b",Byte.TYPE).getByte(p)&255;}catch(Throwable x){return -1;}}
    private static int quality(ci p){try{return field(ci.class,"e",Byte.TYPE).getByte(p)&255;}catch(Throwable x){return 65;}}
    private static short[][] snapshots(){try{return (short[][])field(pfmLineup83.class,"snapByTeamId",short[][].class).get(null);}catch(Throwable x){return null;}}
    private static int snapshotRound(){try{return field(pfmLineup83.class,"snapRound",Integer.TYPE).getInt(null);}catch(Throwable x){return -1;}}
    private static int score(dw t,int round){try{byte[] a=(byte[])field(dw.class,"c",byte[].class).get(t);return a!=null&&round>=0&&round<a.length?(a[round]&255):-1;}catch(Throwable x){return -1;}}
    private static int opponentGoals(dw team,int round){
        try{dw[] teams=(dw[])field(cp.class,"a",dw[].class).get(null);int[][][] schedule=(int[][][])field(cp.class,"a",int[][][].class).get(null);if(teams==null||schedule==null||round<0||round>=schedule.length)return -1;int id=teamId(team);for(int[] f:schedule[round]){if(f==null||f.length<2)continue;dw h=teams[f[0]],a=teams[f[1]];if(teamId(h)==id)return score(a,round);if(teamId(a)==id)return score(h,round);}}catch(Throwable ignored){}return -1;
    }
    private static int startingKeeper(dw team){
        try{short[][] snaps=snapshots();int tid=teamId(team);short[] ids=snaps!=null&&tid>=0&&tid<snaps.length?snaps[tid]:null;if(ids==null)return -1;ci[] players=(ci[])field(cp.class,"a",ci[].class).get(null);for(int i=0;i<Math.min(11,ids.length);i++){int id=ids[i]&65535;if(players!=null&&id<players.length&&players[id]!=null&&pos(players[id])==0)return id;}}catch(Throwable ignored){}return -1;
    }
    private static int modelledSaves(dw team,int playerId,ci keeper,int round,int conceded){int archived=pfmLeagueMatchArchive34.keeperSaves(round+1,team);if(archived>=0)return archived;int h=playerId*1103515245+round*12345+pfm2.getSeason()*97;h^=(h>>>16);int saves=1+Math.abs(h%5);if(quality(keeper)>=78)saves++;if(conceded>=3&&saves>1)saves--;return Math.max(0,saves);}
    private static int get(SharedPreferences p,String key){return p.getInt(prefix()+key,0);}
    private static void add(SharedPreferences.Editor e,SharedPreferences p,int id,String stat,int value){String k="p"+id+"_"+stat;e.putInt(prefix()+k,get(p,k)+value);}

    public static void onMatchdayCompleted(){
        try{
            int round=snapshotRound();if(round<0)round=Math.max(0,cp.pfmPlayedForTable()-1);if(round<0)return;SharedPreferences p=prefs();String lastKey=prefix()+"last";int last=p.getInt(lastKey,-1);SharedPreferences.Editor e=p.edit();
            if(round<last){for(String k:p.getAll().keySet())if(k.startsWith(prefix()))e.remove(k);e.commit();last=-1;}
            if(round==last)return;dw[] teams=(dw[])field(cp.class,"a",dw[].class).get(null);ci[] players=(ci[])field(cp.class,"a",ci[].class).get(null);if(teams==null||players==null)return;
            for(dw team:teams){if(team==null)continue;pfm.android.MatchEngine.Lineup actual=PfmMatchEngineBridge.completedLineup(team,round+1);
                if(actual!=null){java.util.ArrayList<pfm.android.MatchEngine.Member> keepers=new java.util.ArrayList<pfm.android.MatchEngine.Member>();int minutes=0;for(pfm.android.MatchEngine.Member member:actual.squad)if(member.position==0&&member.minutes>0){keepers.add(member);minutes+=member.minutes;}
                    int saves=Math.max(0,pfmLeagueMatchArchive34.keeperSaves(round+1,team))*minutes/90,remaining=saves;
                    for(int i=0;i<keepers.size();i++){pfm.android.MatchEngine.Member keeper=keepers.get(i);int id=Integer.parseInt(keeper.uid.substring(7)),share=i==keepers.size()-1?remaining:saves*keeper.minutes/Math.max(1,minutes);remaining-=share;if(keeper.starter)add(e,p,id,"st",1);add(e,p,id,"ga",keeper.conceded);if(keeper.minutes>=60&&keeper.conceded==0)add(e,p,id,"cs",1);add(e,p,id,"sv",share);}continue;}
                int id=startingKeeper(team);if(id<0||id>=players.length||players[id]==null)continue;int ga=opponentGoals(team,round);if(ga<0)continue;add(e,p,id,"st",1);add(e,p,id,"ga",ga);if(ga==0)add(e,p,id,"cs",1);add(e,p,id,"sv",modelledSaves(team,id,players[id],round,ga));}
            e.putInt(prefix()+"tracked",get(p,"tracked")+1);e.putInt(lastKey,round).apply();
        }catch(Throwable ignored){}
    }
    public static int starts(int id){try{return get(prefs(),"p"+id+"_st");}catch(Throwable t){return 0;}}
    public static int cleanSheets(int id){try{return get(prefs(),"p"+id+"_cs");}catch(Throwable t){return 0;}}
    public static int conceded(int id){try{return get(prefs(),"p"+id+"_ga");}catch(Throwable t){return 0;}}
    public static int saves(int id){try{return get(prefs(),"p"+id+"_sv");}catch(Throwable t){return 0;}}
    public static int trackedThrough(){try{return prefs().getInt(prefix()+"last",-1)+1;}catch(Throwable t){return 0;}}
    public static int trackedMatches(){try{return get(prefs(),"tracked");}catch(Throwable t){return 0;}}
}
