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
    private static final int[] ROUND_AT={4,8,11,14,17,19};
    private static final int[][] PAIRS={
        {0,3,1,2},{2,0,3,1},{0,1,2,3},
        {1,0,3,2},{3,0,2,1},{0,2,1,3}};
    private static final String PREFS="pfm_ucl_groups_v79";
    private static class Row {String id,name;int played,won,drawn,lost,gf,ga,points;}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));return v;}
    private static ArrayList<ArrayList<String>> groups(Context c)throws IOException{
        ArrayList<ArrayList<String>> out=new ArrayList<ArrayList<String>>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open("europe_ucl_groups_2010.tsv"),"UTF-8"));
        try{reader.readLine();String line;while((line=reader.readLine())!=null){String[] p=line.split("\t",-1);if(p.length!=2||p[0].length()!=1)throw new IOException("Bad UCL group row");int g=p[0].charAt(0)-'A';if(g<0||g>7)throw new IOException("Bad UCL group");while(out.size()<=g)out.add(new ArrayList<String>());out.get(g).add(p[1]);}}finally{reader.close();}
        if(out.size()!=8)throw new IOException("Incomplete UCL groups");for(ArrayList<String> group:out)if(group.size()!=4)throw new IOException("Incomplete UCL group");return out;
    }
    private static int completed(){
        int season=NativeWorldCenter.season();if(season>2010)return 6;if(season<2010)return 0;
        int played=NativeTransferCalendar.played(),total=NativeTransferCalendar.total();
        int equivalent=(played*38+total/2)/total,n=0;for(int mark:ROUND_AT)if(equivalent>=mark)n++;return n;
    }
    private static String prefix(){return NativeWorldCenter.identity()+"_2010_";}
    private static String key(int round,int group,int match){return prefix()+round+"_"+group+"_"+match;}
    private static int ability(String id,HashMap<String,ArrayList<Integer>> byClub){
        ArrayList<Integer> ratings=byClub.get(id);if(ratings==null||ratings.isEmpty())return 65;
        Collections.sort(ratings,Collections.reverseOrder());int n=Math.min(11,ratings.size()),sum=0;for(int i=0;i<n;i++)sum+=ratings.get(i);return sum/n;
    }
    private static int[] score(int seed,int home,int away){
        int hg=0,ag=0;for(int chance=0;chance<6;chance++){
            int roll=NativeWorldCenter.hash(seed+chance*911)&0x7fffffff;
            if(roll%100<Math.max(9,Math.min(49,24+(home-away)*2+4)))hg++;
            roll=NativeWorldCenter.hash(seed+chance*1217+77)&0x7fffffff;
            if(roll%100<Math.max(8,Math.min(46,24+(away-home)*2-4)))ag++;
        }return new int[]{hg,ag};
    }
    static synchronized void tick(Context context){
        try{
            NativeWorldCenter.load(context);ArrayList<ArrayList<String>> groups=groups(context);
            NativeCareerStore.Career career=NativeCareerStore.ensure(context);
            HashSet<String> known=new HashSet<String>();for(NativeCareerStore.Club club:career.clubs)known.add(club.id);
            HashMap<String,ArrayList<Integer>> ratings=new HashMap<String,ArrayList<Integer>>();
            for(NativeCareerStore.Player player:career.players){ArrayList<Integer> list=ratings.get(player.owner);if(list==null){list=new ArrayList<Integer>();ratings.put(player.owner,list);}list.add(player.overall);}
            for(ArrayList<String> group:groups)for(String id:group)if(!known.contains(id))throw new IOException("UCL club missing: "+id);
            android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);android.content.SharedPreferences.Editor edit=prefs.edit();boolean changed=false;
            for(int round=0;round<completed();round++)for(int group=0;group<8;group++)for(int match=0;match<2;match++){
                String k=key(round,group,match);if(prefs.contains(k))continue;
                ArrayList<String> ids=groups.get(group);int h=PAIRS[round][match*2],a=PAIRS[round][match*2+1];
                int seed=NativeWorldCenter.identity()+round*104729+group*1009+match*37;
                int[] result=score(seed,ability(ids.get(h),ratings),ability(ids.get(a),ratings));
                edit.putString(k,result[0]+":"+result[1]);changed=true;
            }
            if(changed)edit.commit();
        }catch(Throwable ignored){}
    }
    private static void update(Row home,Row away,int hg,int ag){
        home.played++;away.played++;home.gf+=hg;home.ga+=ag;away.gf+=ag;away.ga+=hg;
        if(hg>ag){home.won++;away.lost++;home.points+=3;}
        else if(hg<ag){away.won++;home.lost++;away.points+=3;}
        else{home.drawn++;away.drawn++;home.points++;away.points++;}
    }
    static void show(Activity activity){
        NativeWorldCenter.load(activity);tick(activity);
        final ArrayList<ArrayList<String>> groups;try{groups=groups(activity);}catch(IOException e){Toast.makeText(activity,"UCL group data unavailable",Toast.LENGTH_SHORT).show();return;}
        final boolean paused=NativePause.begin();final NativeCareerStore.Career career;
        try{career=NativeCareerStore.ensure(activity);}catch(Exception e){NativePause.end(paused);Toast.makeText(activity,"Career data unavailable",Toast.LENGTH_SHORT).show();return;}
        HashMap<String,String> names=new HashMap<String,String>();for(NativeCareerStore.Club c:career.clubs)names.put(c.id,c.name);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        root.addView(text(activity,"2010/11 group stage • six matchdays. Results become final as league rounds advance.",13));
        Spinner selector=new Spinner(activity);String[] labels=new String[8];for(int i=0;i<8;i++)labels[i]="GROUP "+(char)('A'+i);selector.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(selector);
        ListView list=new ListView(activity);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        Runnable refresh=()->{
            int g=selector.getSelectedItemPosition();if(g<0||g>=8)return;
            ArrayList<String> ids=groups.get(g);ArrayList<Row> table=new ArrayList<Row>();for(String id:ids){Row row=new Row();row.id=id;row.name=names.containsKey(id)?names.get(id):id;table.add(row);}
            android.content.SharedPreferences prefs=activity.getSharedPreferences(PREFS,0);ArrayList<String> lines=new ArrayList<String>();
            for(int r=0;r<6;r++)for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=prefs.getString(key(r,g,m),null);if(result!=null){String[] p=result.split(":");int hg=Integer.parseInt(p[0]),ag=Integer.parseInt(p[1]);update(table.get(h),table.get(a),hg,ag);}}
            Collections.sort(table,(x,y)->{if(x.points!=y.points)return y.points-x.points;int xd=x.gf-x.ga,yd=y.gf-y.ga;if(xd!=yd)return yd-xd;if(x.gf!=y.gf)return y.gf-x.gf;return x.name.compareTo(y.name);});
            lines.add("TABLE • matchday "+completed()+" / 6");int rank=1;for(Row row:table)lines.add((rank++)+". "+row.name+"   "+row.points+" pts   "+row.gf+":"+row.ga+"   P "+row.played);
            lines.add("FIXTURES & RESULTS");for(int r=0;r<6;r++){lines.add("Matchday "+(r+1)+" • around league round "+ROUND_AT[r]);for(int m=0;m<2;m++){int h=PAIRS[r][m*2],a=PAIRS[r][m*2+1];String result=prefs.getString(key(r,g,m),null);lines.add(names.get(ids.get(h))+(result==null?"  vs  ":"  "+result.replace(':','–')+"  ")+names.get(ids.get(a)));}}
            list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));
        };
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}});refresh.run();
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Champions League • 2010/11").setView(root).setPositiveButton("Close",null).create();dialog.setOnDismissListener(v->NativePause.end(paused));dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.92f));});dialog.show();
    }
}
