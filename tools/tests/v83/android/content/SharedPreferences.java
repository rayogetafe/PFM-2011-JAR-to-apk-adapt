package android.content;
import java.util.*;
public class SharedPreferences {
    private final HashMap<String,Object> values=new HashMap<>();
    public Map<String,?> getAll(){return values;}
    public String getString(String key,String fallback){Object v=values.get(key);return v instanceof String?(String)v:fallback;}
    public int getInt(String key,int fallback){Object v=values.get(key);return v instanceof Integer?(Integer)v:fallback;}
    public Editor edit(){return new Editor();}
    public class Editor {private final HashMap<String,Object> pending=new HashMap<>();public Editor putInt(String k,int v){pending.put(k,v);return this;}public Editor putString(String k,String v){pending.put(k,v);return this;}public Editor remove(String k){pending.put(k,null);return this;}public boolean commit(){for(Map.Entry<String,Object> entry:pending.entrySet())if(entry.getValue()==null)values.remove(entry.getKey());else values.put(entry.getKey(),entry.getValue());return true;}public void apply(){commit();}}
    public void put(String key,Object value){values.put(key,value);}
}
