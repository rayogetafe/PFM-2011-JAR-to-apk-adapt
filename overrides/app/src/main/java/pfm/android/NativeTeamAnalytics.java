package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Native season leaders and squad-depth overview. */
public final class NativeTeamAnalytics {
    private NativeTeamAnalytics() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(c,7),dp(c,5),dp(c,7),dp(c,5));return v;}
    private static String teamName(){try{return String.valueOf(Class.forName("PfmSquadBridge").getMethod("teamName").invoke(null));}catch(Throwable t){return "Team";}}
    private static final class P {String name,pos;int goals,starts,apps,assists,rating10,ratingMatches,injury,suspension;boolean available(){return injury==0&&suspension==0;}}
    private static List<P> players(){ArrayList<P> out=new ArrayList<>();try{String[] rows=(String[])Class.forName("PfmSquadBridge").getMethod("rows").invoke(null);for(String row:rows){String[] x=row.split("\\t",-1);P p=new P();p.name=x[2];p.pos=x[3];p.goals=Integer.parseInt(x[9]);p.starts=Integer.parseInt(x[10]);p.apps=Integer.parseInt(x[11]);p.assists=Integer.parseInt(x[12]);p.rating10=Integer.parseInt(x[13]);p.ratingMatches=Integer.parseInt(x[14]);p.injury=Integer.parseInt(x[16]);p.suspension=Integer.parseInt(x[19]);out.add(p);}}catch(Throwable ignored){}return out;}
    private static String top(List<P> source,Comparator<P> cmp,int mode){ArrayList<P> a=new ArrayList<>(source);Collections.sort(a,cmp);StringBuilder b=new StringBuilder();int shown=0;for(P p:a){if(shown==5)break;if(mode==4&&p.ratingMatches==0)continue;String value=mode==0?String.valueOf(p.goals):mode==1?String.valueOf(p.assists):mode==2?String.valueOf(p.apps):mode==3?String.valueOf(p.starts):String.format(Locale.US,"%.1f",p.rating10/10.0);b.append(shown+1).append(". ").append(p.name).append("  ").append(p.pos).append("  —  ").append(value).append('\n');shown++;}return shown==0?"No data yet\n":b.toString();}
    public static void show(Activity a){
        List<P> ps=players();if(ps.isEmpty()){Toast.makeText(a,"Start or load a career first",Toast.LENGTH_SHORT).show();return;}final boolean pauseToken=NativePause.begin();
        int[] total=new int[4],ready=new int[4];for(P p:ps){int i="GK".equals(p.pos)?0:"DEF".equals(p.pos)?1:"MID".equals(p.pos)?2:3;total[i]++;if(p.available())ready[i]++;}
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,9),dp(a,3),dp(a,9),dp(a,5));
        root.addView(text(a,"SQUAD DEPTH",13f,true));root.addView(text(a,"GK  "+ready[0]+" / "+total[0]+" ready     DEF  "+ready[1]+" / "+total[1]+" ready\nMID "+ready[2]+" / "+total[2]+" ready     FW   "+ready[3]+" / "+total[3]+" ready",14f,false));
        root.addView(text(a,"TOP SCORERS",13f,true));root.addView(text(a,top(ps,(x,y)->y.goals-x.goals,0),13f,false));
        root.addView(text(a,"TOP ASSISTS",13f,true));root.addView(text(a,top(ps,(x,y)->y.assists-x.assists,1),13f,false));
        root.addView(text(a,"BEST AVERAGE RATING",13f,true));root.addView(text(a,top(ps,(x,y)->y.rating10-x.rating10,4),13f,false));
        root.addView(text(a,"MOST APPEARANCES",13f,true));root.addView(text(a,top(ps,(x,y)->y.apps-x.apps,2),13f,false));
        root.addView(text(a,"MOST STARTS",13f,true));root.addView(text(a,top(ps,(x,y)->y.starts-x.starts,3),13f,false));
        ScrollView scroll=new ScrollView(a);scroll.addView(root);AlertDialog dlg=new AlertDialog.Builder(a).setTitle(teamName()+" — Team Analytics").setView(scroll).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pauseToken));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*0.92f));});dlg.show();
    }
}
