import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Default-package bridge for native Android settings. It talks to the existing
 * pfmPatch38 persisted state without changing the match engine or save format.
 */
public final class PfmSettingsBridge {
    private static final String TAG = "PFM_SETTINGS";
    private static Method loadState;
    private static Method saveState;
    private static Method ensureControlMail;
    private static Field autoField;
    private static boolean resolved;

    private PfmSettingsBridge() {}

    private static synchronized boolean resolve() {
        if (resolved) return loadState != null && saveState != null && autoField != null;
        resolved = true;
        try {
            Class<?> patch = Class.forName("pfmPatch38");
            loadState = patch.getDeclaredMethod("loadState");
            loadState.setAccessible(true);

            Class<?> stateClass = Class.forName("pfmPatch38$State");
            saveState = patch.getDeclaredMethod("saveState", stateClass);
            saveState.setAccessible(true);

            try {
                ensureControlMail = patch.getDeclaredMethod("ensureControlMail");
                ensureControlMail.setAccessible(true);
            } catch (Throwable ignored) {
                ensureControlMail = null;
            }

            autoField = stateClass.getDeclaredField("auto");
            autoField.setAccessible(true);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Unable to resolve pfmPatch38 settings state", t);
            loadState = null;
            saveState = null;
            autoField = null;
            return false;
        }
    }

    public static synchronized boolean getAutoRotation() {
        try {
            if (!resolve()) return false;
            Object state = loadState.invoke(null);
            return state != null && autoField.getBoolean(state);
        } catch (Throwable t) {
            Log.e(TAG, "getAutoRotation failed", t);
            return false;
        }
    }

    /**
     * Persist the same flag used by the legacy AUTO ROTATION CONTROL mail.
     * Deliberately does not rebuild the current XI immediately, so this native
     * setting is safe to change even if the dialog is opened during a match.
     * The new value is used from the next matchday onward.
     */
    public static synchronized boolean setAutoRotation(boolean enabled) {
        try {
            if (!resolve()) return false;
            Object state = loadState.invoke(null);
            if (state == null) return false;
            autoField.setBoolean(state, enabled);
            saveState.invoke(null, state);
            if (ensureControlMail != null) {
                try { ensureControlMail.invoke(null); } catch (Throwable ignored) {}
            }
            return autoField.getBoolean(loadState.invoke(null)) == enabled;
        } catch (Throwable t) {
            Log.e(TAG, "setAutoRotation failed", t);
            return false;
        }
    }

    public static int getTransferFrequency(){try{return pfmTransferPolicy31.get();}catch(Throwable t){return 2;}}
    public static boolean setTransferFrequency(int value){try{return pfmTransferPolicy31.set(value);}catch(Throwable t){return false;}}
    public static boolean careerMode(){try{return pfm2.getMode()==1;}catch(Throwable t){return false;}}
}
