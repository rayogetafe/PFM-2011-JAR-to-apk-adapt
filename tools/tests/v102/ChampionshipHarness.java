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
        int bytes = season.size();
        JournalHarness.restart();
        NativeMatchJournal.advance(context, 38);
        check(JournalHarness.open(context, nonce, 2011).size() == bytes, "restart duplicated matches");
        System.out.println("PASS: 24 Championship clubs, 506 new players, existing ownership preserved, 552 fixtures and a 44-team English cup");
    }
}
