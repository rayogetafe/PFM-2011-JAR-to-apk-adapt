import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import pfm.android.AndroidRuntime;
import pfm.android.ClubEconomy;

/** Uses the same Championship operating scale for legacy matchdays and background rounds. */
public final class PfmChampionshipEconomy {
    private static final Field CLUB_NUMBER = field(dw.class, "n", byte.class);
    private static final Field CAPACITY = field(dw.class, "b", int.class);
    private static final Field TICKET = field(dw.class, "b", short.class);
    private static final Field CASH = field(dw.class, "g", int.class);
    private static final Field HOME = field(bb.class, "a", dw.class);
    private static final Field USER = field(cp.class, "a", dw.class);
    private static final Field CLUBS = field(cp.class, "a", dw[].class);
    private static final Field COMMERCIAL = field(cp.class, "e", int.class);
    private static final Method STOCK_DEMAND = method("b", int.class, int.class, int.class);
    private static final Method STOCK_SETTLEMENT = method("a", void.class, int.class);

    private PfmChampionshipEconomy() {}

    public static int demand(int fixture, int influence) {
        try {
            int stock = (Integer) STOCK_DEMAND.invoke(null, fixture, influence);
            if (!isChampionship()) return stock;
            dw home = (dw) HOME.get(null);
            ClubEconomy.Profile profile = profile(home);
            if (profile == null) return stock;
            int seats = CAPACITY.getInt(home);
            int baseline = profile.attendanceAt(seats);
            int variation = Math.max(-baseline / 10, Math.min(baseline / 10, (stock - seats / 3) / 5));
            // The legacy caller adds its own match-to-match fluctuation and clamps to capacity.
            return Math.max(1, (baseline + variation) * 20 / 21);
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Could not calculate match attendance", error);
        }
    }

    public static int aiDemand(int fixture, int influence) {
        try {
            if (isChampionship() && profile((dw) HOME.get(null)) != null) {
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
            if (!isChampionship()) return;
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

    private static ClubEconomy.Profile profile(dw team) throws IllegalAccessException, IOException {
        String club = PfmEnglishSeasonBridge.clubId(CLUB_NUMBER.getByte(team) & 255, "");
        return ClubEconomy.profile(AndroidRuntime.context(), club);
    }

    private static boolean isChampionship() {
        return PfmSeasonBridge.totalRounds() == 46 && "en2".equals(PfmSeasonBridge.leagueCode());
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
