package pfm.android.compat.lcdui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import pfm.android.AndroidRuntime;
import pfm.android.NativeSettings;
import pfm.android.NativeSquad;
import pfm.android.compat.lcdui.game.GameCanvas;
import pfm.android.compat.midlet.MIDlet;

public final class Display {
    private static final Display INSTANCE = new Display();
    private volatile Displayable current;
    private Display() {}
    public static Display getDisplay(MIDlet ignored) { return INSTANCE; }
    public Displayable getCurrent() { return current; }

    private static int dp(Activity a,int v) {
        return Math.round(v*a.getResources().getDisplayMetrics().density);
    }

    private static Button nativeButton(Activity a,String label){
        Button b=new Button(a);
        b.setText(label);
        b.setTextSize(12f);
        b.setAllCaps(false);
        b.setAlpha(0.92f);
        b.setPadding(dp(a,8),0,dp(a,8),0);
        return b;
    }

    private static View buildAndroidRoot(GameCanvas gc) {
        Activity a=AndroidRuntime.activity();
        View game=gc.androidView();
        ViewParent parent=game.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).removeView(game);

        FrameLayout root=new FrameLayout(a);
        root.addView(game,new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));

        Button settings=nativeButton(a,"SETTINGS");
        settings.setOnClickListener(v -> NativeSettings.show(a));
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(a,108),dp(a,38));
        sp.gravity=Gravity.TOP|Gravity.START;
        sp.leftMargin=dp(a,8);
        sp.topMargin=dp(a,8);
        root.addView(settings,sp);

        Button squad=nativeButton(a,"SQUAD");
        squad.setOnClickListener(v -> NativeSquad.show(a));
        FrameLayout.LayoutParams qp=new FrameLayout.LayoutParams(dp(a,108),dp(a,38));
        qp.gravity=Gravity.TOP|Gravity.END;
        qp.rightMargin=dp(a,8);
        qp.topMargin=dp(a,8);
        root.addView(squad,qp);

        return root;
    }

    public void setCurrent(Displayable next) {
        Displayable prev = current;
        if (next instanceof Alert) {
            current = next;
            ((Alert)next).show(() -> current = prev);
            return;
        }
        if (prev instanceof GameCanvas) ((GameCanvas)prev).markShown(false);
        if (prev != null) prev.hideNotify();
        current = next;
        AndroidRuntime.main().post(() -> {
            if (next instanceof GameCanvas) {
                GameCanvas gc = (GameCanvas) next;
                AndroidRuntime.activity().setContentView(buildAndroidRoot(gc));
                gc.markShown(true);
                gc.showNotifyFromDisplay();
            }
        });
    }
}
