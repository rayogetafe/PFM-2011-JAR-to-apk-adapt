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

    public static Opening opening(Context context, int previousYear) throws IOException {
        int nonce = EnglishLegacyIds.nonce(context);
        NativeCareerStore.Career career = NativeCareerStore.englishOpening(context, previousYear, nonce);
        if (career == null) return null;
        MatchJournal previous = new MatchJournal(new File(context.getFilesDir(), "match-journals"), nonce, previousYear);
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
}
