package pfm.android;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Initial native squads for the background Championship. */
final class NativeChampionship {
    static final String CODE = "en2";
    static final int CLUBS = 24;
    static final int PLAYERS = 506;
    static final int ROUNDS = 46;

    static boolean active(NativeCareerStore.Career career, int season) {
        for (NativeCareerStore.Club club : career.clubs) {
            if (CODE.equals(club.league) && club.activeFrom <= season) return true;
        }
        return false;
    }

    static void load(Context context, Map<String, ArrayList<NativeWorldCenter.T>> world) throws IOException {
        ArrayList<NativeWorldCenter.T> teams = new ArrayList<>();
        try (BufferedReader clubs = reader(context, "second_divisions_2010.tsv");
             BufferedReader players = reader(context, "championship_squads_2010.tsv")) {
            clubs.readLine();
            for (String line; (line = clubs.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                if (!CODE.equals(row[1])) continue;
                NativeWorldCenter.T team = new NativeWorldCenter.T();
                team.id = index(row[0]);
                team.name = row[2];
                team.style = 55;
                team.form = 2;
                if (team.id != teams.size()) throw new IOException("Championship club order");
                teams.add(team);
            }
            int count = 0;
            players.readLine();
            for (String line; (line = players.readLine()) != null;) {
                String[] row = line.split("\t", -1);
                // Existing IDs keep their one owner and contract in the active career.
                if (!row[1].startsWith("espn:")) continue;
                NativeWorldCenter.P player = new NativeWorldCenter.P();
                player.uid = row[1];
                player.id = Integer.parseInt(row[12]);
                player.name = row[2];
                player.pos = Arrays.asList("GK", "DF", "MF", "FW").indexOf(row[3]);
                player.age = ageAtStart(row[4]);
                player.nat = row[5];
                player.ovr = Integer.parseInt(row[6]);
                player.qua = player.ovr;
                player.spe = Math.min(99, player.ovr + (player.pos == 0 ? -4 : 2));
                player.res = Math.min(99, player.ovr + Math.min(5, Integer.parseInt(row[8]) / 8));
                player.mor = 55;
                int strength = player.ovr - 45;
                player.value = (75000 + strength * strength * 2500) / (player.age >= 34 ? 2 : 1);
                player.wage = 35000 + strength * 8000;
                player.provenance = "CHAMPIONSHIP_AUG2010_ESTIMATED";
                teams.get(index(row[0])).ps.add(player);
                count++;
            }
            if (teams.size() != CLUBS || count != PLAYERS) throw new IOException("Incomplete Championship seed");
        }
        world.put(CODE, teams);
    }

    static int ageAtStart(String birthDate) {
        return 2010 - Integer.parseInt(birthDate.substring(0, 4)) -
                (birthDate.substring(5).compareTo("08-07") > 0 ? 1 : 0);
    }

    private static int index(String club) {
        return Integer.parseInt(club.substring(CODE.length() + 1));
    }

    private static BufferedReader reader(Context context, String asset) throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(asset), "UTF-8"));
    }
}
