package pfm.android;

import android.app.*;
import android.widget.*;
import android.view.*;
import java.util.*;

/** Read-only qualification projection; opening this screen never plays a leg or draws groups. */
final class NativeQualifiers {
    static void show(Activity activity,int year){
        final boolean pause=NativePause.begin();
        try{
            NativeCareerStore.Career career=NativeCareerStore.ensure(activity);HashMap<String,String> names=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)names.put(club.id,club.name);
            ArrayList<String[]> pairs=NativeMatchJournal.qualifyingPairs(activity,year);HashMap<String,MatchJournal.Match> saved=new HashMap<String,MatchJournal.Match>();for(MatchJournal.Match m:NativeMatchJournal.qualifyingMatches(activity,year))saved.put(m.id,m);
            LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);TextView note=new TextView(activity);note.setText("22 direct UCL group entrants + 10 playoff winners. Ten losers enter Europa groups. Legs follow league rounds 1 and 2; earlier external rounds are estimated.");note.setPadding(12,10,12,10);root.addView(note);
            LinearLayout entries = new LinearLayout(activity);
            Button direct=new Button(activity);direct.setText("22 DIRECT ENTRANTS");entries.addView(direct,new LinearLayout.LayoutParams(0,-2,1));direct.setOnClickListener(v->{try{ArrayList<String> values=new ArrayList<String>();for(String id:NativeMatchJournal.directUcl(activity,year))values.add(names.get(id));new AlertDialog.Builder(activity).setTitle("Direct UCL group entrants").setItems(values.toArray(new String[values.size()]),null).setPositiveButton("Close",null).show();}catch(Exception e){Toast.makeText(activity,"Direct entries unavailable",Toast.LENGTH_SHORT).show();}});
            Button ranking = new Button(activity);
            ranking.setText("CLUB SEEDING");
            ranking.setOnClickListener(v -> showSeeding(activity, year, names));
            entries.addView(ranking, new LinearLayout.LayoutParams(0,-2,1));
            root.addView(entries);
            LinearLayout buttons=new LinearLayout(activity);for(boolean europa:new boolean[]{false,true}){Button groups=new Button(activity);groups.setText(europa?"EUROPA GROUPS":"UCL GROUPS");groups.setEnabled((europa?NativeMatchJournal.europaGroups(activity,year):NativeMatchJournal.uclGroups(activity,year)).size()==(europa?12:8));groups.setOnClickListener(v->NativeChampionsLeague.showGroups(activity,year,europa));buttons.addView(groups,new LinearLayout.LayoutParams(0,-2,1));}root.addView(buttons);
            Button history=new Button(activity);history.setText("PREVIOUS EUROPEAN SEASONS");root.addView(history);history.setOnClickListener(v->{ArrayList<String> labels=new ArrayList<String>();ArrayList<Integer> years=new ArrayList<Integer>();ArrayList<Boolean> modes=new ArrayList<Boolean>();for(int y=year-1;y>=2010;y--)for(boolean europa:new boolean[]{false,true})try{if((europa?NativeMatchJournal.europaGroups(activity,y):NativeMatchJournal.uclGroups(activity,y)).size()==(europa?12:8)){labels.add((europa?"UEL ":"UCL ")+y+"/"+String.format(java.util.Locale.US,"%02d",(y+1)%100));years.add(y);modes.add(europa);}}catch(Exception ignored){}new AlertDialog.Builder(activity).setTitle("European history").setItems(labels.toArray(new String[labels.size()]),(dialog,which)->NativeChampionsLeague.showGroups(activity,years.get(which),modes.get(which))).setNegativeButton("Cancel",null).show();});
            ArrayList<String> lines=new ArrayList<String>();ArrayList<MatchJournal.Match> reports=new ArrayList<MatchJournal.Match>();int index=0;for(String[] pair:pairs){lines.add((index++<5?"LEAGUE PATH":"CHAMPIONS PATH")+" • "+names.get(pair[0])+" / "+names.get(pair[1])+" (seeded)");reports.add(null);for(int leg=0;leg<2;leg++){String home=leg==0?pair[0]:pair[1],away=leg==0?pair[1]:pair[0];MatchJournal.Match m=saved.get(UclKnockout.key(EuropeanQualification.competition(year),leg+1,home,away));lines.add("Leg "+(leg+1)+": "+names.get(home)+(m==null?" vs ":" "+m.homeGoals+"–"+m.awayGoals+" ")+names.get(away)+(m!=null&&m.duration==120?" (a.e.t.)":"")+(m!=null&&m.homePenalties+m.awayPenalties>0?" pens "+m.homePenalties+"–"+m.awayPenalties:""));reports.add(m);if(m!=null&&m.winner.length()>0){lines.add("UCL GROUPS: "+names.get(m.winner)+"\nEUROPA GROUPS: "+names.get(m.winner.equals(home)?away:home));reports.add(null);}}}
            ListView list=new ListView(activity);list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,lines));list.setOnItemClickListener((parent,view,position,id)->NativeMatchJournal.showReport(activity,reports.get(position)));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
            AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("UCL qualifying playoffs • "+year+"/"+String.format(java.util.Locale.US,"%02d",(year+1)%100)).setView(root).setPositiveButton("Close",null).create();dialog.setOnDismissListener(v->NativePause.end(pause));dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.92f));});dialog.show();
        }catch(Exception e){NativePause.end(pause);Toast.makeText(activity,"Qualifying data unavailable",Toast.LENGTH_SHORT).show();}
    }

    private static void showSeeding(Activity activity, int year, Map<String, String> names) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                .setTitle("Club seeding • " + year + "/" + (year + 1)).setPositiveButton("Close", null);
        try {
            Map<String, Integer> ranking = NativeMatchJournal.qualifyingSeeding(activity, year);
            if (ranking == null) {
                dialog.setMessage("This draw was saved with squad-strength seeding.");
            } else if (ranking.isEmpty()) {
                dialog.setMessage("No European points have been recorded for this draw. Seeding uses squad strength.");
            } else {
                LinearLayout panel = new LinearLayout(activity);
                panel.setOrientation(LinearLayout.VERTICAL);
                TextView note = new TextView(activity);
                note.setPadding(12, 10, 12, 10);
                note.setText("Up to five previous seasons. Wins and draws earn 2 and 1 points in UCL and Europa, half in qualifying. Squad strength breaks ties. European titleholders lead their group draw.");
                panel.addView(note);
                ArrayList<String> rows = new ArrayList<>();
                int rank = 0;
                for (Map.Entry<String, Integer> entry : ranking.entrySet()) {
                    String name = names.containsKey(entry.getKey()) ? names.get(entry.getKey()) : entry.getKey();
                    rows.add(String.format(Locale.US, "%d. %s • %.1f pts", ++rank, name, entry.getValue() / 2.0));
                }
                ListView list = new ListView(activity);
                list.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, rows));
                panel.addView(list, new LinearLayout.LayoutParams(-1, -1));
                dialog.setView(panel);
            }
        } catch (Exception error) {
            dialog.setMessage("Seeding data unavailable.");
        }
        dialog.show();
    }
}
