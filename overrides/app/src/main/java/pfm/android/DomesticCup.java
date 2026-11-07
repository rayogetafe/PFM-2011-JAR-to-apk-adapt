package pfm.android;

import java.util.*;

/** Single-leg domestic cups, with a neutral final and a frozen list of entrants. */
final class DomesticCup {
    private static final int[] MAIN_DATES = {6, 9, 13, 20, 28, 36};
    private static final String[] MAIN_STAGES = {
            "Round of 64", "Round of 32", "Round of 16", "Quarter-finals", "Semi-finals", "Final"
    };

    static final class Bracket {
        final List<String> entrants;
        final int rounds;
        private final int preliminaryMatches;
        private final int drawSeed;

        Bracket(String saved) {
            entrants = Collections.unmodifiableList(EuropeanQualification.ids(saved));
            int size = entrants.size();
            if (size < 2 || size > 64 || new HashSet<>(entrants).size() != size) {
                throw new IllegalStateException("Invalid domestic cup entries: " + size);
            }
            int mainSize = Integer.highestOneBit(size);
            preliminaryMatches = size - mainSize;
            rounds = Integer.numberOfTrailingZeros(mainSize) + (preliminaryMatches > 0 ? 1 : 0);
            drawSeed = saved.hashCode();
        }

        int mark(int round) {
            return round == 1 && preliminaryMatches > 0 ? 6 : MAIN_DATES[stageIndex(round)];
        }

        String stage(int round) {
            return round == 1 && preliminaryMatches > 0 ? "Preliminary" : MAIN_STAGES[stageIndex(round)];
        }

        int matchCount(int round) {
            return round == 1 && preliminaryMatches > 0 ? preliminaryMatches : 1 << (rounds - round);
        }

        private int stageIndex(int round) {
            return MAIN_STAGES.length - rounds + round - 1;
        }

        List<String[]> fixtures(List<MatchJournal.Match> matches, int round) {
            int preliminaryClubs = preliminaryMatches * 2;
            List<String> clubs = new ArrayList<>();
            if (round == 1) {
                clubs.addAll(preliminaryMatches == 0 ? entrants : entrants.subList(0, preliminaryClubs));
            } else {
                for (MatchJournal.Match match : matches) {
                    if (match.round == round - 1 && !match.winner.isEmpty()) clubs.add(match.winner);
                }
                if (clubs.size() != matchCount(round - 1)) return Collections.emptyList();
                if (round == 2 && preliminaryMatches > 0) {
                    clubs.addAll(entrants.subList(preliminaryClubs, entrants.size()));
                }
                Collections.sort(clubs);
                Collections.shuffle(clubs, new Random(drawSeed ^ round * 104729));
            }
            List<String[]> pairs = new ArrayList<>();
            for (int i = 0; i < clubs.size(); i += 2) pairs.add(new String[]{clubs.get(i), clubs.get(i + 1)});
            return pairs;
        }
    }

    static String competition(String league) {
        return "cup:" + league;
    }

    static Bracket bracket(MatchJournal journal, String league) {
        String saved = journal.metadata("cup-entrants-" + league);
        return saved == null ? null : new Bracket(saved);
    }

    static MatchJournal.Match finalResult(MatchJournal journal, String league) {
        Bracket cup = bracket(journal, league);
        if (cup == null) return null;
        for (MatchJournal.Match match : journal.competition(competition(league))) {
            if (match.round == cup.rounds && !match.winner.isEmpty()) return match;
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
            Bracket cup = bracket(journal, league);
            if (cup == null) {
                Set<String> eligible = new TreeSet<>();
                for (NativeCareerStore.Club club : career.clubs) {
                    if (club.activeFrom <= journal.season() && EuropeanPlaces.association(club.league).equals(league)) {
                        eligible.add(club.id);
                    }
                }
                List<String> entrants = new ArrayList<>(eligible);
                Collections.shuffle(entrants, new Random(seed));
                String saved = EuropeanQualification.encode(entrants);
                cup = new Bracket(saved);
                journal.define(key, saved);
            }
            for (int round = 1; round <= cup.rounds; round++) {
                if (step != NativeCompetitionCalendar.sourceRound(cup.mark(round), total)) continue;
                List<String[]> pairs = cup.fixtures(journal.competition(competition(league)), round);
                if (pairs.isEmpty()) throw new IllegalStateException("Domestic cup round incomplete for " + league);
                int fixture = 0;
                for (String[] pair : pairs) {
                    NativeMatchJournal.capture(journal, competition(league), round, pair[0], pair[1],
                            seed ^ (round - 1) * 104729 ^ fixture++ * 1009, teams, players,
                            matchSource, effects, career, null, round == cup.rounds);
                }
            }
        }
    }
}
