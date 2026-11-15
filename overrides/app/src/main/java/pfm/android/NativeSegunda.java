package pfm.android;

import android.content.Context;
import java.io.*;
import java.util.*;

final class NativeSegunda {
    static final String CODE = "es2", INACTIVE = "es3";
    static final int CLUBS = 22, ROUNDS = 42, PLAYERS = 528;
    static final String BARCA_B = "es2:2", VILLARREAL_B = "es2:20";
    static final String SABADELL = "es3:0", GUADALAJARA = "es3:1";
    static final List<String> RESERVES = Arrays.asList(BARCA_B, VILLARREAL_B);
    static final List<String> REPLACEMENTS = Arrays.asList(SABADELL, GUADALAJARA);
    private static final String ASSET = "segunda_squads_2010.tsv", PROVENANCE = "SEGUNDA_2010_ESTIMATED";

    private NativeSegunda() {}

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world) throws IOException {
        NativeLigue2.load(context, world, CODE, CLUBS, PLAYERS - 44, ASSET, "08-27", PROVENANCE);
        ArrayList<NativeWorldCenter.T> standby = new ArrayList<>();
        for (String name : Arrays.asList("Sabadell", "Guadalajara")) {
            NativeWorldCenter.T club = new NativeWorldCenter.T();
            club.id = standby.size();
            club.name = name;
            club.style = 55;
            club.form = 2;
            standby.add(club);
        }
        try (BufferedReader input = NativeLigue2.reader(context, ASSET)) {
            NativeLigue2.readPlayers(input, standby, INACTIVE, 44, "08-27", PROVENANCE);
        }
        world.put(INACTIVE, standby);
    }

    static boolean active(NativeCareerStore.Career career, int season) {
        for (NativeCareerStore.Club club : career.clubs) {
            if (CODE.equals(club.league) && club.activeFrom <= season) return true;
        }
        return false;
    }

    static boolean registered(String id) {
        return id.startsWith(CODE + ":") || id.startsWith(INACTIVE + ":");
    }

    static boolean spanishClub(String id) {
        return id.startsWith("es:") || registered(id);
    }

    static void install(NativeCareerStore.Career career, NativeCareerStore.Career seed) {
        if (active(career, Integer.MAX_VALUE)) return;
        for (NativeCareerStore.Club club : seed.clubs) if (registered(club.id)) career.clubs.add(club);
        for (NativeCareerStore.Player player : seed.players) if (registered(player.owner)) career.players.add(player);
    }

    static int openingCash(ClubEconomy.Profile ground) {
        return 1_100_000 + 150 * ground.attendance;
    }

    public static boolean reserve(String id) {
        return RESERVES.contains(id);
    }

    static String parent(String id) {
        if (BARCA_B.equals(id)) return "es:0";
        if (VILLARREAL_B.equals(id)) return "es:4";
        return "";
    }
}
