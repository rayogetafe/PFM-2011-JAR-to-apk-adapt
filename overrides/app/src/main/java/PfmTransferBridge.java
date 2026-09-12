import java.lang.reflect.Field;
import java.util.ArrayList;

/** Read-only native transfer-market data plus routes into the stock transaction screens. */
public final class PfmTransferBridge {
    private PfmTransferBridge() {}

    private static Field field(Class<?> c,String name,Class<?> type) throws Exception {
        for(Field f:c.getDeclaredFields())if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}
        throw new NoSuchFieldException(c.getName()+"."+name+":"+type.getName());
    }
    private static int ub(Object o,String name) throws Exception{return field(o.getClass(),name,Byte.TYPE).getByte(o)&255;}
    private static String text(byte[] b){if(b==null)return "";try{return new String(b,"ISO-8859-1").replace('\n',' ').trim();}catch(Throwable t){return new String(b).replace('\n',' ').trim();}}
    private static String teamName(dw t){try{return text((byte[])field(dw.class,"a",byte[].class).get(t));}catch(Throwable ignored){return "?";}}
    private static String playerName(ci p){try{String s=pfmPlayerName60.name(p);if(s!=null&&s.length()>0)return s.replace('\t',' ');}catch(Throwable ignored){}try{return text((byte[])field(ci.class,"a",byte[].class).get(p)).replace('\t',' ');}catch(Throwable ignored){return "Unknown";}}
    private static String pos(int p){switch(p){case 0:return "GK";case 1:return "DEF";case 2:return "MID";case 3:return "FW";default:return "?";}}
    private static dw userTeam(){try{return pfmPlayerHistoryV2.userTeam();}catch(Throwable ignored){return null;}}

    public static boolean available(){try{return userTeam()!=null&&field(cp.class,"a",dw[].class).get(null)!=null;}catch(Throwable ignored){return false;}}
    public static boolean transferOpen(){try{return pfm2.isTransferOpen();}catch(Throwable ignored){return false;}}
    public static int budget(){try{return field(dw.class,"g",Integer.TYPE).getInt(userTeam());}catch(Throwable ignored){return 0;}}

    /** TSV: playerId, name, club, position, speed, resistance, quality, morale, age, price. */
    public static String[] marketRows(){
        ArrayList<String> out=new ArrayList<String>();
        try{
            dw user=userTeam();dw[] teams=(dw[])field(cp.class,"a",dw[].class).get(null);ci[] players=(ci[])field(cp.class,"a",ci[].class).get(null);
            if(user==null||teams==null||players==null)return new String[0];
            for(dw team:teams){
                if(team==null||team==user)continue;
                int count=ub(team,"f");short[] listed=(short[])field(dw.class,"b",short[].class).get(team);if(listed==null)continue;count=Math.min(count,listed.length);
                for(int i=0;i<count;i++){
                    int id=listed[i]&65535;if(id>=players.length||players[id]==null)continue;ci p=players[id];int price=field(ci.class,"c",Integer.TYPE).getInt(p);
                    out.add(id+"\t"+playerName(p)+"\t"+teamName(team)+"\t"+pos(ub(p,"b"))+"\t"+ub(p,"c")+"\t"+ub(p,"d")+"\t"+ub(p,"e")+"\t"+ub(p,"h")+"\t"+ub(p,"a")+"\t"+price);
                }
            }
        }catch(Throwable ignored){}
        return out.toArray(new String[out.size()]);
    }

    /** 33 Buy players, 41 Search, 34 Sell players in the stock du screen map. */
    public static boolean openLegacy(int screen){
        if(screen!=33&&screen!=41&&screen!=34)return false;
        try{int item=screen==33?0:(screen==41?1:2);return PfmTouchBridge.openPlayersMenuItem(item);}catch(Throwable ignored){return false;}
    }
}
