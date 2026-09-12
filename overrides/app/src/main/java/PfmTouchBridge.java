import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * Bridge that lives in the legacy core's default package, so it can use the
 * original PFM menu model directly. Android GameCanvas calls this class by
 * reflection because named Java packages cannot import the default package.
 */
public final class PfmTouchBridge {
    private static final String TAG = "PFM_TOUCH";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long VISUAL_SELECTION_DELAY_MS = 66L;
    private static ed cachedUi;
    private static boolean activationPending;
    private static int routeGeneration;

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
            // Prevent a second tap from changing selection while the previous
            // menu item is waiting for its short visual-selection frame window.
            if (activationPending) return true;

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

                // Original buttons have shadows/angled ends. Give the model
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

            // v16 established that the game's own menu model is the reliable
            // source of truth. Keep that selection path unchanged, but do not
            // FIRE in the same instant: give the old renderer about two 30-FPS
            // frames to repaint the newly selected row first. This fixes the
            // cosmetic flash where the old row stayed yellow while the correct
            // item's text already entered its active state.
            active.e(chosen);
            activationPending=true;
            MAIN.postDelayed(new Runnable() {
                public void run() {
                    dd.i=true;
                    activationPending=false;
                }
            },VISUAL_SELECTION_DELAY_MS);
            return true;
        } catch (Throwable t) {
            activationPending=false;
            Log.w(TAG,"tapActiveMenu failed",t);
            return false;
        }
    }

    /**
     * Enter the stock New players controller and select one of its real menu
     * items, but deliberately do not FIRE it. Synthetic FIRE can be consumed
     * by the next controller on a core polling boundary, producing a blank
     * unrelated screen. The user confirms with the visible Android OK button,
     * which is the exact stable path used by the original menus and Back.
     */
    public static boolean openPlayersMenuItem(final int item) {
        if(item<0||item>2)return false;
        try{
            Field runtimeField=null;
            for(Field f:db.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&f.getType()==du.class){f.setAccessible(true);runtimeField=f;break;}
            if(runtimeField==null)return false;
            final du runtime=(du)runtimeField.get(null);
            if(runtime==null)return false;
            final int generation=++routeGeneration;
            runtime.a((byte)20);
            MAIN.postDelayed(new Runnable(){int attempts;
                public void run(){
                    if(generation!=routeGeneration)return;
                    try{
                        Field activeField=null;
                        for(Field f:dd.class.getDeclaredFields())if(f.getType()==bl.class){f.setAccessible(true);activeField=f;break;}
                        Object controller=activeField==null?null:activeField.get(runtime);
                        ed manager=ui();dv active=manager==null?null:manager.a();
                        if(controller instanceof at&&active instanceof v){
                            active.e(item);
                            Log.i(TAG,"New players ready; selected item="+item+" for manual OK");
                            return;
                        }
                    }catch(Throwable ignored){}
                    if(++attempts<24)MAIN.postDelayed(this,40L);
                }
            },80L);
            return true;
        }catch(Throwable t){
            Log.w(TAG,"players route failed",t);
            return false;
        }
    }
}
