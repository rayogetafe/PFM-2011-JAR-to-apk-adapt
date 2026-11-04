package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** Live rotation/workload dashboard and persisted Manual XI / Auto Rotation control. */
public final class NativeSquadPolicy {
    private NativeSquadPolicy() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(c,8),dp(c,5),dp(c,8),dp(c,5));return v;}
    private static boolean auto(){try{return Boolean.TRUE.equals(Class.forName("PfmSettingsBridge").getMethod("getAutoRotation").invoke(null));}catch(Throwable t){return false;}}
    private static boolean setAuto(boolean value){try{return Boolean.TRUE.equals(Class.forName("PfmSettingsBridge").getMethod("setAutoRotation",Boolean.TYPE).invoke(null,Boolean.valueOf(value)));}catch(Throwable t){return false;}}
    private static String teamName(){try{return String.valueOf(Class.forName("PfmSquadBridge").getMethod("teamName").invoke(null));}catch(Throwable t){return "Squad";}}
    private static String[] rows(){try{return (String[])Class.forName("PfmSquadBridge").getMethod("rows").invoke(null);}catch(Throwable t){return new String[0];}}
    private static int playedMatches(){try{return ((Integer)Class.forName("PfmSeasonBridge").getMethod("playedMatches").invoke(null)).intValue();}catch(Throwable t){return 0;}}

    private static String summary(){int highFatigue=0,unavailable=0;for(String row:rows())try{String[] p=row.split("\\t",-1);if(Integer.parseInt(p[15])>=60)highFatigue++;if(Integer.parseInt(p[16])>0||Integer.parseInt(p[19])>0)unavailable++;}catch(Exception ignored){}
        return NativeMatchJournal.squadWorkload(AndroidRuntime.context())+"\n\nCURRENT SQUAD\nHigh fatigue (60+): "+highFatigue+" · Injured/suspended: "+unavailable;
    }

    public static void show(Activity a){
        show(a,false);
    }
    public static void showWorkload(Activity a){show(a,true);}
    private static void show(Activity a,boolean workload){
        final boolean pauseToken=NativePause.begin();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,10),dp(a,5),dp(a,10),dp(a,5));
        LinearLayout tabs=new LinearLayout(a);Button policy=new Button(a),actual=new Button(a);policy.setText("XI POLICY");actual.setText("ACTUAL WORKLOAD");tabs.addView(policy,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(actual,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);
        TextView mode=text(a,"",17f,true);TextView help=text(a,"",13f,false);TextView stats=text(a,summary(),14f,false);stats.setBackgroundColor(0xffeef4ee);
        root.addView(mode);root.addView(help);root.addView(stats);
        LinearLayout buttons=new LinearLayout(a);buttons.setOrientation(LinearLayout.HORIZONTAL);Button manual=new Button(a);manual.setText("MANUAL XI");Button automatic=new Button(a);automatic.setText("AUTO ROTATION");buttons.addView(manual,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));buttons.addView(automatic,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(buttons);
        Runnable refresh=()->{boolean on=auto();mode.setText("Current policy: "+(on?"AUTO ROTATION":"MANUAL XI"));help.setText(on?"The XI may be rebuilt before each matchday using availability, fatigue and form. Formation is preserved. Match substitutions are a separate decision.":"You choose the starting XI through LINE-UP. Automatic match substitutions remain enabled; forced and manual changes share the limit of three.");manual.setEnabled(on);automatic.setEnabled(!on);stats.setText(summary());};
        manual.setOnClickListener(v->{if(setAuto(false)){Toast.makeText(a,"Manual XI enabled for the next matchday",Toast.LENGTH_SHORT).show();refresh.run();}else Toast.makeText(a,"Could not save policy",Toast.LENGTH_SHORT).show();});
        automatic.setOnClickListener(v->{if(setAuto(true)){Toast.makeText(a,"Auto Rotation enabled for the next matchday",Toast.LENGTH_SHORT).show();refresh.run();}else Toast.makeText(a,"Could not save policy",Toast.LENGTH_SHORT).show();});
        final boolean[] showWorkload={workload};Runnable tab=()->{boolean facts=showWorkload[0];mode.setVisibility(facts?android.view.View.GONE:android.view.View.VISIBLE);help.setVisibility(facts?android.view.View.GONE:android.view.View.VISIBLE);buttons.setVisibility(facts?android.view.View.GONE:android.view.View.VISIBLE);stats.setVisibility(facts?android.view.View.VISIBLE:android.view.View.GONE);policy.setEnabled(facts);actual.setEnabled(!facts);if(facts)stats.setText(summary());};policy.setOnClickListener(v->{showWorkload[0]=false;tab.run();});actual.setOnClickListener(v->{showWorkload[0]=true;tab.run();});tab.run();
        refresh.run();AlertDialog dlg=new AlertDialog.Builder(a).setTitle(teamName()+" — Selection & Workload").setView(root).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pauseToken));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);});dlg.show();
    }
}
