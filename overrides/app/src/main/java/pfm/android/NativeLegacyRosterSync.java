package pfm.android;

import android.content.Context;

/** Public load-boundary entry point used by the default-package legacy bridges. */
public final class NativeLegacyRosterSync {
    private NativeLegacyRosterSync() {}
    public static synchronized String reconcile(Context context){
        try{
            NativeWorldCenter.load(context);
            NativeCareerStore.Career career=NativeCareerStore.ensure(context);
            return NativeCareerStore.syncLegacy(career);
        }catch(Throwable t){return "legacy matchday sync unavailable: "+t.getClass().getSimpleName();}
    }
}
