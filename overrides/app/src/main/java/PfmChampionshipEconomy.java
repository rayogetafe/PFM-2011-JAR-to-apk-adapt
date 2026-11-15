import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import pfm.android.AndroidRuntime;
import pfm.android.ClubEconomy;

/** Applies second-division accounting to legacy matchdays. */
public final class PfmChampionshipEconomy {
    private static final Field CLUB_NUMBER = field(dw.class, "n", byte.class);
    private static final Field CAPACITY = field(dw.class, "b", int.class);
    private static final Field TICKET = field(dw.class, "b", short.class);
    private static final Field CASH = field(dw.class, "g", int.class);
    private static final Field HOME = field(bb.class, "a", dw.class);
    private static final Field USER = field(cp.class, "a", dw.class);
    private static final Field CLUBS = field(cp.class, "a", dw[].class);
    private static final Field COMMERCIAL = field(cp.class, "e", int.class);
    private static final Field WAGE_REPORT = field(cp.class, "g", int.class);
    private static final Field UPKEEP_REPORT = field(cp.class, "m", int.class);
    private static final Field ROUND = field(cp.class, "a", byte.class);
    private static final Field CALENDAR = field(cp.class, "a", int[][][].class);
    private static final Field ROSTER = field(dw.class, "a", short[].class);
    private static final Field ROSTER_SIZE = field(dw.class, "e", byte.class);
    private static final Field PLAYERS = field(cp.class, "a", ci[].class);
    private static final Field SALARY = field(ci.class, "a", int.class);
    private static final Method STOCK_DEMAND = method("b", int.class, int.class, int.class);
    private static final Method STOCK_SETTLEMENT = method("a", void.class, int.class);

    private PfmChampionshipEconomy() {}

    public static int demand(int fixture, int influence) {
        try {
            int stock = (Integer) STOCK_DEMAND.invoke(null, fixture, influence);
            String league = PfmSeasonBridge.leagueCode();
            if (!"en2".equals(league) && !continental(league)) return stock;
            dw home = (dw) HOME.get(null);
            ClubEconomy.Profile profile = profile(home);
            if (profile == null) return stock;
            int seats = CAPACITY.getInt(home);
            int baseline = profile.attendanceAt(seats);
            if (continental(league)) {
                baseline = baseline * ClubEconomy.secondTierAttendance(league, clubId(home), profile) / profile.attendance;
            }
            int variation = Math.max(-baseline / 10, Math.min(baseline / 10, (stock - seats / 3) / 5));
            // The legacy caller adds its own match-to-match fluctuation and clamps to capacity.
            return Math.max(1, (baseline + variation) * 20 / 21);
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Could not calculate match attendance", error);
        }
    }

    public static int aiDemand(int fixture, int influence) {
        try {
            String league = PfmSeasonBridge.leagueCode();
            if (continental(league)) {
                TICKET.setShort(HOME.get(null), "fr2".equals(league) ? ClubEconomy.LIGUE2_TICKET : ClubEconomy.BUNDESLIGA2_TICKET);
            } else if ("en2".equals(league) && profile((dw) HOME.get(null)) != null) {
                TICKET.setShort(HOME.get(null), ClubEconomy.CHAMPIONSHIP_TICKET);
            }
            return (Integer) STOCK_DEMAND.invoke(null, fixture, influence);
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Could not update the home ticket price", error);
        }
    }

    public static void settle(int result) {
        try {
            STOCK_SETTLEMENT.invoke(null, result);
            String league = PfmSeasonBridge.leagueCode();
            if (continental(league)) {
                settleContinentalRound(league);
                return;
            }
            if (!"en2".equals(league)) return;
            int distribution = ClubEconomy.distribution("en2");
            for (dw club : (dw[]) CLUBS.get(null)) {
                if (profile(club) == null) continue;
                CASH.setInt(club, CASH.getInt(club) + distribution);
                if (club == USER.get(null)) COMMERCIAL.setInt(null, COMMERCIAL.getInt(null) + distribution);
            }
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Could not settle the league round", error);
        }
    }

    private static boolean continental(String league) {
        return "de2".equals(league) || "fr2".equals(league);
    }

    private static void settleContinentalRound(String league) throws ReflectiveOperationException, IOException {
        dw[] clubs = (dw[]) CLUBS.get(null);
        ci[] players = (ci[]) PLAYERS.get(null);
        int[][][] calendar = (int[][][]) CALENDAR.get(null);
        boolean[] atHome = new boolean[clubs.length];
        for (int[] fixture : calendar[ROUND.getByte(null) & 255]) atHome[fixture[0]] = true;
        dw user = (dw) USER.get(null);
        for (int i = 0; i < clubs.length; i++) {
            dw club = clubs[i];
            ClubEconomy.Profile ground = profile(club);
            if (ground == null) throw new IOException("Missing club finance: " + clubId(club));
            int distribution = ClubEconomy.secondTierDistribution(league,
                    ClubEconomy.secondTierAttendance(league, clubId(club), ground));
            long annualWages = 0;
            int stockWages = 0;
            short[] roster = (short[]) ROSTER.get(club);
            for (int j = 0; j < (ROSTER_SIZE.getByte(club) & 255); j++) {
                int salary = SALARY.getInt(players[roster[j] & 65535]);
                annualWages += salary;
                stockWages += salary / 52;
            }
            int wages = ClubEconomy.secondTierPayroll(league, annualWages);
            int upkeepReduction = atHome[i] ? CAPACITY.getInt(club) : 0;
            CASH.setInt(club, CASH.getInt(club) + distribution + upkeepReduction - (wages - stockWages));
            if (club == user) {
                COMMERCIAL.setInt(null, COMMERCIAL.getInt(null) + distribution);
                WAGE_REPORT.setInt(null, wages);
                UPKEEP_REPORT.setInt(null, UPKEEP_REPORT.getInt(null) - upkeepReduction);
            }
        }
    }

    private static ClubEconomy.Profile profile(dw team) throws IllegalAccessException, IOException {
        return ClubEconomy.profile(AndroidRuntime.context(), clubId(team));
    }

    private static String clubId(dw team) throws IllegalAccessException {
        return PfmEnglishSeasonBridge.clubId(CLUB_NUMBER.getByte(team) & 255, "");
    }

    private static Field field(Class<?> owner, String name, Class<?> type) {
        for (Field field : owner.getDeclaredFields()) {
            if (!field.getName().equals(name) || field.getType() != type) continue;
            field.setAccessible(true);
            return field;
        }
        throw new IllegalStateException(owner.getName() + "." + name);
    }

    private static Method method(String name, Class<?> type, Class<?>... parameters) {
        for (Method method : bb.class.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != type ||
                    !java.util.Arrays.equals(method.getParameterTypes(), parameters)) continue;
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException(bb.class.getName() + "." + name);
    }
}
