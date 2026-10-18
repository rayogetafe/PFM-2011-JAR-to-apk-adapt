package android.content;
import java.io.*;
import java.util.*;
public class Context {
    private final String root;private final File files;
    private final HashMap<String,SharedPreferences> preferences=new HashMap<>();
    public Context(String root,File files){this.root=root;this.files=files;}
    public Context getApplicationContext(){return this;}
    public File getFilesDir(){return files;}
    public SharedPreferences getSharedPreferences(String name,int mode){if(!preferences.containsKey(name))preferences.put(name,new SharedPreferences());return preferences.get(name);}
    public Assets getAssets(){return new Assets();}
    public class Assets {public InputStream open(String name)throws IOException{return new FileInputStream(new File(root,name));}}
}
