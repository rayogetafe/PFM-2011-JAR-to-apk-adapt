package pfm.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Read-only handoff to the legacy matchday code just before round one is installed. */
public final class NativeEnglishCareer {
    private NativeEnglishCareer() {}

    public static final class Opening {
        public String league, userId;
        public final ArrayList<Club> clubs = new ArrayList<>();
    }

    public static final class Club {
        public String id, name;
        public int budget;
        public final ArrayList<Player> players = new ArrayList<>();
    }

    public static final class Player {
        public String id, name;
        public int originalSlot = -1, age, position, speed, resistance, quality, wage, value;
    }

    public static int[][][] calendar(int clubs) {
        int[][][] rounds = new int[2 * (clubs - 1)][clubs / 2][3];
        int[] slots = new int[rounds.length];
        for (NativeWorldCenter.M match : NativeWorldCenter.schedule(clubs)) {
            int round = match.r - 1;
            int[] fixture = rounds[round][slots[round]++];
            fixture[0] = match.h;
            fixture[1] = match.a;
        }
        return rounds;
    }

    public static Opening championshipStart(Context context, int clubIndex) throws IOException {
        Map<String, ArrayList<NativeWorldCenter.T>> world = new HashMap<>();
        NativeChampionship.load(context, world);
        ArrayList<NativeWorldCenter.T> teams = world.get(NativeChampionship.CODE);
        Opening opening = new Opening();
        opening.league = NativeChampionship.CODE;
        opening.userId = opening.league + ":" + teams.get(clubIndex).id;
        for (NativeWorldCenter.T team : teams) {
            Club club = new Club();
            club.id = opening.league + ":" + team.id;
            club.name = team.name;
            long values = 0;
            for (NativeWorldCenter.P source : team.ps) {
                Player player = new Player();
                player.id = source.uid;
                player.name = source.name;
                player.age = source.age;
                player.position = source.pos;
                player.speed = source.spe;
                player.resistance = source.res;
                player.quality = source.qua;
                player.wage = source.wage;
                player.value = source.value;
                values += source.value;
                club.players.add(player);
            }
            club.budget = (int) Math.max(2000000L, values / 4);
            opening.clubs.add(club);
        }
        return opening;
    }

    public static Opening opening(Context context, int previousYear) throws IOException {
        int nonce = EnglishLegacyIds.nonce(context);
        NativeCareerStore.Career career = NativeCareerStore.englishOpening(context, previousYear, nonce);
        if (career == null) return null;
        MatchJournal previous = new MatchJournal(new File(context.getFilesDir(), "match-journals"), nonce, previousYear);
        if ("en".equals(career.activeLeague) && previous.metadata(NativeEnglishDivisions.FINAL_PREMIER_ORDER) == null) return null;
        Set<String> premier = NativeEnglishDivisions.nextPremier(previous, career);
        if (premier.isEmpty()) return null;
        Opening opening = new Opening();
        for (NativeCareerStore.Club club : career.clubs) {
            if (NativeEnglishDivisions.includes(club.league) && club.name.equalsIgnoreCase(career.userClub)) {
                opening.userId = club.id;
                break;
            }
        }
        if (opening.userId == null) throw new IOException("English career club unavailable");
        boolean top = premier.contains(opening.userId);
        opening.league = top ? "en" : "en2";
        Map<String, Club> selected = new LinkedHashMap<>();
        for (NativeCareerStore.Club source : career.clubs) {
            if (!NativeEnglishDivisions.includes(source.league) || premier.contains(source.id) != top) continue;
            Club club = new Club();
            club.id = source.id;
            club.name = source.name;
            club.budget = source.balance;
            selected.put(club.id, club);
        }
        for (NativeCareerStore.Player source : career.players) {
            Club club = selected.get(source.owner);
            if (club == null) continue;
            Player player = new Player();
            player.id = source.id;
            player.name = source.name;
            if (source.id.startsWith("en:")) player.originalSlot = source.sourceId;
            player.age = Math.min(50, source.age + 1);
            player.position = source.position;
            player.speed = source.spe;
            player.resistance = source.res;
            player.quality = source.qua;
            player.wage = source.contractWage;
            player.value = source.value;
            club.players.add(player);
        }
        opening.clubs.addAll(selected.values());
        if (opening.clubs.size() != (top ? 20 : NativeChampionship.CLUBS)) throw new IOException("Wrong English division size");
        return opening;
    }

    public static void recordFinalPremier(Context context, int year, List<String> order) throws IOException {
        int nonce = EnglishLegacyIds.nonce(context);
        if (nonce == 0 || order.size() != 20) return;
        MatchJournal journal = new MatchJournal(new File(context.getFilesDir(), "match-journals"), nonce, year);
        Set<String> members = new HashSet<>(NativeEnglishDivisions.members(journal, "en"));
        if (members.size() != 20 || !members.equals(new HashSet<>(order))) return;
        journal.define(NativeEnglishDivisions.FINAL_PREMIER_ORDER, EuropeanQualification.encode(order));
        journal.save();
    }
}
