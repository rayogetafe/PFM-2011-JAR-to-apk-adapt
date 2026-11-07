package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public class EuropeanSeedingHarness {
    private static void result(MatchJournal journal, String competition, int round,
                               String home, String away, int homeGoals, int awayGoals) {
        MatchJournal.Match match = new MatchJournal.Match();
        match.competition = competition + ":" + journal.season();
        match.id = NativeMatchJournal.key(match.competition, round, home, away);
        match.home = home;
        match.away = away;
        match.round = round;
        match.homeGoals = homeGoals;
        match.awayGoals = awayGoals;
        journal.put(match);
    }

    private static void checkPots(List<? extends List<String>> groups, Comparator<String> order) {
        check(!groups.isEmpty(), "group draw missing");
        ArrayList<String> sorted = new ArrayList<>(EuropaSeason.entrants(groups));
        Collections.sort(sorted, order);
        for (List<String> group : groups) {
            check(group.size() == 4, "incomplete group");
            for (int pot = 0; pot < group.size(); pot++) {
                check(sorted.indexOf(group.get(pot)) / groups.size() == pot, "club allocated to wrong pot");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int nonce = 970;
        int season = 2016;
        int seed = nonce ^ season * 10007;
        Context context = new Context("overrides/app/src/main/assets", new File(args[1]));
        AndroidRuntime.current = context;
        JournalHarness.world(args[0], nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        MatchJournal previous = JournalHarness.open(context, nonce, season - 1);
        MatchJournal baseline = JournalHarness.open(context, nonce, season);
        EuropeanQualification.prepare(baseline, previous, career, seed);
        String club = EuropeanQualification.pairs(baseline).get(0)[0];
        String other = EuropeanQualification.pairs(baseline).get(0)[1];

        MatchJournal outside = previous.openSeason(2010);
        result(outside, "uel", 1, other, club, 2, 0);
        outside.save();
        MatchJournal oldest = previous.openSeason(2011);
        result(oldest, "ucl", 1, club, other, 3, 0);
        result(oldest, "cup:en", 1, other, club, 4, 0);
        oldest.save();
        result(previous, "uel", 1, other, club, 2, 2);
        result(previous, "uclq", 1, other, club, 0, 1);
        result(previous, "uclq", 2, club, other, 0, 0);
        previous.save();

        NativeWorldCenter.year = season;
        NativeMatchJournal.advance(context, 0);
        MatchJournal plan = JournalHarness.open(context, nonce, season);
        Map<String, Integer> points = NativeMatchJournal.qualifyingSeeding(context, season);
        check(points.size() == 2 && points.get(club) == 9 && points.get(other) == 3,
                "five-season window, match weights or domestic exclusions");
        check(points.keySet().iterator().next().equals(club), "ranked view out of order");
        check(EuropeanQualification.pairs(plan).stream().anyMatch(pair -> pair[1].equals(club)),
                "strong European results did not move the club into the seeded playoff pot");
        check(EuropeanQualification.candidates(baseline).equals(EuropeanQualification.candidates(plan)) &&
                baseline.metadata("uelq-base").equals(plan.metadata("uelq-base")), "seeding changed entry places");

        String frozenPairs = plan.metadata("uclq-pairs");
        String frozenPoints = plan.metadata("europe-seeding-points");
        JournalHarness.restart();
        for (int round = 1; round <= 2; round++) {
            NativeWorldCenter.played = round;
            NativeMatchJournal.advance(context, round);
        }
        MatchJournal groups = JournalHarness.open(context, nonce, season);
        check(frozenPairs.equals(groups.metadata("uclq-pairs")) && frozenPoints.equals(groups.metadata("europe-seeding-points")),
                "qualifying matches or reload changed the frozen ranking");
        Comparator<String> order = EuropeanQualification.seedingOrder(points, EuropeanQualification.frozenStrengths(groups));
        checkPots(NativeMatchJournal.uclGroups(context, season), EuropeanQualification.holderFirst(groups.metadata("uclq-holder"), order));
        checkPots(NativeMatchJournal.europaGroups(context, season), EuropeanQualification.holderFirst(groups.metadata("uelq-holder"), order));

        MatchJournal legacy = new MatchJournal(new File(args[1], "legacy"), nonce, season);
        legacy.define("uclq-pairs", baseline.metadata("uclq-pairs"));
        EuropeanQualification.prepare(legacy, previous, career, seed);
        check(legacy.metadata("europe-seeding-points") == null &&
                legacy.metadata("uclq-pairs").equals(baseline.metadata("uclq-pairs")), "legacy playoff plan redrawn");
        System.out.println("PASS: five-season window, UCL/UEL/qualifying points, frozen playoff and group pots, unchanged entrants, old draw retained");
    }
}
