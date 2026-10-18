package android.content;
import java.io.*;
public class Context {
    private final String root;
    public Context(String root){this.root=root;}
    public Assets getAssets(){return new Assets();}
    public class Assets {public InputStream open(String name)throws IOException{return new FileInputStream(new File(root,name));}}
}
