package pfm.android;

import android.app.*;import android.view.*;import android.widget.*;import java.util.*;

/** Persisted native deal journal, ready to include future AI-to-AI activity. */
public final class NativeTransferHistory {
    private NativeTransferHistory() {}
    public static void show(Activity a){NativeCareerStore.runAiMarket(a);final boolean pause=NativePause.begin();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);Spinner scope=new Spinner(a);scope.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"MY CLUB","ALL WORLD ACTIVITY"}));root.addView(scope);ListView list=new ListView(a);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));final Runnable[] refresh=new Runnable[1];refresh[0]=()->{ArrayList<String> rows=new ArrayList<String>();try{rows.addAll(NativeCareerStore.transferHistory(a,scope.getSelectedItemPosition()==1));}catch(Throwable t){rows.add("Transfer journal unavailable: "+t.getMessage());}if(rows.isEmpty())rows.add("No completed native transfers yet.");list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,rows));};scope.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){refresh[0].run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});refresh[0].run();AlertDialog d=new AlertDialog.Builder(a).setTitle("Transfer Activity").setView(root).setPositiveButton("Close",null).create();d.setOnDismissListener(v->NativePause.end(pause));d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.9f));});d.show();}
}
