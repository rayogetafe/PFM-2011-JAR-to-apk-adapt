package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Native career archive: final tables and persisted player-season statistics. */
public final class NativeHistory {
    private NativeHistory() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge()throws Exception{return Class.forName("PfmHistoryBridge");}
    private static boolean available(){try{return Boolean.TRUE.equals(bridge().getMethod("available").invoke(null));}catch(Throwable t){return false;}}
    private static int current(){try{return ((Integer)bridge().getMethod("currentSeason").invoke(null)).intValue();}catch(Throwable t){return 2010;}}
    private static int[] seasons(){try{return (int[])bridge().getMethod("seasons").invoke(null);}catch(Throwable t){return new int[0];}}
    private static String[] rows(String method,int season){try{return (String[])bridge().getMethod(method,Integer.TYPE).invoke(null,Integer.valueOf(season));}catch(Throwable t){return new String[0];}}

    private static final class TableRow {int rank,p,w,d,l,gf,ga,gd,pts;String club="";boolean user;static TableRow parse(String s){TableRow r=new TableRow();try{String[] x=s.split("\\t",-1);r.rank=Integer.parseInt(x[0]);r.club=x[1];r.p=Integer.parseInt(x[2]);r.w=Integer.parseInt(x[3]);r.d=Integer.parseInt(x[4]);r.l=Integer.parseInt(x[5]);r.gf=Integer.parseInt(x[6]);r.ga=Integer.parseInt(x[7]);r.gd=Integer.parseInt(x[8]);r.pts=Integer.parseInt(x[9]);r.user="1".equals(x[10]);}catch(Throwable ignored){}return r;}}
    private static final class PlayerRow {String name="";int g,ap,st,rat,spe,res,qua;static PlayerRow parse(String s){PlayerRow r=new PlayerRow();try{String[] x=s.split("\\t",-1);r.name=x[1];r.g=Integer.parseInt(x[2]);r.ap=Integer.parseInt(x[3]);r.st=Integer.parseInt(x[4]);r.rat=Integer.parseInt(x[5]);r.spe=Integer.parseInt(x[6]);r.res=Integer.parseInt(x[7]);r.qua=Integer.parseInt(x[8]);}catch(Throwable ignored){}return r;}String rating(){return rat<=0?"n/a":(rat/10)+"."+(rat%10);}}
    private static ArrayList<TableRow> table(int y){ArrayList<TableRow> a=new ArrayList<TableRow>();for(String s:rows("tableRows",y))a.add(TableRow.parse(s));return a;}
    private static ArrayList<PlayerRow> players(int y){ArrayList<PlayerRow> a=new ArrayList<PlayerRow>();for(String s:rows("playerRows",y))a.add(PlayerRow.parse(s));return a;}

    public static void show(Activity a){
        if(!available()){Toast.makeText(a,"Career history is unavailable until a career is loaded",Toast.LENGTH_SHORT).show();return;}
        final int[] years=seasons();if(years.length==0)return;final boolean pause=NativePause.begin();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,2),dp(a,8),0);
        TextView intro=text(a,"FINAL TABLES & PLAYER-SEASON RECORDS",12f,true);intro.setPadding(dp(a,6),dp(a,3),dp(a,6),dp(a,4));root.addView(intro);
        final Spinner picker=new Spinner(a);ArrayList<String> labels=new ArrayList<String>();for(int y:years)labels.add(y+"/"+(y+1)+(y==current()?"  •  CURRENT":""));ArrayAdapter<String> pickAdapter=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels);picker.setAdapter(pickAdapter);root.addView(picker);
        LinearLayout tabs=new LinearLayout(a);final Button tableBtn=new Button(a);tableBtn.setText("FINAL TABLE");final Button squadBtn=new Button(a);squadBtn.setText("SQUAD STATS");tabs.addView(tableBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));tabs.addView(squadBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(tabs);
        final TextView head=text(a,"",10f,true);head.setPadding(dp(a,3),dp(a,3),dp(a,3),dp(a,3));root.addView(head);
        final ListView list=new ListView(a);list.setDividerHeight(1);root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));final boolean[] tableMode={true};
        final Runnable refresh=()->{int y=years[picker.getSelectedItemPosition()];if(tableMode[0]){final ArrayList<TableRow> rs=table(y);head.setText("#   CLUB                         P    W    D    L    GF   GA   GD   PTS");list.setAdapter(new ArrayAdapter<TableRow>(a,android.R.layout.simple_list_item_1,rs){@Override public View getView(int pos,View cv,ViewGroup parent){TableRow r=getItem(pos);TextView v=text(a,String.format(java.util.Locale.US,"%-2d  %-20s %2d  %2d  %2d  %2d  %2d  %2d  %+3d  %3d",r.rank,r.club,r.p,r.w,r.d,r.l,r.gf,r.ga,r.gd,r.pts),10.4f,r.user);v.setTypeface(Typeface.MONOSPACE,r.user?Typeface.BOLD:Typeface.NORMAL);v.setSingleLine(true);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(a,3),dp(a,2),dp(a,3),dp(a,2));if(r.user)v.setBackgroundColor(0xffdff1df);return v;}});}else{final ArrayList<PlayerRow> rs=players(y);head.setText("PLAYER-SEASON STATS   •   ST starts, AP apps, G goals");list.setAdapter(new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,rs){@Override public View getView(int pos,View cv,ViewGroup parent){PlayerRow r=getItem(pos);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));TextView n=text(a,r.name,14f,true);TextView s=text(a,"ST "+r.st+"   AP "+r.ap+"   G "+r.g+"   RAT "+r.rating()+"   •   SPE "+r.spe+" RES "+r.res+" QUA "+r.qua,11.5f,false);row.addView(n);row.addView(s);return row;}});}tableBtn.setEnabled(!tableMode[0]);squadBtn.setEnabled(tableMode[0]);};
        tableBtn.setOnClickListener(v->{tableMode[0]=true;refresh.run();});squadBtn.setOnClickListener(v->{tableMode[0]=false;refresh.run();});picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});refresh.run();
        AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Career History").setView(root).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pause));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});dlg.show();
    }
}
