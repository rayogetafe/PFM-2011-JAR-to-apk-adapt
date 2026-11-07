package pfm.android;

import java.util.*;

/** Single-leg cups for the existing top divisions, with a neutral final. */
final class DomesticCup {
    static final int[] ROUNDS = {6, 13, 20, 28, 36};
    static final String[] STAGES = {"Preliminary", "Round of 16", "Quarter-finals", "Semi-finals", "Final"};

    static String competition(String league) {
        return "cup:" + league;
    }

    static List<String[]> fixtures(MatchJournal journal, String league, int round) {
        String saved = journal.metadata("cup-entrants-" + league);
        if (saved == null) return Collections.emptyList();
        List<String> entrants = EuropeanQualification.ids(saved);
        int preliminary = 2 * (entrants.size() - 16);
        List<String> clubs = new ArrayList<>();
        if (round == 1) {
            clubs.addAll(entrants.subList(0, preliminary));
        } else {
            int expected = round == 2 ? preliminary / 2 : 16 >> (round - 2);
            for (MatchJournal.Match match : journal.competition(competition(league))) {
                if (match.round == round - 1 && !match.winner.isEmpty()) clubs.add(match.winner);
            }
            if (clubs.size() != expected) return Collections.emptyList();
            if (round == 2) clubs.addAll(entrants.subList(preliminary, entrants.size()));
            Collections.sort(clubs);
            Collections.shuffle(clubs, new Random(saved.hashCode() ^ round * 104729));
        }
        List<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < clubs.size(); i += 2) {
            pairs.add(new String[]{clubs.get(i), clubs.get(i + 1)});
        }
        return pairs;
    }

    static MatchJournal.Match finalResult(MatchJournal journal, String league) {
        for (MatchJournal.Match match : journal.competition(competition(league))) {
            if (match.round == ROUNDS.length && !match.winner.isEmpty()) return match;
        }
        return null;
    }

    static String europaEntrant(MatchJournal journal, String league, Set<String> champions) {
        MatchJournal.Match result = finalResult(journal, league);
        if (result == null) return "";
        if (!champions.contains(result.winner)) return result.winner;
        String runnerUp = result.winner.equals(result.home) ? result.away : result.home;
        return champions.contains(runnerUp) ? "" : runnerUp;
    }

    static void advance(MatchJournal journal, int nonce, int step, int total,
                        HashMap<String, NativeWorldCenter.T> teams,
                        HashMap<String, NativeWorldCenter.P> players, String source,
                        MatchEffects effects, NativeCareerStore.Career career) {
        String savedCutoff = journal.metadata("cup-v95-cutoff");
        int cutoff = savedCutoff == null ? 0 : Integer.parseInt(savedCutoff);
        String matchSource = step <= cutoff ? "reconstructed-v95" : source;
        for (String league : NativeWorldCenter.C) {
            String key = "cup-entrants-" + league;
            int seed = nonce ^ journal.season() * 10007 ^ league.hashCode() ^ 0x9521;
            if (journal.metadata(key) == null) {
                List<String> entrants = new ArrayList<>();
                for (NativeCareerStore.Club club : career.clubs) {
                    if (club.league.equals(league)) entrants.add(club.id);
                }
                if (entrants.size() != (league.equals("de") ? 18 : 20)) {
                    throw new IllegalStateException("Incomplete domestic cup entries for " + league);
                }
                Collections.sort(entrants);
                Collections.shuffle(entrants, new Random(seed));
                journal.define(key, EuropeanQualification.encode(entrants));
            }
            for (int stage = 0; stage < ROUNDS.length; stage++) {
                if (step != NativeCompetitionCalendar.sourceRound(ROUNDS[stage], total)) continue;
                List<String[]> pairs = fixtures(journal, league, stage + 1);
                if (pairs.isEmpty()) throw new IllegalStateException("Domestic cup round incomplete for " + league);
                int fixture = 0;
                for (String[] pair : pairs) {
                    NativeMatchJournal.capture(journal, competition(league), stage + 1, pair[0], pair[1],
                            seed ^ stage * 104729 ^ fixture++ * 1009, teams, players,
                            matchSource, effects, career, null, stage == ROUNDS.length - 1);
                }
            }
        }
    }
}
