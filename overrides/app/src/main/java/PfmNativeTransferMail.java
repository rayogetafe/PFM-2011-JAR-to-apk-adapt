/** Writes committed native transfers into the original PFM mail list. */
public final class PfmNativeTransferMail {
    private PfmNativeTransferMail() {}
    private static java.lang.reflect.Field field(Class<?> c,String name,Class<?> type)throws Exception{for(java.lang.reflect.Field f:c.getDeclaredFields())if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}throw new NoSuchFieldException(name);}
    public static synchronized boolean post(String player,String from,String to,String fee,boolean big){
        try{
            String shortName=player==null?"PLAYER":player.trim();
            if(shortName.length()>12)shortName=shortName.substring(0,12);
            String subject=big?"BIG: "+shortName:shortName+" CHANGES THE TEAM.";
            String body=(big?"BIG TRANSFER\n\n":"TRANSFER\n\n")+player+" moves from "+from+" to "+to+" for "+fee+".";
            cr mail=(cr)field(db.class,"a",cr.class).get(null);byte context=field(cp.class,"a",Byte.TYPE).getByte(null);
            if(mail==null)return false;mail.a(new z((byte)1,context,subject,body),true);
            return true;
        }catch(Throwable ignored){return false;}
    }
}
