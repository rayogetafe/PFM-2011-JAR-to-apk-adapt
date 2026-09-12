package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Matchday squad, deterministic automatic-substitution plan and persistent event reports. */
public final class NativeMatchday {
    private NativeMatchday() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge()throws Exception{return Class.forName("PfmMatchdayBridge");}
    private static boolean bool(String n){try{return Boolean.TRUE.equals(bridge().getMethod(n).invoke(null));}catch(Throwable t){return false;}}
    private static String string(String n){try{return String.valueOf(bridge().getMethod(n).invoke(null));}catch(Throwable t){return "";}}
    private static String[] rows(String n){try{return (String[])bridge().getMethod(n).invoke(null);}catch(Throwable t){return new String[0];}}

    private static final class SquadRow{int slot,minutes,spe,res,qua,fat;String name="",pos="",status="";static SquadRow parse(String s){SquadRow r=new SquadRow();try{String[] p=s.split("\\t",-1);r.slot=Integer.parseInt(p[0]);r.name=p[2];r.pos=p[3];r.minutes=Integer.parseInt(p[4]);r.spe=Integer.parseInt(p[5]);r.res=Integer.parseInt(p[6]);r.qua=Integer.parseInt(p[7]);r.fat=Integer.parseInt(p[8]);r.status=p[9];}catch(Throwable ignored){}return r;}String section(){return slot<11?"XI "+(slot+1):(slot<18?"BENCH "+(slot-10):"RES "+(slot-17));}}
    private static final class SubRow{int minute;String out="",in="";static SubRow parse(String s){SubRow r=new SubRow();try{String[] p=s.split("\\t",-1);r.minute=Integer.parseInt(p[0]);r.out=p[1];r.in=p[2];}catch(Throwable ignored){}return r;}}
    private static String firstLine(String s){int n=s.indexOf('\n');return n<0?s:s.substring(0,n);}

    public static void show(Activity a){
        if(!bool("available")){Toast.makeText(a,"Matchday squad is unavailable until a career is loaded",Toast.LENGTH_SHORT).show();return;}
        final boolean pause=NativePause.begin();final boolean live=bool("live");
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,2),dp(a,8),0);
        TextView opponent=text(a,string("opponent"),14f,true);opponent.setPadding(dp(a,6),dp(a,3),dp(a,6),dp(a,3));root.addView(opponent);
        TextView hint=text(a,live?"MATCH PAUSED • Make live changes through the original Options → Substitutions screen after closing this panel.":"The first XI starts. All eligible outfield reserves can be selected by the deterministic auto-sub engine; reserve goalkeepers are excluded.",11.5f,false);hint.setPadding(dp(a,6),dp(a,2),dp(a,6),dp(a,4));hint.setBackgroundColor(live?0xffffefc2:0xfff2f2f2);root.addView(hint);
        LinearLayout tabs=new LinearLayout(a);Button squad=new Button(a);squad.setText("MATCH SQUAD");Button subs=new Button(a);subs.setText("AUTO SUBS");Button events=new Button(a);events.setText("EVENTS");tabs.addView(squad,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));tabs.addView(subs,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));tabs.addView(events,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(tabs);
        Button lineup=new Button(a);lineup.setText(live?"LINE-UP (VIEW ONLY)":"EDIT LINE-UP / BENCH");root.addView(lineup);
        TextView head=text(a,"",11f,true);head.setPadding(dp(a,7),dp(a,3),dp(a,7),dp(a,3));root.addView(head);ListView list=new ListView(a);list.setDividerHeight(1);root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));final int[] mode={0};
        final Runnable refresh=()->{if(mode[0]==0){head.setText("EXPECTED MATCHDAY MINUTES • XI / BENCH / RESERVES");final ArrayList<SquadRow> data=new ArrayList<SquadRow>();for(String s:rows("squadRows"))data.add(SquadRow.parse(s));list.setOnItemClickListener(null);list.setAdapter(new ArrayAdapter<SquadRow>(a,android.R.layout.simple_list_item_1,data){@Override public View getView(int p,View cv,ViewGroup parent){SquadRow r=getItem(p);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));TextView n=text(a,r.section()+"   "+r.name+"   "+r.pos+(r.minutes>0?"   "+r.minutes+" min":"   not selected"),13.5f,true);TextView s=text(a,"SPE "+r.spe+"  RES "+r.res+"  QUA "+r.qua+"   FAT "+r.fat+"   "+r.status,11.3f,false);row.addView(n);row.addView(s);if(r.slot<11)row.setBackgroundColor(0xfff2f7f2);else if(r.minutes>0)row.setBackgroundColor(0xffe4efff);return row;}});}else if(mode[0]==1){head.setText("PLANNED AUTOMATIC SUBSTITUTIONS • MAXIMUM 3");final ArrayList<SubRow> data=new ArrayList<SubRow>();for(String s:rows("substitutionPlan"))data.add(SubRow.parse(s));if(data.isEmpty()){list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new String[]{"No eligible automatic substitutions are planned for this matchday."}));}else list.setAdapter(new ArrayAdapter<SubRow>(a,android.R.layout.simple_list_item_1,data){@Override public View getView(int p,View cv,ViewGroup parent){SubRow r=getItem(p);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,10),dp(a,8),dp(a,10),dp(a,8));row.addView(text(a,r.minute+"'   "+r.out+"  →  "+r.in,14f,true));row.addView(text(a,"Position-aware deterministic substitution",11.5f,false));return row;}});list.setOnItemClickListener(null);}else{head.setText("PERSISTED GOALS & SUBSTITUTIONS • NEWEST FIRST");final String[] data=rows("eventRecords");if(data.length==0){list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new String[]{"No completed match reports have been stored in this save yet."}));list.setOnItemClickListener(null);}else{list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,data){@Override public View getView(int p,View cv,ViewGroup parent){String report=getItem(p);String[] lines=report.split("\\n");String title=lines.length>0?lines[0]:"MATCH";String score=lines.length>1?lines[1]:"";LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,10),dp(a,7),dp(a,10),dp(a,7));row.addView(text(a,title,13.5f,true));row.addView(text(a,score+"   •   tap for full event log",11.5f,false));return row;}});list.setOnItemClickListener((p,v,pos,id)->new AlertDialog.Builder(a).setTitle(firstLine(data[pos])).setMessage(data[pos]).setPositiveButton("Close",null).show());}}squad.setEnabled(mode[0]!=0);subs.setEnabled(mode[0]!=1);events.setEnabled(mode[0]!=2);};
        squad.setOnClickListener(v->{mode[0]=0;refresh.run();});subs.setOnClickListener(v->{mode[0]=1;refresh.run();});events.setOnClickListener(v->{mode[0]=2;refresh.run();});refresh.run();
        final AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Matchday Center").setView(root).setPositiveButton("Close",null).create();lineup.setOnClickListener(v->{NativeLineup.show(a);dlg.dismiss();});dlg.setOnDismissListener(v->NativePause.end(pause));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});dlg.show();
    }
}
