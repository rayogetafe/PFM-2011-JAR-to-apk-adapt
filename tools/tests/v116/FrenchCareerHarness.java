package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public final class FrenchCareerHarness {
    public static void main(String[] args) throws Exception {
        run(args[0], new File(args[1], "promotion"), "fr2", "fr2:13", true);
        run(args[0], new File(args[1], "relegation"), "fr", "fr:0", false);
        upgradedCareer(args[0], new Context("overrides/app/src/main/assets", new File(args[1], "upgrade")));
    }

    private static void run(String world, File files, String league, String user, boolean promoted) throws Exception {
        int nonce = promoted ? 1160 : 1161;
        Context context = new Context("overrides/app/src/main/assets", files);
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.activeLeague = league;
        LongCareerHarness.economics(world);
        NativeLigue2.load(context, NativeWorldCenter.leagues);
        NativeLigue2.install(career, NativeCareerStore.imported(context));
        NativeLigue2.install(career, NativeCareerStore.imported(context));
        List<NativeCareerStore.Club> lowerClubs = new ArrayList<>();
        Set<String> historicalPlayers = new HashSet<>();
        for (NativeCareerStore.Club club : career.clubs) {
            if (!"fr2".equals(club.league)) continue;
            lowerClubs.add(club);
            List<NativeCareerStore.Player> players = new ArrayList<>();
            for (NativeCareerStore.Player player : career.players) {
                if (club.id.equals(player.owner)) players.add(player);
            }
            check(players.size() >= 18 && players.size() <= 24 &&
                    players.stream().filter(player -> player.position == 0).count() >= 2 &&
                    ClubEconomy.profile(context, club.id) != null && club.balance > 0,
                    "Ligue 2 club missing a playable squad, stadium or budget");
            for (NativeCareerStore.Player player : players) check(historicalPlayers.add(player.id), "repeated player");
        }
        check(lowerClubs.size() == 20 && historicalPlayers.size() == 405, "wrong historical seed");
        NativeCareerStore.Club managed = career.clubs.stream().filter(c -> c.id.equals(user)).findFirst().get();
        career.userClub = managed.name;
        NativeCareerStore.Player retained = career.players.stream().filter(p -> p.owner.equals(user)).findFirst().get();
        int wages = retained.contractWage, balance = managed.balance;
        LongCareerHarness.legacyBudget = balance;

        context.getSharedPreferences("pfm_world_careers", 0).edit()
                .putInt("slot_" + NativeWorldCenter.slot() + "_nonce", nonce).commit();

        MatchJournal journal = JournalHarness.open(context, nonce, 2010);
        for (MatchJournal.Match match : JournalHarness.legacy(38)) {
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
        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal completed = JournalHarness.open(context, nonce, 2010);
        List<UclSeason.Standing> top = UclSeason.standings(completed, "fr", career.clubs);
        List<UclSeason.Standing> lower = UclSeason.standings(completed, "fr2", career.clubs);
        check(completed.competition("league:fr").size() == 380 &&
                completed.competition("league:fr2").size() == 380 && top.size() == 20 && lower.size() == 20,
                "incomplete French leagues after round 38");
        check(DomesticCup.bracket(completed, "fr").entrants.size() == 40 &&
                completed.competition("cup:fr").size() == 39, "French cup did not include both divisions");
        if ("fr".equals(league)) {
            for (MatchJournal.Match match : completed.competition("league:fr2")) {
                check("native-v84".equals(match.source) && match.homeCredit != match.awayCredit,
                        "background Ligue 2 match was missing matchday finances");
            }
            double goals = completed.competition("league:fr2").stream()
                    .mapToInt(match -> match.homeGoals + match.awayGoals).average().getAsDouble();
            check(Math.abs(goals - MatchEngine.profile("fr2").historicalRate) < 0.23, "Ligue 2 scoring drift: " + goals);
            System.out.printf(Locale.ROOT, "Ligue 2 scoring %.2f goals per match, historical %.2f%n",
                    goals, MatchEngine.profile("fr2").historicalRate);
        }

        Set<String> newTop = NativeFrenchDivisions.nextTop(completed, career);
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 17; i++) expected.add(top.get(i).id);
        for (int i = 0; i < 3; i++) expected.add(lower.get(i).id);
        check(newTop.equals(expected), "wrong clubs after French promotion and relegation");
        NativeFrenchDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(league), "season transition occurred before legacy installation");

        NativeEnglishCareer.Opening opening = NativeEnglishCareer.opening(context, 2010, "fr");
        String next = promoted ? "fr" : "fr2";
        int closingBalance = managed.balance;
        check(opening != null && opening.league.equals(next) && opening.userId.equals(user) &&
                opening.clubs.size() == 20 && opening.clubs.stream().anyMatch(c -> c.id.equals(user) &&
                c.budget == closingBalance && c.players.stream().anyMatch(p -> p.id.equals(retained.id) && p.wage == wages)),
                "active French club or contracts were lost at season opening");
        EnglishLegacyIds.season(context, 2011).edit().putString("installed", "yes").putString("league", next).commit();
        NativeFrenchDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && managed.league.equals(next) && retained.owner.equals(user),
                "French promotion/relegation did not preserve a played career");
        MatchJournal following = JournalHarness.open(context, nonce, 2011);
        NativeGermanDivisions.freeze(following, career, "fr");
        check(new HashSet<>(NativeEnglishDivisions.members(following, "fr")).equals(newTop) &&
                NativeEnglishDivisions.members(following, "fr2").size() == 20, "new league membership was not frozen");
        following.save();
        NativeFrenchDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && JournalHarness.open(context, nonce, 2010)
                .competition("league:" + league).size() == 380, "repeated rollover rewrote French history");
        System.out.println("PASS: active " + league + " -> " + next + " · 38 rounds, 40 cup entrants, stable club, contracts and budget");
    }

    private static void upgradedCareer(String world, Context context) throws Exception {
        int nonce = 1162;
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        LongCareerHarness.economics(world);
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        NativeWorldCenter.played = 8;
        NativeMatchJournal.advance(context, 8);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        String oldDraw = old.metadata("cup-entrants-fr");
        MatchJournal.Match oldGame = old.competition("cup:fr").get(0);
        check(DomesticCup.bracket(old, "fr").entrants.size() == 20, "wrong pre-upgrade French draw");

        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeLigue2.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career seed = NativeCareerStore.imported(context);
        NativeCareerStore.Player retained = career.players.get(0);
        retained.owner = "en:8";
        int existingBalance = career.clubs.get(0).balance;
        NativeLigue2.install(career, seed);
        NativeLigue2.install(career, seed);
        check(career.clubs.size() == 247 && retained.owner.equals("en:8") &&
                career.clubs.get(0).balance == existingBalance && !NativeLigue2.active(career, 2010),
                "upgrade duplicated or replaced existing career data");

        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal saved = JournalHarness.open(context, nonce, 2010);
        check(saved.competition("league:fr2").isEmpty() && oldDraw.equals(saved.metadata("cup-entrants-fr")) &&
                JournalHarness.frozen(oldGame).equals(JournalHarness.frozen(saved.get(oldGame.id))),
                "upgrade backfilled the old season or changed its cup");
        NativeWorldCenter.year = 2011;
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal next = JournalHarness.open(context, nonce, 2011);
        check(next.competition("league:fr2").size() == 10 && DomesticCup.bracket(next, "fr").entrants.size() == 40,
                "new French league and cup did not activate together");
        System.out.println("PASS: upgraded career preserved its table and cup, activated Ligue 2 the following season");
    }
}
