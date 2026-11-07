package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public class CupBracketHarness {
    private static void verifySizes() {
        List<String> entrants = new ArrayList<>();
        for (int i = 0; i < 64; i++) entrants.add("test:" + i);
        for (int size = 2; size <= 64; size++) {
            DomesticCup.Bracket cup = new DomesticCup.Bracket(EuropeanQualification.encode(entrants.subList(0, size)));
            List<MatchJournal.Match> matches = new ArrayList<>();
            int date = 0;
            for (int round = 1; round <= cup.rounds; round++) {
                check(cup.mark(round) > date, "rounds overlap for " + size + " clubs");
                date = cup.mark(round);
                List<String[]> pairs = cup.fixtures(matches, round);
                Set<String> clubs = new HashSet<>();
                for (String[] pair : pairs) {
                    check(clubs.add(pair[0]) && clubs.add(pair[1]), "duplicate in one round");
                    MatchJournal.Match result = new MatchJournal.Match();
                    result.round = round;
                    result.winner = pair[0];
                    matches.add(result);
                }
            }
            check(matches.size() == size - 1 && date == 36, "wrong fixture count or final date");
        }
    }

    public static void main(String[] args) throws Exception {
        verifySizes();
        int nonce = 990;
        Context context = new Context("overrides/app/src/main/assets", new File(args[1]));
        AndroidRuntime.current = context;
        JournalHarness.world(args[0], nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeMatchJournal.advance(context, 1);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        DomesticCup.Bracket oldCup = DomesticCup.bracket(old, "en");
        check(oldCup.entrants.size() == 20 && oldCup.rounds == 5, "existing cup format");
        String frozen = old.metadata("cup-entrants-en");

        ArrayList<NativeCareerStore.Player> template = new ArrayList<>();
        for (NativeCareerStore.Player p : career.players) if (p.owner.equals("en:0")) template.add(p);
        for (int i = 0; i < 25; i++) {
            NativeCareerStore.Club club = new NativeCareerStore.Club();
            club.id = "en2:" + i;
            club.name = "Test club " + i;
            club.league = "en2";
            club.sourceId = i;
            club.activeFrom = i < 24 ? 2011 : 2012;
            career.clubs.add(club);
            for (NativeCareerStore.Player base : template) {
                NativeCareerStore.Player p = new NativeCareerStore.Player();
                p.id = club.id + ":" + base.sourceId;
                p.sourceId = base.sourceId;
                p.owner = club.id;
                p.name = base.name;
                p.position = base.position;
                p.overall = base.overall;
                p.spe = base.spe;
                p.res = base.res;
                p.qua = base.qua;
                p.morale = base.morale;
                career.players.add(p);
            }
        }
        NativeWorldCenter.played = 6;
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 6);
        old = JournalHarness.open(context, nonce, 2010);
        check(frozen.equals(old.metadata("cup-entrants-en")), "old cup changed after roster expansion");
        check(old.competition("cup:en").size() == 4, "old preliminary draw was replaced");

        // Simulate the expanded cup on the same match engine as a career fixture.
        MatchJournal expanded = JournalHarness.open(context, nonce, 2011);
        NativeWorldCenter.year = 2011;
        MatchEffects effects = new MatchEffects(38);
        HashMap<String, NativeWorldCenter.T> teams = NativeMatchJournal.teams(career);
        HashMap<String, NativeWorldCenter.P> players = new HashMap<>();
        for (NativeWorldCenter.T team : teams.values()) for (NativeWorldCenter.P p : team.ps) players.put(p.uid, p);
        for (int step = 1; step <= 36; step++) {
            effects.rest(step);
            DomesticCup.advance(expanded, nonce, step, 38, teams, players, "native-v84", effects, career);
        }
        expanded.save();
        expanded = JournalHarness.open(context, nonce, 2011);
        DomesticCup.Bracket cup = DomesticCup.bracket(expanded, "en");
        check(cup.entrants.size() == 44 && !cup.entrants.contains("en2:24") && cup.rounds == 6,
                "expanded entry list or activation date");
        int[] expectedMatches = {12, 16, 8, 4, 2, 1};
        int[] expectedDates = {6, 9, 13, 20, 28, 36};
        List<MatchJournal.Match> matches = expanded.competition("cup:en");
        Set<String> eliminated = new HashSet<>();
        Set<String> appeared = new HashSet<>();
        int[] count = new int[6];
        for (MatchJournal.Match m : matches) {
            check(!eliminated.contains(m.home) && !eliminated.contains(m.away), "knocked-out club returned");
            check(m.winner.equals(m.home) || m.winner.equals(m.away), "tie unresolved");
            check(m.clock == expectedDates[m.round - 1] && m.effectsVersion > 0, "cup date or match effects");
            eliminated.add(m.winner.equals(m.home) ? m.away : m.home);
            appeared.add(m.home);
            appeared.add(m.away);
            count[m.round - 1]++;
        }
        MatchJournal.Match finalMatch = DomesticCup.finalResult(expanded, "en");
        check(matches.size() == 43 && eliminated.size() == 43 && appeared.equals(new HashSet<>(cup.entrants)) &&
                Arrays.equals(count, expectedMatches), "expanded bracket lost or duplicated a club");
        check(finalMatch.report.contains("neutral venue"), "expanded final was not neutral");

        // Give a second-division finalist a cup win and run the actual European entry planner.
        MatchJournal cupWin = new MatchJournal(new File(args[1], "qualification"), nonce, 2011);
        cupWin.define("cup-entrants-en", expanded.metadata("cup-entrants-en"));
        finalMatch.home = "en2:0";
        finalMatch.away = "en:0";
        finalMatch.winner = finalMatch.home;
        cupWin.put(finalMatch);
        MatchJournal next = cupWin.openSeason(2012);
        EuropeanQualification.prepare(next, cupWin, career, nonce);
        check("en2:0".equals(next.metadata("uel-cup-en")), "second-division cup winner missed Europa");
        int england = 0;
        for (String id : EuropeanQualification.ids(next.metadata("uelq-base"))) {
            if (id.startsWith("en:") || id.startsWith("en2:")) england++;
        }
        check(england == 3, "expanded cup created an extra Europa place");

        for (NativeCareerStore.Club club : career.clubs) {
            if (club.id.equals("en2:0")) club.league = "en";
            if (club.id.equals("en:0")) club.league = "en2";
        }
        DomesticCup.advance(next, nonce, 1, 38, teams, players, "native-v84", effects, career);
        List<String> newEntrants = DomesticCup.bracket(next, "en").entrants;
        check(newEntrants.size() == 45 && new HashSet<>(newEntrants).size() == 45 &&
                newEntrants.containsAll(cup.entrants) && newEntrants.contains("en2:24"), "promotion, relegation or new activation altered entries");
        System.out.println("PASS: brackets for 2 to 64 entrants, legacy 20-club draw, 44-club cup with 43 decided ties, lower-tier Europa place, new entrants and promotion/relegation");
    }
}
