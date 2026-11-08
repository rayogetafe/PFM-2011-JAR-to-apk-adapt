package pfm.android;

import java.util.*;

/** Persisted outcome of the Championship promotion race. */
final class ChampionshipPlayoffs {
    static final String COMPETITION = EuropeanPlaces.CHAMPIONSHIP_PLAYOFF;
    static final String TABLE = "en2-promotion-table";
    private static final String SOURCE = "en2-playoff-source";
    private static final int FINAL = 3;

    static void complete(MatchJournal journal, int nonce, int total,
                         HashMap<String, NativeWorldCenter.T> teams, HashMap<String, NativeWorldCenter.P> players,
                         String source, MatchEffects effects, NativeCareerStore.Career career) {
        List<String> order = EuropeanQualification.ids(journal.metadata(TABLE));
        if (order.isEmpty()) {
            List<UclSeason.Standing> table = UclSeason.standings(journal, NativeChampionship.CODE, career.clubs);
            if (table.size() != NativeChampionship.CLUBS) return;
            for (UclSeason.Standing row : table) {
                if (row.played != NativeChampionship.ROUNDS) return;
                order.add(row.id);
            }
            journal.define(TABLE, EuropeanQualification.encode(order));
            journal.define(SOURCE, source);
        }
        String fixtureSource = journal.metadata(SOURCE);
        boolean live = "native-v84".equals(fixtureSource);

        int seed = nonce ^ journal.season() * 10007 ^ 0x454e32;
        MatchJournal.Match[] first = new MatchJournal.Match[2];
        if (live) effects.rest(total + 1);
        for (int i = 0; i < first.length; i++) {
            first[i] = NativeMatchJournal.capture(journal, COMPETITION, 1, order.get(5 - i), order.get(2 + i),
                    seed ^ i * 1009, teams, players, fixtureSource, effects, career);
        }
        if (live) effects.rest(total + 2);
        String[] finalists = new String[2];
        for (int i = 0; i < first.length; i++) {
            MatchJournal.Match leg = first[i];
            finalists[i] = NativeMatchJournal.capture(journal, COMPETITION, 2, leg.away, leg.home,
                    seed ^ 104729 ^ i * 1009, teams, players, fixtureSource, effects, career, leg, false).winner;
        }
        if (live) effects.rest(total + 3);
        NativeMatchJournal.capture(journal, COMPETITION, FINAL, finalists[0], finalists[1],
                seed ^ 209458, teams, players, fixtureSource, effects, career, null, true);
    }

    static int aggregateDecision(MatchJournal.Match first, int home, int away) {
        return Integer.compare(first.awayGoals + home, first.homeGoals + away);
    }

    static List<String> promotionCandidates(MatchJournal journal) {
        List<String> order = EuropeanQualification.ids(journal.metadata(TABLE));
        if (order.isEmpty()) return Collections.emptyList();
        List<String> candidates = new ArrayList<>(order.subList(0, 2));
        for (MatchJournal.Match match : journal.competition(COMPETITION)) {
            if (match.round == FINAL && !match.winner.isEmpty()) {
                candidates.add(match.winner);
                break;
            }
        }
        return candidates;
    }
}
