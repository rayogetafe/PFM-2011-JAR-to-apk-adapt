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
        if (!NativeChampionship.active(career, year - 1)) return;
        boolean active = includes(career.activeLeague);
        if (active && !EnglishLegacyIds.installed(context, year)) return;
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
            setPremier(career, premier);
            return;
        }
        // A season already played in an older release retains its fixtures and clubs.
        if (!next.competition("league:en").isEmpty() || !next.competition("league:en2").isEmpty()) return;
        MatchJournal previous = new MatchJournal(directory, identity, year - 1);
        Set<String> premier = nextPremier(previous, career);
        if (!premier.isEmpty()) setPremier(career, premier);
    }

    static Set<String> nextPremier(MatchJournal previous, NativeCareerStore.Career career) throws IOException {
        List<String> up = ChampionshipPlayoffs.promotionCandidates(previous), down = relegated(previous, career);
        if (up.size() != 3 || down.size() != 3) return Collections.emptySet();
        Set<String> premier = new LinkedHashSet<>(members(previous, PREMIER));
        if (!members(previous, CHAMPIONSHIP).containsAll(up) || !premier.containsAll(down)) {
            throw new IOException("Invalid English promotion and relegation places");
        }
        premier.removeAll(down);
        premier.addAll(up);
        if (premier.size() != 20) throw new IOException("Invalid Premier League size");
        return premier;
    }

    private static void setPremier(NativeCareerStore.Career career, Set<String> premier) {
        for (NativeCareerStore.Club club : career.clubs) {
            if (includes(club.league)) club.league = premier.contains(club.id) ? PREMIER : CHAMPIONSHIP;
            if (includes(career.activeLeague) && includes(club.league) && club.name != null && club.name.equalsIgnoreCase(career.userClub)) {
                career.activeLeague = club.league;
            }
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

    static String badge(Context context, String clubName, String league, int slot) {
        if (includes(league)) {
            if (NativeWorldCenter.leagues == null) NativeWorldCenter.load(context);
            for (String origin : Arrays.asList(PREMIER, CHAMPIONSHIP)) {
                List<NativeWorldCenter.T> clubs = NativeWorldCenter.leagues.get(origin);
                if (clubs != null) for (NativeWorldCenter.T club : clubs) {
                    if (!club.name.equalsIgnoreCase(clubName)) continue;
                    return origin.equals(CHAMPIONSHIP) ? "second_division_badges/en2_" + club.id + ".png" :
                            "badges/en" + (club.id + 1) + ".png";
                }
            }
        }
        return slot < 0 ? null : "badges/" + league + (slot + 1) + ".png";
    }

    static int championshipZone(int rank) {
        return rank <= 2 ? 0xffdcefd8 : rank <= 6 ? 0xffdcecff : 0;
    }

    static void moveOpeningBans(Map<String, Integer> bans, MatchJournal previous, NativeCareerStore.Career career) {
        if (bans.isEmpty()) return;
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
