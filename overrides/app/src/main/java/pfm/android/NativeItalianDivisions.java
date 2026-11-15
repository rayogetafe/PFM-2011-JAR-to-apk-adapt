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
        List<UclSeason.Standing> table = NativeGermanDivisions.finalTable(journal, NativeSerieB.CODE, career);
        if (table.isEmpty()) return table;
        List<MatchJournal.Match> fixtures = journal.competition("league:" + NativeSerieB.CODE);
        for (int start = 0; start < table.size();) {
            int end = start + 1;
            while (end < table.size() && table.get(end).points == table.get(start).points) end++;
            if (end - start > 1) orderTiedGroup(table.subList(start, end), fixtures);
            start = end;
        }
        return table;
    }

    private static void orderTiedGroup(List<UclSeason.Standing> group, List<MatchJournal.Match> fixtures) {
        Map<String, int[]> direct = new HashMap<>();
        for (UclSeason.Standing row : group) direct.put(row.id, new int[2]);
        for (MatchJournal.Match match : fixtures) {
            int[] home = direct.get(match.home), away = direct.get(match.away);
            if (home == null || away == null) continue;
            int difference = match.homeGoals - match.awayGoals;
            home[0] += difference > 0 ? 3 : difference == 0 ? 1 : 0;
            away[0] += difference < 0 ? 3 : difference == 0 ? 1 : 0;
            home[1] += difference;
            away[1] -= difference;
        }
        group.sort(Comparator.<UclSeason.Standing>comparingInt(row -> direct.get(row.id)[0]).reversed()
                .thenComparing(Comparator.<UclSeason.Standing>comparingInt(row -> direct.get(row.id)[1]).reversed())
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
        effects.rest(total + 1);
        MatchJournal.Match lowSeed = NativeMatchJournal.capture(journal, PLAYOFF, 1, table.get(5).id, table.get(2).id,
                seed, teams, players, source, effects, career);
        MatchJournal.Match highSeed = NativeMatchJournal.capture(journal, PLAYOFF, 1, table.get(4).id, table.get(3).id,
                seed ^ 1009, teams, players, source, effects, career);
        effects.rest(total + 2);
        String left = NativeMatchJournal.capture(journal, PLAYOFF, 2, lowSeed.away, lowSeed.home,
                seed ^ 104729, teams, players, source, effects, career, lowSeed, false).winner;
        String right = NativeMatchJournal.capture(journal, PLAYOFF, 2, highSeed.away, highSeed.home,
                seed ^ 99041, teams, players, source, effects, career, highSeed, false).winner;
        int best = Math.min(order.indexOf(left), order.indexOf(right));
        if (best < 0) throw new IllegalStateException("Serie B play-off semi-final missing a winner");
        String higher = order.get(best);
        effects.rest(total + 3);
        MatchJournal.Match first = NativeMatchJournal.capture(journal, PLAYOFF, 3,
                higher.equals(left) ? right : left, higher, seed ^ 209458, teams, players, source, effects, career);
        effects.rest(total + 4);
        NativeMatchJournal.capture(journal, PLAYOFF, FINAL, first.away, first.home,
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
