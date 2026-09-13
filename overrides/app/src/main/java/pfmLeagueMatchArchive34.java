import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;
import pfm.android.AndroidRuntime;

/** Persistent, per-fixture match facts captured before the simulator discards them. */
public final class pfmLeagueMatchArchive34 {
    private static final String PREFS="pfm_match_archive_v34";
    private pfmLeagueMatchArchive34() {}

    private static SharedPreferences prefs(){return AndroidRuntime.context().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static Field f(Class<?> c,String n,Class<?> t)throws Exception{for(Field x:c.getDeclaredFields())if(x.getName().equals(n)&&x.getType()==t){x.setAccessible(true);return x;}throw new NoSuchFieldException(n);}
    private static int ub(Object o,String n){try{return f(o.getClass(),n,Byte.TYPE).getByte(o)&255;}catch(Throwable x){return 0;}}
    private static String clean(String s){return s==null?"?":s.replace('\t',' ').replace('\n',' ').trim();}
    private static String playerName(ci p){try{return clean(pfmPlayerName60.name(p));}catch(Throwable x){return "Player";}}
    private static String teamName(dw t){try{return clean(pfmAccess50.name(t));}catch(Throwable x){return "Team";}}
    private static String pos(ci p){int x=ub(p,"b");return x==0?"GK":x==1?"DEF":x==2?"MID":x==3?"FW":"?";}
    private static int id(ci p){try{ci[] all=(ci[])f(cp.class,"a",ci[].class).get(null);for(int i=0;i<all.length;i++)if(all[i]==p)return i;}catch(Throwable ignored){}return -1;}
    private static int teamId(dw t){return ub(t,"n");}
    private static int roundOne(){try{return (f(cp.class,"a",Byte.TYPE).getByte(null)&255)+1;}catch(Throwable x){try{return cp.pfmPlayedForTable()+1;}catch(Throwable y){return 1;}}}
    private static String key(int round,dw h,dw a){int season=0,slot=0;try{season=pfm2.getSeason();slot=pfm2.getSlot();}catch(Throwable ignored){}return "m_"+slot+'_'+season+'_'+round+'_'+teamId(h)+'_'+teamId(a);}
    private static String minute(Object x){String s=String.valueOf(x).trim();StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++)if(Character.isDigit(s.charAt(i)))b.append(s.charAt(i));return b.length()==0?"?":b.toString();}
    private static int goalsFor(Vector v,ci p){int n=0;for(int i=0;i+1<v.size();i+=2)if(v.elementAt(i)==p)n++;return n;}
    private static String goalMinutes(Vector v,ci p){StringBuilder b=new StringBuilder();for(int i=0;i+1<v.size();i+=2)if(v.elementAt(i)==p){if(b.length()>0)b.append(',');b.append(minute(v.elementAt(i+1)));}return b.toString();}
    private static int assister(dw team,int scorerId,int round,int ordinal){try{Method m=pfmContrib80.class.getDeclaredMethod("pickAssister",dw.class,Integer.TYPE,Integer.TYPE,Integer.TYPE);m.setAccessible(true);return ((Integer)m.invoke(null,team,Integer.valueOf(scorerId),Integer.valueOf(round),Integer.valueOf(ordinal))).intValue();}catch(Throwable x){return -1;}}
    private static String assistMinutes(dw team,Vector goals,ci p,int round){StringBuilder b=new StringBuilder();for(int i=0;i+1<goals.size();i+=2){ci scorer=(ci)goals.elementAt(i);int aid=assister(team,id(scorer),round,i/2);if(aid==id(p)){if(b.length()>0)b.append(',');b.append(minute(goals.elementAt(i+1)));}}return b.toString();}
    private static int points(int gf,int ga){return gf>ga?3:gf==ga?1:0;}

    private static void appendTeam(StringBuilder out,int side,dw team,Vector goals,int gf,int ga,int round){
        int count=0;try{count=Math.min(25,pfmRosterAccess50.count(team));}catch(Throwable ignored){}
        for(int i=0;i<count;i++)try{
            ci p=cp.a(team,i);if(p==null)continue;
            int mins=pfmCondition60.minutesForRosterIndex(team,i,round);
            boolean starter=i<11;
            if(!starter&&mins<=0)continue;
            if(starter&&mins<=0)mins=90;
            int pid=pfmRosterId60.idAt(team,i),pg=goalsFor(goals,p);
            int rating=pfmPatch36.buildRating(p,points(gf,ga),gf,ga,pg,pid,round);
            String gm=goalMinutes(goals,p),am=assistMinutes(team,goals,p,round);
            out.append("P\t").append(side).append('\t').append(ub(p,"f")).append('\t').append(playerName(p)).append('\t').append(pos(p)).append('\t').append(starter?1:0).append('\t').append(mins).append('\t').append(rating).append('\t').append(gm).append('\t').append(am).append('\n');
        }catch(Throwable ignored){}
    }

    /** Replacement for ca's private result finalizer: preserve stock behavior, then archive. */
    public static void finish(dw home,dw away,Vector[] events){
        try{Method m=ca.class.getDeclaredMethod("a",dw.class,dw.class,Vector[].class);m.setAccessible(true);m.invoke(null,home,away,events);}catch(Throwable fatal){return;}
        try{
            int round=roundOne(),hg=events[0].size()/2,ag=events[1].size()/2;
            StringBuilder out=new StringBuilder();
            out.append("H\t").append(teamName(home)).append('\t').append(teamName(away)).append('\t').append(hg).append('\t').append(ag).append('\n');
            for(int side=0;side<2;side++)for(int i=0;i+1<events[side].size();i+=2){ci scorer=(ci)events[side].elementAt(i);int aid=assister(side==0?home:away,id(scorer),round,i/2);String an="";try{ci[] ps=(ci[])f(cp.class,"a",ci[].class).get(null);if(aid>=0&&aid<ps.length)an=playerName(ps[aid]);}catch(Throwable ignored){}out.append("G\t").append(side).append('\t').append(minute(events[side].elementAt(i+1))).append('\t').append(playerName(scorer)).append('\t').append(an).append('\n');}
            appendTeam(out,0,home,events[0],hg,ag,round);appendTeam(out,1,away,events[1],ag,hg,round);
            prefs().edit().putString(key(round,home,away),out.toString()).commit();
        }catch(Throwable ignored){}
    }

    public static String report(int round,dw home,dw away){try{return prefs().getString(key(round,home,away),"");}catch(Throwable x){return "";}}
}
