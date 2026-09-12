package pfm.android;

import java.lang.reflect.Method;

/** Reflection wrapper for the default-package live-match pause bridge. */
final class NativePause {
    private NativePause() {}

    static boolean begin() {
        try {
            return Boolean.TRUE.equals(Class.forName("PfmNativePauseBridge").getMethod("begin").invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void end(boolean token) {
        if(!token)return;
        try {
            Method m=Class.forName("PfmNativePauseBridge").getMethod("end",Boolean.TYPE);
            m.invoke(null,Boolean.TRUE);
        } catch (Throwable ignored) {}
    }
}
