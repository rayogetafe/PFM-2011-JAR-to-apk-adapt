package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
import java.io.IOException;
import java.util.*;

/** Displays saved results and draws that can be derived from completed rounds. */
final class NativeDomesticCups {
    static void show(Activity activity) {
        boolean pause = NativePause.begin();
        try {
            NativeCareerStore.Career career = NativeCareerStore.ensure(activity);
            Map<String, String> names = new HashMap<>();
            for (NativeCareerStore.Club club : career.clubs) names.put(club.id, club.name);
            int currentYear = NativeWorldCenter.season();
            List<String> seasons = new ArrayList<>();
            for (int year = currentYear; year >= 2010; year--) seasons.add(year + "/" + (year + 1));

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            Spinner year = spinner(activity, seasons);
            Spinner league = spinner(activity, Arrays.asList(NativeWorldCenter.N));
            root.addView(year);
            root.addView(league);
            league.setSelection(Math.max(0, Arrays.asList(NativeWorldCenter.C).indexOf(career.activeLeague)));
            Button entrants = new Button(activity);
            root.addView(entrants);
            ListView list = new ListView(activity);
            root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
            Runnable refresh = () -> {
                int selectedYear = currentYear - Math.max(0, year.getSelectedItemPosition());
                int selectedLeague = Math.max(0, league.getSelectedItemPosition());
                String code = NativeWorldCenter.C[selectedLeague];
                List<String> lines = new ArrayList<>();
                List<MatchJournal.Match> reports = new ArrayList<>();
                entrants.setText("Entrants · draw pending");
                entrants.setEnabled(false);
                try {
                    MatchJournal journal = NativeMatchJournal.domesticCupJournal(activity, selectedYear);
                    DomesticCup.Bracket cup = DomesticCup.bracket(journal, code);
                    if (cup != null) {
                        ArrayList<String> clubNames = new ArrayList<>();
                        for (String id : cup.entrants) clubNames.add(names.containsKey(id) ? names.get(id) : id);
                        Collections.sort(clubNames, String.CASE_INSENSITIVE_ORDER);
                        entrants.setText("Entrants · " + clubNames.size() + " clubs");
                        entrants.setEnabled(true);
                        entrants.setOnClickListener(v -> new AlertDialog.Builder(activity)
                                .setTitle("Cup entrants · " + selectedYear + "/" + (selectedYear + 1))
                                .setItems(clubNames.toArray(new String[0]), null).setPositiveButton("Close", null).show());
                    } else {
                        lines.add("Draw pending");
                        reports.add(null);
                    }
                    MatchJournal.Match finalResult = DomesticCup.finalResult(journal, code);
                    if (finalResult != null) {
                        lines.add("CUP WINNER  •  " + names.get(finalResult.winner));
                        reports.add(finalResult);
                    }
                    String cupEntrant = NativeMatchJournal.europaCupEntrant(activity, selectedYear + 1, code);
                    if (cupEntrant != null && !cupEntrant.isEmpty()) {
                        lines.add("EUROPA LEAGUE CUP PLACE  •  " + names.get(cupEntrant));
                        reports.add(null);
                    }
                    int total = career.activeLeague.equals("de") ? 34 : 38;
                    List<MatchJournal.Match> matches = journal.competition(DomesticCup.competition(code));
                    for (int round = 1; cup != null && round <= cup.rounds; round++) {
                        int leagueRound = NativeCompetitionCalendar.sourceRound(cup.mark(round), total);
                        lines.add(cup.stage(round) + "  •  after league round " + leagueRound);
                        reports.add(null);
                        List<String[]> pairs = cup.fixtures(matches, round);
                        if (pairs.isEmpty()) {
                            lines.add("Draw pending");
                            reports.add(null);
                        }
                        for (String[] pair : pairs) {
                            MatchJournal.Match match = journal.get(NativeMatchJournal.key(
                                    DomesticCup.competition(code), round, pair[0], pair[1]));
                            String score = match == null ? " vs " : "  " + match.homeGoals + "–" + match.awayGoals + "  ";
                            String line = names.get(pair[0]) + score + names.get(pair[1]);
                            if (match != null && match.duration == 120) line += " (a.e.t.)";
                            if (match != null && match.homePenalties + match.awayPenalties > 0) {
                                line += "  pens " + match.homePenalties + "–" + match.awayPenalties;
                            }
                            lines.add(line);
                            reports.add(match);
                        }
                    }
                } catch (IOException error) {
                    lines.clear();
                    reports.clear();
                    lines.add("Cup history unavailable");
                    reports.add(null);
                }
                list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, lines));
                list.setOnItemClickListener((parent, view, position, id) ->
                        NativeMatchJournal.showReport(activity, reports.get(position)));
            };
            AdapterView.OnItemSelectedListener change = new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    refresh.run();
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            };
            year.setOnItemSelectedListener(change);
            league.setOnItemSelectedListener(change);
            refresh.run();
            AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Domestic cups")
                    .setView(root).setPositiveButton("Close", null).create();
            dialog.setOnDismissListener(v -> NativePause.end(pause));
            dialog.setOnShowListener(v -> {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setLayout(-1, Math.round(activity.getResources().getDisplayMetrics().heightPixels * .92f));
                }
            });
            dialog.show();
        } catch (Exception error) {
            NativePause.end(pause);
            Toast.makeText(activity, "Cup data unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private static Spinner spinner(Activity activity, List<String> options) {
        Spinner spinner = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }
}
