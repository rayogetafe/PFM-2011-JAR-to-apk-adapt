import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/** Immutable v42 identity sidecar; does not change the legacy save/player binary layout. */
public final class pfmNationality42 {
    private static final HashMap<String,String> DATA=new HashMap<String,String>();
    private static boolean loaded;
    private pfmNationality42(){}
    private static String clean(String s){return s==null?"":s.trim().toLowerCase(java.util.Locale.US);}
    private static String key(String league,String club,String player){return clean(league)+'\t'+clean(club)+'\t'+clean(player);}
    private static synchronized void load(){
        if(loaded)return;loaded=true;InputStream in=null;BufferedReader r=null;
        try{in=pfmNationality42.class.getResourceAsStream("/resources/nationalities_v42.tsv");if(in==null)return;r=new BufferedReader(new InputStreamReader(in,"UTF-8"));String line;r.readLine();while((line=r.readLine())!=null){String[] p=line.split("\\t",-1);if(p.length>=4)DATA.put(key(p[0],p[1],p[2]),p[3]);}}catch(Throwable ignored){}finally{try{if(r!=null)r.close();else if(in!=null)in.close();}catch(Throwable ignored){}}
    }
    public static String get(String league,String club,String player){load();String n=DATA.get(key(league,club,player));return n==null?"Unknown":n;}
    public static int count(){load();return DATA.size();}
}
