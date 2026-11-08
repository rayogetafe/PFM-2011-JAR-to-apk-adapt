package android.content;
import java.io.*;
import java.util.*;
public class Context {
    public static final int MODE_PRIVATE=0;
    private final String root;private final File files;
    private final HashMap<String,SharedPreferences> preferences=new HashMap<>();
    public Context(String root,File files){this.root=root;this.files=files;}
    public Context getApplicationContext(){return this;}
    public File getFilesDir(){return files;}
    public SharedPreferences getSharedPreferences(String name,int mode){return preferences.computeIfAbsent(name, key -> new SharedPreferences.Memory());}
    public Assets getAssets(){return new Assets();}
    public class Assets {public InputStream open(String name)throws IOException{return new FileInputStream(new File(root,name));}}
}
