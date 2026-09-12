import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.lang.reflect.Field;
import pfm.android.compat.rms.RecordStore;

/** Read-only access to the career and player-season archives already maintained by Alpha084. */
public final class PfmHistoryBridge {
    private PfmHistoryBridge() {}

    private static String tableStore(){int s=pfm2.getSlot();return s==1?"P2H002":(s==2?"P2H003":"P2H001");}
    private static String playerStore(){int s=pfm2.getSlot();return s==1?"P2UH002":(s==2?"P2UH003":"P2UH001");}
    private static String text(byte[] b,int off,int len){try{return new String(b,off,len,"ISO-8859-1").replace('\n',' ').replace('\t',' ').trim();}catch(Throwable t){return "?";}}
    private static Field field(Class<?> c,String name,Class<?> type)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}throw new NoSuchFieldException(name);}
    private static String playerName(int id){try{ci[] all=(ci[])field(cp.class,"a",ci[].class).get(null);if(all!=null&&id>=0&&id<all.length&&all[id]!=null)return pfmPlayerName60.name(all[id]).replace('\t',' ');}catch(Throwable ignored){}return "Player "+id;}
    private static byte[] record(String store,int season){RecordStore rs=null;try{rs=RecordStore.openRecordStore(store,false);for(int i=1;i<=rs.getNumRecords();i++){byte[] b=rs.getRecord(i);if(b==null)continue;if(store.startsWith("P2UH")&&b.length>=8&&b[0]==85&&b[1]==50&&b[2]==72&&b[3]==49&&(((b[5]&255)<<8)|(b[6]&255))==season)return b;if(store.startsWith("P2H")&&b.length>=5&&(b[0]&255)==72&&(2010+(b[2]&255))==season)return b;}}catch(Throwable ignored){}finally{if(rs!=null)try{rs.closeRecordStore();}catch(Throwable ignored){}}return null;}

    public static boolean available(){try{return pfmPlayerHistoryV2.userTeam()!=null;}catch(Throwable t){return false;}}
    public static int currentSeason(){try{return pfm2.getSeason();}catch(Throwable t){return 2010;}}

    /** Newest-first list of seasons which have a live or archived player/table view. */
    public static int[] seasons(){
        ArrayList<Integer> out=new ArrayList<Integer>();int current=currentSeason();out.add(Integer.valueOf(current));RecordStore rs=null;
        try{rs=RecordStore.openRecordStore(tableStore(),false);for(int i=1;i<=rs.getNumRecords();i++){byte[] b=rs.getRecord(i);if(b!=null&&b.length>=5&&(b[0]&255)==72){int y=2010+(b[2]&255);if(!out.contains(Integer.valueOf(y)))out.add(Integer.valueOf(y));}}}catch(Throwable ignored){}finally{if(rs!=null)try{rs.closeRecordStore();}catch(Throwable ignored){}}
        Collections.sort(out,Collections.reverseOrder());int[] a=new int[out.size()];for(int i=0;i<a.length;i++)a[i]=out.get(i).intValue();return a;
    }

    /** TSV: rank, club, P, W, D, L, GF, GA, GD, PTS, isUser. */
    public static String[] tableRows(int season){
        if(season==currentSeason())try{return PfmSeasonBridge.tableRows();}catch(Throwable ignored){}
        byte[] b=record(tableStore(),season);if(b==null)return new String[0];final class R{String n;int pts,w,l,gf,ga;}
        ArrayList<R> rows=new ArrayList<R>();int count=b[3]&255,pos=5;
        for(int i=0;i<count&&pos<b.length;i++){int len=b[pos++]&255;if(pos+len+5>b.length)break;R r=new R();r.n=text(b,pos,len);pos+=len;r.pts=b[pos++]&255;r.w=b[pos++]&255;r.l=b[pos++]&255;r.gf=b[pos++]&255;r.ga=b[pos++]&255;rows.add(r);}
        Collections.sort(rows,new Comparator<R>(){public int compare(R a,R z){int c=z.pts-a.pts;if(c!=0)return c;c=(z.gf-z.ga)-(a.gf-a.ga);if(c!=0)return c;return z.gf-a.gf;}});
        int userRank=b[4]&255;String[] out=new String[rows.size()];int played=Math.max(0,2*(rows.size()-1));for(int i=0;i<rows.size();i++){R r=rows.get(i);int d=Math.max(0,played-r.w-r.l);out[i]=(i+1)+"\t"+r.n+"\t"+played+"\t"+r.w+"\t"+d+"\t"+r.l+"\t"+r.gf+"\t"+r.ga+"\t"+(r.gf-r.ga)+"\t"+r.pts+"\t"+((i+1)==userRank?1:0)+"\t-1";}return out;
    }

    /** TSV: playerId, name, goals, appearances, starts, rating10, speed, resistance, quality. */
    public static String[] playerRows(int season){
        byte[] b=record(playerStore(),season);
        if(b==null&&season==currentSeason()){
            ArrayList<String> live=new ArrayList<String>();try{dw team=pfmPlayerHistoryV2.userTeam();short[] ids=(short[])field(dw.class,"a",short[].class).get(team);int count=field(dw.class,"e",Byte.TYPE).getByte(team)&255;ci[] all=(ci[])field(cp.class,"a",ci[].class).get(null);count=Math.min(count,ids.length);for(int i=0;i<count;i++){int id=ids[i]&65535;if(id<0||id>=all.length||all[id]==null)continue;ci p=all[id];int g=field(ci.class,"g",Byte.TYPE).getByte(p)&255,spe=field(ci.class,"c",Byte.TYPE).getByte(p)&255,res=field(ci.class,"d",Byte.TYPE).getByte(p)&255,qua=field(ci.class,"e",Byte.TYPE).getByte(p)&255;live.add(id+"\t"+playerName(id)+"\t"+g+"\t"+pfmPlayerStats.appearances(id)+"\t"+pfmPlayerStats.starts(id)+"\t"+Math.max(0,pfmPlayerStats.averageRating10(id))+"\t"+spe+"\t"+res+"\t"+qua);}}catch(Throwable ignored){}return live.toArray(new String[live.size()]);
        }
        if(b==null||b.length<8)return new String[0];int count=b[7]&255,pos=8;ArrayList<String> out=new ArrayList<String>();
        for(int i=0;i<count&&pos+8<b.length;i++){int id=((b[pos]&255)<<8)|(b[pos+1]&255);int g=b[pos+2]&255,ap=b[pos+3]&255,st=b[pos+4]&255,rat=b[pos+5]&255,spe=b[pos+6]&255,res=b[pos+7]&255,qua=b[pos+8]&255;pos+=9;out.add(id+"\t"+playerName(id)+"\t"+g+"\t"+ap+"\t"+st+"\t"+rat+"\t"+spe+"\t"+res+"\t"+qua);}
        return out.toArray(new String[out.size()]);
    }
}
