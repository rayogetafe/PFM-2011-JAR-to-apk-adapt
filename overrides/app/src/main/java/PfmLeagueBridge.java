import java.lang.reflect.Field;
import java.util.ArrayList;

/** Read-only league-wide fixtures, squads, durability and goalkeeper statistics. */
public final class PfmLeagueBridge {
    private PfmLeagueBridge() {}
    private static Field field(Class<?> c,String n,Class<?> t)throws Exception{for(Field f:c.getDeclaredFields())if(f.getName().equals(n)&&f.getType()==t){f.setAccessible(true);return f;}throw new NoSuchFieldException(n);}
    private static dw[] teams()throws Exception{return (dw[])field(cp.class,"a",dw[].class).get(null);}
    private static ci[] players()throws Exception{return (ci[])field(cp.class,"a",ci[].class).get(null);}
    private static int[][][] schedule()throws Exception{return (int[][][])field(cp.class,"a",int[][][].class).get(null);}
    private static int ub(Object o,String n){try{return field(o.getClass(),n,Byte.TYPE).getByte(o)&255;}catch(Throwable t){return 0;}}
    private static int statByte(Class<?> c,String n,int id){try{byte[] a=(byte[])field(c,n,byte[].class).get(null);return id>=0&&id<a.length?(a[id]&255):0;}catch(Throwable t){return 0;}}
    private static String text(byte[] b){if(b==null)return "?";try{return new String(b,"ISO-8859-1").replace('\n',' ').trim();}catch(Throwable t){return new String(b).trim();}}
    private static String teamName(dw t){try{return pfmAccess50.name(t).replace('\t',' ');}catch(Throwable x){try{return text((byte[])field(dw.class,"a",byte[].class).get(t)).replace('\t',' ');}catch(Throwable y){return "?";}}}
    private static String playerName(ci p){try{return pfmPlayerName60.name(p).replace('\t',' ');}catch(Throwable x){return "Player";}}
    private static String pos(int p){return p==0?"GK":p==1?"DEF":p==2?"MID":p==3?"FW":"?";}
    private static int played(){try{return Math.max(0,cp.pfmPlayedForTable());}catch(Throwable t){return 0;}}
    private static int score(dw t,int r){try{byte[] a=(byte[])field(dw.class,"c",byte[].class).get(t);return a!=null&&r>=0&&r<a.length?(a[r]&255):-1;}catch(Throwable x){return -1;}}
    private static boolean user(dw t){try{return t==pfmPlayerHistoryV2.userTeam()||ub(t,"n")==ub(pfmPlayerHistoryV2.userTeam(),"n");}catch(Throwable x){return false;}}
    private static short[] order(dw t){try{return (short[])field(dw.class,"a",short[].class).get(t);}catch(Throwable x){return null;}}
    private static int count(dw t){try{return pfmRosterAccess50.count(t);}catch(Throwable x){return 0;}}
    public static boolean available(){try{try{pfmRosterIntegrity32.repairAll();}catch(Throwable ignored){}return teams()!=null&&players()!=null&&schedule()!=null;}catch(Throwable t){return false;}}
    public static int totalRounds(){try{return schedule().length;}catch(Throwable t){return 0;}}
    public static int playedMatches(){return played();}
    public static int goalkeeperTrackedThrough(){try{return pfmGoalkeeperStats31.trackedThrough();}catch(Throwable t){return 0;}}
    public static int goalkeeperTrackedMatches(){try{return pfmGoalkeeperStats31.trackedMatches();}catch(Throwable t){return 0;}}

    /** TSV: fixture, homeIndex, awayIndex, home, away, hg, ag, played, userMatch. */
    public static String[] roundRows(int roundOne){ArrayList<String> out=new ArrayList<String>();try{dw[] ts=teams();int[][][] s=schedule();int r=roundOne-1;if(r<0||r>=s.length)return new String[0];for(int f=0;f<s[r].length;f++){int[] x=s[r][f];if(x==null||x.length<2||x[0]<0||x[1]<0||x[0]>=ts.length||x[1]>=ts.length)continue;dw h=ts[x[0]],a=ts[x[1]];boolean done=r<played();out.add((f+1)+"\t"+x[0]+"\t"+x[1]+"\t"+teamName(h)+"\t"+teamName(a)+"\t"+(done?score(h,r):-1)+"\t"+(done?score(a,r):-1)+"\t"+(done?1:0)+"\t"+((user(h)||user(a))?1:0));}}catch(Throwable ignored){}return out.toArray(new String[out.size()]);}

    /** TSV: index, teamId, name, squadCount, GK, DEF, MID, FW, average, cleanSheets, GA, user. */
    public static String[] teamRows(){ArrayList<String> out=new ArrayList<String>();try{dw[] ts=teams();ci[] ps=players();for(int ti=0;ti<ts.length;ti++){dw t=ts[ti];if(t==null)continue;int n=count(t),sum=0,valid=0,cs=0;int[] depth=new int[4];short[] ids=order(t);for(int i=0;i<n&&ids!=null&&i<ids.length;i++){int id=ids[i]&65535;if(id>=ps.length||ps[id]==null)continue;ci p=ps[id];int po=ub(p,"b");if(po<4)depth[po]++;sum+=ub(p,"c")+ub(p,"d")+ub(p,"e");valid++;}for(int r=0;r<played();r++){int ga=opponentScore(ti,r,ts);if(ga==0)cs++;}out.add(ti+"\t"+ub(t,"n")+"\t"+teamName(t)+"\t"+n+"\t"+depth[0]+"\t"+depth[1]+"\t"+depth[2]+"\t"+depth[3]+"\t"+(valid==0?0:sum/(valid*3))+"\t"+cs+"\t"+ub(t,"l")+"\t"+(user(t)?1:0));}}catch(Throwable ignored){}return out.toArray(new String[out.size()]);}
    private static int opponentScore(int teamIndex,int r,dw[] ts){try{int[][][] s=schedule();for(int[] x:s[r]){if(x[0]==teamIndex)return score(ts[x[1]],r);if(x[1]==teamIndex)return score(ts[x[0]],r);}}catch(Throwable ignored){}return -1;}

