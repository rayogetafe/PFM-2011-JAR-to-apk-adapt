package pfm.android;
import android.app.*;
import android.graphics.Typeface;
import android.widget.*;
import android.view.Window;

/** Group navigation by the manager's task; keep the squad list clear of stacked tools. */
public final class NativeTeamMenu {
    private NativeTeamMenu(){}
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static void heading(Activity a,LinearLayout root,String title){TextView v=new TextView(a);v.setText(title);v.setTextSize(13);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(a,8),dp(a,12),dp(a,8),dp(a,4));root.addView(v);}
    private static void action(Activity a,LinearLayout root,String label,AlertDialog[] dialog,Runnable open){Button b=new Button(a);b.setText(label);root.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->{open.run();dialog[0].dismiss();});}
    public static void show(Activity a){show(a,false);}
    public static void showCompetitions(Activity a){show(a,true);}
    private static void show(Activity a,boolean competitions){final boolean pause=NativePause.begin();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),0,dp(a,8),dp(a,8));final AlertDialog[] dialog=new AlertDialog[1];
        if(!competitions){
            heading(a,root,"SELECTION & MATCH PREPARATION");action(a,root,"Line-up & tactics",dialog,()->NativeLineup.show(a));action(a,root,"Starting XI policy",dialog,()->NativeSquadPolicy.show(a));action(a,root,"Matchday squad & automatic plan",dialog,()->NativeMatchday.show(a));
            heading(a,root,"PLAYERS & CONTRACTS");action(a,root,"Contracts",dialog,()->NativeContracts.show(a));action(a,root,"Transfer offers for your players",dialog,()->NativeSales.show(a));action(a,root,"Player careers",dialog,()->NativePlayerCareer.show(a));
            heading(a,root,"RESULTS & WORKLOAD");action(a,root,"Actual rotation & substitutions",dialog,()->NativeSquadPolicy.showWorkload(a));action(a,root,"Team analytics",dialog,()->NativeTeamAnalytics.show(a));action(a,root,"Season & competitions",dialog,()->NativeSeasonHub.show(a));
            heading(a,root,"ADVANCED");action(a,root,"Data checks & diagnostics",dialog,()->NativeLeagueCenter.showAudit(a));
        }else{
            heading(a,root,"DOMESTIC");action(a,root,"Current league · results & clubs",dialog,()->NativeLeagueCenter.show(a));action(a,root,"Other top-five leagues",dialog,()->NativeWorldCompetition.show(a));
            heading(a,root,"EUROPE");action(a,root,"European qualification & competitions",dialog,()->NativeEuropeCenter.show(a));
        }
        ScrollView scroll=new ScrollView(a);scroll.addView(root);dialog[0]=new AlertDialog.Builder(a).setTitle(competitions?"Competitions":"Team menu").setView(scroll).setPositiveButton("Close",null).create();dialog[0].setOnDismissListener(v->NativePause.end(pause));dialog[0].setOnShowListener(v->{Window w=dialog[0].getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.85f));});dialog[0].show();
    }
}
