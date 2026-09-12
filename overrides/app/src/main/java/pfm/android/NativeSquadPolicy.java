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

    private static String summary(){
        int players=0,starters=0,starts=0,apps=0,played=0,highFatigue=0,unavailable=0;
        for(String row:rows())try{
            String[] p=row.split("\\t",-1);players++;int st=Integer.parseInt(p[10]);int ap=Integer.parseInt(p[11]);int fatigue=Integer.parseInt(p[15]);int injury=Integer.parseInt(p[16]);int suspension=Integer.parseInt(p[19]);
            starts+=st;apps+=ap;if(st>0)starters++;if(ap>played)played=ap;if(fatigue>=60)highFatigue++;if(injury>0||suspension>0)unavailable++;
        }catch(Throwable ignored){}
        int subApps=Math.max(0,apps-starts);double perMatch=played<=0?0.0:(double)subApps/(double)played;
        String rotation=starters<16?"narrow":(starters<=21?"balanced":"wide");
        String subs=played<=0?"not available":(perMatch<2.65?"slightly low":(perMatch<=2.95?"realistic":"high"));
        return "Season matches: "+played+"\nPlayers used in starting XI: "+starters+" / "+players+"  ("+rotation+")\nSubstitute appearances: "+subApps+"  •  "+String.format(Locale.US,"%.2f",perMatch)+" per match  ("+subs+")\nHigh fatigue (60+): "+highFatigue+"  •  Injured/suspended: "+unavailable;
    }

    public static void show(Activity a){
        final boolean pauseToken=NativePause.begin();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,10),dp(a,5),dp(a,10),dp(a,5));
        TextView mode=text(a,"",17f,true);TextView help=text(a,"",13f,false);TextView stats=text(a,summary(),14f,false);stats.setBackgroundColor(0xffeef4ee);
        root.addView(mode);root.addView(help);root.addView(stats);
        LinearLayout buttons=new LinearLayout(a);buttons.setOrientation(LinearLayout.HORIZONTAL);Button manual=new Button(a);manual.setText("MANUAL XI");Button automatic=new Button(a);automatic.setText("AUTO ROTATION");buttons.addView(manual,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));buttons.addView(automatic,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(buttons);
        Runnable refresh=()->{boolean on=auto();mode.setText("Current policy: "+(on?"AUTO ROTATION":"MANUAL XI"));help.setText(on?"The core may rebuild the XI before each matchday using availability, fatigue and form. Your formation is preserved.":"Your saved squad order remains the XI. Use LINE-UP to make every change yourself.");manual.setEnabled(on);automatic.setEnabled(!on);stats.setText(summary());};
        manual.setOnClickListener(v->{if(setAuto(false)){Toast.makeText(a,"Manual XI enabled for the next matchday",Toast.LENGTH_SHORT).show();refresh.run();}else Toast.makeText(a,"Could not save policy",Toast.LENGTH_SHORT).show();});
        automatic.setOnClickListener(v->{if(setAuto(true)){Toast.makeText(a,"Auto Rotation enabled for the next matchday",Toast.LENGTH_SHORT).show();refresh.run();}else Toast.makeText(a,"Could not save policy",Toast.LENGTH_SHORT).show();});
        refresh.run();AlertDialog dlg=new AlertDialog.Builder(a).setTitle(teamName()+" — Squad Policy").setView(root).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pauseToken));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);});dlg.show();
    }
}
