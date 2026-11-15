package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public final class SpanishCareerHarness {
    public static void main(String[] args) throws Exception {
        run(args[0], new File(args[1], "promotion"), "es2", "es2:9", true);
        run(args[0], new File(args[1], "relegation"), "es", "es:4", false);
        Set<String> top = new HashSet<>(Arrays.asList("es:0", "es:4"));
        Set<String> lower = new LinkedHashSet<>();
        for (int i = 0; i < 22; i++) lower.add("es2:" + i);
        top.remove("es:4");
        NativeSpanishDivisions.fillReservePlaces(top, lower);
        check(lower.contains(NativeSegunda.SABADELL) && !lower.contains(NativeSegunda.VILLARREAL_B) && lower.size() == 22,
                "Sabadell did not replace the dependent club");
        top.add("es:4");
        NativeSpanishDivisions.fillReservePlaces(top, lower);
        check(lower.contains(NativeSegunda.VILLARREAL_B) && !lower.contains(NativeSegunda.SABADELL) && lower.size() == 22,
                "Villarreal B did not regain eligibility after its parent's promotion");
        upgradedCareer(args[0], new Context("overrides/app/src/main/assets", new File(args[1], "upgrade")));
    }

    private static void run(String world, File files, String league, String user, boolean promoted) throws Exception {
        int nonce = promoted ? 1180 : 1181;
        Context context = new Context("overrides/app/src/main/assets", files);
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.activeLeague = league;
        LongCareerHarness.economics(world);
        NativeSegunda.load(context, NativeWorldCenter.leagues);
        NativeSegunda.install(career, NativeCareerStore.imported(context));
        NativeSegunda.install(career, NativeCareerStore.imported(context));
        List<NativeCareerStore.Club> lowerClubs = new ArrayList<>();
        Set<String> historicalPlayers = new HashSet<>();
        for (NativeCareerStore.Club club : career.clubs) {
            if (!NativeSegunda.registered(club.id)) continue;
            lowerClubs.add(club);
            List<NativeCareerStore.Player> players = new ArrayList<>();
            for (NativeCareerStore.Player player : career.players) {
                if (club.id.equals(player.owner)) players.add(player);
            }
            check(players.size() >= 18 && players.size() <= 24 &&
                    players.stream().filter(player -> player.position == 0).count() >= 2 &&
                    ClubEconomy.profile(context, club.id) != null && club.balance > 0,
                    "Segunda club missing a playable squad, stadium or budget");
            for (NativeCareerStore.Player player : players) check(historicalPlayers.add(player.id), "repeated player");
        }
        check(lowerClubs.size() == 24 && historicalPlayers.size() == 528, "wrong historical seed");
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
        List<UclSeason.Standing> top = UclSeason.standings(completed, "es", career.clubs);
        List<UclSeason.Standing> lower = UclSeason.standings(completed, "es2", career.clubs);
        check(completed.competition("league:es").size() == 380 &&
                completed.competition("league:es2").size() == 462 && top.size() == 20 && lower.size() == 22,
                "incomplete Spanish leagues at the end of the season");
        check(DomesticCup.bracket(completed, "es").entrants.size() == 40 &&
                Collections.disjoint(DomesticCup.bracket(completed, "es").entrants, NativeSegunda.RESERVES) &&
                completed.competition("cup:es").size() == 39, "Spanish cup did not include both divisions");
        if ("es".equals(league)) {
            for (MatchJournal.Match match : completed.competition("league:es2")) {
                check("native-v84".equals(match.source) && match.homeCredit != match.awayCredit,
                        "background Segunda match was missing matchday finances");
            }
            double goals = completed.competition("league:es2").stream()
                    .mapToInt(match -> match.homeGoals + match.awayGoals).average().getAsDouble();
            check(Math.abs(goals - MatchEngine.profile("es2").historicalRate) < 0.25, "Segunda scoring drift: " + goals);
            System.out.printf(Locale.ROOT, "Segunda scoring %.2f goals per match, historical %.2f%n",
                    goals, MatchEngine.profile("es2").historicalRate);
        }

        NativeSpanishDivisions.Opening membership = NativeSpanishDivisions.opening(completed, career);
        List<MatchJournal.Match> fixtures = completed.competition(NativeSpanishDivisions.PLAYOFF);
        List<String> order = EuropeanQualification.ids(completed.metadata(NativeSpanishDivisions.TABLE));
        List<String> eligible = NativeSpanishDivisions.eligibleOrder(order);
        check(fixtures.size() == 6 && membership.top.size() == 20 && membership.lower.size() == 22,
                "expected six play-off matches and complete divisions");
        for (MatchJournal.Match leg : fixtures) {
            check(!NativeSegunda.reserve(leg.home) && !NativeSegunda.reserve(leg.away) &&
                    leg.homeCredit == 0 && leg.awayCredit == 0, "wrong play-off team or matchday payroll");
        }
        MatchJournal.Match finalLeg = fixtures.get(5);
        check(finalLeg.round == 4 && eligible.subList(2, 6).contains(finalLeg.winner) &&
                membership.top.contains(finalLeg.winner) && membership.top.containsAll(eligible.subList(0, 2)),
                "wrong promotion places");
        if (!promoted) {
            check(membership.lower.contains("es:4") && !membership.lower.contains(NativeSegunda.VILLARREAL_B) &&
                    membership.lower.contains(NativeSegunda.SABADELL), "parent and reserve entered the same division");
        }
        Set<String> newTop = membership.top;
        NativeSpanishDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(league), "season transition occurred before legacy installation");

        NativeEnglishCareer.Opening opening = NativeEnglishCareer.opening(context, 2010, "es");
        String next = promoted ? "es" : "es2";
        int closingBalance = managed.balance;
        check(opening != null && opening.league.equals(next) && opening.userId.equals(user) &&
                opening.clubs.size() == ("es2".equals(next) ? 22 : 20) && opening.clubs.stream().anyMatch(c -> c.id.equals(user) &&
                c.budget == closingBalance && c.players.stream().anyMatch(p -> p.id.equals(retained.id) && p.wage == wages)),
                "active Spanish club or contracts were lost at season opening");
        EnglishLegacyIds.season(context, 2011).edit().putString("installed", "yes").putString("league", next).commit();
        NativeSpanishDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && managed.league.equals(next) && retained.owner.equals(user),
                "Spanish promotion/relegation did not preserve a played career");
        MatchJournal following = JournalHarness.open(context, nonce, 2011);
        NativeGermanDivisions.freeze(following, career, "es");
        check(new HashSet<>(NativeEnglishDivisions.members(following, "es")).equals(newTop) &&
                NativeEnglishDivisions.members(following, "es2").size() == 22, "new league membership was not frozen");
        if (!promoted) {
            NativeCareerStore.Club reserve = career.clubs.stream().filter(c -> c.id.equals(NativeSegunda.VILLARREAL_B)).findFirst().get();
            NativeCareerStore.Club entrant = career.clubs.stream().filter(c -> c.id.equals(NativeSegunda.SABADELL)).findFirst().get();
            check(reserve.league.equals(NativeSegunda.INACTIVE) && reserve.activeFrom == Integer.MAX_VALUE &&
                    entrant.league.equals("es2") && entrant.activeFrom == 2011, "reserve replacement did not transfer roster eligibility");
        }
        following.save();
        NativeSpanishDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && JournalHarness.open(context, nonce, 2010)
                .competition("league:" + league).size() == ("es2".equals(league) ? 462 : 380), "repeated rollover rewrote Spanish history");
        System.out.println("PASS: active " + league + " -> " + next + " · full calendar, 40 cup entrants, stable club, contracts and budget");
    }

    private static void upgradedCareer(String world, Context context) throws Exception {
        int nonce = 1182;
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        LongCareerHarness.economics(world);
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        NativeWorldCenter.played = 8;
        NativeMatchJournal.advance(context, 8);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        String oldDraw = old.metadata("cup-entrants-es");
        MatchJournal.Match oldGame = old.competition("cup:es").get(0);
        check(DomesticCup.bracket(old, "es").entrants.size() == 20, "wrong pre-upgrade Spanish draw");

        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeSegunda.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career seed = NativeCareerStore.imported(context);
        NativeCareerStore.Player retained = career.players.get(0);
        retained.owner = "en:8";
        int existingBalance = career.clubs.get(0).balance;
        NativeSegunda.install(career, seed);
        NativeSegunda.install(career, seed);
        check(career.clubs.size() == 251 && retained.owner.equals("en:8") &&
                career.clubs.get(0).balance == existingBalance && !NativeSegunda.active(career, 2010),
                "upgrade duplicated or replaced existing career data");

        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal saved = JournalHarness.open(context, nonce, 2010);
        check(saved.competition("league:es2").isEmpty() && oldDraw.equals(saved.metadata("cup-entrants-es")) &&
                JournalHarness.frozen(oldGame).equals(JournalHarness.frozen(saved.get(oldGame.id))),
                "upgrade backfilled the old season or changed its cup");
        NativeWorldCenter.year = 2011;
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal next = JournalHarness.open(context, nonce, 2011);
        check(next.competition("league:es2").size() == 11 && DomesticCup.bracket(next, "es").entrants.size() == 40,
                "new Spanish league and cup did not activate together");
        System.out.println("PASS: upgraded career preserved its table and cup, activated Segunda the following season");
    }
}
