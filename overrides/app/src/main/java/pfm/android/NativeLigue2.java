package pfm.android;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class NativeLigue2 {
    static final String CODE = "fr2";
    static final int CLUBS = 20, PLAYERS = 405, ROUNDS = 38;

    private NativeLigue2() {}

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world) throws IOException {
        load(context, world, CODE, CLUBS, PLAYERS, "ligue2_squads_2010.tsv", "08-06", "LIGUE2_AUG2010_ESTIMATED");
    }

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world, String division,
                     int clubCount, int playerCount, String asset, String kickoff, String provenance) throws IOException {
        ArrayList<NativeWorldCenter.T> teams = new ArrayList<>();
        try (BufferedReader clubs = reader(context, "second_divisions_2010.tsv");
             BufferedReader players = reader(context, asset)) {
            clubs.readLine();
            for (String line; (line = clubs.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                if (!division.equals(row[1])) continue;
                NativeWorldCenter.T team = new NativeWorldCenter.T();
                team.id = Integer.parseInt(row[0].substring(4));
                if (team.id != teams.size()) throw new IOException("Invalid club order: " + division);
                team.name = row[2];
                team.style = 55;
                team.form = 2;
                teams.add(team);
            }
            if (teams.size() != clubCount) throw new IOException("Incomplete club list: " + division);
            int count = 0;
            Set<String> ids = new HashSet<>();
            players.readLine();
            for (String line; (line = players.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                NativeWorldCenter.P player = new NativeWorldCenter.P();
                player.uid = row[1];
                if (!ids.add(player.uid)) throw new IOException("Duplicate player: " + player.uid);
                player.id = player.uid.hashCode();
                player.name = row[2];
                player.pos = Arrays.asList("GK", "DF", "MF", "FW").indexOf(row[3]);
                player.age = 2010 - Integer.parseInt(row[4].substring(0, 4))
                        - (row[4].substring(5).compareTo(kickoff) > 0 ? 1 : 0);
                player.nat = row[5];
                player.ovr = player.qua = Integer.parseInt(row[6]);
                player.spe = player.ovr + (player.pos == 0 ? -4 : 2);
                player.res = player.ovr + Math.min(3, Integer.parseInt(row[9]));
                player.mor = 55;
                int strength = player.ovr - 45;
                player.value = (65_000 + strength * strength * 1_900) / (player.age >= 34 ? 2 : 1);
                player.wage = 28_000 + strength * 6_500;
                player.provenance = provenance;
                teams.get(Integer.parseInt(row[0].substring(4))).ps.add(player);
                count++;
            }
            if (count != playerCount) throw new IOException("Incomplete squads: " + division);
        }
        world.put(division, teams);
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
        return 1_200_000 + 155 * ground.attendance;
    }

    private static BufferedReader reader(Context context, String asset) throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(asset), StandardCharsets.UTF_8));
    }
}
