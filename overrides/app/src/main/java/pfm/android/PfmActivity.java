package pfm.android;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PfmActivity extends Activity {
    private static final String TAG = "PFM2011Android";
    private Object gameMidlet;
    private Method startApp;
    private Method pauseApp;
    private boolean launched;
    private boolean paused;
    private Thread.UncaughtExceptionHandler previousHandler;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        AndroidRuntime.attach(this);
        installCrashScreen();
        try {
            Class<?> c = Class.forName("midlet");
            gameMidlet = c.getDeclaredConstructor().newInstance();
            startApp = c.getDeclaredMethod("startApp");
            startApp.setAccessible(true);
            pauseApp = c.getDeclaredMethod("pauseApp");
            pauseApp.setAccessible(true);
            startApp.invoke(gameMidlet);
            launched = true;
        } catch (Throwable t) {
            showFatal("PFM startup failed", unwrap(t));
        }
    }

    private void installCrashScreen() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception on " + thread.getName(), throwable);
            final Throwable root = unwrap(throwable);
            runOnUiThread(() -> showFatal("PFM runtime crash on " + thread.getName(), root));
        });
    }

    private static Throwable unwrap(Throwable t) {
        Throwable x = t;
        while (x instanceof InvocationTargetException && x.getCause() != null) x = x.getCause();
        return x;
    }

    private void showFatal(String title, Throwable t) {
        Log.e(TAG, title, t);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(title);
        pw.println();
        if (t != null) t.printStackTrace(pw);
        pw.flush();

        TextView text = new TextView(this);
        text.setText(sw.toString());
        text.setTextColor(Color.WHITE);
        text.setBackgroundColor(Color.rgb(35, 0, 0));
        text.setTextSize(13f);
        text.setPadding(24, 24, 24, 24);
        text.setGravity(Gravity.START);
        text.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(text);
        setContentView(scroll);
    }

    @Override protected void onPause() {
        if (launched && gameMidlet != null && pauseApp != null) {
            try { pauseApp.invoke(gameMidlet); } catch (Throwable t) { Log.e(TAG, "pauseApp failed", unwrap(t)); }
            paused = true;
        }
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        AndroidRuntime.attach(this);
        if (paused && launched && gameMidlet != null && startApp != null) {
            try { startApp.invoke(gameMidlet); } catch (Throwable t) { showFatal("PFM resume failed", unwrap(t)); }
            paused = false;
        }
    }

    @Override protected void onDestroy() {
        if (Thread.getDefaultUncaughtExceptionHandler() != previousHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
        super.onDestroy();
    }

    public void finishFromGame() { finish(); }
}
