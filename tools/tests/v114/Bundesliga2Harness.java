package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public final class Bundesliga2Harness {
    public static void main(String[] args) throws Exception {
        File directory = new File(args[1]);
        freshCareer(args[0], new Context("overrides/app/src/main/assets", new File(directory, "fresh")));
        upgradedCareer(args[0], new Context("overrides/app/src/main/assets", new File(directory, "upgrade")));
    }

    private static void freshCareer(String world, Context context) throws Exception {
        int nonce = 1140;
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        LongCareerHarness.economics(world);
        NativeChampionship.load(context, NativeWorldCenter.leagues);
        NativeBundesliga2.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeCareerStore.Career seed = NativeCareerStore.imported(context);
        NativeCareerStore.installChampionship(career, seed);
        NativeBundesliga2.install(career, seed);
        career.schema = seed.schema;
        career.slot = seed.slot;
        String audit = NativeCareerStore.validate(career);
        check(audit.equals("OK"), "invalid production career seed: " + audit);
        check(NativeBundesliga2.active(career, 2010) && career.clubs.size() == 269 &&
                career.players.stream().filter(p -> p.owner.startsWith("de2:")).count() == 499,
                "wrong new German career seed");
        check(career.players.stream().anyMatch(p -> p.id.equals("de:1:37") && p.owner.equals("de:1")) &&
                career.players.stream().noneMatch(p -> p.id.equals("dfb:10450")), "duplicated legacy player");

        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal journal = JournalHarness.open(context, nonce, 2010);
        List<MatchJournal.Match> league = journal.competition("league:de2");
        List<MatchJournal.Match> cup = journal.competition("cup:de");
        check(league.size() == 306 && cup.size() == 35 && DomesticCup.bracket(journal, "de").entrants.size() == 36,
                "wrong German league or cup calendar");
        Set<String> participants = new HashSet<>();
        Map<String, Integer> played = new HashMap<>();
        Map<String, Long> payroll = new HashMap<>();
        Map<String, Integer> seasonCredit = new HashMap<>();
        for (NativeCareerStore.Player player : career.players) payroll.merge(player.owner, (long) player.contractWage, Long::sum);
        for (MatchJournal.Match match : cup) {
            participants.add(match.home);
            participants.add(match.away);
        }
        check(participants.size() == 36 && participants.stream().filter(id -> id.startsWith("de2:")).count() == 18,
                "a second-division club missed the cup");
        int goals = 0, draws = 0;
        for (MatchJournal.Match match : league) {
            goals += match.homeGoals + match.awayGoals;
            if (match.homeGoals == match.awayGoals) draws++;
            played.merge(match.home, 1, Integer::sum);
            played.merge(match.away, 1, Integer::sum);
            check(match.homeCredit == NativeBundesliga2.matchCredit(ClubEconomy.profile(context, match.home), true, payroll.get(match.home)) &&
                    match.awayCredit == NativeBundesliga2.matchCredit(ClubEconomy.profile(context, match.away), false, payroll.get(match.away)),
                    "German fixture did not charge the correct wages and stadium costs");
            seasonCredit.merge(match.home, match.homeCredit, Integer::sum);
            seasonCredit.merge(match.away, match.awayCredit, Integer::sum);
        }
        check(played.size() == 18 && played.values().stream().allMatch(n -> n == 34), "wrong club fixture count");
        check(NativeMatchJournal.project("de2", 34, 2010).stream().allMatch(m -> m.hg >= 0),
                "background result projection failed");
        int min = Collections.min(seasonCredit.values()), max = Collections.max(seasonCredit.values());
        check(min < 0 && max > 0 && career.clubs.stream().filter(c -> c.league.equals("de2")).allMatch(c -> c.balance > 0),
                "no-transfer club economy is uniformly profitable or insolvent");
        System.out.printf(Locale.ROOT, "Germany: +%d fixtures, %.3f goals/game, %.1f%% draws. Seasonal net GBP %,d to %,d%n",
                league.size(), (double) goals / league.size(), 100.0 * draws / league.size(), min, max);
        int receipts = career.matchReceipts.size();
        String frozen = JournalHarness.frozen(league.get(0));
        int budget = career.clubs.stream().filter(c -> c.id.equals("de2:11")).findFirst().get().balance;
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 38);
        check(career.matchReceipts.size() == receipts &&
                career.clubs.stream().filter(c -> c.id.equals("de2:11")).findFirst().get().balance == budget &&
                frozen.equals(JournalHarness.frozen(JournalHarness.open(context, nonce, 2010).get(league.get(0).id))),
                "restart changed a fixture or credited it twice");
        verifyCupPlace(context, career);
    }

    private static void verifyCupPlace(Context context, NativeCareerStore.Career career) throws Exception {
        File root = new File(context.getFilesDir(), "cup-qualification");
        MatchJournal journal = new MatchJournal(root, 1142, 2011);
        List<String> entrants = new ArrayList<>();
        for (NativeCareerStore.Club club : career.clubs) {
            if (EuropeanPlaces.association(club.league).equals("de")) entrants.add(club.id);
        }
        journal.define("cup-entrants-de", EuropeanQualification.encode(entrants));
        MatchJournal.Match finalMatch = new MatchJournal.Match();
        finalMatch.id = "test-final";
        finalMatch.competition = "cup:de";
        finalMatch.round = DomesticCup.bracket(journal, "de").rounds;
        finalMatch.home = "de2:11";
        finalMatch.away = "de:0";
        finalMatch.winner = finalMatch.home;
        journal.put(finalMatch);
        check(EuropaSeason.leaguePlaces("de", journal, career, Collections.singleton("de:0"),
                UclSeason.strengths(career), "").contains("de2:11"), "an eligible second-division cup winner missed Europa");
    }

    private static void upgradedCareer(String world, Context context) throws Exception {
        int nonce = 1141;
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        LongCareerHarness.economics(world);
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        NativeWorldCenter.played = 8;
        NativeMatchJournal.advance(context, 8);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        String oldDraw = old.metadata("cup-entrants-de");
        MatchJournal.Match oldGame = old.competition("cup:de").get(0);
        check(DomesticCup.bracket(old, "de").entrants.size() == 18, "wrong pre-upgrade German draw");

        NativeCareerStore.Career career = NativeCareerStore.current;
        NativeBundesliga2.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career seed = NativeCareerStore.imported(context);
        NativeCareerStore.Player retained = career.players.get(0);
        retained.owner = "en:8";
        int existingBalance = career.clubs.get(0).balance;
        NativeBundesliga2.install(career, seed);
        NativeBundesliga2.install(career, seed);
        check(career.clubs.size() == 245 && retained.owner.equals("en:8") &&
                career.clubs.get(0).balance == existingBalance && !NativeBundesliga2.active(career, 2010),
                "upgrade duplicated or replaced existing career data");

        NativeWorldCenter.played = 38;
        NativeMatchJournal.advance(context, 38);
        MatchJournal saved = JournalHarness.open(context, nonce, 2010);
        check(saved.competition("league:de2").isEmpty() && oldDraw.equals(saved.metadata("cup-entrants-de")) &&
                JournalHarness.frozen(oldGame).equals(JournalHarness.frozen(saved.get(oldGame.id))),
                "upgrade backfilled the old season or changed its cup");
        NativeWorldCenter.year = 2011;
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal next = JournalHarness.open(context, nonce, 2011);
        check(next.competition("league:de2").size() == 9 && DomesticCup.bracket(next, "de").entrants.size() == 36,
                "new German league and cup did not activate together");
        System.out.println("PASS: 18 clubs and 499 players, frozen older cup, German cup qualification, restart and balanced settlements");
    }
}
