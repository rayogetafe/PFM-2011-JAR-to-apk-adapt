package pfm.android;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Qualification uses the same archived standings and frozen entrants as the recurring UCL. */
public final class NativeEuropeCenter {
 private NativeEuropeCenter(){}
 private static class Q{String code,club,route;int rank,points;Q(String c,String n,int r,int p){code=c;club=n;rank=r;points=p;}}
 private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);v.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private static String league(String code){for(int i=0;i<NativeWorldCenter.C.length;i++)if(code.equals(NativeWorldCenter.C[i]))return NativeWorldCenter.N[i];return code;}
 private static int cl(String code){return UclSeason.slots(code);}
 private static int el(String code){return EuropeanPlaces.europa(code);}
 private static ArrayList<Q> qualifiers(boolean champions) {
     ArrayList<Q> out = new ArrayList<>();
     android.content.Context context = AndroidRuntime.context();
     int nextYear = NativeWorldCenter.season() + 1;
     try {
         Map<String, String> names = new HashMap<>();
         List<NativeCareerStore.Club> clubs = NativeCareerStore.ensure(context).clubs;
         for (NativeCareerStore.Club club : clubs) names.put(club.id, club.name);
         Set<String> frozen = Collections.emptySet();
         if (NativeTransferCalendar.played() >= NativeTransferCalendar.total()) {
             frozen = champions ? NativeMatchJournal.uclCandidates(context, nextYear)
                                : NativeMatchJournal.europaCandidates(context, nextYear);
         }
         Set<String> direct = new HashSet<>(NativeMatchJournal.directUcl(context, nextYear));
         boolean planned = !NativeMatchJournal.qualifyingPairs(context, nextYear).isEmpty();
         for (String code : NativeWorldCenter.C) {
             String cupEntrant = NativeMatchJournal.europaCupEntrant(context, nextYear, code);
             int from = champions ? 1 : cl(code) + 1;
             int to = champions ? cl(code) : cl(code) + el(code);
             int rank = 0;
             for (UclSeason.Standing row : NativeMatchJournal.qualifyingTable(context, code)) {
                 rank++;
                 if (frozen.isEmpty() ? rank < from || rank > to : !frozen.contains(row.id)) continue;
                 Q q = new Q(code, names.get(row.id), rank, row.points);
                 if (!champions) {
                     q.route = !frozen.isEmpty() && row.id.equals(cupEntrant) ? "UEL GROUPS · cup place" : "UEL GROUPS";
                 } else if (planned) {
                     q.route = direct.contains(row.id) ? "UCL GROUPS" : "UCL PLAYOFF";
                 } else if (!frozen.isEmpty()) {
                     q.route = "UCL GROUPS · frozen draw";
                 } else {
                     q.route = rank < cl(code) ? "UCL GROUPS" : "UCL PLAYOFF";
                 }
                 out.add(q);
             }
             if (!frozen.isEmpty()) {
                 for (NativeCareerStore.Club club : clubs) {
                     if (!club.league.equals(code) && EuropeanPlaces.association(club.league).equals(code) && frozen.contains(club.id)) {
                         Q q = new Q(code, club.name, 0, 0);
                         q.route = champions ? direct.contains(club.id) ? "UCL GROUPS" : "UCL PLAYOFF"
                                 : club.id.equals(cupEntrant) ? "UEL GROUPS · cup place" : "UEL GROUPS";
                         out.add(q);
                     }
                 }
             }
         }
     } catch (Exception ignored) {}
     return out;
 }
 private static ArrayAdapter<Q> adapter(Activity a,ArrayList<Q> qs){return new ArrayAdapter<Q>(a,android.R.layout.simple_list_item_1,qs){public View getView(int n,View old,ViewGroup parent){Q q=getItem(n);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.addView(text(a,q.club,15f,true));String position=q.rank>0?"league position #"+q.rank+"  •  "+q.points+" pts":"second division";row.addView(text(a,league(q.code)+"  •  "+position+" • "+q.route,11.8f,false));return row;}};}
 public static void show(Activity a){NativeWorldCenter.load(a);final boolean pause=NativePause.begin();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);TextView note=text(a,"Europa League: two league places and one cup place per top-five league. League positions are provisional until qualification is fixed.",13f,false);note.setBackgroundColor(0xfff2f2f2);root.addView(note);Button groups=new Button(a);groups.setText("CHAMPIONS LEAGUE • CURRENT SEASON / HISTORY");groups.setOnClickListener(v->NativeChampionsLeague.show(a));root.addView(groups);Button europaResults=new Button(a);europaResults.setText("EUROPA LEAGUE • CURRENT SEASON / HISTORY");europaResults.setOnClickListener(v->NativeChampionsLeague.showEuropa(a));root.addView(europaResults);Button quotas=new Button(a);quotas.setText("QUOTAS • UCL / EUROPA LEAGUE");quotas.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("European places").setMessage(EuropeanPlaces.quotas()).setPositiveButton("Close",null).show());root.addView(quotas);Button registry=new Button(a);registry.setText("EUROPEAN CLUB REGISTRY");registry.setOnClickListener(v->NativeEuropeanRegistry.show(a));LinearLayout extra=new LinearLayout(a);extra.addView(registry,new LinearLayout.LayoutParams(0,-2,1));Button cups=new Button(a);cups.setText("DOMESTIC CUPS");cups.setOnClickListener(v->NativeDomesticCups.show(a));extra.addView(cups,new LinearLayout.LayoutParams(0,-2,1));root.addView(extra);LinearLayout tabs=new LinearLayout(a);Button champions=new Button(a),europa=new Button(a);champions.setText("CHAMPIONS LEAGUE");europa.setText("EUROPA LEAGUE");tabs.addView(champions,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(europa,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);TextView status=text(a,"",12.5f,true);root.addView(status);ListView list=new ListView(a);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));final boolean[] clMode={true};Runnable refresh=()->{ArrayList<Q> rows=qualifiers(clMode[0]);list.setAdapter(adapter(a,rows));status.setText((NativeTransferCalendar.played()>=NativeTransferCalendar.total()?"ENTRY PLACES FOR NEXT SEASON":"RACE FOR NEXT SEASON")+"  •  "+rows.size()+" TOP-5 SLOTS");champions.setEnabled(!clMode[0]);europa.setEnabled(clMode[0]);};champions.setOnClickListener(v->{clMode[0]=true;refresh.run();});europa.setOnClickListener(v->{clMode[0]=false;refresh.run();});refresh.run();AlertDialog d=new AlertDialog.Builder(a).setTitle("European Qualification").setView(root).setPositiveButton("Close",null).create();d.setOnDismissListener(v->NativePause.end(pause));d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});d.show();}
}
