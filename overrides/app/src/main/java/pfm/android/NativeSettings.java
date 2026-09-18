package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;

/** First native Android settings surface for the standalone PFM port. */
public final class NativeSettings {
    private NativeSettings() {}

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static Boolean readAuto() {
        try {
            Class<?> c=Class.forName("PfmSettingsBridge");
            Method m=c.getMethod("getAutoRotation");
            return (Boolean)m.invoke(null);
        } catch (Throwable t) { return null; }
    }

    private static boolean writeAuto(boolean enabled) {
        try {
            Class<?> c=Class.forName("PfmSettingsBridge");
            Method m=c.getMethod("setAutoRotation", Boolean.TYPE);
            Object r=m.invoke(null, Boolean.valueOf(enabled));
            return Boolean.TRUE.equals(r);
        } catch (Throwable t) { return false; }
    }

    private static int readTransfers(){try{return ((Integer)Class.forName("PfmSettingsBridge").getMethod("getTransferFrequency").invoke(null)).intValue();}catch(Throwable t){return 2;}}
    private static boolean writeTransfers(int value){try{return Boolean.TRUE.equals(Class.forName("PfmSettingsBridge").getMethod("setTransferFrequency",Integer.TYPE).invoke(null,Integer.valueOf(value)));}catch(Throwable t){return false;}}
    private static boolean careerMode(){try{return Boolean.TRUE.equals(Class.forName("PfmSettingsBridge").getMethod("careerMode").invoke(null));}catch(Throwable t){return false;}}
    private static String transferLabel(int v){String[] a={"OFF — no AI transfers","LOW — up to 2 deals per window","REALISTIC — up to 5 deals per window","HIGH — up to 8 deals per window","VERY HIGH — up to 12 deals per window"};return a[Math.max(0,Math.min(4,v))];}

    public static void show(Activity a) {
        final Boolean initial=readAuto();
        if (initial==null) {
            Toast.makeText(a,"PFM settings state is unavailable",Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout box=new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a,22),dp(a,10),dp(a,22),dp(a,6));

        TextView intro=new TextView(a);
        intro.setText("Native Android settings\nAlpha 0.84 game core");
        intro.setTextSize(14f);
        intro.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        intro.setPadding(0,0,0,dp(a,12));
        box.addView(intro);

        Switch auto=new Switch(a);
        auto.setText("Auto rotation");
        auto.setTextSize(18f);
        auto.setChecked(initial.booleanValue());
        box.addView(auto,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView status=new TextView(a);
        status.setText(initial.booleanValue()?"Status: ON":"Status: OFF");
        status.setTextSize(14f);
        status.setPadding(0,dp(a,8),0,dp(a,8));
        box.addView(status);

        TextView note=new TextView(a);
        note.setText("Uses the same saved flag as AUTO ROTATION CONTROL. " +
                "Changes apply from the next matchday. The legacy mail remains synchronized for now.");
        note.setTextSize(13f);
        note.setGravity(Gravity.START);
        box.addView(note);

        TextView transferTitle=new TextView(a);
        transferTitle.setText("Career transfer frequency");transferTitle.setTextSize(18f);transferTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);transferTitle.setPadding(0,dp(a,16),0,0);box.addView(transferTitle);
        final int transferInitial=readTransfers();
        TextView transferStatus=new TextView(a);transferStatus.setText(careerMode()?transferLabel(transferInitial):"SEASON MODE — transfers are disabled");transferStatus.setTextSize(14f);box.addView(transferStatus);
        SeekBar transfer=new SeekBar(a);transfer.setMax(4);transfer.setProgress(transferInitial);transfer.setEnabled(careerMode());box.addView(transfer,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView transferNote=new TextView(a);transferNote.setText("Controls the maximum number of committed native AI deals in each transfer window. Season Mode always keeps transfers off.");transferNote.setTextSize(13f);box.addView(transferNote);
        transfer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int v,boolean user){transferStatus.setText(transferLabel(v));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){int v=b.getProgress();if(!writeTransfers(v)){b.setProgress(readTransfers());Toast.makeText(a,"Could not save transfer frequency",Toast.LENGTH_SHORT).show();}else Toast.makeText(a,"Transfer frequency: "+transferLabel(v),Toast.LENGTH_SHORT).show();}});

        auto.setOnCheckedChangeListener((button,checked) -> {
            button.setEnabled(false);
            boolean ok=writeAuto(checked);
            if (!ok) {
                button.setOnCheckedChangeListener(null);
                button.setChecked(!checked);
                button.setOnCheckedChangeListener((b,c) -> {
                    b.setEnabled(false);
                    boolean ok2=writeAuto(c);
                    if (ok2) status.setText(c?"Status: ON":"Status: OFF");
                    b.setEnabled(true);
                });
                Toast.makeText(a,"Could not save Auto rotation",Toast.LENGTH_SHORT).show();
            } else {
                status.setText(checked?"Status: ON":"Status: OFF");
            }
            button.setEnabled(true);
        });

        new AlertDialog.Builder(a)
                .setTitle("PFM Settings")
                .setView(box)
                .setPositiveButton("Close",null)
                .show();
    }
}
