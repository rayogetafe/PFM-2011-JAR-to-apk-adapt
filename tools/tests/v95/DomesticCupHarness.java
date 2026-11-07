package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;

public class DomesticCupHarness {
    public static void main(String[] args) throws Exception {
        File root = new File(args[1]);
        Context context = new Context("overrides/app/src/main/assets", root);
        AndroidRuntime.current = context;
        JournalHarness.world(args[0], 950);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.activeLeague = "de";

        for (int step = 1; step <= 34; step++) {
            NativeWorldCenter.played = step;
            NativeMatchJournal.advance(context, step);
        }
        MatchJournal season = JournalHarness.open(context, 950, 2010);
        MatchJournal next = JournalHarness.open(context, 950, 2011);
        Set<String> champions = EuropeanQualification.candidates(next);
        Set<String> europa = new HashSet<>(EuropeanQualification.ids(next.metadata("uelq-base")));
        for (String league : NativeWorldCenter.C) {
            List<MatchJournal.Match> matches = season.competition(DomesticCup.competition(league));
            check(matches.size() == (league.equals("de") ? 17 : 19), "cup fixture count");
            Set<String> eliminated = new HashSet<>();
            for (MatchJournal.Match match : matches) {
                check(!eliminated.contains(match.home) && !eliminated.contains(match.away), "eliminated club returned");
                check(match.winner.equals(match.home) || match.winner.equals(match.away), "unresolved tie");
                eliminated.add(match.winner.equals(match.home) ? match.away : match.home);
                check(match.clock == NativeCompetitionCalendar.sourceRound(DomesticCup.ROUNDS[match.round - 1], 34),
                        "cup scheduled on wrong round");
                check(match.effectsVersion > 0 && match.homeCredit == 0 && match.awayCredit == 0, "cup effects or budget");
            }
            MatchJournal.Match finalMatch = DomesticCup.finalResult(season, league);
            String runnerUp = finalMatch.winner.equals(finalMatch.home) ? finalMatch.away : finalMatch.home;
            check(DomesticCup.europaEntrant(season, league, Collections.emptySet()).equals(finalMatch.winner),
                    "winner did not receive a cup place");
            check(DomesticCup.europaEntrant(season, league, Collections.singleton(finalMatch.winner)).equals(runnerUp),
                    "eligible finalist did not inherit place");
            check(DomesticCup.europaEntrant(season, league, new HashSet<>(Arrays.asList(finalMatch.winner, runnerUp))).isEmpty(),
                    "blocked finalists should return a place to league table");
            String entrant = next.metadata("uel-cup-" + league);
            check(entrant != null && (entrant.isEmpty() || europa.contains(entrant) && !champions.contains(entrant)),
                    "cup allocation not used in frozen Europa plan");
            int count = 0;
            for (String club : europa) if (club.startsWith(league + ":")) count++;
            check(count == 3, "cup place changed association quota");
        }
        System.out.println("Five cups and 93 resolved ties. Finalists, Europa allocation and German calendar verified.");

        JournalHarness.world(args[0], 951);
        NativeCareerStore.current.activeLeague = "de";
        MatchJournal legacy = JournalHarness.open(context, 951, 2010);
        for (MatchJournal.Match match : season.all()) {
            if (!match.competition.startsWith("cup:")) legacy.put(match);
        }
        legacy.save();
        MatchJournal legacyNext = JournalHarness.open(context, 951, 2011);
        EuropeanQualification.prepare(legacyNext, legacy, NativeCareerStore.current, 951);
        legacyNext.save();
        String frozenBase = legacyNext.metadata("uelq-base");
        String frozenPairs = legacyNext.metadata("uclq-pairs");
        NativeWorldCenter.played = 34;
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 34);
        MatchJournal upgraded = JournalHarness.open(context, 951, 2010);
        for (MatchJournal.Match match : legacy.all()) {
            check(JournalHarness.frozen(match).equals(JournalHarness.frozen(upgraded.get(match.id))),
                    "existing fixture rewritten");
        }
        for (String league : NativeWorldCenter.C) {
            for (MatchJournal.Match match : upgraded.competition(DomesticCup.competition(league))) {
                check(match.effectsVersion == 0 && match.source.equals("reconstructed-v95"), "retroactive cup effects");
            }
        }
        MatchJournal upgradedNext = JournalHarness.open(context, 951, 2011);
        check(frozenBase.equals(upgradedNext.metadata("uelq-base")) && frozenPairs.equals(upgradedNext.metadata("uclq-pairs")),
                "frozen qualification overwritten");
        System.out.println("PASS: v94 results and frozen qualification preserved, reconstructed cups without retroactive effects.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
