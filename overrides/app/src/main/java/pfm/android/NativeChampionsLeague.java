package pfm.android;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Read-only 2010/11 Champions League groups and knockouts, paced by the career calendar. */
final class NativeChampionsLeague {
    private NativeChampionsLeague() {}
    private static final int[] ROUND_AT=NativeCompetitionCalendar.UCL_AT;
    private static final int[][] PAIRS=NativeCompetitionCalendar.UCL_PAIRS;
    private static class Row {String id,name;int played,won,drawn,lost,gf,ga,points;}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));return v;}
    private static ArrayList<ArrayList<String>> groups(Context c)throws IOException{return NativeCompetitionCalendar.groups(c);}
    private static int completed(){try{int n=0;for(MatchJournal.Match m:NativeMatchJournal.ucl(AndroidRuntime.context()))if(m.round<=6)n++;return Math.min(6,n/16);}catch(IOException e){return 0;}}
    static synchronized void tick(Context context){try{NativeMatchJournal.advance(context,NativeTransferCalendar.played());}catch(Exception e){android.util.Log.e("PFM-Journal","UCL archive unavailable",e);}}
    private static String result(HashMap<String,MatchJournal.Match> saved,int round,String home,String away){MatchJournal.Match m=saved.get(NativeMatchJournal.key("ucl:2010",round+1,home,away));return m==null?null:m.homeGoals+":"+m.awayGoals;}
    private static void update(Row home,Row away,int hg,int ag){
        home.played++;away.played++;home.gf+=hg;home.ga+=ag;away.gf+=ag;away.ga+=hg;
        if(hg>ag){home.won++;away.lost++;home.points+=3;}
        else if(hg<ag){away.won++;home.lost++;away.points+=3;}
        else{home.drawn++;away.drawn++;home.points++;away.points++;}
    }
    static void show(Activity activity){
        NativeWorldCenter.load(activity);
        final ArrayList<ArrayList<String>> groups;try{groups=groups(activity);}catch(IOException e){Toast.makeText(activity,"UCL group data unavailable",Toast.LENGTH_SHORT).show();return;}
        final boolean paused=NativePause.begin();final NativeCareerStore.Career career;final HashMap<String,MatchJournal.Match> archive=new HashMap<String,MatchJournal.Match>();
        try{career=NativeCareerStore.ensure(activity);for(MatchJournal.Match m:NativeMatchJournal.ucl(activity))archive.put(m.id,m);}catch(Exception e){NativePause.end(paused);Toast.makeText(activity,"Career data unavailable",Toast.LENGTH_SHORT).show();return;}
        HashMap<String,String> names=new HashMap<String,String>();for(NativeCareerStore.Club c:career.clubs)names.put(c.id,c.name);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        root.addView(text(activity,"2010/11 • groups to final. Results advance with the career calendar. Tap a played fixture for its report.",13));
        Spinner selector=new Spinner(activity);String[] labels=new String[12];for(int i=0;i<8;i++)labels[i]="GROUP "+(char)('A'+i);for(int i=0;i<4;i++)labels[i+8]=UclKnockout.STAGES[i];selector.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(selector);
        final HashMap<Integer,ArrayList<String[]>> draws=new HashMap<Integer,ArrayList<String[]>>();try{for(int s=0;s<4;s++)draws.put(s,NativeMatchJournal.uclDraw(activity,s));}catch(IOException ignored){}
        ListView list=new ListView(activity);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        Runnable refresh=()->{
            int g=selector.getSelectedItemPosition();if(g<0)return;
            if(g>=8){int stage=g-8,round=7+stage*2;ArrayList<String> lines=new ArrayList<String>();ArrayList<MatchJournal.Match> reports=new ArrayList<MatchJournal.Match>();ArrayList<String[]> ties=draws.get(stage);
                lines.add(stage==3?"NEUTRAL FINAL • Wembley • around league round 38":"TWO LEGS • away goals apply, including extra time");reports.add(null);
                if(ties==null||ties.isEmpty()){lines.add("Draw pending completion of the previous stage.");reports.add(null);}
                else for(String[] tie:ties){for(int leg=0;leg<(stage==3?1:2);leg++){String home=leg==0?tie[0]:tie[1],away=leg==0?tie[1]:tie[0];MatchJournal.Match m=archive.get(UclKnockout.key(round+leg,home,away));String score=m==null?" vs ":" "+m.homeGoals+"–"+m.awayGoals+" ";String extra=m==null?"":(m.duration==120?" (a.e.t.)":"")+(m.homePenalties+m.awayPenalties>0?" pens "+m.homePenalties+"–"+m.awayPenalties:"");lines.add(names.get(home)+score+names.get(away)+extra+"\n"+(stage==3?"Final":"Leg "+(leg+1))+" • around league round "+UclKnockout.AT[stage*2+leg]);reports.add(m);if(m!=null&&m.winner.length()>0){lines.add((stage==3?"CHAMPIONS: ":"QUALIFIED: ")+names.get(m.winner));reports.add(null);}}}
                list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));list.setOnItemClickListener((parent,view,position,id)->NativeMatchJournal.showReport(activity,reports.get(position)));return;}
            ArrayList<String> ids=groups.get(g);ArrayList<Row> table=new ArrayList<Row>();for(String id:ids){Row row=new Row();row.id=id;row.name=names.containsKey(id)?names.get(id):id;table.add(row);}
            ArrayList<String> lines=new ArrayList<String>();
            for(int r=0;r<6;r++)for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,r,ids.get(h),ids.get(a));if(result!=null){String[] p=result.split(":");int hg=Integer.parseInt(p[0]),ag=Integer.parseInt(p[1]);update(table.get(h),table.get(a),hg,ag);}}
            final ArrayList<UclKnockout.Row> ordered=UclKnockout.table(ids,new ArrayList<MatchJournal.Match>(archive.values()));Collections.sort(table,(x,y)->{int a=0,b=0;for(int i=0;i<ordered.size();i++){if(ordered.get(i).id.equals(x.id))a=i;if(ordered.get(i).id.equals(y.id))b=i;}return a-b;});
            lines.add("TABLE • matchday "+completed()+" / 6");int rank=1;for(Row row:table)lines.add((rank++)+". "+row.name+"   "+row.points+" pts   "+row.gf+":"+row.ga+"   P "+row.played);
            lines.add("FIXTURES & RESULTS");for(int r=0;r<6;r++){lines.add("Matchday "+(r+1)+" • around league round "+ROUND_AT[r]);for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,r,ids.get(h),ids.get(a));lines.add(names.get(ids.get(h))+(result==null?"  vs  ":"  "+result.replace(':','–')+"  ")+names.get(ids.get(a)));}}
            list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));
            list.setOnItemClickListener((parent,view,position,id)->{int offset=position-6;if(offset>=0&&offset/3<6&&offset%3>0){int r=offset/3,m=offset%3-1;NativeMatchJournal.showReport(activity,archive.get(NativeMatchJournal.key("ucl:2010",r+1,ids.get(PAIRS[r][m*2]),ids.get(PAIRS[r][m*2+1]))));}});
        };
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}});refresh.run();
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Champions League • 2010/11").setView(root).setPositiveButton("Close",null).create();dialog.setOnDismissListener(v->NativePause.end(paused));dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.92f));});dialog.show();
    }
}
