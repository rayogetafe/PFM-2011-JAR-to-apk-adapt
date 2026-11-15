package pfm.android;

import android.content.Context;
import java.io.IOException;
import java.util.*;

/** Serie B promotion under FIGC 65/A of 4 August 2010. */
final class NativeItalianDivisions {
    static final String PLAYOFF = "playoff:it2";
    static final int FINAL = 4;
    static final String TABLE = "it2-final-order";
    private static final int TOP_CLUBS = 20, RETAINED = 17;

    private NativeItalianDivisions() {}

    static boolean includes(String league) {
        return "it".equals(league) || NativeSerieB.CODE.equals(league);
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year) throws IOException {
        if (NativeSerieB.active(career, year - 1)) {
            NativeGermanDivisions.rollover(context, career, year, "it", TOP_CLUBS);
        }
    }

    static boolean needsPlayoff(List<UclSeason.Standing> table) {
        return table.get(2).points - table.get(3).points < 10;
    }

    static List<UclSeason.Standing> finalSerieBTable(MatchJournal journal, NativeCareerStore.Career career) {
        return finalTable(journal, NativeSerieB.CODE, career, false);
    }

    static List<UclSeason.Standing> finalTable(MatchJournal journal, String league, NativeCareerStore.Career career,
                                               boolean headToHeadGoals) {
        List<UclSeason.Standing> table = NativeGermanDivisions.finalTable(journal, league, career);
        if (table.isEmpty()) return table;
        List<MatchJournal.Match> fixtures = journal.competition("league:" + league);
        for (int start = 0; start < table.size();) {
            int end = start + 1;
            while (end < table.size() && table.get(end).points == table.get(start).points) end++;
            if (end - start > 1) orderTiedGroup(table.subList(start, end), fixtures, headToHeadGoals);
            start = end;
        }
        return table;
    }

    private static void orderTiedGroup(List<UclSeason.Standing> group, List<MatchJournal.Match> fixtures, boolean headToHeadGoals) {
        Map<String, int[]> direct = new HashMap<>();
        for (UclSeason.Standing row : group) direct.put(row.id, new int[3]);
        for (MatchJournal.Match match : fixtures) {
            int[] home = direct.get(match.home), away = direct.get(match.away);
            if (home == null || away == null) continue;
            int difference = match.homeGoals - match.awayGoals;
            home[0] += difference > 0 ? 3 : difference == 0 ? 1 : 0;
            away[0] += difference < 0 ? 3 : difference == 0 ? 1 : 0;
            home[1] += difference;
            away[1] -= difference;
            home[2] += match.homeGoals;
            away[2] += match.awayGoals;
        }
        group.sort(Comparator.<UclSeason.Standing>comparingInt(row -> direct.get(row.id)[0]).reversed()
                .thenComparing(Comparator.<UclSeason.Standing>comparingInt(row -> direct.get(row.id)[1]).reversed())
                .thenComparingInt(row -> headToHeadGoals ? -direct.get(row.id)[2] : 0)
                .thenComparing(Comparator.<UclSeason.Standing>comparingInt(row -> row.gf - row.ga).reversed())
                .thenComparing(Comparator.<UclSeason.Standing>comparingInt(row -> row.gf).reversed())
                .thenComparing(row -> row.id));
    }

    static void complete(MatchJournal journal, int nonce, int total, HashMap<String, NativeWorldCenter.T> teams,
                         HashMap<String, NativeWorldCenter.P> players, String source, MatchEffects effects,
                         NativeCareerStore.Career career) {
        List<UclSeason.Standing> table = finalSerieBTable(journal, career);
        if (table.isEmpty()) return;
        List<String> order = new ArrayList<>();
        for (UclSeason.Standing row : table) order.add(row.id);
        journal.define(TABLE, EuropeanQualification.encode(order));
        if (!needsPlayoff(table)) return;
        int seed = nonce ^ journal.season() * 10007 ^ 0x495432;
        playPromotion(journal, PLAYOFF, order, seed, total, teams, players, source, effects, career);
    }

    static void playPromotion(MatchJournal journal, String competition, List<String> order, int seed, int total,
                              HashMap<String, NativeWorldCenter.T> teams, HashMap<String, NativeWorldCenter.P> players,
                              String source, MatchEffects effects, NativeCareerStore.Career career) {
        effects.rest(total + 1);
        MatchJournal.Match lowSeed = NativeMatchJournal.capture(journal, competition, 1, order.get(5), order.get(2),
                seed, teams, players, source, effects, career);
        MatchJournal.Match highSeed = NativeMatchJournal.capture(journal, competition, 1, order.get(4), order.get(3),
                seed ^ 1009, teams, players, source, effects, career);
        effects.rest(total + 2);
        String left = NativeMatchJournal.capture(journal, competition, 2, lowSeed.away, lowSeed.home,
                seed ^ 104729, teams, players, source, effects, career, lowSeed, false).winner;
        String right = NativeMatchJournal.capture(journal, competition, 2, highSeed.away, highSeed.home,
                seed ^ 99041, teams, players, source, effects, career, highSeed, false).winner;
        int best = Math.min(order.indexOf(left), order.indexOf(right));
        if (best < 0) throw new IllegalStateException("Serie B play-off semi-final missing a winner");
        String higher = order.get(best);
        effects.rest(total + 3);
        MatchJournal.Match first = NativeMatchJournal.capture(journal, competition, 3,
                higher.equals(left) ? right : left, higher, seed ^ 209458, teams, players, source, effects, career);
        effects.rest(total + 4);
        NativeMatchJournal.capture(journal, competition, FINAL, first.away, first.home,
                seed ^ 314187, teams, players, source, effects, career, first, false);
    }

    static Set<String> nextTop(MatchJournal previous, NativeCareerStore.Career career) throws IOException {
        List<UclSeason.Standing> top = NativeGermanDivisions.finalTable(previous, "it", career);
        List<UclSeason.Standing> lower = finalSerieBTable(previous, career);
        if (top.isEmpty() || lower.isEmpty()) return Collections.emptySet();
        String third = lower.get(2).id;
        if (needsPlayoff(lower)) {
            third = "";
            for (MatchJournal.Match match : previous.competition(PLAYOFF)) {
                if (match.round == FINAL) third = match.winner;
            }
            if (third.isEmpty()) throw new IOException("Missing Serie B playoff result");
        }
        Set<String> clubs = new LinkedHashSet<>();
        for (int i = 0; i < RETAINED; i++) clubs.add(top.get(i).id);
        clubs.add(lower.get(0).id);
        clubs.add(lower.get(1).id);
        clubs.add(third);
        if (clubs.size() != TOP_CLUBS) throw new IOException("Invalid Serie A membership");
        return clubs;
    }

    static int promotionZone(int rank) {
        return rank <= 2 ? 0xffdcefd8 : rank <= 6 ? 0xffdcecff : 0;
    }
}
