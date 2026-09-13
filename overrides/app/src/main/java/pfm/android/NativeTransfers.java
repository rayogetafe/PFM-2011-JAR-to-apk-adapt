package pfm.android;

import android.app.Activity;
import android.widget.Toast;

/**
 * Compatibility stub for older entry points. The experimental native transfer
 * hand-off is intentionally retired: the original JAR menus remain the only
 * transaction UI and therefore keep their own controller stack intact.
 */
public final class NativeTransfers {
    private NativeTransfers() {}
    public static void show(Activity activity) {
        Toast.makeText(activity,"Use the original New players menu for transfers",Toast.LENGTH_LONG).show();
    }
}
