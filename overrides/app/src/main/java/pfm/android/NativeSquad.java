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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Native Android squad/player screen against the Alpha 0.84 core. */
public final class NativeSquad {
    private NativeSquad() {}

    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}

    private static final class PlayerRow {
        int id,number,spe,res,qua,mor,age,goals,starts,apps,assists,rating10,ratingMatches;
        int fatigue,injury,yellow,red,suspension;String name,pos;
        static PlayerRow parse(String s){String[] p=s.split("\\t",-1);PlayerRow r=new PlayerRow();try{r.id=Integer.parseInt(p[0]);r.number=Integer.parseInt(p[1]);r.name=p[2];r.pos=p[3];r.spe=Integer.parseInt(p[4]);r.res=Integer.parseInt(p[5]);r.qua=Integer.parseInt(p[6]);r.mor=Integer.parseInt(p[7]);r.age=Integer.parseInt(p[8]);r.goals=Integer.parseInt(p[9]);r.starts=Integer.parseInt(p[10]);r.apps=Integer.parseInt(p[11]);r.assists=Integer.parseInt(p[12]);r.rating10=Integer.parseInt(p[13]);r.ratingMatches=Integer.parseInt(p[14]);r.fatigue=Integer.parseInt(p[15]);r.injury=Integer.parseInt(p[16]);r.yellow=Integer.parseInt(p[17]);r.red=Integer.parseInt(p[18]);r.suspension=Integer.parseInt(p[19]);}catch(Throwable t){r.name=s;r.pos="?";}return r;}
        String rating(){return ratingMatches<=0?"n/a":((rating10/10)+"."+Math.abs(rating10%10));}
        boolean unavailable(){return injury>0||suspension>0;}
        String status(){StringBuilder b=new StringBuilder();if(injury>0)b.append("INJ ").append(injury).append("r");if(suspension>0){if(b.length()>0)b.append("  ");b.append("SUSP ").append(suspension).append("r");}if(b.length()==0)b.append("available");return b.toString();}
    }

    private static String teamName(){try{Object r=Class.forName("PfmSquadBridge").getMethod("teamName").invoke(null);return r==null?"Squad":String.valueOf(r);}catch(Throwable t){return "Squad";}}
    private static List<PlayerRow> loadRows(){ArrayList<PlayerRow> out=new ArrayList<>();try{Class<?> c=Class.forName("PfmSquadBridge");if(!Boolean.TRUE.equals(c.getMethod("available").invoke(null)))return out;String[] rows=(String[])c.getMethod("rows").invoke(null);if(rows!=null)for(String s:rows)out.add(PlayerRow.parse(s));}catch(Throwable ignored){}return out;}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}

    private static void showPlayer(Activity a,PlayerRow p){StringBuilder b=new StringBuilder();b.append("No. ").append(p.number).append("   ").append(p.pos).append('\n');b.append("Age: ").append(p.age).append("\n\n");b.append("SPE  ").append(p.spe).append("\nRES  ").append(p.res).append("\nQUA  ").append(p.qua).append("\nMOR  ").append(p.mor).append("\n\n");b.append("Condition\nFatigue: ").append(p.fatigue).append(" / 100 (lower is better)\nStatus: ").append(p.status()).append("\nCards: ").append(p.yellow).append(" Y / ").append(p.red).append(" R\n\n");b.append("Season\nStarts: ").append(p.starts).append("\nAppearances: ").append(p.apps).append("\nGoals: ").append(p.goals).append("\nAssists: ").append(p.assists).append("\nRating: ").append(p.rating()).append("\n");if(p.apps>0&&p.ratingMatches<=0)b.append("(No persisted rating samples are available in this loaded season.)\n");b.append("\nUse LINE-UP / TACTICS for pre-match squad order and formation changes.");new AlertDialog.Builder(a).setTitle("#"+p.number+"  "+p.name).setMessage(b.toString()).setPositiveButton("Close",null).show();}
    private static boolean matchesFilter(PlayerRow p,String filter){if("All".equals(filter))return true;if("Unavailable".equals(filter))return p.unavailable();return filter.equals(p.pos);}
    private static Comparator<PlayerRow> comparator(String sort){if("Position".equals(sort))return (a,b)->{int c=a.pos.compareTo(b.pos);if(c!=0)return c;return a.number-b.number;};if("Starts".equals(sort))return (a,b)->b.starts-a.starts;if("Goals".equals(sort))return (a,b)->b.goals-a.goals;if("Rating".equals(sort))return (a,b)->{if(a.ratingMatches==0&&b.ratingMatches>0)return 1;if(b.ratingMatches==0&&a.ratingMatches>0)return -1;return b.rating10-a.rating10;};if("Fatigue".equals(sort))return (a,b)->b.fatigue-a.fatigue;return null;}

    public static void show(Activity a){
        final List<PlayerRow> source=loadRows();if(source.isEmpty()){Toast.makeText(a,"Start or load a career first",Toast.LENGTH_SHORT).show();return;}
        final boolean pauseToken=NativePause.begin();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,10),dp(a,4),dp(a,10),0);
        TextView info=text(a,"Tap a player for details. FAT = fatigue (0 is fresh, 100 is exhausted).",13f,false);info.setPadding(dp(a,6),dp(a,2),dp(a,6),dp(a,5));root.addView(info);

        LinearLayout navigation=new LinearLayout(a);navigation.setOrientation(LinearLayout.HORIZONTAL);
        Button season=new Button(a);season.setText("SEASON");season.setOnClickListener(v->NativeSeasonHub.show(a));navigation.addView(season,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        Button editXi=new Button(a);editXi.setText("LINE-UP");editXi.setOnClickListener(v->NativeLineup.show(a));navigation.addView(editXi,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        Button matchday=new Button(a);matchday.setText("MATCHDAY");matchday.setOnClickListener(v->NativeMatchday.show(a));navigation.addView(matchday,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        // NativeTransfers.show is intentionally retired; transactions stay in the original JAR menu.
        root.addView(navigation);
        Button policy=new Button(a);policy.setText("SQUAD POLICY / ROTATION");root.addView(policy,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        Button analytics=new Button(a);analytics.setText("TEAM ANALYTICS");root.addView(analytics,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        Button careers=new Button(a);careers.setText("PLAYER CAREERS");root.addView(careers,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        Button contracts=new Button(a);contracts.setText("CONTRACT CENTER");root.addView(contracts,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout controls=new LinearLayout(a);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setPadding(0,0,0,dp(a,5));
        Spinner filter=new Spinner(a);String[] filters={"All","GK","DEF","MID","FW","Unavailable"};ArrayAdapter<String> fa=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,filters);fa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);filter.setAdapter(fa);controls.addView(filter,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        Spinner sort=new Spinner(a);String[] sorts={"Squad order","Position","Starts","Goals","Rating","Fatigue"};ArrayAdapter<String> sa=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,sorts);sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sort.setAdapter(sa);controls.addView(sort,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(controls);

        ListView list=new ListView(a);list.setDividerHeight(1);final ArrayList<PlayerRow> visible=new ArrayList<>();
        ArrayAdapter<PlayerRow> adapter=new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,visible){@Override public View getView(int position,View convertView,ViewGroup parent){LinearLayout row;TextView l1,l2,l3;if(convertView instanceof LinearLayout&&((LinearLayout)convertView).getChildCount()==3){row=(LinearLayout)convertView;l1=(TextView)row.getChildAt(0);l2=(TextView)row.getChildAt(1);l3=(TextView)row.getChildAt(2);}else{row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,12),dp(a,7),dp(a,12),dp(a,7));row.setMinimumHeight(dp(a,72));l1=text(a,"",17f,true);l2=text(a,"",13f,false);l3=text(a,"",12f,false);row.addView(l1);row.addView(l2);row.addView(l3);}PlayerRow p=getItem(position);l1.setText("#"+p.number+"  "+p.name+"   "+p.pos+(p.unavailable()?"   !":""));l2.setText("SPE "+p.spe+"  RES "+p.res+"  QUA "+p.qua+"  MOR "+p.mor+"  AGE "+p.age+"\nST "+p.starts+"  AP "+p.apps+"  G "+p.goals+"  A "+p.assists+"  RAT "+p.rating());l3.setText("FAT "+p.fatigue+"   "+p.status()+"   YC "+p.yellow+" RC "+p.red);return row;}};
        list.setAdapter(adapter);list.setOnItemClickListener((parent,view,position,id)->showPlayer(a,visible.get(position)));root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        Runnable refresh=()->{String f=String.valueOf(filter.getSelectedItem());String s=String.valueOf(sort.getSelectedItem());visible.clear();for(PlayerRow p:source)if(matchesFilter(p,f))visible.add(p);Comparator<PlayerRow> cmp=comparator(s);if(cmp!=null)Collections.sort(visible,cmp);adapter.notifyDataSetChanged();};
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});sort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});refresh.run();
        AlertDialog dlg=new AlertDialog.Builder(a).setTitle(teamName()+" — Squad  "+source.size()+" / 25").setView(root).setPositiveButton("Close",null).create();
        policy.setOnClickListener(v->{NativeSquadPolicy.show(a);dlg.dismiss();});
        analytics.setOnClickListener(v->{NativeTeamAnalytics.show(a);dlg.dismiss();});
        careers.setOnClickListener(v->{NativePlayerCareer.show(a);dlg.dismiss();});
        contracts.setOnClickListener(v->{NativeContracts.show(a);dlg.dismiss();});
        dlg.setOnDismissListener(x->NativePause.end(pauseToken));dlg.setOnShowListener(x->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*0.90f));});dlg.show();
    }
}
