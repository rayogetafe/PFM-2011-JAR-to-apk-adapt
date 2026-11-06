package pfm.android;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Read-only Champions League seasons, groups and knockouts, paced by the career calendar. */
final class NativeChampionsLeague {
    private NativeChampionsLeague() {}
    private static final int[] ROUND_AT=NativeCompetitionCalendar.UCL_AT;
    private static final int[][] PAIRS=NativeCompetitionCalendar.UCL_PAIRS;
    private static class Row {String id,name;int played,won,drawn,lost,gf,ga,points;}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));return v;}
    static synchronized void tick(Context context){try{NativeMatchJournal.advance(context,NativeTransferCalendar.played());}catch(Exception e){android.util.Log.e("PFM-Journal","UCL archive unavailable",e);}}
    private static String result(HashMap<String,MatchJournal.Match> saved,String competition,int round,String home,String away){MatchJournal.Match m=saved.get(NativeMatchJournal.key(competition,round+1,home,away));return m==null?null:m.homeGoals+":"+m.awayGoals;}
    private static void update(Row home,Row away,int hg,int ag){
        home.played++;away.played++;home.gf+=hg;home.ga+=ag;away.gf+=ag;away.ga+=hg;
        if(hg>ag){home.won++;away.lost++;home.points+=3;}
        else if(hg<ag){away.won++;home.lost++;away.points+=3;}
        else{home.drawn++;away.drawn++;home.points++;away.points++;}
    }
    static void show(Activity activity){open(activity,false);}
    static void showEuropa(Activity activity){open(activity,true);}
    private static void open(Activity activity,boolean europa){NativeWorldCenter.load(activity);int year=NativeWorldCenter.season();try{while(year>2010&&(europa?NativeMatchJournal.europaGroups(activity,year):NativeMatchJournal.uclGroups(activity,year)).isEmpty())year--;}catch(IOException ignored){year=2010;}show(activity,year,europa);}
    private static void show(Activity activity,int year,boolean europa){
        final String seasonLabel=year+"/"+String.format(java.util.Locale.US,"%02d",(year+1)%100),competition=europa?EuropaSeason.competition(year):UclSeason.competition(year),title=europa?"Europa League":"Champions League";
        final int groupCount=europa?12:8,finalStage=europa?4:3;final int[] koAt=europa?EuropaSeason.AT:UclKnockout.AT;final String[] stages=europa?EuropaSeason.STAGES:UclKnockout.STAGES;
        NativeWorldCenter.load(activity);
        final ArrayList<ArrayList<String>> groups;try{groups=europa?NativeMatchJournal.europaGroups(activity,year):NativeMatchJournal.uclGroups(activity,year);if(groups.size()!=groupCount){Toast.makeText(activity,"This European season is not initialized yet. Advance the career calendar.",Toast.LENGTH_LONG).show();return;}}catch(IOException e){Toast.makeText(activity,"European group data unavailable",Toast.LENGTH_SHORT).show();return;}
        final boolean paused=NativePause.begin();final NativeCareerStore.Career career;final HashMap<String,MatchJournal.Match> archive=new HashMap<String,MatchJournal.Match>();
        try{career=NativeCareerStore.ensure(activity);for(MatchJournal.Match m:(europa?NativeMatchJournal.europa(activity,year):NativeMatchJournal.ucl(activity,year)))archive.put(m.id,m);}catch(Exception e){NativePause.end(paused);Toast.makeText(activity,"Career data unavailable",Toast.LENGTH_SHORT).show();return;}
        HashMap<String,String> names=new HashMap<String,String>();for(NativeCareerStore.Club c:career.clubs)names.put(c.id,c.name);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        root.addView(text(activity,seasonLabel+" • groups to final. Results advance with the career calendar. Tap a played fixture for its report.",13));
        String qualification;try{qualification=europa?NativeMatchJournal.europaQualification(activity,year):NativeMatchJournal.uclQualification(activity,year);}catch(IOException ignored){qualification="";}if(qualification!=null)root.addView(text(activity,qualification,11));
        final AlertDialog[] shown=new AlertDialog[1];Button seasons=new Button(activity);seasons.setText("SEASON / HISTORY • "+seasonLabel);root.addView(seasons);
        seasons.setOnClickListener(v->{ArrayList<Integer> available=new ArrayList<Integer>();ArrayList<String> labels=new ArrayList<String>();for(int y=NativeWorldCenter.season();y>=2010;y--)try{if((europa?NativeMatchJournal.europaGroups(activity,y):NativeMatchJournal.uclGroups(activity,y)).size()==groupCount){available.add(y);labels.add(y+"/"+String.format(java.util.Locale.US,"%02d",(y+1)%100));}}catch(IOException ignored){}new AlertDialog.Builder(activity).setTitle(title+" season").setItems(labels.toArray(new String[labels.size()]),(dialog,which)->{shown[0].dismiss();show(activity,available.get(which),europa);}).setNegativeButton("Cancel",null).show();});

        Spinner selector=new Spinner(activity);String[] labels=new String[groupCount+stages.length];for(int i=0;i<groupCount;i++)labels[i]="GROUP "+(char)('A'+i);for(int i=0;i<stages.length;i++)labels[i+groupCount]=stages[i];selector.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(selector);
        final HashMap<Integer,ArrayList<String[]>> draws=new HashMap<Integer,ArrayList<String[]>>();try{for(int s=0;s<stages.length;s++)draws.put(s,europa?NativeMatchJournal.europaDraw(activity,year,s):NativeMatchJournal.uclDraw(activity,year,s));}catch(IOException ignored){}
        ListView list=new ListView(activity);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        Runnable refresh=()->{
            int g=selector.getSelectedItemPosition();if(g<0)return;
            if(g>=groupCount){int stage=g-groupCount,round=7+stage*2;ArrayList<String> lines=new ArrayList<String>();ArrayList<MatchJournal.Match> reports=new ArrayList<MatchJournal.Match>();ArrayList<String[]> ties=draws.get(stage);
                lines.add(stage==finalStage?"NEUTRAL FINAL • "+(europa?"Dublin":"Wembley")+" • around league round "+NativeCompetitionCalendar.sourceRound(koAt[koAt.length-1],NativeTransferCalendar.total()):"TWO LEGS • away goals apply, including extra time");reports.add(null);
                if(ties==null||ties.isEmpty()){lines.add("Draw pending completion of the previous stage.");reports.add(null);}
                else for(String[] tie:ties){for(int leg=0;leg<(stage==finalStage?1:2);leg++){String home=leg==0?tie[0]:tie[1],away=leg==0?tie[1]:tie[0];MatchJournal.Match m=archive.get(UclKnockout.key(competition,round+leg,home,away));String score=m==null?" vs ":" "+m.homeGoals+"–"+m.awayGoals+" ";String extra=m==null?"":(m.duration==120?" (a.e.t.)":"")+(m.homePenalties+m.awayPenalties>0?" pens "+m.homePenalties+"–"+m.awayPenalties:"");lines.add(names.get(home)+score+names.get(away)+extra+"\n"+(stage==finalStage?"Final":"Leg "+(leg+1))+" • around league round "+NativeCompetitionCalendar.sourceRound(koAt[stage*2+leg],NativeTransferCalendar.total()));reports.add(m);if(m!=null&&m.winner.length()>0){lines.add((stage==finalStage?"CHAMPIONS: ":"QUALIFIED: ")+names.get(m.winner));reports.add(null);}}}
                list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));list.setOnItemClickListener((parent,view,position,id)->NativeMatchJournal.showReport(activity,reports.get(position)));return;}
            ArrayList<String> ids=groups.get(g);ArrayList<Row> table=new ArrayList<Row>();for(String id:ids){Row row=new Row();row.id=id;row.name=names.containsKey(id)?names.get(id):id;table.add(row);}
            ArrayList<String> lines=new ArrayList<String>();
            for(int r=0;r<6;r++)for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,competition,r,ids.get(h),ids.get(a));if(result!=null){String[] p=result.split(":");int hg=Integer.parseInt(p[0]),ag=Integer.parseInt(p[1]);update(table.get(h),table.get(a),hg,ag);}}
            final ArrayList<UclKnockout.Row> ordered=UclKnockout.table(ids,new ArrayList<MatchJournal.Match>(archive.values()));Collections.sort(table,(x,y)->{int a=0,b=0;for(int i=0;i<ordered.size();i++){if(ordered.get(i).id.equals(x.id))a=i;if(ordered.get(i).id.equals(y.id))b=i;}return a-b;});
            int recorded=0;for(MatchJournal.Match m:archive.values())if(m.round<=6)recorded++;lines.add("TABLE • matchday "+Math.min(6,recorded/(groupCount*2))+" / 6");int rank=1;for(Row row:table)lines.add((rank++)+". "+row.name+"   "+row.points+" pts   "+row.gf+":"+row.ga+"   P "+row.played);
            lines.add("FIXTURES & RESULTS");for(int r=0;r<6;r++){lines.add("Matchday "+(r+1)+" • around league round "+NativeCompetitionCalendar.sourceRound(ROUND_AT[r],NativeTransferCalendar.total()));for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=result(archive,competition,r,ids.get(h),ids.get(a));lines.add(names.get(ids.get(h))+(result==null?"  vs  ":"  "+result.replace(':','–')+"  ")+names.get(ids.get(a)));}}
            list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));
            list.setOnItemClickListener((parent,view,position,id)->{int offset=position-6;if(offset>=0&&offset/3<6&&offset%3>0){int r=offset/3,m=offset%3-1;NativeMatchJournal.showReport(activity,archive.get(NativeMatchJournal.key(competition,r+1,ids.get(PAIRS[r][m*2]),ids.get(PAIRS[r][m*2+1]))));}});
        };
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}});refresh.run();
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title+" • "+seasonLabel).setView(root).setPositiveButton("Close",null).create();shown[0]=dialog;dialog.setOnDismissListener(v->NativePause.end(paused));dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.92f));});dialog.show();
    }
}
