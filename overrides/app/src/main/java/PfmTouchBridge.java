import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * Bridge that lives in the legacy core's default package, so it can use the
 * original PFM menu model directly.  Android GameCanvas calls this class by
 * reflection because named Java packages cannot import the default package.
 */
public final class PfmTouchBridge {
    private static final String TAG = "PFM_TOUCH";
    private static ed cachedUi;

    private PfmTouchBridge() {}

    private static ed ui() {
        if (cachedUi != null) return cachedUi;
        try {
            Field[] fs = du.class.getDeclaredFields();
            for (int i=0;i<fs.length;i++) {
                Field f=fs[i];
                if (Modifier.isStatic(f.getModifiers()) && f.getType()==ed.class) {
                    f.setAccessible(true);
                    Object x=f.get(null);
                    if (x instanceof ed) {
                        cachedUi=(ed)x;
                        return cachedUi;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG,"cannot resolve ed UI manager",t);
        }
        return null;
    }

    /**
     * Select and activate the actual currently-active legacy menu item at x/y.
     * Returns false when the active controller is not a classic v/w menu or the
     * tap does not hit one of that menu's real selectable children.
     */
    public static boolean tapActiveMenu(int x,int y) {
        try {
            ed manager=ui();
            if (manager==null) return false;
            dv active=manager.a();
            if (!(active instanceof v)) return false;

            cb root=((aw)active).a;
            if (!(root instanceof ab)) return false;
            Vector items=((ab)root).b;
            if (items==null || items.size()==0) return false;

            int chosen=-1;
            int best=Integer.MAX_VALUE;
            StringBuffer trace=new StringBuffer();
            trace.append(active.getClass().getName())
                 .append(" n=").append(items.size())
                 .append(" sel=").append(active.d)
                 .append(" tap=").append(x).append(',').append(y);

            for (int i=0;i<items.size();i++) {
                Object o=items.elementAt(i);
                if (!(o instanceof cb)) continue;
                cb child=(cb)o;
                if (!child.f) continue;

                int left=child.k;
                int top=child.l;
                int right=left+Math.max(1,child.i);
                int bottom=top+Math.max(1,child.j);
                int cx=(left+right)/2;
                int cy=(top+bottom)/2;
                trace.append(" [").append(i).append(':')
                     .append(left).append(',').append(top).append('-')
                     .append(right).append(',').append(bottom).append(']');

                // Original buttons have shadows/angled ends.  Give the model
                // hitbox a small margin but keep selection scoped strictly to
                // the actual children of the active menu.
                if (x>=left-18 && x<=right+18 && y>=top-14 && y<=bottom+18) {
                    int d=Math.abs(y-cy)+Math.abs(x-cx)/8;
                    if (d<best) { best=d; chosen=i; }
                }
            }

            if (chosen<0) {
                // Some PFM containers clip a few pixels from their child bounds.
                // Permit a vertical-nearest fallback only when the tap is still
                // close to a real active-menu child.
                best=34;
                for (int i=0;i<items.size();i++) {
                    Object o=items.elementAt(i);
                    if (!(o instanceof cb)) continue;
                    cb child=(cb)o;
                    if (!child.f) continue;
                    int cy=child.l+Math.max(1,child.j)/2;
                    int d=Math.abs(y-cy);
                    if (d<best) { best=d; chosen=i; }
                }
            }

            if (chosen<0) {
                Log.d(TAG,trace.append(" -> MISS").toString());
                return false;
            }

            trace.append(" -> idx=").append(chosen);
            Log.d(TAG,trace.toString());

            // Use the game's own selection setter.  Then request FIRE through
            // the same dd.i flag that v.b() consumes on the next game update.
            // No synthetic UP/DOWN pulses are involved.
            active.e(chosen);
            dd.i=true;
            return true;
        } catch (Throwable t) {
            Log.w(TAG,"tapActiveMenu failed",t);
            return false;
        }
    }
}
