package pfm.android;

import android.content.Context;
import java.io.File;
import java.util.*;
import static pfm.android.JournalHarness.check;

public final class GermanCareerHarness {
    public static void main(String[] args) throws Exception {
        run(args[0], new File(args[1], "promotion"), "de2", "de2:11", true);
        run(args[0], new File(args[1], "relegation"), "de", "de:0", false);
    }

    private static void run(String world, File files, String league, String user, boolean promoted) throws Exception {
        int nonce = promoted ? 1150 : 1151;
        Context context = new Context("overrides/app/src/main/assets", files);
        AndroidRuntime.current = context;
        JournalHarness.world(world, nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        career.activeLeague = league;
        LongCareerHarness.economics(world);
        NativeBundesliga2.load(context, NativeWorldCenter.leagues);
        NativeBundesliga2.install(career, NativeCareerStore.imported(context));
        NativeCareerStore.Club managed = career.clubs.stream().filter(c -> c.id.equals(user)).findFirst().get();
        career.userClub = managed.name;
        NativeCareerStore.Player retained = career.players.stream().filter(p -> p.owner.equals(user)).findFirst().get();
        int wages = retained.contractWage, balance = managed.balance;
        LongCareerHarness.legacyBudget = balance;

        context.getSharedPreferences("pfm_world_careers", 0).edit()
                .putInt("slot_" + NativeWorldCenter.slot() + "_nonce", nonce).commit();

        MatchJournal journal = JournalHarness.open(context, nonce, 2010);
        for (MatchJournal.Match match : JournalHarness.legacy(34)) {
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
        NativeWorldCenter.played = 34;
        NativeMatchJournal.advance(context, 34);
        MatchJournal completed = JournalHarness.open(context, nonce, 2010);
        List<UclSeason.Standing> top = UclSeason.standings(completed, "de", career.clubs);
        List<UclSeason.Standing> lower = UclSeason.standings(completed, "de2", career.clubs);
        List<MatchJournal.Match> legs = completed.competition(NativeGermanDivisions.PLAYOFF);
        check(legs.size() == 2, "German play-off missing");
        MatchJournal.Match first = legs.get(0), second = legs.get(1);
        check(first.home.equals(top.get(15).id) && first.away.equals(lower.get(2).id) &&
                second.home.equals(first.away) && second.away.equals(first.home) && !second.winner.isEmpty(),
                "German play-off used an incorrect club or order");
        check(first.homeCredit == 0 && second.awayCredit == 0, "play-off billed an extra regular-season payroll");

        Set<String> newTop = NativeGermanDivisions.nextTop(completed, career, nonce);
        check(newTop.size() == 18 && newTop.contains(second.winner) && newTop.contains(lower.get(0).id) &&
                newTop.contains(lower.get(1).id) && !newTop.contains(top.get(16).id) &&
                !newTop.contains(top.get(17).id), "wrong clubs after German play-off");
        NativeGermanDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(league), "season transition occurred before legacy installation");

        NativeEnglishCareer.Opening opening = NativeEnglishCareer.opening(context, 2010, "de");
        String next = promoted ? "de" : "de2";
        int closingBalance = managed.balance;
        check(opening != null && opening.league.equals(next) && opening.userId.equals(user) &&
                opening.clubs.size() == 18 && opening.clubs.stream().anyMatch(c -> c.id.equals(user) &&
                c.budget == closingBalance && c.players.stream().anyMatch(p -> p.id.equals(retained.id) && p.wage == wages)),
                "active German club or contracts were lost at season opening");
        EnglishLegacyIds.season(context, 2011).edit().putString("installed", "yes").putString("league", next).commit();
        NativeGermanDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && managed.league.equals(next) && retained.owner.equals(user),
                "German promotion/relegation did not preserve a played career");
        MatchJournal following = JournalHarness.open(context, nonce, 2011);
        NativeGermanDivisions.freeze(following, career);
        check(new HashSet<>(NativeEnglishDivisions.members(following, "de")).equals(newTop) &&
                NativeEnglishDivisions.members(following, "de2").size() == 18, "new league membership was not frozen");
        following.save();
        NativeGermanDivisions.rollover(context, career, 2011);
        check(career.activeLeague.equals(next) && JournalHarness.open(context, nonce, 2010)
                .competition(NativeGermanDivisions.PLAYOFF).get(1).winner.equals(second.winner),
                "repeated rollover rewrote German history");
        System.out.println("PASS: active " + league + " -> " + next + " · 34 rounds, two playoff legs, stable club, contracts and budget");
    }
}
