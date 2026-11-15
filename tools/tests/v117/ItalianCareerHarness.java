package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public final class ItalianCareerHarness {
    public static void main(String[] args) throws Exception {
        run(args[0], new File(args[1], "promotion"), "it2", "it2:18", true);
        run(args[0], new File(args[1], "relegation"), "it", "it:0", false);
        List<UclSeason.Standing> boundary = Arrays.asList(new UclSeason.Standing("1"), new UclSeason.Standing("2"),
                new UclSeason.Standing("3"), new UclSeason.Standing("4"));
        boundary.get(2).points = 70;
        boundary.get(3).points = 61;
        check(NativeItalianDivisions.needsPlayoff(boundary), "nine points must trigger play-offs");
        boundary.get(3).points = 60;
        check(!NativeItalianDivisions.needsPlayoff(boundary), "ten points must give automatic promotion");
        upgradedCareer(args[0], new Context("overrides/app/src/main/assets", new File(args[1], "upgrade")));
    }

    private static void run(String world, File files, String league, String user, boolean promoted) throws Exception {
        int nonce = promoted ? 1170 : 1171;
        Context context = new Context("overrides/app/src/main/assets", files);
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.activeLeague = league;
        LongCareerHarness.economics(world);
        NativeSerieB.load(context, NativeWorldCenter.leagues);
        NativeSerieB.install(career, NativeCareerStore.imported(context));
        NativeSerieB.install(career, NativeCareerStore.imported(context));
        List<NativeCareerStore.Club> lowerClubs = new ArrayList<>();
        Set<String> historicalPlayers = new HashSet<>();
        for (NativeCareerStore.Club club : career.clubs) {
            if (!"it2".equals(club.league)) continue;
            lowerClubs.add(club);
            List<NativeCareerStore.Player> players = new ArrayList<>();
            for (NativeCareerStore.Player player : career.players) {
                if (club.id.equals(player.owner)) players.add(player);
            }
            check(players.size() >= 18 && players.size() <= 24 &&
                    players.stream().filter(player -> player.position == 0).count() >= 2 &&
                    ClubEconomy.profile(context, club.id) != null && club.balance > 0,
                    "Serie B club missing a playable squad, stadium or budget");
            for (NativeCareerStore.Player player : players) check(historicalPlayers.add(player.id), "repeated player");
        }
        check(lowerClubs.size() == 22 && historicalPlayers.size() == 483, "wrong historical seed");
        NativeCareerStore.Club managed = career.clubs.stream().filter(c -> c.id.equals(user)).findFirst().get();
        career.userClub = managed.name;
        NativeCareerStore.Player retained = career.players.stream().filter(p -> p.owner.equals(user)).findFirst().get();
        int wages = retained.contractWage, balance = managed.balance;
        LongCareerHarness.legacyBudget = balance;

        context.getSharedPreferences("pfm_world_careers", 0).edit()
                .putInt("slot_" + NativeWorldCenter.slot() + "_nonce", nonce).commit();

        int rounds = NativeCompetitionCalendar.leagueRounds(league);
        MatchJournal journal = JournalHarness.open(context, nonce, 2010);
        for (MatchJournal.Match match : JournalHarness.legacy(rounds)) {
            if (match.home.equals(user)) {
                match.homeGoals = promoted ? 3 : 0;
                match.awayGoals = promoted ? 0 : 3;
            } else if (match.away.equals(user)) {
                match.homeGoals = promoted ? 0 : 3;
                match.awayGoals = promoted ? 3 : 0;
            }
            journal.put(match);
        }
        journal.save();
        NativeWorldCenter.played = rounds;
        NativeMatchJournal.advance(context, rounds);
        MatchJournal completed = JournalHarness.open(context, nonce, 2010);
        List<UclSeason.Standing> top = UclSeason.standings(completed, "it", career.clubs);
        List<UclSeason.Standing> lower = UclSeason.standings(completed, "it2", career.clubs);
        check(completed.competition("league:it").size() == 380 &&
                completed.competition("league:it2").size() == 462 && top.size() == 20 && lower.size() == 22,
                "incomplete Italian leagues at the end of the season");
        check(DomesticCup.bracket(completed, "it").entrants.size() == 42 &&
                completed.competition("cup:it").size() == 41, "Italian cup did not include both divisions");
        if ("it".equals(league)) {
            for (MatchJournal.Match match : completed.competition("league:it2")) {
                check("native-v84".equals(match.source) && match.homeCredit != match.awayCredit,
                        "background Serie B match was missing matchday finances");
            }
            double goals = completed.competition("league:it2").stream()
                    .mapToInt(match -> match.homeGoals + match.awayGoals).average().getAsDouble();
            check(Math.abs(goals - MatchEngine.profile("it2").historicalRate) < 0.25, "Serie B scoring drift: " + goals);
            System.out.printf(Locale.ROOT, "Serie B scoring %.2f goals per match, historical %.2f%n",
                    goals, MatchEngine.profile("it2").historicalRate);
        }

        Set<String> newTop = NativeItalianDivisions.nextTop(completed, career);
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 17; i++) expected.add(top.get(i).id);
        expected.add(lower.get(0).id);
        expected.add(lower.get(1).id);
        List<MatchJournal.Match> fixtures = completed.competition(NativeItalianDivisions.PLAYOFF);
        if (NativeItalianDivisions.needsPlayoff(lower)) {
            check(fixtures.size() == 6, "expected two-legged semi-finals and final");
            for (MatchJournal.Match leg : fixtures) {
                check(leg.homeCredit == 0 && leg.awayCredit == 0 && leg.homePenalties == 0 && leg.awayPenalties == 0,
                        "unexpected play-off payroll or penalty shootout");
                if (leg.round == 2) check(leg.duration == 90 && !leg.winner.isEmpty(), "invalid semi-final return leg");
            }
            MatchJournal.Match last = fixtures.get(5);
            check(last.round == NativeItalianDivisions.FINAL && !last.winner.isEmpty(), "play-off missing a winner");
            expected.add(last.winner);
        } else {
            check(fixtures.isEmpty(), "unexpected play-offs with a clear third place");
            expected.add(lower.get(2).id);
        }
        check(newTop.equals(expected), "wrong clubs after Italian promotion and relegation");
        NativeItalianDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(league), "season transition occurred before legacy installation");

        NativeEnglishCareer.Opening opening = NativeEnglishCareer.opening(context, 2010, "it");
        String next = promoted ? "it" : "it2";
        int closingBalance = managed.balance;
        check(opening != null && opening.league.equals(next) && opening.userId.equals(user) &&
                opening.clubs.size() == ("it2".equals(next) ? 22 : 20) && opening.clubs.stream().anyMatch(c -> c.id.equals(user) &&
                c.budget == closingBalance && c.players.stream().anyMatch(p -> p.id.equals(retained.id) && p.wage == wages)),
                "active Italian club or contracts were lost at season opening");
        EnglishLegacyIds.season(context, 2011).edit().putString("installed", "yes").putString("league", next).commit();
        NativeItalianDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && managed.league.equals(next) && retained.owner.equals(user),
                "Italian promotion/relegation did not preserve a played career");
        MatchJournal following = JournalHarness.open(context, nonce, 2011);
        NativeGermanDivisions.freeze(following, career, "it");
        check(new HashSet<>(NativeEnglishDivisions.members(following, "it")).equals(newTop) &&
                NativeEnglishDivisions.members(following, "it2").size() == 22, "new league membership was not frozen");
        following.save();
        NativeItalianDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && JournalHarness.open(context, nonce, 2010)
                .competition("league:" + league).size() == ("it2".equals(league) ? 462 : 380), "repeated rollover rewrote Italian history");
        System.out.println("PASS: active " + league + " -> " + next + " · full calendar, 42 cup entrants, stable club, contracts and budget");
    }

    private static void upgradedCareer(String world, Context context) throws Exception {
        int nonce = 1172;
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        LongCareerHarness.economics(world);
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        NativeWorldCenter.played = 8;
        NativeMatchJournal.advance(context, 8);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        String oldDraw = old.metadata("cup-entrants-it");
        MatchJournal.Match oldGame = old.competition("cup:it").get(0);
        check(DomesticCup.bracket(old, "it").entrants.size() == 20, "wrong pre-upgrade Italian draw");

        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeSerieB.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career seed = NativeCareerStore.imported(context);
        NativeCareerStore.Player retained = career.players.get(0);
        retained.owner = "en:8";
        int existingBalance = career.clubs.get(0).balance;
        NativeSerieB.install(career, seed);
        NativeSerieB.install(career, seed);
        check(career.clubs.size() == 249 && retained.owner.equals("en:8") &&
                career.clubs.get(0).balance == existingBalance && !NativeSerieB.active(career, 2010),
                "upgrade duplicated or replaced existing career data");

        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal saved = JournalHarness.open(context, nonce, 2010);
        check(saved.competition("league:it2").isEmpty() && oldDraw.equals(saved.metadata("cup-entrants-it")) &&
                JournalHarness.frozen(oldGame).equals(JournalHarness.frozen(saved.get(oldGame.id))),
                "upgrade backfilled the old season or changed its cup");
        NativeWorldCenter.year = 2011;
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal next = JournalHarness.open(context, nonce, 2011);
        check(next.competition("league:it2").size() == 11 && DomesticCup.bracket(next, "it").entrants.size() == 42,
                "new Italian league and cup did not activate together");
        System.out.println("PASS: upgraded career preserved its table and cup, activated Serie B the following season");
    }
}
