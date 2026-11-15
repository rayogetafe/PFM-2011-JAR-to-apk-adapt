package pfm.android;

import android.content.Context;
import java.io.IOException;
import java.util.*;

final class NativeSerieB {
    static final String CODE = "it2";
    static final int CLUBS = 22, PLAYERS = 483, ROUNDS = 42;

    private NativeSerieB() {}

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world) throws IOException {
        NativeLigue2.load(context, world, CODE, CLUBS, PLAYERS, "serie_b_squads_2010.tsv", "08-20", "SERIEB_AUG2010_ESTIMATED");
    }

    static boolean active(NativeCareerStore.Career career, int season) {
        for (NativeCareerStore.Club club : career.clubs) {
            if (CODE.equals(club.league) && club.activeFrom <= season) return true;
        }
        return false;
    }

    static void install(NativeCareerStore.Career career, NativeCareerStore.Career seed) {
        if (active(career, Integer.MAX_VALUE)) return;
        for (NativeCareerStore.Club club : seed.clubs) if (CODE.equals(club.league)) career.clubs.add(club);
        for (NativeCareerStore.Player player : seed.players) if (player.owner.startsWith(CODE + ":")) career.players.add(player);
    }

    static int openingCash(ClubEconomy.Profile ground) {
        return 1_300_000 + 160 * ground.attendance;
    }
}
