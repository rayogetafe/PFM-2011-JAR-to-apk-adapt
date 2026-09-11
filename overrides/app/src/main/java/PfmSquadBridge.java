import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Android bridge to the current user's real PFM squad and pre-match XI order. */
public final class PfmSquadBridge {
    private PfmSquadBridge() {}

    private static Method squadCountMethod;
    private static Method playerIdMethod;
    private static Method playerAtMethod;

    private static Field field(Class<?> c,String name,Class<?> type) throws Exception {
        Field[] fs=c.getDeclaredFields();
        for(int i=0;i<fs.length;i++){
            Field f=fs[i];
            if(f.getName().equals(name)&&f.getType()==type){
                f.setAccessible(true);
                return f;
            }
        }
        throw new NoSuchFieldException(c.getName()+"."+name+":"+type.getName());
    }

    private static int ub(Object o,String name) throws Exception {
        return field(o.getClass(),name,Byte.TYPE).getByte(o)&255;
    }

    private static dw userTeam(){
        try{return pfmPlayerHistoryV2.userTeam();}
        catch(Throwable t){return null;}
    }

    private static int squadCount(dw team) throws Exception {
        if(squadCountMethod==null){
            squadCountMethod=pfmPlayerHistoryV2.class.getDeclaredMethod("squadCount",dw.class);
            squadCountMethod.setAccessible(true);
        }
        return ((Integer)squadCountMethod.invoke(null,team)).intValue();
    }

    private static int playerId(dw team,int index) throws Exception {
        if(playerIdMethod==null){
            playerIdMethod=pfmPlayerHistoryV2.class.getDeclaredMethod("playerId",dw.class,Integer.TYPE);
            playerIdMethod.setAccessible(true);
        }
        return ((Integer)playerIdMethod.invoke(null,team,Integer.valueOf(index))).intValue();
    }

    private static ci playerAt(dw team,int index) throws Exception {
        if(playerAtMethod==null){
            playerAtMethod=cp.class.getMethod("a",dw.class,Integer.TYPE);
            playerAtMethod.setAccessible(true);
        }
        return (ci)playerAtMethod.invoke(null,team,Integer.valueOf(index));
    }

    private static String text(byte[] b){
        if(b==null)return "";
        try{return new String(b,"ISO-8859-1").replace('\n',' ').trim();}
        catch(Throwable t){return new String(b).replace('\n',' ').trim();}
    }

    private static String pos(int p){
        switch(p){
            case 0:return "GK";
            case 1:return "DEF";
            case 2:return "MID";
            case 3:return "FW";
            default:return "?";
        }
    }

    private static void invokeEnsure(Class<?> c){
        try{
            Method m=c.getDeclaredMethod("ensure");
            m.setAccessible(true);
            m.invoke(null);
        }catch(Throwable ignored){}
    }

    private static int staticByte(Class<?> c,String name,int id){
        if(id<0||id>=512)return 0;
        try{
            invokeEnsure(c);
            Field f=field(c,name,byte[].class);
            byte[] a=(byte[])f.get(null);
            if(a==null||id>=a.length)return 0;
            return a[id]&255;
        }catch(Throwable t){return 0;}
    }

    public static boolean available(){
        try{
            dw t=userTeam();
            return t!=null&&squadCount(t)>0;
        }catch(Throwable t){return false;}
    }

    public static String teamName(){
        try{
            dw t=userTeam();
            if(t==null)return "Squad";
            byte[] name=(byte[])field(dw.class,"a",byte[].class).get(t);
            String s=text(name);
            return s.length()==0?"Squad":s;
        }catch(Throwable t){return "Squad";}
    }

    /**
     * The legacy match substitution screen sets dg.c to the remaining number of
     * live substitutions. A positive value means this is not a safe place to
     * edit the pre-match squad order from the native screen.
     */
    public static boolean canEditLineup(){
        try{
            if(!available())return false;
            Field f=field(dg.class,"c",Byte.TYPE);
            return (f.getByte(null)&255)==0;
        }catch(Throwable t){
            return false;
        }
    }

