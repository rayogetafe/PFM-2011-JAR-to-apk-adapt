package pfm.android;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Top-five qualification layer for the future native European competitions. */
public final class NativeEuropeCenter {
 private NativeEuropeCenter(){}
 private static class Q{String code,club;int rank,points;Q(String c,String n,int r,int p){code=c;club=n;rank=r;points=p;}}
 private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private static String active(){try{return String.valueOf(Class.forName("PfmSeasonBridge").getMethod("leagueCode").invoke(null));}catch(Throwable t){return NativeWorldCenter.active();}}
 private static String league(String code){for(int i=0;i<NativeWorldCenter.C.length;i++)if(code.equals(NativeWorldCenter.C[i]))return NativeWorldCenter.N[i];return code;}
 private static int cl(String code){return "en".equals(code)||"es".equals(code)||"it".equals(code)?4:3;}
 private static int el(String code){return "fr".equals(code)?1:2;}
 private static ArrayList<Q> activeRows(String code){ArrayList<Q> out=new ArrayList<Q>();try{String[] rows=(String[])Class.forName("PfmSeasonBridge").getMethod("tableRows").invoke(null);for(String row:rows){String[] p=row.split("\t",-1);if(p.length>=10)out.add(new Q(code,p[1],Integer.parseInt(p[0]),Integer.parseInt(p[9])));}}catch(Throwable ignored){}return out;}
 private static ArrayList<Q> parallelRows(String code){ArrayList<Q> out=new ArrayList<Q>();ArrayList<NativeWorldCenter.T> teams=NativeWorldCenter.leagues.get(code);if(teams==null)return out;NativeWorldCenter.simulate(code,NativeWorldCenter.progress(code));ArrayList<NativeWorldCenter.T> order=new ArrayList<NativeWorldCenter.T>(teams);Collections.sort(order,(a,b)->{if(a.pts!=b.pts)return b.pts-a.pts;int ag=a.gf-a.ga,bg=b.gf-b.ga;if(ag!=bg)return bg-ag;return b.gf-a.gf;});for(int i=0;i<order.size();i++)out.add(new Q(code,order.get(i).name,i+1,order.get(i).pts));return out;}
 private static ArrayList<Q> qualifiers(boolean champions){ArrayList<Q> out=new ArrayList<Q>();String active=active();for(String code:NativeWorldCenter.C){ArrayList<Q> table=code.equals(active)?activeRows(code):parallelRows(code);int from=champions?1:cl(code)+1,to=champions?cl(code):cl(code)+el(code);for(Q q:table)if(q.rank>=from&&q.rank<=to)out.add(q);}return out;}
 private static ArrayAdapter<Q> adapter(Activity a,ArrayList<Q> qs){return new ArrayAdapter<Q>(a,android.R.layout.simple_list_item_1,qs){public View getView(int n,View old,ViewGroup parent){Q q=getItem(n);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.addView(text(a,q.club,15f,true));row.addView(text(a,league(q.code)+"  •  league position #"+q.rank+"  •  "+q.points+" pts",11.8f,false));return row;}};}
 public static void show(Activity a){NativeWorldCenter.load(a);final boolean pause=NativePause.begin();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);TextView note=text(a,"Qualification race still uses live top-five league tables. The 129 additional clubs now have native players and can participate in the transfer market; European fixtures are not yet simulated.",13f,false);note.setBackgroundColor(0xfff2f2f2);root.addView(note);Button registry=new Button(a);registry.setText("EUROPEAN CLUB REGISTRY • 129 CLUBS");registry.setOnClickListener(v->NativeEuropeanRegistry.show(a));root.addView(registry);LinearLayout tabs=new LinearLayout(a);Button champions=new Button(a),europa=new Button(a);champions.setText("CHAMPIONS LEAGUE");europa.setText("EUROPA LEAGUE");tabs.addView(champions,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(europa,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);TextView status=text(a,"",12.5f,true);root.addView(status);ListView list=new ListView(a);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));final boolean[] clMode={true};Runnable refresh=()->{ArrayList<Q> rows=qualifiers(clMode[0]);list.setAdapter(adapter(a,rows));status.setText((NativeTransferCalendar.played()>=NativeTransferCalendar.total()?"QUALIFIED FOR NEXT SEASON":"CURRENTLY QUALIFYING")+"  •  "+rows.size()+" TOP-5 SLOTS");champions.setEnabled(!clMode[0]);europa.setEnabled(clMode[0]);};champions.setOnClickListener(v->{clMode[0]=true;refresh.run();});europa.setOnClickListener(v->{clMode[0]=false;refresh.run();});refresh.run();AlertDialog d=new AlertDialog.Builder(a).setTitle("European Qualification").setView(root).setPositiveButton("Close",null).create();d.setOnDismissListener(v->NativePause.end(pause));d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});d.show();}
}
