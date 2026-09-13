import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import pfm.android.AndroidRuntime;

/** Persistent per-fixture facts captured while the simulator still owns them. */
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
    private static ci player(int id){try{ci[] a=(ci[])f(cp.class,"a",ci[].class).get(null);return id>=0&&id<a.length?a[id]:null;}catch(Throwable x){return null;}}
    private static int id(ci p){try{ci[] a=(ci[])f(cp.class,"a",ci[].class).get(null);for(int i=0;i<a.length;i++)if(a[i]==p)return i;}catch(Throwable ignored){}return -1;}
    private static int teamId(dw t){return ub(t,"n");}
    private static int roundOne(){try{return (f(cp.class,"a",Byte.TYPE).getByte(null)&255)+1;}catch(Throwable x){try{return cp.pfmPlayedForTable()+1;}catch(Throwable y){return 1;}}}
    private static String prefix(int round){int season=0,slot=0;try{season=pfm2.getSeason();slot=pfm2.getSlot();}catch(Throwable ignored){}return "m_"+slot+'_'+season+'_'+round+'_';}
    private static String key(int round,dw h,dw a){return prefix(round)+teamId(h)+'_'+teamId(a);}
    private static String minute(Object x){String s=String.valueOf(x).trim();StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++)if(Character.isDigit(s.charAt(i)))b.append(s.charAt(i));return b.length()==0?"?":b.toString();}
    private static int goalsFor(Vector v,ci p){int n=0;for(int i=0;i+1<v.size();i+=2)if(v.elementAt(i)==p)n++;return n;}
    private static String goalMinutes(Vector v,ci p){StringBuilder b=new StringBuilder();for(int i=0;i+1<v.size();i+=2)if(v.elementAt(i)==p){if(b.length()>0)b.append(',');b.append(minute(v.elementAt(i+1)));}return b.toString();}
    private static int points(int gf,int ga){return gf>ga?3:gf==ga?1:0;}
    private static int hash(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;x^=x>>>16;return x&0x7fffffff;}
    private static int mod(int x,int n){return n<=0?0:hash(x)%n;}
    private static String[] grow(String[] p,int n){if(p.length>=n)return p;String[] x=new String[n];System.arraycopy(p,0,x,0,p.length);for(int i=p.length;i<n;i++)x[i]="";return x;}

    /** side,index,id,number,name,pos,starter,minutes */
    private static ArrayList<String[]> participants(dw team,int side,int round){ArrayList<String[]> out=new ArrayList<String[]>();int count=0;try{count=Math.min(25,pfmRosterAccess50.count(team));}catch(Throwable ignored){}for(int i=0;i<count;i++)try{ci p=cp.a(team,i);if(p==null)continue;int mins=pfmCondition60.minutesForRosterIndex(team,i,round);boolean starter=i<11;if(!starter&&mins<=0)continue;if(starter&&mins<=0)mins=90;out.add(new String[]{String.valueOf(side),String.valueOf(i),String.valueOf(id(p)),String.valueOf(ub(p,"f")),playerName(p),pos(p),starter?"1":"0",String.valueOf(mins)});}catch(Throwable ignored){}return out;}
    private static void appendTeam(StringBuilder out,ArrayList<String[]> ps,Vector goals,int gf,int ga,int round){for(String[] x:ps)try{int pid=Integer.parseInt(x[2]);ci p=player(pid);int pg=goalsFor(goals,p),rating=pfmPatch36.buildRating(p,points(gf,ga),gf,ga,pg,pid,round);out.append("P\t").append(x[0]).append('\t').append(x[3]).append('\t').append(x[4]).append('\t').append(x[5]).append('\t').append(x[6]).append('\t').append(x[7]).append('\t').append(rating).append('\t').append(goalMinutes(goals,p)).append("\t\t").append(pid).append("\t\t\n");}catch(Throwable ignored){}}
    private static void appendSubs(StringBuilder out,ArrayList<String[]> ps,int side){ArrayList<String[]> outs=new ArrayList<String[]>(),ins=new ArrayList<String[]>();for(String[] p:ps){int m=Integer.parseInt(p[7]);if("1".equals(p[6])&&m<90)outs.add(p);if("0".equals(p[6])&&m>0)ins.add(p);}boolean[] used=new boolean[ins.size()];for(String[] o:outs){int om=Integer.parseInt(o[7]),best=-1;for(int i=0;i<ins.size();i++)if(!used[i]&&ins.get(i)[5].equals(o[5])&&90-Integer.parseInt(ins.get(i)[7])==om){best=i;break;}if(best<0)for(int i=0;i<ins.size();i++)if(!used[i]&&90-Integer.parseInt(ins.get(i)[7])==om){best=i;break;}if(best>=0){used[best]=true;String[] in=ins.get(best);out.append("S\t").append(side).append('\t').append(om).append('\t').append(o[4]).append('\t').append(in[4]).append('\t').append(o[2]).append('\t').append(in[2]).append('\n');}}}
    private static int strength(dw t){int sum=0,n=0;try{for(int i=0;i<11;i++){ci p=cp.a(t,i);if(p!=null){sum+=ub(p,"c")+ub(p,"d")+ub(p,"e");n+=3;}}}catch(Throwable ignored){}return n==0?70:sum/n;}
    private static void appendStats(StringBuilder out,dw h,dw a,int hg,int ag,int round){int hs=strength(h),as=strength(a),seed=teamId(h)*1009+teamId(a)*719+round*313;int hp=Math.max(35,Math.min(65,50+(hs-as)/7+mod(seed,7)-3)),ap=100-hp;int hsh=Math.max(hg,hg+4+hs/18+mod(seed+1,5)),ash=Math.max(ag,ag+4+as/18+mod(seed+2,5));int hst=Math.min(hsh,Math.max(hg,hg+1+mod(seed+3,4))),ast=Math.min(ash,Math.max(ag,ag+1+mod(seed+4,4)));out.append("M\t").append(hp).append('\t').append(ap).append('\t').append(hsh).append('\t').append(ash).append('\t').append(hst).append('\t').append(ast).append('\t').append(2+mod(seed+5,8)).append('\t').append(2+mod(seed+6,8)).append('\t').append(7+mod(seed+7,10)).append('\t').append(7+mod(seed+8,10)).append('\t').append(mod(seed+9,5)).append('\t').append(mod(seed+10,5)).append("\t0\t0\t0\t0\n");}

    /** Preserve the stock result and archive the exact participants/goal vectors. */
    public static void finish(dw home,dw away,Vector[] events){try{Method m=ca.class.getDeclaredMethod("a",dw.class,dw.class,Vector[].class);m.setAccessible(true);m.invoke(null,home,away,events);}catch(Throwable fatal){return;}try{int round=roundOne(),hg=events[0].size()/2,ag=events[1].size()/2;ArrayList<String[]> hp=participants(home,0,round),ap=participants(away,1,round);StringBuilder out=new StringBuilder();out.append("H\t").append(teamName(home)).append('\t').append(teamName(away)).append('\t').append(hg).append('\t').append(ag).append('\n');for(int side=0;side<2;side++)for(int i=0;i+1<events[side].size();i+=2){ci scorer=(ci)events[side].elementAt(i);out.append("G\t").append(side).append('\t').append(minute(events[side].elementAt(i+1))).append('\t').append(playerName(scorer)).append("\t\t").append(id(scorer)).append('\n');}appendSubs(out,hp,0);appendSubs(out,ap,1);appendStats(out,home,away,hg,ag,round);appendTeam(out,hp,events[0],hg,ag,round);appendTeam(out,ap,events[1],ag,hg,round);prefs().edit().putString(key(round,home,away),out.toString()).commit();}catch(Throwable ignored){}}

    private static byte[] bytes(Class<?> c,String name){try{byte[] b=(byte[])f(c,name,byte[].class).get(null);return b==null?new byte[0]:(byte[])b.clone();}catch(Throwable x){return new byte[0];}}
    public static byte[] assistSnapshot(){try{pfmContrib80.assists(0);}catch(Throwable ignored){}return bytes(pfmContrib80.class,"assists");}
    public static byte[] yellowSnapshot(){try{pfmDiscipline70.roundsOut(0);}catch(Throwable ignored){}return bytes(pfmDiscipline70.class,"yellow");}
    public static byte[] redSnapshot(){try{pfmDiscipline70.roundsOut(0);}catch(Throwable ignored){}return bytes(pfmDiscipline70.class,"red");}
    private static int delta(byte[] a,byte[] b,int id){return id>=0&&id<b.length?Math.max(0,(b[id]&255)-(id<a.length?(a[id]&255):0)):0;}
    private static ArrayList<String[]> parse(String raw){ArrayList<String[]> x=new ArrayList<String[]>();for(String s:raw.split("\n",-1))x.add(s.split("\t",-1));return x;}
    private static String serialize(ArrayList<String[]> ps){StringBuilder b=new StringBuilder();for(String[] p:ps){for(int i=0;i<p.length;i++){if(i>0)b.append('\t');b.append(p[i]);}b.append('\n');}return b.toString();}
    private static int clampRating(int x){return Math.max(40,Math.min(100,x));}
    private static int rate(String[] p,int delta){try{int old=Integer.parseInt(p[7]),now=clampRating(old+delta);p[7]=String.valueOf(now);return now-old;}catch(Throwable ignored){return 0;}}

    /** Enrich after stock contribution ledger, when AP-based assist candidates exist. */
    public static void applyAssists(byte[] before,byte[] after,int round){try{SharedPreferences.Editor edit=prefs().edit();for(Map.Entry<String,?> e:prefs().getAll().entrySet()){if(!e.getKey().startsWith(prefix(round))||!(e.getValue() instanceof String))continue;ArrayList<String[]> ps=parse((String)e.getValue());for(int gi=0;gi<ps.size();gi++){String[] g=ps.get(gi);if(g.length<6||!"G".equals(g[0])||g[4].length()>0)continue;int side=Integer.parseInt(g[1]),scorer=Integer.parseInt(g[5]);for(int pi=0;pi<ps.size();pi++){String[] p=ps.get(pi);if(p.length<11||!"P".equals(p[0])||Integer.parseInt(p[1])!=side)continue;int pid=Integer.parseInt(p[10]);if(pid!=scorer&&delta(before,after,pid)>0){before[pid]=(byte)((before[pid]&255)+1);g[4]=p[3];p=grow(p,13);p[9]=p[9].length()==0?g[2]:p[9]+","+g[2];int applied=rate(p,4);ps.set(pi,p);pfmRatingPost36.adjust(pid,applied);break;}}}edit.putString(e.getKey(),serialize(ps));}edit.commit();}catch(Throwable ignored){}}
    /** Enrich after stock discipline ledger; the legacy core has facts but no minutes. */
    public static void applyCards(byte[] y0,byte[] y1,byte[] r0,byte[] r1,int round){try{SharedPreferences.Editor edit=prefs().edit();for(Map.Entry<String,?> e:prefs().getAll().entrySet()){if(!e.getKey().startsWith(prefix(round))||!(e.getValue() instanceof String))continue;ArrayList<String[]> ps=parse((String)e.getValue());int[] c=new int[4];for(int i=0;i<ps.size();i++){String[] p=ps.get(i);if(p.length<11||!"P".equals(p[0]))continue;p=grow(p,13);int side=Integer.parseInt(p[1]),pid=Integer.parseInt(p[10]),dy=delta(y0,y1,pid),dr=delta(r0,r1,pid);for(int n=0;n<dy;n++){String m=String.valueOf(8+mod(pid*131+round*719+n*97,80));p[11]=p[11].length()==0?m:p[11]+","+m;c[side]++;}for(int n=0;n<dr;n++){String m=String.valueOf(12+mod(pid*173+round*977+n*101,77));p[12]=p[12].length()==0?m:p[12]+","+m;c[2+side]++;}int applied=rate(p,-2*dy-7*dr);ps.set(i,p);pfmRatingPost36.adjust(pid,applied);}for(int i=0;i<ps.size();i++){String[] p=ps.get(i);if("M".equals(p[0])){p=grow(p,17);p[13]=String.valueOf(c[0]);p[14]=String.valueOf(c[1]);p[15]=String.valueOf(c[2]);p[16]=String.valueOf(c[3]);ps.set(i,p);}}edit.putString(e.getKey(),serialize(ps));}edit.commit();}catch(Throwable ignored){}}
    public static String report(int round,dw home,dw away){try{return prefs().getString(key(round,home,away),"");}catch(Throwable x){return "";}}
}
