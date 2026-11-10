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
            enterMenu(runtime);
            if ("save".equals(args.getString("phase"))) {
                playAndSave(runtime);
                loadRound(runtime);
                result.putString("stream", "PASS: real first matchday played and saved on Android\n");
            } else {
                loadRound(runtime);
                runOnMainSync(activity::finish);
                for (int i=0; i<50 && !activity.isDestroyed(); i++) Thread.sleep(100);
                if (!activity.isDestroyed()) throw new AssertionError("original activity still open");
                startActivitySync(intent);
                Object restarted = field("bu", "a", core("du")).get(null);
                if (runtime == restarted) throw new AssertionError("relaunch kept the detached game canvas");
                enterMenu(restarted);
                loadRound(restarted);
                result.putString("stream", "PASS: Android Load Game reached the board after a process restart\n");
                result.putString("stream", result.getString("stream") +
                        "PASS: Load Game also works after recreating the Android activity in the same process\n");
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
    private Object awaitScreen(Object runtime, String type, String fieldName, String fieldType) throws Exception {
        Field active = field("dd", "a", core("bl"));
        Field component = field(type, fieldName, core(fieldType));
        for (int i = 0; i < 150; i++) {
            Object screen = active.get(runtime);
            if (core(type).isInstance(screen) && component.get(screen) != null) return screen;
            Thread.sleep(100);
        }
        throw new AssertionError("expected screen " + type + ", found " + active.get(runtime));
    }
    private void fire(Object menu, int row) throws Exception {
        call("dv", menu, "e", new Class<?>[]{int.class}, row);
        field("dd", "i", boolean.class).setBoolean(null, true);
    }
    private void awaitMatchPhase(Object match, int phase) throws Exception {
        Field current = field("ca", "c", byte.class);
        for (int i=0;i<150;i++) {
            if (current.getByte(match) == phase) {
                Thread.sleep(500);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("expected match phase " + phase + ", found " + current.getByte(match));
    }
    private void checkRound() throws Exception {
        Object played = call("cp", null, "pfmPlayedForTable", none);
        if (!Integer.valueOf(1).equals(played)) throw new AssertionError("wrong saved round " + played);
    }
    private void loadRound(Object runtime) throws Exception {
        int row = field("du", "v", boolean.class).getBoolean(null) ? 2 : 1;
        Object main = awaitScreen(runtime, "dy", "a", "v");
        fire(field("dy", "a", core("v")).get(main), row);
        awaitScreen(runtime, "i", "a", "v");
        checkRound();
    }
    private void playAndSave(Object runtime) throws Exception {
        call("pfm2", null, "slot0", none);
        call("pfm2", null, "setMode", new Class<?>[]{int.class}, 1);
        try (DataInputStream in = new DataInputStream(core("cp").getResourceAsStream("/resources/england.bin"))) {
            call("cp", null, "a", new Class<?>[]{DataInputStream.class, boolean.class}, in, true);
        }
        call("cp", null, "a", new Class<?>[]{byte.class}, (byte)3);
        call("pfmRound", null, "startFirstRound", none);
        call("db", null, "b", new Class<?>[]{byte.class}, (byte)3);
        Object main = awaitScreen(runtime, "dy", "a", "v");
        Object manager = field("du", "a", core("ed")).get(null);
        call("ed", manager, "c", new Class<?>[]{core("dv")}, field("dy", "a", core("v")).get(main));
        field("ca", "b", byte.class).setByte(null, (byte)6); // Return to the career board after the match.
        call("dd", runtime, "a", new Class<?>[]{byte.class}, (byte)22);
        Object match = awaitScreen(runtime, "ca", "a", "v");
        Field menu = field("ca", "a", core("v"));
        fire(menu.get(match), 0);
        awaitMatchPhase(match, 1);
        fire(menu.get(match), 1);
        awaitMatchPhase(match, 4);
        fire(menu.get(match), 1);
        Object board = awaitScreen(runtime, "i", "a", "v");
        checkRound();
        fire(field("i", "a", core("v")).get(board), 6);
        awaitScreen(runtime, "i", "b", "ei");
        Object confirmation = field("i", "b", core("ei")).get(board);
        call("i", board, "a", new Class<?>[]{Object.class, int.class}, confirmation, 0);
        enterMenu(runtime);
        File save = new File(getTargetContext().getFilesDir(), "rms/P2S001.rms");
        if (save.length() < 1024) throw new AssertionError("Save Game did not write a career file");
    }
}
