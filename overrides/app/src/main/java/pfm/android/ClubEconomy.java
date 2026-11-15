package pfm.android;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Historical club inputs and the matchday accounting scale shared by both engines. */
public final class ClubEconomy {
    public static final short CHAMPIONSHIP_TICKET = 40;
    public static final short BUNDESLIGA2_TICKET = 37;
    private static final int CHAMPIONSHIP_DISTRIBUTION = 25_000;
    private static Map<String, Profile> profiles;

    private ClubEconomy() {}

    public static final class Profile {
        public final String stadium;
        public final int capacity, attendance;

        Profile(String stadium, int capacity, int attendance) {
            this.stadium = stadium;
            this.capacity = capacity;
            this.attendance = attendance;
        }

        public int openingCash() {
            return 1_000_000 + attendance * 200;
        }

        public int attendanceAt(int seats) {
            return (int) Math.min(seats, Math.round(attendance * Math.sqrt((double) seats / capacity)));
        }

        public int matchCredit(boolean home, long annualPayroll) {
            int matchday = home ? attendance * CHAMPIONSHIP_TICKET * 30 / 100 - 3 * capacity : 0;
            return CHAMPIONSHIP_DISTRIBUTION + matchday - (int) (annualPayroll / 52);
        }
    }

    public static int distribution(String league) {
        return "en2".equals(league) ? CHAMPIONSHIP_DISTRIBUTION : 0;
    }

    public static int germanSecondTierAttendance(String club, Profile ground) {
        return club.startsWith("de:") ? ground.attendance * 3 / 4 : ground.attendance;
    }

    public static int germanSecondTierDistribution(int attendance) {
        return 65_000 + attendance;
    }

    public static int germanSecondTierPayroll(long annualPayroll) {
        return (int) (annualPayroll / 34);
    }

    public static synchronized Profile profile(Context context, String club) throws IOException {
        if (profiles == null) {
            Map<String, Profile> loaded = new HashMap<>();
            try (BufferedReader input = new BufferedReader(new InputStreamReader(
                    context.getAssets().open("club_economy_2010.tsv"), StandardCharsets.UTF_8))) {
                if (!"club_id\tstadium\tcapacity\taverage_attendance".equals(input.readLine())) {
                    throw new IOException("Invalid club economy header");
                }
                for (String line; (line = input.readLine()) != null;) {
                    String[] row = line.split("\t", -1);
                    if (row.length != 4 || row[0].isEmpty() || row[1].isEmpty()) {
                        throw new IOException("Invalid club economy row: " + line);
                    }
                    int seats = Integer.parseInt(row[2]), fans = Integer.parseInt(row[3]);
                    if (fans <= 0 || fans > seats || loaded.put(row[0], new Profile(row[1], seats, fans)) != null) {
                        throw new IOException("Invalid club economy parameters: " + row[0]);
                    }
                }
            } catch (NumberFormatException error) {
                throw new IOException("Invalid club economy number", error);
            }
            profiles = loaded;
        }
        return profiles.get(club);
    }
}
