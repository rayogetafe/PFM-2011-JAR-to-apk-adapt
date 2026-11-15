package pfm.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Two automatic exchanges and a two-leg German promotion/relegation tie. */
final class NativeGermanDivisions {
    static final String TOP = "de";
    static final String LOWER = NativeBundesliga2.CODE;
    static final String PLAYOFF = "playoff:de";
    private static final int SIZE = NativeBundesliga2.CLUBS;

    private NativeGermanDivisions() {}

    static boolean includes(String league) {
        return TOP.equals(league) || LOWER.equals(league);
    }

    static void freeze(MatchJournal journal, NativeCareerStore.Career career) {
        if (!NativeBundesliga2.active(career, journal.season())) return;
        freeze(journal, career, TOP);
    }

    static void freeze(MatchJournal journal, NativeCareerStore.Career career, String country) {
        for (String league : Arrays.asList(country, country + "2")) {
            String key = UclSeason.membersKey(league);
            if (journal.metadata(key) != null) continue;
            List<String> members = new ArrayList<>();
            for (NativeCareerStore.Club club : career.clubs) {
                if (league.equals(club.league)) members.add(club.id);
            }
            journal.define(key, EuropeanQualification.encode(members));
        }
    }

    static List<UclSeason.Standing> finalTable(MatchJournal journal, String league,
                                                      NativeCareerStore.Career career) {
        List<UclSeason.Standing> standings = UclSeason.standings(journal, league, career.clubs);
        if (standings.size() != (NativeFrenchDivisions.includes(league) ? 20 : SIZE)) return Collections.emptyList();
        for (UclSeason.Standing row : standings) {
            if (row.played != NativeCompetitionCalendar.leagueRounds(league)) return Collections.emptyList();
        }
        return standings;
    }

    static void complete(MatchJournal journal, int nonce, int total, HashMap<String, NativeWorldCenter.T> teams,
                         HashMap<String, NativeWorldCenter.P> players, String source, MatchEffects effects,
                         NativeCareerStore.Career career) {
        List<UclSeason.Standing> top = finalTable(journal, TOP, career);
        List<UclSeason.Standing> lower = finalTable(journal, LOWER, career);
        if (top.isEmpty() || lower.isEmpty()) return;
        String upperClub = top.get(15).id, lowerClub = lower.get(2).id;
        int seed = nonce ^ journal.season() * 10007 ^ 0x444532;
        effects.rest(total + 1);
        MatchJournal.Match first = NativeMatchJournal.capture(journal, PLAYOFF, 1, upperClub, lowerClub, seed,
                teams, players, source, effects, career);
        effects.rest(total + 2);
        NativeMatchJournal.capture(journal, PLAYOFF, 2, lowerClub, upperClub, seed ^ 104729,
                teams, players, source, effects, career, first, false);
    }

    static Set<String> nextTop(MatchJournal previous, NativeCareerStore.Career career, int nonce) throws IOException {
        List<UclSeason.Standing> top = finalTable(previous, TOP, career);
        List<UclSeason.Standing> lower = finalTable(previous, LOWER, career);
        if (top.isEmpty() || lower.isEmpty()) return Collections.emptySet();
        String upperClub = top.get(15).id, lowerClub = lower.get(2).id;
        String finalId = NativeMatchJournal.key(PLAYOFF, 2, lowerClub, upperClub);
        MatchJournal.Match second = previous.get(finalId);
        if (second == null) {
            // A completed season saved before German play-offs were introduced still gets a final tie.
            HashMap<String, NativeWorldCenter.T> teams = NativeMatchJournal.teams(career);
            HashMap<String, NativeWorldCenter.P> players = new HashMap<>();
            for (NativeWorldCenter.T team : teams.values()) {
                for (NativeWorldCenter.P player : team.ps) players.put(player.uid, player);
            }
            int rounds = NativeCompetitionCalendar.leagueRounds(career.activeLeague);
            complete(previous, nonce, rounds, teams, players, "reconstructed-v115", new MatchEffects(rounds), career);
            previous.save();
            second = previous.get(finalId);
        }
        if (second == null || !(upperClub.equals(second.winner) || lowerClub.equals(second.winner))) {
            throw new IOException("Missing German play-off result");
        }
        Set<String> clubs = new LinkedHashSet<>();
        for (int i = 0; i < 15; i++) clubs.add(top.get(i).id);
        clubs.add(second.winner);
        clubs.add(lower.get(0).id);
        clubs.add(lower.get(1).id);
        if (clubs.size() != SIZE) throw new IOException("Invalid Bundesliga membership");
        return clubs;
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year) throws IOException {
        if (!NativeBundesliga2.active(career, year - 1)) return;
        rollover(context, career, year, TOP, SIZE);
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year, String first, int size) throws IOException {
        boolean active = first.equals(EuropeanPlaces.association(career.activeLeague));
        if (active && !EnglishLegacyIds.installed(context, year)) return;
        String second = first + "2";
        File directory = new File(context.getFilesDir(), "match-journals");
        int nonce = NativeWorldCenter.identity();
        MatchJournal next = new MatchJournal(directory, nonce, year);
        String saved = next.metadata(UclSeason.membersKey(first));
        Set<String> top;
        if (saved != null) {
            top = new LinkedHashSet<>(EuropeanQualification.ids(saved));
            Set<String> lower = new HashSet<>(EuropeanQualification.ids(next.metadata(UclSeason.membersKey(second))));
            Set<String> all = new HashSet<>(top);
            all.addAll(lower);
            Set<String> careerClubs = new HashSet<>();
            for (NativeCareerStore.Club club : career.clubs) {
                if (first.equals(EuropeanPlaces.association(club.league))) careerClubs.add(club.id);
            }
            if (top.size() != size || lower.size() != size || all.size() != 2 * size || !all.equals(careerClubs)) {
                throw new IOException("Invalid saved league membership for " + first);
            }
        } else {
            // Keep the original membership if the new season already has pre-v115 fixtures.
            if (!next.competition("league:" + first).isEmpty() || !next.competition("league:" + second).isEmpty()) return;
            MatchJournal previous = new MatchJournal(directory, nonce, year - 1);
            top = "fr".equals(first) ? NativeFrenchDivisions.nextTop(previous, career) : nextTop(previous, career, nonce);
        }
        if (top.isEmpty()) return;
        for (NativeCareerStore.Club club : career.clubs) {
            if (!first.equals(EuropeanPlaces.association(club.league))) continue;
            club.league = top.contains(club.id) ? first : second;
            if (active && club.name.equalsIgnoreCase(career.userClub)) career.activeLeague = club.league;
        }
    }

    static int zone(String league, int rank) {
        if (LOWER.equals(league)) return rank <= 2 ? 0xffdcefd8 : rank == 3 ? 0xffdcecff : 0;
        return rank == 16 ? 0xffdcecff : EuropeanPlaces.zone(league, rank, SIZE);
    }

    static String legend(String league) {
        return LOWER.equals(league) ? "Green: automatic promotion · Blue: play-off"
                : "Blue: UCL or play-off (16th) · Gold: UEL · Red: relegation";
    }
}
