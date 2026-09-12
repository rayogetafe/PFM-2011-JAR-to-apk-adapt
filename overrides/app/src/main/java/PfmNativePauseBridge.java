import java.lang.reflect.Field;

/** Freezes/resumes the legacy PFM match loop while an Android-native overlay is open. */
public final class PfmNativePauseBridge {
    private static du pausedRuntime;
    private static int depth;
    private PfmNativePauseBridge() {}

    private static Field field(Class<?> c,String name,Class<?> type) throws Exception {
        for(Field f:c.getDeclaredFields()){
            if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);return f;}
        }
        throw new NoSuchFieldException(c.getName()+"."+name+":"+type.getName());
    }

    public static synchronized boolean begin(){
        try{
            if(depth>0&&pausedRuntime!=null){depth++;return true;}
            du runtime=(du)field(db.class,"a",du.class).get(null);
            if(runtime==null)return false;
            bl current=(bl)field(dd.class,"a",bl.class).get(runtime);
            if(!(current instanceof ca))return false;
            // du.d() is the stock pause primitive: it sets the dd loop pause flag
            // and pauses audio. ca does not auto-unpause itself via bl.d().
            runtime.d();
            pausedRuntime=runtime;
            depth=1;
            return true;
        }catch(Throwable t){
            pausedRuntime=null;depth=0;return false;
        }
    }

    public static synchronized void end(boolean token){
        if(!token)return;
        if(depth>0)depth--;
        if(depth>0)return;
        du runtime=pausedRuntime;
        pausedRuntime=null;
        depth=0;
        if(runtime!=null){
            try{runtime.e();}catch(Throwable ignored){}
            try{runtime.j();}catch(Throwable ignored){}
        }
    }
}
