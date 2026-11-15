package pfm.android;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Starting squads for the simulated second German division. */
final class NativeBundesliga2 {
    static final String CODE = "de2";
    static final String NAME = "2. Bundesliga";
    static final int CLUBS = 18;
    static final int ROUNDS = 34;
    static final int PLAYERS = 499;
    // These players already exist in the base career. Keep their current owners and contracts.
    private static final Set<String> BASE_GAME_PLAYERS = new HashSet<>(Arrays.asList(
            "dfb:22956", // Eke Uzoma, de:13:274
            "dfb:3121",  // Lukas Sinkiewicz, de:3:74
            "dfb:28394", // Anthar Yahia, fr:10:202
            "dfb:425",   // Milorad Peković, de:8:167
            "dfb:10450"  // Levan Kobiashvili, de:1:37
    ));

    private NativeBundesliga2() {}

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world) throws IOException {
        ArrayList<NativeWorldCenter.T> teams = new ArrayList<>();
        List<ArrayList<String[]>> squads = new ArrayList<>();
        try (BufferedReader clubs = reader(context, "second_divisions_2010.tsv");
             BufferedReader players = reader(context, "bundesliga2_squads_2010.tsv")) {
            clubs.readLine();
            for (String line; (line = clubs.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                if (!CODE.equals(row[1])) continue;
                NativeWorldCenter.T team = new NativeWorldCenter.T();
                team.id = index(row[0]);
                team.name = row[2];
                team.style = 55;
                team.form = 2;
                if (team.id != teams.size()) throw new IOException("Invalid German second-division club order");
                teams.add(team);
                squads.add(new ArrayList<>());
            }
            players.readLine();
            for (String line; (line = players.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                if (row[4].compareTo("1994-08-20") > 0 || BASE_GAME_PLAYERS.contains(row[1])) continue;
                squads.get(index(row[0])).add(row);
            }
        }

        // Fill the 30 legacy-compatible slots with first-team players before unused reserves.
        int count = 0;
        int[] rankingColumns = {9, 8, 6};
        for (NativeWorldCenter.T team : teams) {
            ArrayList<String[]> candidates = squads.get(team.id);
            Collections.sort(candidates, (a, b) -> {
                for (int column : rankingColumns) {
                    int order = Integer.compare(Integer.parseInt(b[column]), Integer.parseInt(a[column]));
                    if (order != 0) return order;
                }
                return a[1].compareTo(b[1]);
            });
            int keepers = 0;
            for (int i = 0; i < Math.min(30, candidates.size()); i++) {
                String[] row = candidates.get(i);
                NativeWorldCenter.P player = new NativeWorldCenter.P();
                player.uid = row[1];
                player.id = row[1].hashCode();
                player.name = row[2];
                player.pos = Arrays.asList("GK", "DF", "MF", "FW").indexOf(row[3]);
                if (player.pos == 0) keepers++;
                player.age = 2010 - Integer.parseInt(row[4].substring(0, 4)) -
                        (row[4].substring(5).compareTo("08-20") > 0 ? 1 : 0);
                player.nat = "Unknown";
                player.ovr = player.qua = Integer.parseInt(row[6]);
                player.spe = player.ovr + (player.pos == 0 ? -4 : 2);
                player.res = player.ovr + Math.min(3, Integer.parseInt(row[9]));
                player.mor = 55;
                int strength = player.ovr - 45;
                player.value = (75_000 + strength * strength * 2_100) / (player.age >= 34 ? 2 : 1);
                player.wage = 30_000 + strength * 7_000;
                player.provenance = "BUNDESLIGA2_AUG2010_ESTIMATED";
                team.ps.add(player);
                count++;
            }
            if (team.ps.size() < 18 || keepers < 2) {
                throw new IOException("Incomplete German second-division squad: " + team.name);
            }
        }
        if (teams.size() != CLUBS || count != PLAYERS) throw new IOException("Incomplete German second-division seed");
        world.put(CODE, teams);
    }

    static boolean active(NativeCareerStore.Career career, int season) {
        for (NativeCareerStore.Club club : career.clubs) {
            if (CODE.equals(club.league) && club.activeFrom <= season) return true;
        }
        return false;
    }

    static boolean present(NativeCareerStore.Career career) {
        return active(career, Integer.MAX_VALUE);
    }

    static void install(NativeCareerStore.Career career, NativeCareerStore.Career seed) {
        if (present(career)) return;
        for (NativeCareerStore.Club club : seed.clubs) {
            if (CODE.equals(club.league)) career.clubs.add(club);
        }
        for (NativeCareerStore.Player player : seed.players) {
            if (player.owner.startsWith(CODE + ":")) career.players.add(player);
        }
    }

    static int openingCash(ClubEconomy.Profile ground) {
        return 2_000_000 + 110 * ground.attendance;
    }

    static int matchCredit(ClubEconomy.Profile ground, boolean home, long annualPayroll) {
        int matchday = home ? 11 * ground.attendance - 2 * ground.capacity : 0;
        int mediaAndSponsorship = 65_000 + ground.attendance;
        return mediaAndSponsorship + matchday - (int) (annualPayroll / ROUNDS);
    }

    private static int index(String club) {
        return Integer.parseInt(club.substring(CODE.length() + 1));
    }

    private static BufferedReader reader(Context context, String asset) throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(asset), StandardCharsets.UTF_8));
    }
}
