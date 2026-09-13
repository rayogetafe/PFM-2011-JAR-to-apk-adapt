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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** League-wide results, every club squad and season durability diagnostics. */
public final class NativeLeagueCenter {
    private NativeLeagueCenter() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge()throws Exception{return Class.forName("PfmLeagueBridge");}
    private static int callInt(String n){try{return ((Integer)bridge().getMethod(n).invoke(null)).intValue();}catch(Throwable t){return 0;}}
    private static String callString(String n){try{return String.valueOf(bridge().getMethod(n).invoke(null));}catch(Throwable t){return "";}}
    private static String[] rows(String n,Class<?>[] types,Object[] args){try{return (String[])bridge().getMethod(n,types).invoke(null,args);}catch(Throwable t){return new String[0];}}
    private static final class Match {String home,away;int hg,ag;boolean done,user;static Match parse(String s){Match r=new Match();try{String[] p=s.split("\\t",-1);r.home=p[3];r.away=p[4];r.hg=Integer.parseInt(p[5]);r.ag=Integer.parseInt(p[6]);r.done="1".equals(p[7]);r.user="1".equals(p[8]);}catch(Throwable ignored){}return r;}}
    private static final class Team {int index,count,gk,def,mid,fw,ovr,cs,ga;String name;boolean user;static Team parse(String s){Team r=new Team();try{String[] p=s.split("\\t",-1);r.index=Integer.parseInt(p[0]);r.name=p[2];r.count=Integer.parseInt(p[3]);r.gk=Integer.parseInt(p[4]);r.def=Integer.parseInt(p[5]);r.mid=Integer.parseInt(p[6]);r.fw=Integer.parseInt(p[7]);r.ovr=Integer.parseInt(p[8]);r.cs=Integer.parseInt(p[9]);r.ga=Integer.parseInt(p[10]);r.user="1".equals(p[11]);}catch(Throwable ignored){}return r;}}
    private static final class Player {String name,pos;int number,age,ovr,st,ap,g,a,rat,rm,gkst,cs,ga,sv;static Player parse(String s){Player r=new Player();try{String[] p=s.split("\\t",-1);r.number=Integer.parseInt(p[1]);r.name=p[2];r.pos=p[3];r.age=Integer.parseInt(p[4]);r.ovr=Integer.parseInt(p[5]);r.st=Integer.parseInt(p[6]);r.ap=Integer.parseInt(p[7]);r.g=Integer.parseInt(p[8]);r.a=Integer.parseInt(p[9]);r.rat=Integer.parseInt(p[10]);r.rm=Integer.parseInt(p[11]);r.gkst=Integer.parseInt(p[12]);r.cs=Integer.parseInt(p[13]);r.ga=Integer.parseInt(p[14]);r.sv=Integer.parseInt(p[15]);}catch(Throwable ignored){}return r;}String rating(){return rm==0?"n/a":String.format(java.util.Locale.US,"%.1f",rat/10.0);}}
    private static ArrayList<Team> teams(){ArrayList<Team> out=new ArrayList<>();for(String s:rows("teamRows",new Class<?>[0],new Object[0]))out.add(Team.parse(s));return out;}
    private static ArrayList<Match> matches(int round){ArrayList<Match> out=new ArrayList<>();for(String s:rows("roundRows",new Class<?>[]{Integer.TYPE},new Object[]{Integer.valueOf(round)}))out.add(Match.parse(s));return out;}
    private static void showClub(Activity a,Team team){ArrayList<Player> ps=new ArrayList<>();for(String s:rows("playerRows",new Class<?>[]{Integer.TYPE},new Object[]{Integer.valueOf(team.index)}))ps.add(Player.parse(s));ListView list=new ListView(a);ArrayAdapter<Player> ad=new ArrayAdapter<Player>(a,android.R.layout.simple_list_item_1,ps){@Override public View getView(int n,View cv,ViewGroup parent){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,10),dp(a,5),dp(a,10),dp(a,5));Player p=getItem(n);row.addView(text(a,"#"+p.number+"  "+p.name+"  "+p.pos+"   OVR "+p.ovr,15f,true));String extra="ST "+p.st+"  AP "+p.ap+"  G "+p.g+"  A "+p.a+"  RAT "+p.rating();if("GK".equals(p.pos))extra+="\nGK LEDGER  ST "+p.gkst+"  CS "+p.cs+"  GA "+p.ga+"  SV "+p.sv;row.addView(text(a,extra,12.5f,false));return row;}};list.setAdapter(ad);AlertDialog d=new AlertDialog.Builder(a).setTitle(team.name+" — "+team.count+" / 25 players").setMessage("GK "+team.gk+"  DEF "+team.def+"  MID "+team.mid+"  FW "+team.fw+"   •   OVR "+team.ovr).setView(list).setPositiveButton("Close",null).create();d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.90f));});d.show();}

    public static void show(Activity a){
        try{if(!Boolean.TRUE.equals(bridge().getMethod("available").invoke(null))){Toast.makeText(a,"League data is unavailable",Toast.LENGTH_SHORT).show();return;}}catch(Throwable t){return;}
        final boolean pause=NativePause.begin();final int total=callInt("totalRounds"),played=callInt("playedMatches");final ArrayList<Team> clubs=teams();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,3),dp(a,8),0);
        LinearLayout tabs=new LinearLayout(a);tabs.setOrientation(LinearLayout.HORIZONTAL);Button results=new Button(a);results.setText("RESULTS");Button squads=new Button(a);squads.setText("CLUBS");Button durability=new Button(a);durability.setText("37–38");tabs.addView(results,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(squads,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(durability,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);
        Spinner round=new Spinner(a);String[] labels=new String[total];for(int i=0;i<total;i++)labels[i]="MATCHDAY "+(i+1)+(i<played?" — played":"");ArrayAdapter<String> ra=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,labels);ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);round.setAdapter(ra);if(total>0)round.setSelection(Math.max(0,Math.min(total-1,played==0?0:played-1)));root.addView(round);
        ListView list=new ListView(a);list.setDividerHeight(1);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        final int[] mode={0};Runnable refresh=()->{
            if(mode[0]==0){ArrayList<Match> ms=matches(round.getSelectedItemPosition()+1);list.setAdapter(new ArrayAdapter<Match>(a,android.R.layout.simple_list_item_1,ms){@Override public View getView(int n,View cv,ViewGroup p){Match m=getItem(n);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(dp(a,6),dp(a,8),dp(a,6),dp(a,8));TextView h=text(a,m.home,14f,m.user),sc=text(a,m.done?(m.hg+"  –  "+m.ag):"vs",15f,true),aw=text(a,m.away,14f,m.user);h.setGravity(android.view.Gravity.END);aw.setGravity(android.view.Gravity.START);sc.setGravity(android.view.Gravity.CENTER);row.addView(h,new LinearLayout.LayoutParams(0,-2,1));row.addView(sc,new LinearLayout.LayoutParams(dp(a,70),-2));row.addView(aw,new LinearLayout.LayoutParams(0,-2,1));if(m.user)row.setBackgroundColor(0xffe6f4e6);return row;}});list.setOnItemClickListener(null);}
            else if(mode[0]==1){list.setAdapter(new ArrayAdapter<Team>(a,android.R.layout.simple_list_item_1,clubs){@Override public View getView(int n,View cv,ViewGroup p){Team t=getItem(n);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,9),dp(a,6),dp(a,9),dp(a,6));row.addView(text(a,t.name+"   "+t.count+" / 25   OVR "+t.ovr,15f,true));row.addView(text(a,"GK "+t.gk+"  DEF "+t.def+"  MID "+t.mid+"  FW "+t.fw+"   •   CS "+t.cs+"  GA "+t.ga,12.5f,false));if(t.user)row.setBackgroundColor(0xffe6f4e6);return row;}});list.setOnItemClickListener((p,v,n,id)->showClub(a,clubs.get(n)));}
            else{String[] x=callString("durability").split("\\t",-1);String body="Finish a 38-round season to compare the simulation with the real 2010/11 league benchmark.";try{int p=Integer.parseInt(x[0]),all38=Integer.parseInt(x[2]),all37=Integer.parseInt(x[3]),target=Integer.parseInt(x[12]);body="Season progress: "+p+" / "+x[1]+"\n\n38 starts: "+all38+"\n37 starts: "+all37+"\nTOTAL: "+(all38+all37)+"   •   real 2010/11 target: "+target+"\n\nBy position (38 + 37)\nGK  "+x[4]+" + "+x[5]+"\nDEF "+x[6]+" + "+x[7]+"\nMID "+x[8]+" + "+x[9]+"\nFW  "+x[10]+" + "+x[11]+"\n\nThis screen measures every current league roster, not only the user squad.";}catch(Throwable ignored){}ArrayList<String> one=new ArrayList<>();one.add(body);list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,one){@Override public View getView(int n,View cv,ViewGroup p){TextView v=text(a,getItem(n),15f,false);v.setPadding(dp(a,14),dp(a,14),dp(a,14),dp(a,14));return v;}});list.setOnItemClickListener(null);}
            round.setVisibility(mode[0]==0?View.VISIBLE:View.GONE);};
        round.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});results.setOnClickListener(v->{mode[0]=0;refresh.run();});squads.setOnClickListener(v->{mode[0]=1;refresh.run();});durability.setOnClickListener(v->{mode[0]=2;refresh.run();});refresh.run();
        TextView note=text(a,"GK ledger: "+callInt("goalkeeperTrackedMatches")+" matchdays recorded (current R"+callInt("goalkeeperTrackedThrough")+"). CS/GA are exact; SV is the added deterministic keeper-stat simulation.",11f,false);note.setPadding(dp(a,5),dp(a,3),dp(a,5),dp(a,3));root.addView(note);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("League Center").setView(root).setPositiveButton("Close",null).create();d.setOnDismissListener(v->NativePause.end(pause));d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});d.show();
    }
}
