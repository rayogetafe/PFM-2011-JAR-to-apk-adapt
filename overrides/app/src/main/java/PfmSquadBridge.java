import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Read-only Android bridge to the current user's real PFM squad. */
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
     * TSV columns: playerId, number, name, pos, spe, res, qua, morale, age,
     * starts, appearances, assists, rating10.
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
                    out[i]=id+"\t0\tUnknown\t?\t0\t0\t0\t0\t0\t0\t0\t0\t0";
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
                int mor=ub(p,"h");
                int age=ub(p,"a");
                int starts=pfmPlayerStats.starts(id);
                int apps=pfmPlayerStats.appearances(id);
                int assists=pfmContrib80.assists(id);
                int rat10=pfmPlayerStats.averageRating10(id);
                out[i]=id+"\t"+number+"\t"+name.replace('\t',' ')+"\t"+pos(position)+"\t"+
                        spe+"\t"+res+"\t"+qua+"\t"+mor+"\t"+age+"\t"+
                        starts+"\t"+apps+"\t"+assists+"\t"+rat10;
            }
            return out;
        }catch(Throwable t){
            return new String[]{"-1\t0\tSquad unavailable: "+t.getClass().getSimpleName()+"\t?\t0\t0\t0\t0\t0\t0\t0\t0\t0"};
        }
    }
}
