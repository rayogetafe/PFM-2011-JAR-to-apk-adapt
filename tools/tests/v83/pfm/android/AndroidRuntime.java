package pfm.android;
import android.content.Context;
public class AndroidRuntime {
    static Context current;
    public static Context context() { return current; }
    public static android.app.Activity activity() { throw new UnsupportedOperationException("Headless test"); }
}
