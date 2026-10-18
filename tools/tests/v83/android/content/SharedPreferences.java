package android.content;
import java.util.*;
public class SharedPreferences {
    private final HashMap<String,Object> values=new HashMap<>();
    public Map<String,?> getAll(){return values;}
    public String getString(String key,String fallback){Object v=values.get(key);return v instanceof String?(String)v:fallback;}
    public void put(String key,Object value){values.put(key,value);}
}
