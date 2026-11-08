package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public class ChampionshipHarness {
    public static void main(String[] args) throws Exception {
        int nonce = 1020;
        Context context = new Context("overrides/app/src/main/assets", new File(args[1]));
        AndroidRuntime.current = context;
        JournalHarness.world(args[0], nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.userClub = NativeWorldCenter.leagues.get("en").get(0).name;
        LongCareerHarness.economics(args[0]);
        NativeMatchJournal.advance(context, 6);
        MatchJournal old = JournalHarness.open(context, nonce, 2010);
        String originalCup = old.metadata("cup-entrants-en");
        MatchJournal.Match original = old.competition("cup:en").get(0);

        NativeChampionship.load(context, NativeWorldCenter.leagues);
        NativeCareerStore.Career seed = NativeCareerStore.imported();
        check(NativeChampionship.active(seed, 2010), "new career cannot start in 2010");
        check(seed.clubs.size() == 251 && seed.players.size() == career.players.size() + 506, "wrong seed size");
        NativeWorldCenter.played = 6;
        seed = NativeCareerStore.imported();
        NativeCareerStore.Player linked = career.players.stream().filter(p -> p.id.equals("en:14:299")).findFirst().get();
        linked.owner = "en:0"; // A previous career transfer must survive the import.
        String owner = linked.owner;
        int wage = linked.contractWage;
        NativeCareerStore.installChampionship(career, seed);
        NativeCareerStore.installChampionship(career, seed);
        check(career.players.size() == seed.players.size() && career.clubs.size() == 251 &&
                linked.owner.equals(owner) && linked.contractWage == wage, "migration duplicated or rewrote a player");
        check(!NativeChampionship.active(career, 2010) && NativeChampionship.active(career, 2011), "migration activation season");
        NativeMatchJournal.advance(context, 8);
        old = JournalHarness.open(context, nonce, 2010);
        check(old.competition("league:en2").isEmpty() && originalCup.equals(old.metadata("cup-entrants-en")) &&
                JournalHarness.frozen(original).equals(JournalHarness.frozen(old.get(original.id))), "old calendar or cup changed");

        NativeWorldCenter.year = 2011;
        for (int step = 1; step <= 38; step++) {
            NativeWorldCenter.played = step;
            NativeMatchJournal.advance(context, step);
            if (step == 20) JournalHarness.restart();
        }
        MatchJournal season = JournalHarness.open(context, nonce, 2011);
        List<MatchJournal.Match> matches = season.competition("league:en2");
        check(matches.size() == 552 && season.competition("cup:en").size() == 43 &&
                DomesticCup.bracket(season, "en").entrants.size() == 44, "wrong Championship or cup calendar");
        Map<String, Integer> played = new HashMap<>();
        for (MatchJournal.Match m : matches) {
            check(m.effectsVersion > 0 && m.clock > 0 && m.clock <= 38 &&
                    m.homeCredit > 0 && m.awayCredit < 0, "Championship match lost consequences");
            for (String club : Arrays.asList(m.home, m.away)) {
                played.put(club, played.getOrDefault(club, 0) + 1);
                check(m.players.stream().filter(p -> p.club.equals(club) && p.stats[3] > 0).count() == 11,
                        "missing Championship starters");
            }
        }
        check(played.size() == 24 && played.values().stream().allMatch(n -> n == 46), "wrong round count per club");
        List<UclSeason.Standing> standings = UclSeason.standings(season, NativeChampionship.CODE, career.clubs);
        List<String> promotion = ChampionshipPlayoffs.promotionCandidates(season);
        List<MatchJournal.Match> playoffs = season.competition(ChampionshipPlayoffs.COMPETITION);
        check(playoffs.size() == 5 && promotion.size() == 3 && promotion.get(0).equals(standings.get(0).id) &&
                promotion.get(1).equals(standings.get(1).id), "wrong promotion places");
        Set<String> finalists = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            MatchJournal.Match first = playoffs.get(i), second = playoffs.get(i + 2);
            check(first.home.equals(standings.get(5 - i).id) && first.away.equals(standings.get(2 + i).id) &&
                    second.home.equals(first.away) && second.away.equals(first.home), "wrong play-off pair or home order");
            check(first.duration == 90 && first.winner.isEmpty() && second.clock == 40 &&
                    second.report.contains("Aggregate:") && !second.report.contains("away goals"), "wrong semi-final rules");
            check(finalists.add(second.winner) && (second.winner.equals(second.home) || second.winner.equals(second.away)),
                    "unresolved semi-final");
        }
        MatchJournal.Match finalMatch = playoffs.get(4);
        check(finalists.equals(new HashSet<>(Arrays.asList(finalMatch.home, finalMatch.away))) &&
                finalMatch.report.contains("Wembley") && promotion.get(2).equals(finalMatch.winner), "wrong play-off final");
        for (MatchJournal.Match m : playoffs) {
            check(m.effectsVersion > 0 && m.homeCredit == 0 && m.awayCredit == 0, "wrong play-off match effects or costs");
        }
        MatchJournal.Match first = new MatchJournal.Match();
        first.homeGoals = 2;
        first.awayGoals = 1;
        check(ChampionshipPlayoffs.aggregateDecision(first, 3, 2) == 0 &&
                EuropeanPlaces.disciplineCompetition(ChampionshipPlayoffs.COMPETITION).equals("league:en2"),
                "away-goals tie or discipline scope");

        int bytes = season.size();
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 38);
        MatchJournal reopened = JournalHarness.open(context, nonce, 2011);
        check(reopened.size() == bytes && promotion.equals(ChampionshipPlayoffs.promotionCandidates(reopened)) &&
                JournalHarness.frozen(finalMatch).equals(JournalHarness.frozen(reopened.get(finalMatch.id))),
                "restart changed matches or promotion outcome");

        MatchJournal oldFinish = new MatchJournal(new File(args[1], "v102-finish"), nonce, 2011);
        for (MatchJournal.Match m : matches) oldFinish.put(m);
        HashMap<String, NativeWorldCenter.T> teams = NativeMatchJournal.teams(career);
        HashMap<String, NativeWorldCenter.P> players = new HashMap<>();
        for (NativeWorldCenter.T team : teams.values()) for (NativeWorldCenter.P p : team.ps) players.put(p.uid, p);
        MatchEffects effects = new MatchEffects(38);
        effects.rest(38);
        ChampionshipPlayoffs.complete(oldFinish, nonce, 38, teams, players, "reconstructed-v103", effects, career);
        ChampionshipPlayoffs.complete(oldFinish, nonce, 38, teams, players, "native-v84", effects, career);
        check(oldFinish.competition(ChampionshipPlayoffs.COMPETITION).size() == 5 &&
                effects.clock == 38 && oldFinish.competition(ChampionshipPlayoffs.COMPETITION).stream().allMatch(m -> m.effectsVersion == 0),
                "backfilled play-offs created retroactive effects");

        List<String> finalTable = NativeEnglishDivisions.members(reopened, "en");
        String blackpool = "en:19";
        finalTable.remove(blackpool);
        finalTable.add(14, blackpool);
        context.getSharedPreferences("pfm_world_careers", 0).edit().putInt("slot_1_nonce", nonce).commit();
        NativeEnglishCareer.recordFinalPremier(context, 2011, finalTable);
        reopened = JournalHarness.open(context, nonce, 2011);
        career.userClub = "Blackpool";
        NativeEnglishCareer.Opening blackpoolOpening = NativeEnglishCareer.opening(context, 2011);
        check(blackpoolOpening != null && blackpoolOpening.league.equals("en") && blackpoolOpening.userId.equals(blackpool),
                "Blackpool was relegated from fifteenth place");
        List<String> relegated = NativeEnglishDivisions.relegated(reopened, career);
        check(relegated.equals(finalTable.subList(17, 20)), "relegation ignored the visible final table");
        check(relegated.size() == 3, "missing Premier League relegation places");
        NativeEnglishDivisions.rollover(context, career, 2012);
        check(leagueOf(career, promotion.get(0)).equals("en2"), "changed active English core membership");
        context.getSharedPreferences("pfm_world_careers", 0).edit().putInt("slot_1_nonce", nonce).commit();
        NativeCareerStore.Club relegatedUser = career.clubs.stream().filter(c -> c.id.equals(relegated.get(0))).findFirst().get();
        career.userClub = relegatedUser.name;
        NativeEnglishCareer.Opening lower = NativeEnglishCareer.opening(context, 2011);
        check(lower != null && lower.league.equals("en2") && lower.userId.equals(relegatedUser.id) &&
                lower.clubs.size() == 24 && lower.clubs.stream().anyMatch(c -> c.id.equals(relegatedUser.id) &&
                        c.budget == relegatedUser.balance && c.players.stream().allMatch(p -> career.players.stream()
                                .anyMatch(player -> player.id.equals(p.id) && player.owner.equals(c.id)))),
                "active manager's club or roster lost on relegation");
        Map<NativeCareerStore.Club, String> originalLeagues = new HashMap<>();
        for (NativeCareerStore.Club club : career.clubs) originalLeagues.put(club, club.league);
        Context installedCareer = new Context("overrides/app/src/main/assets", context.getFilesDir());
        installedCareer.getSharedPreferences("pfm_world_careers", 0).edit().putInt("slot_1_nonce", nonce).commit();
        EnglishLegacyIds.season(installedCareer, 2012).edit().putString("installed", "yes").commit();
        NativeEnglishDivisions.rollover(installedCareer, career, 2012);
        check(career.activeLeague.equals("en2") && leagueOf(career, relegatedUser.id).equals("en2"),
                "legacy transition did not update the native career");
        for (Map.Entry<NativeCareerStore.Club, String> entry : originalLeagues.entrySet()) entry.getKey().league = entry.getValue();
        career.activeLeague = "en2";
        career.userClub = career.clubs.stream().filter(c -> c.id.equals(promotion.get(2))).findFirst().get().name;
        NativeEnglishCareer.Opening upper = NativeEnglishCareer.opening(context, 2011);
        check(upper != null && upper.league.equals("en") && upper.userId.equals(promotion.get(2)) &&
                upper.clubs.size() == 20, "active play-off winner did not return to Premier League");

        career.activeLeague = "es";
        career.userClub = NativeWorldCenter.leagues.get("es").get(0).name;
        Map<String, String> owners = new HashMap<>();
        for (NativeCareerStore.Player player : career.players) owners.put(player.id, player.owner);
        NativeWorldCenter.year = 2012;
        NativeWorldCenter.played = 0;
        NativeMatchJournal.advance(context, 0);
        MatchJournal next = JournalHarness.open(context, nonce, 2012);
        List<String> premier = NativeEnglishDivisions.members(next, "en");
        List<String> championship = NativeEnglishDivisions.members(next, "en2");
        Set<String> englishClubs = new HashSet<>(premier);
        englishClubs.addAll(championship);
        check(premier.size() == 20 && championship.size() == 24 && englishClubs.size() == 44 &&
                premier.containsAll(promotion) && championship.containsAll(relegated), "wrong opening division membership");
        check(career.players.stream().allMatch(p -> p.owner.equals(owners.get(p.id))) &&
                promotion.stream().allMatch(id -> leagueOf(career, id).equals("en")) &&
                relegated.stream().allMatch(id -> leagueOf(career, id).equals("en2")), "club or player identity lost");
        NativeCareerStore.Player promotedPlayer = career.players.stream().filter(p -> promotion.contains(p.owner)).findFirst().get();
        Map<String, Integer> bans = new HashMap<>();
        bans.put("league:en2|" + promotedPlayer.id, 2);
        NativeEnglishDivisions.moveOpeningBans(bans, reopened, career);
        check(bans.size() == 1 && bans.get("league:en|" + promotedPlayer.id) == 2, "promotion lost a suspension");
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        next = JournalHarness.open(context, nonce, 2012);
        check(next.competition("league:en").size() == 10 && next.competition("league:en2").size() == 12 &&
                next.competition("league:en").stream().allMatch(m -> premier.contains(m.home) && premier.contains(m.away)) &&
                next.competition("league:en2").stream().allMatch(m -> championship.contains(m.home) && championship.contains(m.away)),
                "new fixtures use old divisions");
        check(DomesticCup.bracket(next, "en").entrants.size() == 44, "English cup lost a promoted or relegated club");
        check(NativeMatchJournal.leagueTeams("en2", 2011).stream().anyMatch(t -> t.clubId.equals(promotion.get(0))) &&
                NativeMatchJournal.leagueTeams("en", 2012).stream().anyMatch(t -> t.clubId.equals(promotion.get(0))),
                "history or live table lost the club");
        int nextCount = next.size();
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 1);
        check(JournalHarness.open(context, nonce, 2012).size() == nextCount, "restart duplicated league fixtures");
        for (int step = 2; step <= 38; step++) {
            NativeWorldCenter.played = step;
            NativeMatchJournal.advance(context, step);
        }
        next = JournalHarness.open(context, nonce, 2012);
        List<String> promotedAgain = ChampionshipPlayoffs.promotionCandidates(next);
        List<String> relegatedAgain = NativeEnglishDivisions.relegated(next, career);
        check(next.competition("league:en").size() == 380 && next.competition("league:en2").size() == 552 &&
                promotedAgain.size() == 3 && relegatedAgain.size() == 3, "mixed divisions could not finish the season");
        NativeWorldCenter.year = 2013;
        NativeWorldCenter.played = 0;
        NativeMatchJournal.advance(context, 0);
        MatchJournal secondOpening = JournalHarness.open(context, nonce, 2013);
        check(NativeEnglishDivisions.members(secondOpening, "en").containsAll(promotedAgain) &&
                NativeEnglishDivisions.members(secondOpening, "en2").containsAll(relegatedAgain) &&
                premier.equals(NativeEnglishDivisions.members(next, "en")) &&
                championship.equals(NativeEnglishDivisions.members(next, "en2")), "second exchange changed archived membership");
        System.out.println("PASS: 24 Championship clubs, 506 new players, existing ownership preserved, 552 fixtures and a 44-team English cup");
        System.out.println("PASS: 5 play-off matches, positions 3-6, aggregate without away goals, Wembley final, three frozen promotion places");
        System.out.println("PASS: two successive three-club exchanges, stable ownership and history, next-season fixtures and active-England guard");
    }

    private static String leagueOf(NativeCareerStore.Career career, String id) {
        return career.clubs.stream().filter(club -> club.id.equals(id)).findFirst().get().league;
    }
}
