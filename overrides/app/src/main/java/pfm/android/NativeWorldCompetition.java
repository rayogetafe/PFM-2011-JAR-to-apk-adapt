package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Complete read-only competition browser plus an all-five-league market entry point. */
public final class NativeWorldCompetition {
    private static final String[] MODES={"TABLE","CALENDAR","STATISTICS","CLUBS","HISTORY"};
    private NativeWorldCompetition() {}
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setPadding(dp(c,5),dp(c,4),dp(c,5),dp(c,4));if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static int year(){int s=NativeWorldCenter.season();return s>=2000?s:2010+s;}
    private static int rounds(String code){return NativeCompetitionCalendar.leagueRounds(code);}
    private static String position(int p){return p==0?"GK":p==1?"DEF":p==2?"MID":"FW";}
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","");}
    private static String activeCode(){try{String code=String.valueOf(Class.forName("PfmSeasonBridge").getMethod("leagueCode").invoke(null));if(!code.isEmpty())return code;}catch(Exception ignored){}try{String[] rows=(String[])Class.forName("PfmSeasonBridge").getMethod("tableRows").invoke(null);String best="";int bestScore=0;for(String code:NativeWorldCenter.C){int score=0;for(NativeWorldCenter.T t:NativeWorldCenter.leagues.get(code))for(String row:rows)if(row!=null&&norm(row).contains(norm(t.name)))score++;if(score>bestScore){bestScore=score;best=code;}}if(bestScore>2)return best;}catch(Throwable ignored){}try{return String.valueOf(Class.forName("PfmSeasonBridge").getMethod("leagueCode").invoke(null));}catch(Throwable t){return NativeWorldCenter.active();}}
    private static ArrayList<String> leagueCodes(){ArrayList<String> out=new ArrayList<String>();String active=activeCode();for(String c:NativeWorldCenter.C)if(!c.equals(active))out.add(c);if(NativeWorldCenter.leagues.containsKey(NativeChampionship.CODE))out.add(NativeChampionship.CODE);return out;}
    private static String leagueName(String code){if(NativeChampionship.CODE.equals(code))return "Championship";for(int i=0;i<NativeWorldCenter.C.length;i++)if(NativeWorldCenter.C[i].equals(code))return NativeWorldCenter.N[i];return code;}
    private static int zone(String league,int rank,int count){
        if(NativeChampionship.CODE.equals(league))return rank<=2?0xffdcefd8:rank<=6?0xffdcecff:0xffffffff;
        return EuropeanPlaces.zone(league,rank,count);
    }
    private static ImageView badge(Context c,String code,int id){ImageView v=new ImageView(c);try{InputStream in=c.getAssets().open(NativeChampionship.CODE.equals(code)?"second_division_badges/en2_"+id+".png":"badges/"+code+(id+1)+".png");v.setImageBitmap(android.graphics.BitmapFactory.decodeStream(in));in.close();}catch(Throwable ignored){}v.setScaleType(ImageView.ScaleType.FIT_CENTER);return v;}
    private static TextView fixedCell(Context c,String value,int width,boolean bold,int gravity){TextView v=text(c,value,9.2f,bold);v.setGravity(gravity);v.setSingleLine(true);v.setPadding(0,0,0,0);v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,width),dp(c,27)));return v;}
    private static View tableRow(Context c,String code,NativeWorldCenter.T team,int rank,boolean header){LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);if(header)row.addView(new View(c),new LinearLayout.LayoutParams(dp(c,27),1));else row.addView(badge(c,team.clubId.substring(0,team.clubId.lastIndexOf(':')),team.id),new LinearLayout.LayoutParams(dp(c,27),dp(c,27)));row.addView(fixedCell(c,header?"#":String.valueOf(rank),26,header,Gravity.CENTER));TextView club=text(c,header?"CLUB":team.name,9.4f,header||!header);club.setSingleLine(true);club.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);club.setPadding(dp(c,2),0,0,0);row.addView(club,new LinearLayout.LayoutParams(0,dp(c,27),1f));String[] values=header?new String[]{"P","W","D","L","GF","GA","GD","PTS"}:new String[]{String.valueOf(team.p),String.valueOf(team.w),String.valueOf(team.d),String.valueOf(team.l),String.valueOf(team.gf),String.valueOf(team.ga),String.valueOf(team.gf-team.ga),String.valueOf(team.pts)};int[] widths={24,24,24,24,28,28,31,32};for(int i=0;i<values.length;i++)row.addView(fixedCell(c,values[i],widths[i],header||i==7,Gravity.CENTER));if(!header){GradientDrawable bg=new GradientDrawable();bg.setColor(zone(code,rank,NativeWorldCenter.leagues.get(code).size()));row.setBackground(bg);}return row;}
    private static ArrayList<NativeWorldCenter.T> ordered(ArrayList<NativeWorldCenter.T> teams,String code){ArrayList<NativeWorldCenter.T> out=new ArrayList<NativeWorldCenter.T>(teams);Collections.sort(out,new Comparator<NativeWorldCenter.T>(){public int compare(NativeWorldCenter.T a,NativeWorldCenter.T b){if(a.pts!=b.pts)return b.pts-a.pts;int ag=a.gf-a.ga,bg=b.gf-b.ga;if(ag!=bg)return bg-ag;if(a.gf!=b.gf)return b.gf-a.gf;return NativeEnglishDivisions.includes(code)?a.clubId.compareTo(b.clubId):0;}});return out;}
    private static ArrayAdapter<String> rows(Activity a,ArrayList<String> values){return new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,values){public View getView(int n,View old,ViewGroup parent){return text(a,getItem(n),13f,false);}};}
    private static String rating(NativeWorldCenter.P p){return p.ap==0?"n/a":String.format(Locale.US,"%.1f",p.rs/(10.0*p.ap));}
    private static void showProfile(Activity a,NativeWorldCenter.P p,String club){String country=p.nat.split(" • ")[0];String body="Club: "+club+"\nNationality: "+country+"\nPosition: "+position(p.pos)+"\nAge: "+p.age+"\nOverall: "+p.ovr+"\n\nSeason\nStarts: "+p.st+"\nAppearances: "+p.ap+"\nGoals: "+p.g+"\nAssists: "+p.a+"\nAverage rating: "+rating(p);body+=NativeMatchJournal.conditionText(p.uid);if(p.pos==0)body+="\nClean sheets: "+p.cs+"\nConceded: "+p.ga+"\nSaves: "+p.sv;new AlertDialog.Builder(a).setTitle(p.name).setMessage(body).setPositiveButton("Close",null).show();}
    private static void showClub(Activity a,NativeWorldCenter.T team){ArrayList<NativeWorldCenter.P> players=new ArrayList<NativeWorldCenter.P>(team.ps);Collections.sort(players,(x,y)->x.pos!=y.pos?x.pos-y.pos:y.ovr-x.ovr);ListView list=new ListView(a);list.setAdapter(new ArrayAdapter<NativeWorldCenter.P>(a,android.R.layout.simple_list_item_1,players){public View getView(int n,View old,ViewGroup parent){NativeWorldCenter.P p=getItem(n);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.addView(text(a,p.name+"  ·  "+p.nat.split(" • ")[0],14f,true));row.addView(text(a,position(p.pos)+"   AGE "+p.age+"   OVR "+p.ovr+"   G "+p.g+" A "+p.a,11.5f,false));return row;}});list.setOnItemClickListener((parent,v,n,id)->showProfile(a,players.get(n),team.name));AlertDialog d=new AlertDialog.Builder(a).setTitle(team.name+" — "+team.ps.size()+" players").setView(list).setPositiveButton("Close",null).create();d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.9f));});d.show();}
    private static int metric(NativeWorldCenter.P p,int mode){return mode==0?p.g:mode==1?p.a:mode==2?(p.ap==0?0:p.rs/p.ap):mode==3?p.ap:p.cs;}
    private static void showPlayoffs(Activity activity, List<NativeWorldCenter.T> clubs, int season) {
        try {
            MatchJournal journal = NativeMatchJournal.championshipJournal(activity, season);
            Map<String, String> names = new HashMap<>();
            for (NativeWorldCenter.T team : clubs) names.put(team.clubId, team.name);
            List<String> candidates = ChampionshipPlayoffs.promotionCandidates(journal);
            List<MatchJournal.Match> matches = journal.competition(ChampionshipPlayoffs.COMPETITION);
            ArrayList<String> lines = new ArrayList<>();
            ArrayList<MatchJournal.Match> reports = new ArrayList<>();
            if (candidates.isEmpty()) {
                lines.add(season < year() ? "No saved play-offs for this season." : "Played after the final league round.");
                reports.add(null);
            } else {
                lines.add("AUTOMATIC PLACES\n" + names.get(candidates.get(0)) + " · " + names.get(candidates.get(1)));
                reports.add(null);
                if (candidates.size() == 3) {
                    lines.add("PLAY-OFF WINNER\n" + names.get(candidates.get(2)));
                    reports.add(null);
                }
            }
            for (MatchJournal.Match match : matches) {
                String stage = match.round == 3 ? "Final · Wembley" : "Semi-final · leg " + match.round;
                String score = names.get(match.home) + "  " + match.homeGoals + "–" + match.awayGoals + "  " + names.get(match.away);
                if (match.duration == 120) score += " (a.e.t.)";
                if (match.homePenalties + match.awayPenalties > 0) score += " · pens " + match.homePenalties + "–" + match.awayPenalties;
                lines.add(stage + "\n" + score);
                reports.add(match);
            }
            ListView list = new ListView(activity);
            list.setAdapter(rows(activity, lines));
            list.setOnItemClickListener((p, v, index, id) -> NativeMatchJournal.showReport(activity, reports.get(index)));
            new AlertDialog.Builder(activity).setTitle("Championship play-offs · " + season + "/" + (season + 1))
                    .setView(list).setPositiveButton("Close", null).show();
        } catch (IOException error) {
            Toast.makeText(activity, "Play-off results unavailable", Toast.LENGTH_SHORT).show();
        }
    }
    private static void configureSub(Context c,Spinner sub,int mode,String code){ArrayList<String> values=new ArrayList<String>();if(mode==1)for(int i=1;i<=rounds(code);i++)values.add("MATCHDAY "+i);else if(mode==2)Collections.addAll(values,"GOALS","ASSISTS","RATING","APPEARANCES","GK CLEAN SHEETS");else if(mode==4){int last=year()-1;if(last<2010)values.add("NO COMPLETED SEASONS");else for(int y=2010;y<=last;y++)values.add(y+"/"+(y+1));}sub.setAdapter(new ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item,values));}
    public static void show(Activity a){
        NativeWorldCenter.load(a);try{NativeMatchJournal.advance(a,NativeTransferCalendar.played());NativeCareerStore.Career career=NativeCareerStore.ensure(a);NativeCareerStore.runAiMarket(a);career=NativeCareerStore.ensure(a);NativeCareerStore.applyToWorld(career);NativeCareerStore.syncLegacy(career);}catch(Throwable error){Toast.makeText(a,"Saved match history unavailable",Toast.LENGTH_LONG).show();return;}final ArrayList<String> codes=leagueCodes();if(codes.isEmpty()||NativeWorldCenter.leagues.isEmpty()){Toast.makeText(a,"Other league data is unavailable",Toast.LENGTH_SHORT).show();return;}final boolean paused=NativePause.begin();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);final Spinner league=new Spinner(a);ArrayList<String> names=new ArrayList<String>();for(String code:codes)names.add(leagueName(code));league.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,names));root.addView(league);Button market=new Button(a);market.setText("GLOBAL PLAYER MARKET");market.setOnClickListener(v->NativeGlobalMarket.show(a,NativeWorldCenter.allCodes()));root.addView(market);Button data=new Button(a);data.setText("NATIVE CAREER DATA • SCHEMA v14");data.setOnClickListener(v->NativeCareerStore.show(a));root.addView(data);
        HorizontalScrollView scrollTabs=new HorizontalScrollView(a);LinearLayout tabs=new LinearLayout(a);Button[] buttons=new Button[MODES.length];for(int i=0;i<MODES.length;i++){buttons[i]=new Button(a);buttons[i].setText(MODES[i]);tabs.addView(buttons[i]);}scrollTabs.addView(tabs);root.addView(scrollTabs);final Spinner sub=new Spinner(a);root.addView(sub);final LinearLayout header=new LinearLayout(a);header.setOrientation(LinearLayout.VERTICAL);root.addView(header);final ListView list=new ListView(a);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));final int[] mode={0};final Runnable[] refresh=new Runnable[1];
        refresh[0]=()->{String code=codes.get(league.getSelectedItemPosition());int through=NativeWorldCenter.progress(code);header.removeAllViews();list.setOnItemClickListener(null);sub.setVisibility(mode[0]==1||mode[0]==2||mode[0]==4?View.VISIBLE:View.GONE);
            if(mode[0]==4 && year()<=2010){list.setAdapter(rows(a,new ArrayList<String>(Collections.singletonList("No completed parallel seasons yet."))));return;}
            int seasonYear = mode[0] == 4 ? 2010 + Math.max(0, sub.getSelectedItemPosition()) : year();
            final ArrayList<NativeWorldCenter.T> teams;
            try {
                teams = NativeMatchJournal.leagueTeams(code, seasonYear);
            } catch (IOException error) {
                list.setAdapter(rows(a, new ArrayList<>(Collections.singletonList("Saved league membership unavailable."))));
                return;
            }
            if (mode[0] == 4) {
                NativeWorldCenter.simulateAt(code, rounds(code), seasonYear);
                if (teams.get(0).p == 0) {
                    list.setAdapter(rows(a, new ArrayList<>(Collections.singletonList("This season predates the saved match journal."))));
                    return;
                }
                showTable(a, list, header, code, teams, seasonYear);
                return;
            }
            if(NativeChampionship.CODE.equals(code)&&mode[0]!=3&&teams.get(0).activeFrom>year()){
                list.setAdapter(rows(a,new ArrayList<String>(Collections.singletonList("Championship fixtures start in "+teams.get(0).activeFrom+"/"+(teams.get(0).activeFrom+1)+". Open CLUBS for the imported squads."))));return;
            }
            ArrayList<NativeWorldCenter.M> matches=NativeWorldCenter.simulate(code,through);
            if(mode[0]==0){showTable(a,list,header,code,teams,year());}
            else if(mode[0]==1){int round=Math.max(1,sub.getSelectedItemPosition()+1);ArrayList<String> values=new ArrayList<String>();ArrayList<NativeWorldCenter.M> selected=new ArrayList<NativeWorldCenter.M>();for(NativeWorldCenter.M m:matches)if(m.r==round){selected.add(m);values.add(teams.get(m.h).name+"   "+(m.hg>=0?m.hg+" – "+m.ag:"vs")+"   "+teams.get(m.a).name);}list.setAdapter(rows(a,values));list.setOnItemClickListener((parent,view,n,id)->{NativeWorldCenter.M m=selected.get(n);if(m.hg<0)return;try{NativeMatchJournal.showReport(a,NativeMatchJournal.match(a,"league:"+code,m.r,teams.get(m.h).clubId,teams.get(m.a).clubId,NativeWorldCenter.season()));}catch(Exception error){Toast.makeText(a,"Saved fixture unavailable",Toast.LENGTH_SHORT).show();}});}
            else if(mode[0]==2){int sort=Math.max(0,sub.getSelectedItemPosition());ArrayList<NativeWorldCenter.P> players=new ArrayList<NativeWorldCenter.P>();for(NativeWorldCenter.T t:teams)players.addAll(t.ps);Collections.sort(players,(x,y)->metric(y,sort)-metric(x,sort));ArrayList<String> values=new ArrayList<String>();for(int i=0;i<Math.min(100,players.size());i++){NativeWorldCenter.P p=players.get(i);values.add((i+1)+". "+p.name+"  ·  "+p.nat.split(" • ")[0]+"\nG "+p.g+"  A "+p.a+"  ST "+p.st+"  AP "+p.ap+"  RAT "+rating(p)+(p.pos==0?"  CS "+p.cs+" SV "+p.sv:""));}list.setAdapter(rows(a,values));}
            else{ArrayList<NativeWorldCenter.T> clubs=new ArrayList<NativeWorldCenter.T>(teams);Collections.sort(clubs,(x,y)->x.name.compareToIgnoreCase(y.name));ArrayList<String> values=new ArrayList<String>();for(NativeWorldCenter.T t:clubs)values.add(t.name+"\n"+t.ps.size()+" players   OVR "+t.ovr);list.setAdapter(rows(a,values));list.setOnItemClickListener((parent,v,n,id)->showClub(a,clubs.get(n)));}
        };
        league.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){configureSub(a,sub,mode[0],codes.get(n));refresh[0].run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});sub.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){refresh[0].run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});for(int i=0;i<buttons.length;i++){final int selected=i;buttons[i].setOnClickListener(v->{mode[0]=selected;configureSub(a,sub,selected,codes.get(league.getSelectedItemPosition()));refresh[0].run();});}configureSub(a,sub,0,codes.get(0));refresh[0].run();AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Other Top-5 Leagues").setView(root).setNeutralButton("Second divisions",null).setPositiveButton("Close",null).create();dialog.setOnDismissListener(v->NativePause.end(paused));dialog.setOnShowListener(v->{dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(b->NativeSecondDivisionRegistry.show(a));Window w=dialog.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.95f));});dialog.show();
    }
    private static void showTable(Activity a,ListView list,LinearLayout header,String code,ArrayList<NativeWorldCenter.T> teams,int season){int played=0,goals=0,drawn=0;for(NativeWorldCenter.T team:teams){played+=team.p;goals+=team.gf;drawn+=team.d;}int games=played/2;header.addView(text(a,"Goals: "+goals+" / "+games+" matches · "+(games==0?"n/a":String.format(Locale.US,"%.2f",goals/(double)games))+" per match"+(NativeChampionship.CODE.equals(code)?"":" · 2010/11 ref "+String.format(Locale.US,"%.2f",MatchEngine.profile(code).historicalRate))+"\nDraws: "+(drawn/2)+" / "+games+" ("+(games==0?"n/a":String.format(Locale.US,"%.1f%%",100.0*(drawn/2)/games))+")"+(NativeChampionship.CODE.equals(code)?"":" · 2010/11 ref "+String.format(Locale.US,"%.1f%%",100*MatchEngine.profile(code).historicalDrawRate)),10.5f,false));if(NativeChampionship.CODE.equals(code)){Button playoffs=new Button(a);playoffs.setText("PROMOTION PLAY-OFFS");playoffs.setOnClickListener(v->showPlayoffs(a,teams,season));header.addView(playoffs);}header.addView(tableRow(a,code,null,0,true));TextView legend=text(a,NativeChampionship.CODE.equals(code)?"Green: automatic places · Blue: play-off places":"Base places: Blue UCL · Gold UEL · Red relegation",9.2f,true);legend.setBackgroundColor(0xfff4f4f4);header.addView(legend);final ArrayList<NativeWorldCenter.T> values=ordered(teams,code);list.setAdapter(new ArrayAdapter<NativeWorldCenter.T>(a,android.R.layout.simple_list_item_1,values){public View getView(int n,View old,ViewGroup parent){return tableRow(a,code,getItem(n),n+1,false);}});}
}
