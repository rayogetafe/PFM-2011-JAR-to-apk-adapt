package pfm.android;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** 2010/11 Champions League group stage, paced by the active league calendar. */
final class NativeChampionsLeague {
    private NativeChampionsLeague() {}
    private static final int[] ROUND_AT=NativeCompetitionCalendar.UCL_AT;
    private static final int[][] PAIRS=NativeCompetitionCalendar.UCL_PAIRS;
    private static class Row {String id,name;int played,won,drawn,lost,gf,ga,points;}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));return v;}
    private static ArrayList<ArrayList<String>> groups(Context c)throws IOException{return NativeCompetitionCalendar.groups(c);}
    private static int completed(){try{return Math.min(6,NativeMatchJournal.ucl(AndroidRuntime.context()).size()/16);}catch(IOException e){return 0;}}
    static synchronized void tick(Context context){try{NativeMatchJournal.advance(context,NativeTransferCalendar.played());}catch(Exception e){android.util.Log.e("PFM-Journal","UCL archive unavailable",e);}}
    private static String result(HashMap<String,MatchJournal.Match> saved,int round,String home,String away){MatchJournal.Match m=saved.get(NativeMatchJournal.key("ucl:2010",round+1,home,away));return m==null?null:m.homeGoals+":"+m.awayGoals;}
    private static void update(Row home,Row away,int hg,int ag){
        home.played++;away.played++;home.gf+=hg;home.ga+=ag;away.gf+=ag;away.ga+=hg;
        if(hg>ag){home.won++;away.lost++;home.points+=3;}
        else if(hg<ag){away.won++;home.lost++;away.points+=3;}
        else{home.drawn++;away.drawn++;home.points++;away.points++;}
    }
    static void show(Activity activity){
        NativeWorldCenter.load(activity);tick(activity);
        final ArrayList<ArrayList<String>> groups;try{groups=groups(activity);}catch(IOException e){Toast.makeText(activity,"UCL group data unavailable",Toast.LENGTH_SHORT).show();return;}
        final boolean paused=NativePause.begin();final NativeCareerStore.Career career;final HashMap<String,MatchJournal.Match> archive=new HashMap<String,MatchJournal.Match>();
        try{career=NativeCareerStore.ensure(activity);for(MatchJournal.Match m:NativeMatchJournal.ucl(activity))archive.put(m.id,m);}catch(Exception e){NativePause.end(paused);Toast.makeText(activity,"Career data unavailable",Toast.LENGTH_SHORT).show();return;}
        HashMap<String,String> names=new HashMap<String,String>();for(NativeCareerStore.Club c:career.clubs)names.put(c.id,c.name);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        root.addView(text(activity,"2010/11 group stage • six matchdays. Tap a played fixture for its recorded line-up.",13));
        Spinner selector=new Spinner(activity);String[] labels=new String[8];for(int i=0;i<8;i++)labels[i]="GROUP "+(char)('A'+i);selector.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(selector);
        ListView list=new ListView(activity);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        Runnable refresh=()->{
            int g=selector.getSelectedItemPosition();if(g<0||g>=8)return;
            ArrayList<String> ids=groups.get(g);ArrayList<Row> table=new ArrayList<Row>();for(String id:ids){Row row=new Row();row.id=id;row.name=names.containsKey(id)?names.get(id):id;table.add(row);}
            ArrayList<String> lines=new ArrayList<String>();
            for(int r=0;r<6;r++)for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,r,ids.get(h),ids.get(a));if(result!=null){String[] p=result.split(":");int hg=Integer.parseInt(p[0]),ag=Integer.parseInt(p[1]);update(table.get(h),table.get(a),hg,ag);}}
            Collections.sort(table,(x,y)->{if(x.points!=y.points)return y.points-x.points;int xd=x.gf-x.ga,yd=y.gf-y.ga;if(xd!=yd)return yd-xd;if(x.gf!=y.gf)return y.gf-x.gf;return x.name.compareTo(y.name);});
            lines.add("TABLE • matchday "+completed()+" / 6");int rank=1;for(Row row:table)lines.add((rank++)+". "+row.name+"   "+row.points+" pts   "+row.gf+":"+row.ga+"   P "+row.played);
            lines.add("FIXTURES & RESULTS");for(int r=0;r<6;r++){lines.add("Matchday "+(r+1)+" • around league round "+ROUND_AT[r]);for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,r,ids.get(h),ids.get(a));lines.add(names.get(ids.get(h))+(result==null?"  vs  ":"  "+result.replace(':','–')+"  ")+names.get(ids.get(a)));}}
            list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));
            list.setOnItemClickListener((parent,view,position,id)->{int offset=position-6;if(offset>=0&&offset/3<6&&offset%3>0){int r=offset/3,m=offset%3-1;NativeMatchJournal.showReport(activity,archive.get(NativeMatchJournal.key("ucl:2010",r+1,ids.get(PAIRS[r][m*2]),ids.get(PAIRS[r][m*2+1]))));}});
        };
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}});refresh.run();
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Champions League • 2010/11").setView(root).setPositiveButton("Close",null).create();dialog.setOnDismissListener(v->NativePause.end(paused));dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.92f));});dialog.show();
    }
}
