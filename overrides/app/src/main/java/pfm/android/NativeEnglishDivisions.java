package pfm.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** The original club ID survives promotion, relegation and archive lookup. */
final class NativeEnglishDivisions {
    static final String PREMIER = "en";
    static final String CHAMPIONSHIP = NativeChampionship.CODE;

    static boolean includes(String league) {
        return PREMIER.equals(league) || CHAMPIONSHIP.equals(league);
    }

    static void freeze(MatchJournal journal, NativeCareerStore.Career career) {
        if (!NativeChampionship.active(career, journal.season())) return;
        for (String league : Arrays.asList(PREMIER, CHAMPIONSHIP)) {
            if (journal.metadata(UclSeason.membersKey(league)) != null) continue;
            List<String> members = new ArrayList<>();
            for (NativeCareerStore.Club club : career.clubs) {
                if (club.league.equals(league)) members.add(club.id);
            }
            journal.define(UclSeason.membersKey(league), EuropeanQualification.encode(members));
        }
    }

    static List<String> relegated(MatchJournal previous, NativeCareerStore.Career career) {
        List<UclSeason.Standing> standings = UclSeason.standings(previous, PREMIER, career.clubs);
        if (standings.size() != 20) return Collections.emptyList();
        for (UclSeason.Standing row : standings) if (row.played != 38) return Collections.emptyList();
        List<String> clubs = new ArrayList<>();
        for (UclSeason.Standing row : standings.subList(17, 20)) clubs.add(row.id);
        return clubs;
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year) throws IOException {
        if (includes(career.activeLeague) || !NativeChampionship.active(career, year - 1)) return;
        File directory = new File(context.getFilesDir(), "match-journals");
        int identity = NativeWorldCenter.identity();
        MatchJournal next = new MatchJournal(directory, identity, year);
        String recorded = next.metadata(UclSeason.membersKey(PREMIER));
        if (recorded != null) {
            Set<String> premier = new HashSet<>(EuropeanQualification.ids(recorded));
            Set<String> championship = new HashSet<>(EuropeanQualification.ids(next.metadata(UclSeason.membersKey(CHAMPIONSHIP))));
            Set<String> all = new HashSet<>();
            for (NativeCareerStore.Club club : career.clubs) if (includes(club.league)) all.add(club.id);
            Set<String> recordedClubs = new HashSet<>(premier);
            recordedClubs.addAll(championship);
            if (premier.size() != 20 || championship.size() != NativeChampionship.CLUBS ||
                    recordedClubs.size() != 20 + NativeChampionship.CLUBS || !recordedClubs.equals(all)) {
                throw new IOException("Invalid English league membership");
            }
            for (NativeCareerStore.Club club : career.clubs) {
                if (includes(club.league)) club.league = premier.contains(club.id) ? PREMIER : CHAMPIONSHIP;
            }
            return;
        }
        // A season already played in an older release retains its fixtures and clubs.
        if (!next.competition("league:en").isEmpty() || !next.competition("league:en2").isEmpty()) return;
        MatchJournal previous = new MatchJournal(directory, identity, year - 1);
        List<String> up = ChampionshipPlayoffs.promotionCandidates(previous);
        List<String> down = relegated(previous, career);
        if (up.size() != 3 || down.size() != 3) return;
        if (!members(previous, CHAMPIONSHIP).containsAll(up) || !members(previous, PREMIER).containsAll(down)) {
            throw new IOException("Invalid English promotion and relegation places");
        }
        for (NativeCareerStore.Club club : career.clubs) {
            if (up.contains(club.id)) club.league = PREMIER;
            if (down.contains(club.id)) club.league = CHAMPIONSHIP;
        }
    }

    static NativeWorldCenter.T team(String id) {
        int colon = id.lastIndexOf(':');
        NativeWorldCenter.T team = NativeWorldCenter.leagues.get(id.substring(0, colon)).get(Integer.parseInt(id.substring(colon + 1)));
        team.clubId = id;
        return team;
    }

    static List<String> members(MatchJournal journal, String code) {
        String frozen = journal.metadata(UclSeason.membersKey(code));
        if (frozen != null) return EuropeanQualification.ids(frozen);
        List<String> ids = new ArrayList<>();
        List<NativeWorldCenter.T> teams = NativeWorldCenter.leagues.get(code);
        if (teams != null) for (NativeWorldCenter.T team : teams) ids.add(code + ":" + team.id);
        return ids;
    }

    static void moveOpeningBans(Map<String, Integer> bans, MatchJournal previous, NativeCareerStore.Career career) {
        if (includes(career.activeLeague) || bans.isEmpty()) return;
        Set<String> oldPremier = new HashSet<>(members(previous, PREMIER));
        Map<String, String> oldLeagues = new HashMap<>(), newLeagues = new HashMap<>();
        for (NativeCareerStore.Club club : career.clubs) {
            if (!includes(club.league)) continue;
            String oldLeague = oldPremier.contains(club.id) ? PREMIER : CHAMPIONSHIP;
            if (!club.league.equals(oldLeague)) {
                oldLeagues.put(club.id, oldLeague);
                newLeagues.put(club.id, club.league);
            }
        }
        if (oldLeagues.isEmpty()) return;
        for (NativeCareerStore.Player player : career.players) {
            String oldLeague = oldLeagues.get(player.owner);
            if (oldLeague == null) continue;
            Integer remaining = bans.remove("league:" + oldLeague + "|" + player.id);
            if (remaining != null) {
                String key = "league:" + newLeagues.get(player.owner) + "|" + player.id;
                Integer existing = bans.get(key);
                bans.put(key, existing == null ? remaining : Math.max(existing, remaining));
            }
        }
    }
}
