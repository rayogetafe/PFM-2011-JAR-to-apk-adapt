import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Read-only Android bridge for the current league season. */
public final class PfmSeasonBridge {
    private PfmSeasonBridge() {}

    private static Field field(Class<?> c,String name,Class<?> type) throws Exception {
        Field[] fs=c.getDeclaredFields();
        for(int i=0;i<fs.length;i++){
            Field f=fs[i];
            if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}
        }
        throw new NoSuchFieldException(c.getName()+"."+name+":"+type.getName());
    }

    private static Method method(Class<?> c,String name,Class<?> ret,Class<?>... args) throws Exception {
        Method[] ms=c.getDeclaredMethods();
        outer: for(int i=0;i<ms.length;i++){
            Method m=ms[i];
            if(!m.getName().equals(name)||m.getReturnType()!=ret)continue;
            Class<?>[] p=m.getParameterTypes();
            if(p.length!=args.length)continue;
            for(int j=0;j<p.length;j++)if(p[j]!=args[j])continue outer;
            m.setAccessible(true);return m;
        }
        throw new NoSuchMethodException(c.getName()+"."+name);
    }

    private static dw userTeam(){
        try{return pfmPlayerHistoryV2.userTeam();}catch(Throwable t){return null;}
    }

    private static String name(dw t){
        if(t==null)return "?";
        try{
            String s=pfmAccess50.name(t);
            if(s!=null&&s.length()>0)return s;
        }catch(Throwable ignored){}
        try{
            byte[] b=(byte[])field(dw.class,"a",byte[].class).get(t);
            if(b==null)return "?";
            return new String(b,"ISO-8859-1").replace('\n',' ').trim();
        }catch(Throwable ignored){return "?";}
    }

    private static dw[] teams() throws Exception {
        return (dw[])field(cp.class,"a",dw[].class).get(null);
    }

    private static int[][][] schedule() throws Exception {
        return (int[][][])field(cp.class,"a",int[][][].class).get(null);
    }

    private static byte[] tableOrder() throws Exception {
        return (byte[])field(cp.class,"c",byte[].class).get(null);
    }

    private static int ub(dw t,String n){
        try{return field(dw.class,n,Byte.TYPE).getByte(t)&255;}catch(Throwable e){return 0;}
    }

    private static int played(){
        try{return Math.max(0,cp.pfmPlayedForTable());}catch(Throwable t){
            try{return Math.max(0,pfmRoundAccess60.round());}catch(Throwable ignored){return 0;}
        }
    }

    private static boolean same(dw a,dw b){
        if(a==b)return true;
        if(a==null||b==null)return false;
        try{return ub(a,"n")==ub(b,"n");}catch(Throwable t){return false;}
    }

    public static boolean available(){
        try{
            return userTeam()!=null&&teams()!=null&&schedule()!=null&&tableOrder()!=null;
        }catch(Throwable t){return false;}
    }

    public static String seasonLabel(){
        int s=0;
        try{s=Math.max(0,pfm2.getSeason());}catch(Throwable ignored){}
        int y=2010+s;
        return y+"/"+(y+1);
    }

    public static int playedMatches(){return played();}

    public static int totalRounds(){
        try{int[][][] s=schedule();return s==null?0:s.length;}catch(Throwable t){return 0;}
    }

    public static String teamName(){return name(userTeam());}

    /**
     * Overview TSV:
     * team, season, played, total, rank, points, wins, draws, losses, gf, ga, nextMatch
     */
    public static String overview(){
        try{
            dw u=userTeam();
            if(u==null)return "";
            int p=played();
            int w=ub(u,"i"),l=ub(u,"j"),d=Math.max(0,p-w-l);
            int gf=ub(u,"k"),ga=ub(u,"l"),pts=ub(u,"h"),rank=ub(u,"m")+1;
            return name(u)+"\t"+seasonLabel()+"\t"+p+"\t"+totalRounds()+"\t"+rank+"\t"+pts+"\t"+
                    w+"\t"+d+"\t"+l+"\t"+gf+"\t"+ga+"\t"+nextMatch();
        }catch(Throwable t){return "";}
    }

    /**
     * Table TSV: rank, club, P, W, D, L, GF, GA, GD, PTS, isUser.
     * Uses cp.c[] — the exact legacy table permutation after the game's sorter.
     */
    public static String[] tableRows(){
        try{
            dw[] all=teams();byte[] order=tableOrder();dw u=userTeam();
            if(all==null||order==null)return new String[0];
            int p=played();
            String[] out=new String[order.length];
            for(int r=0;r<order.length;r++){
                int idx=order[r]&255;
                if(idx<0||idx>=all.length||all[idx]==null){out[r]=(r+1)+"\t?\t0\t0\t0\t0\t0\t0\t0\t0\t0";continue;}
                dw t=all[idx];int w=ub(t,"i"),l=ub(t,"j"),d=Math.max(0,p-w-l),gf=ub(t,"k"),ga=ub(t,"l");
                out[r]=(r+1)+"\t"+name(t).replace('\t',' ')+"\t"+p+"\t"+w+"\t"+d+"\t"+l+"\t"+
                        gf+"\t"+ga+"\t"+(gf-ga)+"\t"+ub(t,"h")+"\t"+(same(t,u)?1:0);
            }
            return out;
        }catch(Throwable t){return new String[0];}
    }

    /**
     * User schedule TSV: round, venue(H/A), opponent, home, away, homeGoals, awayGoals, played, isNext.
     * dw.c[round] is written by the stock match engine with that team's goals.
     */
    public static String[] scheduleRows(){
        try{
            int[][][] s=schedule();dw[] all=teams();dw u=userTeam();int p=played();
            if(s==null||all==null||u==null)return new String[0];
            String[] out=new String[s.length];
            for(int r=0;r<s.length;r++){
                dw home=null,away=null;
                int[][] round=s[r];
                if(round!=null)for(int f=0;f<round.length;f++){
                    int[] x=round[f];if(x==null||x.length<2)continue;
                    if(x[0]>=0&&x[0]<all.length&&x[1]>=0&&x[1]<all.length){
                        dw h=all[x[0]],a=all[x[1]];
                        if(same(h,u)||same(a,u)){home=h;away=a;break;}
                    }
                }
                if(home==null||away==null){out[r]=(r+1)+"\t?\t?\t?\t?\t-1\t-1\t0\t0";continue;}
                boolean userHome=same(home,u);dw opp=userHome?away:home;
                boolean done=r<p;
                int hg=-1,ag=-1;
                if(done){
                    try{byte[] hc=(byte[])field(dw.class,"c",byte[].class).get(home);byte[] ac=(byte[])field(dw.class,"c",byte[].class).get(away);
                        if(hc!=null&&ac!=null&&r<hc.length&&r<ac.length){hg=hc[r]&255;ag=ac[r]&255;}
                    }catch(Throwable ignored){}
                }
                boolean next=!done&&r==p;
                out[r]=(r+1)+"\t"+(userHome?"H":"A")+"\t"+name(opp).replace('\t',' ')+"\t"+
                        name(home).replace('\t',' ')+"\t"+name(away).replace('\t',' ')+"\t"+hg+"\t"+ag+"\t"+(done?1:0)+"\t"+(next?1:0);
            }
            return out;
        }catch(Throwable t){return new String[0];}
    }

    public static String nextMatch(){
        try{
            String[] rows=scheduleRows();
            for(int i=0;i<rows.length;i++){
                String[] p=rows[i].split("\\t",-1);
                if(p.length>=9&&"1".equals(p[8]))return "R"+p[0]+"  "+p[3]+" vs "+p[4];
            }
            return "Season complete";
        }catch(Throwable t){return "?";}
    }
}