    /**
     * Swap any two squad-order positions. The legacy dg Line-up editor uses the
     * same primitive operation: swap dw.a player IDs and, when the user's team
     * is the currently loaded cp team, swap the matching live eg[] entries too.
     * Slots 0..10 are the Starting XI; slots 11+ are bench/reserves.
     * Formation is intentionally untouched.
     *
     * Return: 1 success; -1 invalid/core unavailable; -3 native pre-match
     * editing is unavailable (typically a live match).
     */
    public static int swapLineupPositions(int first,int second){
        try{
            dw team=userTeam();
            if(team==null)return -1;
            int n=squadCount(team);
            if(first<0||second<0||first>=n||second>=n||first==second)return -1;
            if(!canEditLineup())return -3;

            short[] order=(short[])field(dw.class,"a",short[].class).get(team);
            if(order==null||first>=order.length||second>=order.length)return -1;
            short s=order[first]; order[first]=order[second]; order[second]=s;

            // Match the old dg editor exactly when this team is the active cp team.
            try{
                dw active=(dw)field(cp.class,"a",dw.class).get(null);
                eg[] live=(eg[])field(cp.class,"a",eg[].class).get(null);
                if(active==team&&live!=null&&first<live.length&&second<live.length){
                    eg e=live[first]; live[first]=live[second]; live[second]=e;
                }
            }catch(Throwable ignored){}
            return 1;
        }catch(Throwable t){
            return -1;
        }
    }

    /**
     * TSV columns:
     * playerId, number, name, pos, spe, res, qua, morale, age,
     * goals, starts, appearances, assists, rating10, ratingMatches,
     * fatigue, injuryRounds, yellow, red, suspensionRounds.
     */
    public static String[] rows(){
        try{
            dw team=userTeam();
            if(team==null)return new String[0];
            int n=squadCount(team);
            String[] out=new String[n];
            for(int i=0;i<n;i++){
                int id=playerId(team,i);
                ci p=playerAt(team,i);
                if(p==null){
                    out[i]=id+"\t0\tUnknown\t?\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0";
                    continue;
                }
                String name=pfmPlayerName60.name(p);
                if(name==null||name.length()==0)
                    name=text((byte[])field(ci.class,"a",byte[].class).get(p));
                int number=ub(p,"f");
                int position=ub(p,"b");
                int spe=ub(p,"c");
                int res=ub(p,"d");
                int qua=ub(p,"e");
                int goals=ub(p,"g");
                int mor=ub(p,"h");
                int age=ub(p,"a");
                int starts=pfmPlayerStats.starts(id);
                int apps=pfmPlayerStats.appearances(id);
                int assists=pfmContrib80.assists(id);
                int ratingMatches=pfmPlayerStats.ratingMatches(id);
                int rat10=pfmPlayerStats.averageRating10(id);
                int fatigue=staticByte(pfmCondition60.class,"fatigue",id);
                int injury=staticByte(pfmCondition60.class,"injury",id);
                int suspension=0;
                try{suspension=pfmDiscipline70.roundsOut(id);}catch(Throwable ignored){}
                int yellow=staticByte(pfmDiscipline70.class,"yellow",id);
                int red=staticByte(pfmDiscipline70.class,"red",id);
                out[i]=id+"\t"+number+"\t"+name.replace('\t',' ')+"\t"+pos(position)+"\t"+
                        spe+"\t"+res+"\t"+qua+"\t"+mor+"\t"+age+"\t"+
                        goals+"\t"+starts+"\t"+apps+"\t"+assists+"\t"+rat10+"\t"+ratingMatches+"\t"+
                        fatigue+"\t"+injury+"\t"+yellow+"\t"+red+"\t"+suspension;
            }
            return out;
        }catch(Throwable t){
            return new String[]{"-1\t0\tSquad unavailable: "+t.getClass().getSimpleName()+"\t?\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0"};
        }
    }
}
