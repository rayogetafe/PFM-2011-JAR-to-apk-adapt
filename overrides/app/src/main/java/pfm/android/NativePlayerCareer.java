package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Selectable cross-season player history backed by the existing career archive. */
public final class NativePlayerCareer {
    private NativePlayerCareer() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float z,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge()throws Exception{return Class.forName("PfmHistoryBridge");}
    private static int[] seasons(){try{return (int[])bridge().getMethod("seasons").invoke(null);}catch(Throwable t){return new int[0];}}
    private static String[] rows(int season){try{return (String[])bridge().getMethod("playerRows",Integer.TYPE).invoke(null,Integer.valueOf(season));}catch(Throwable t){return new String[0];}}
    private static final class Entry {int id,season,g,ap,st,rat,spe,res,qua;String name;int subs(){return Math.max(0,ap-st);}String rating(){return rat<=0?"n/a":(rat/10)+"."+Math.abs(rat%10);}}
    private static Entry parse(String s,int year){Entry e=new Entry();try{String[] x=s.split("\\t",-1);e.id=Integer.parseInt(x[0]);e.name=x[1];e.g=Integer.parseInt(x[2]);e.ap=Integer.parseInt(x[3]);e.st=Integer.parseInt(x[4]);e.rat=Integer.parseInt(x[5]);e.spe=Integer.parseInt(x[6]);e.res=Integer.parseInt(x[7]);e.qua=Integer.parseInt(x[8]);e.season=year;}catch(Throwable t){e.name="Unknown";}return e;}
    public static void show(Activity a){
        int[] years=seasons();LinkedHashMap<Integer,String> names=new LinkedHashMap<>();ArrayList<Entry> all=new ArrayList<>();for(int y:years)for(String s:rows(y)){Entry e=parse(s,y);all.add(e);if(!names.containsKey(e.id))names.put(e.id,e.name);}
        if(all.isEmpty()){Toast.makeText(a,"Player career history is not available yet",Toast.LENGTH_SHORT).show();return;}final boolean pause=NativePause.begin();
        ArrayList<Integer> ids=new ArrayList<>(names.keySet());Collections.sort(ids,(x,y)->names.get(x).compareToIgnoreCase(names.get(y)));ArrayList<String> labels=new ArrayList<>();for(Integer id:ids)labels.add(names.get(id));
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,3),dp(a,8),0);TextView hint=text(a,"Choose a player to inspect every saved season. SUB = appearances minus starts.",12f,false);hint.setPadding(dp(a,5),dp(a,3),dp(a,5),dp(a,4));root.addView(hint);
        Spinner picker=new Spinner(a);ArrayAdapter<String> pick=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels);picker.setAdapter(pick);root.addView(picker);TextView totals=text(a,"",14f,true);totals.setPadding(dp(a,6),dp(a,5),dp(a,6),dp(a,5));totals.setBackgroundColor(0xffe8f1e8);root.addView(totals);ListView list=new ListView(a);list.setDividerHeight(1);root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        Runnable refresh=()->{int id=ids.get(picker.getSelectedItemPosition());ArrayList<Entry> career=new ArrayList<>();int ap=0,st=0,g=0;for(Entry e:all)if(e.id==id){career.add(e);ap+=e.ap;st+=e.st;g+=e.g;}Collections.sort(career,(x,y)->y.season-x.season);Entry newest=career.get(0),oldest=career.get(career.size()-1);totals.setText("Career totals: AP "+ap+"  ST "+st+"  SUB "+Math.max(0,ap-st)+"  G "+g+"\nAttribute change: SPE "+delta(newest.spe-oldest.spe)+"  RES "+delta(newest.res-oldest.res)+"  QUA "+delta(newest.qua-oldest.qua));list.setAdapter(new ArrayAdapter<Entry>(a,android.R.layout.simple_list_item_1,career){@Override public View getView(int p,View cv,ViewGroup parent){Entry e=getItem(p);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,8),dp(a,6),dp(a,8),dp(a,6));row.addView(text(a,e.season+"/"+(e.season+1),15f,true));row.addView(text(a,"AP "+e.ap+"   ST "+e.st+"   SUB "+e.subs()+"   G "+e.g+"   RAT "+e.rating()+"\nSPE "+e.spe+"   RES "+e.res+"   QUA "+e.qua,12.5f,false));return row;}});};
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});refresh.run();AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Player Careers").setView(root).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pause));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});dlg.show();
    }
    private static String delta(int x){return x>0?"+"+x:String.valueOf(x);}
}