    /** TSV includes season totals, keeper ledger, cards, fatigue and availability. */
    public static String[] playerRows(int teamIndex){ArrayList<String> out=new ArrayList<String>();try{dw[] ts=teams();ci[] ps=players();if(teamIndex<0||teamIndex>=ts.length)return new String[0];dw t=ts[teamIndex];short[] ids=order(t);int n=count(t);for(int i=0;i<n&&ids!=null&&i<ids.length;i++){int id=ids[i]&65535;if(id>=ps.length||ps[id]==null)continue;ci p=ps[id];int rat=pfmPlayerStats.averageRating10(id),rm=pfmPlayerStats.ratingMatches(id);out.add(id+"\t"+ub(p,"f")+"\t"+playerName(p)+"\t"+pos(ub(p,"b"))+"\t"+ub(p,"a")+"\t"+((ub(p,"c")+ub(p,"d")+ub(p,"e"))/3)+"\t"+pfmPlayerStats.starts(id)+"\t"+pfmPlayerStats.appearances(id)+"\t"+ub(p,"g")+"\t"+pfmContrib80.assists(id)+"\t"+rat+"\t"+rm+"\t"+pfmGoalkeeperStats31.starts(id)+"\t"+pfmGoalkeeperStats31.cleanSheets(id)+"\t"+pfmGoalkeeperStats31.conceded(id)+"\t"+pfmGoalkeeperStats31.saves(id)+"\t"+statByte(pfmDiscipline70.class,"yellow",id)+"\t"+statByte(pfmDiscipline70.class,"red",id)+"\t"+statByte(pfmCondition60.class,"fatigue",id)+"\t"+Math.max(statByte(pfmCondition60.class,"injury",id),pfmDiscipline70.roundsOut(id)));}}catch(Throwable ignored){}return out.toArray(new String[out.size()]);}
    /** teamIndex, teamName, then the normal player row. */
    public static String playerDetail(int playerId){try{for(String t:teamRows()){String[] q=t.split("\t",-1);int ti=Integer.parseInt(q[0]);for(String p:playerRows(ti))if(p.startsWith(playerId+"\t"))return ti+"\t"+q[2]+"\t"+p;}}catch(Throwable ignored){}return "";}

    /** The persisted detailed event report exists for the user's own match. */
    public static String eventReport(int roundOne){
        try{for(String body:PfmMatchdayBridge.eventRecords()){if(body.indexOf("\nMATCH ")<0)continue;int at=body.indexOf("ROUND ");if(at<0)continue;int s=at+6,e=s;while(e<body.length()&&Character.isDigit(body.charAt(e)))e++;if(e>s&&Integer.parseInt(body.substring(s,e))==roundOne)return body;}}catch(Throwable ignored){}return "";
    }

    /** Exact per-fixture facts captured by v34 at the moment the match was played. */
    public static String matchReport(int roundOne,int homeIndex,int awayIndex){
        try{dw[] ts=teams();if(homeIndex<0||awayIndex<0||homeIndex>=ts.length||awayIndex>=ts.length)return "";return pfmLeagueMatchArchive34.report(roundOne,ts[homeIndex],ts[awayIndex]);}catch(Throwable ignored){return "";}
    }

    /** TSV: played,total,all38,all37,GK38,GK37,DEF38,DEF37,MID38,MID37,FW38,FW37,targetTotal. */
    public static String durability(){int[] a38=new int[4],a37=new int[4];int all38=0,all37=0;try{dw[] ts=teams();ci[] ps=players();for(dw t:ts){short[] ids=order(t);int n=count(t);for(int i=0;i<n&&ids!=null&&i<ids.length;i++){int id=ids[i]&65535;if(id>=ps.length||ps[id]==null)continue;int st=pfmPlayerStats.starts(id),po=ub(ps[id],"b");if(st==38){all38++;if(po<4)a38[po]++;}if(st==37){all37++;if(po<4)a37[po]++;}}}}catch(Throwable ignored){}int target=targetTotal();return played()+"\t"+totalRounds()+"\t"+all38+"\t"+all37+"\t"+a38[0]+"\t"+a37[0]+"\t"+a38[1]+"\t"+a37[1]+"\t"+a38[2]+"\t"+a37[2]+"\t"+a38[3]+"\t"+a37[3]+"\t"+target;}
    private static int targetTotal(){try{int league=field(ea.class,"a",Integer.TYPE).getInt(null);int[] targets={21,8,13,0,20,0};return league>=0&&league<targets.length?targets[league]:0;}catch(Throwable t){return 0;}}
}
