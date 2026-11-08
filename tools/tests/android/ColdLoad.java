package pfm.diagnostic;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import java.io.*;
import java.lang.reflect.*;

public final class ColdLoad extends Instrumentation {
    private Bundle args;
    private ClassLoader loader;
    @Override public void onCreate(Bundle args) { this.args = args; start(); }
    private Class<?> core(String name) throws Exception { return Class.forName(name, false, loader); }
    private Field field(String owner, String name, Class<?> type) throws Exception {
        for (Field f : core(owner).getDeclaredFields()) if (f.getName().equals(name) && f.getType() == type) {
            f.setAccessible(true); return f;
        }
        throw new NoSuchFieldException(owner + "." + name);
    }
    private Object call(String owner, Object receiver, String name, Class<?>[] types, Object... args) throws Exception {
        for (Method m : core(owner).getMethods()) if (m.getName().equals(name) && java.util.Arrays.equals(m.getParameterTypes(), types)
                && (receiver != null || Modifier.isStatic(m.getModifiers()))) {
            m.setAccessible(true); return m.invoke(receiver, args);
        }
        throw new NoSuchMethodException(owner + "." + name);
    }
    private final Class<?>[] none = {};
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            loader = getTargetContext().getClassLoader();
            Intent intent = new Intent().setClassName(getTargetContext(), "pfm.android.PfmActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = startActivitySync(intent);
            Object runtime = field("bu", "a", core("du")).get(null);
            Object mainMenu = enterMenu(runtime);
            if ("save".equals(args.getString("phase"))) {
                save();
                result.putString("stream", "PASS: career fixture saved on Android\n");
            } else {
                Object manager = field("du", "a", core("ed")).get(null);
                Object menu = null;
                for (Method m : core("ed").getMethods()) if (m.getName().equals("a") &&
                        m.getParameterCount() == 0 && m.getReturnType() == core("dv")) menu = m.invoke(manager);
                if (!core("v").isInstance(menu)) throw new AssertionError("main menu unavailable " + menu);
                int row = field("du", "v", boolean.class).getBoolean(null) ? 2 : 1;
                call("dv", menu, "e", new Class<?>[]{int.class}, row);
                field("dd", "i", boolean.class).setBoolean(null, true);
                for (int i=0; i<80 && !core("i").isInstance(field("dd", "a", core("bl")).get(runtime)); i++) Thread.sleep(100);
                if (!core("i").isInstance(field("dd", "a", core("bl")).get(runtime)))
                    throw new AssertionError("cold Load Game did not reach the career board");
                Object played = call("cp", null, "pfmPlayedForTable", none);
                if (!Integer.valueOf(1).equals(played)) throw new AssertionError("wrong saved round " + played);
                result.putString("stream", "PASS: Android Load Game reached the board after a process restart\n");
            }
            finish(Activity.RESULT_OK, result);
        } catch (Throwable e) {
            android.util.Log.e("PFM-COLD-TEST", "Career load test failed", e);
            result.putString("stream", android.util.Log.getStackTraceString(e));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private Object enterMenu(Object runtime) throws Exception {
        Field active = field("dd", "a", core("bl"));
        for (int i=0;i<100;i++) {
            Object screen = active.get(runtime);
            if (core("dy").isInstance(screen) && field("dy", "a", core("v")).get(screen) != null) return screen;
            if (i % 15 == 5) field("dd", "i", boolean.class).setBoolean(null, true);
            Thread.sleep(100);
        }
        throw new AssertionError("could not reach the main menu " + active.get(runtime));
    }
    private void save() throws Exception {
        call("pfm2", null, "slot0", none);
        call("pfm2", null, "setMode", new Class<?>[]{int.class}, 1);
        try (DataInputStream in = new DataInputStream(core("cp").getResourceAsStream("/resources/england.bin"))) {
            call("cp", null, "a", new Class<?>[]{DataInputStream.class, boolean.class}, in, true);
        }
        call("cp", null, "a", new Class<?>[]{byte.class}, (byte)3);
        call("pfmRound", null, "startFirstRound", none);
        field("cp", "a", byte.class).setByte(null, (byte)1);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!Boolean.TRUE.equals(call("cp", null, "a", new Class<?>[]{DataOutputStream.class}, new DataOutputStream(buffer))))
            throw new AssertionError("legacy save failed");
        Object store = call("pfm2", null, "openStoreForSave", none);
        byte[] bytes = buffer.toByteArray();
        String rms = "pfm.android.compat.rms.RecordStore";
        call("PfmDomesticReplay", null, "add", new Class<?>[]{core(rms), byte[].class, int.class, int.class}, store, bytes, 0, bytes.length);
        call(rms, store, "closeRecordStore", none);
    }
}
