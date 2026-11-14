import android.content.Context;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import pfm.android.AndroidRuntime;
import pfm.android.EnglishLegacyIds;
import pfm.android.NativeEnglishCareer;
import pfm.android.compat.lcdui.Image;

/** Projects the next active English division into the existing, saved matchday state. */
public final class PfmEnglishSeasonBridge {
    private static volatile NativeEnglishCareer.Opening startingChampionship;
    private PfmEnglishSeasonBridge() {}

    public static void chooseStartingClub(ea screen, byte destination) {
        startingChampionship = null;
        if (pfm2.getMode() != 1 || !isEngland()) {
            screen.a(destination);
            return;
        }
        Activity activity = AndroidRuntime.activity();
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity).setTitle("English division")
                .setItems(new String[]{"Premier League", "Championship"}, (dialog, choice) -> {
                    if (choice == 0) screen.a(destination);
                    else chooseChampionshipClub(activity, screen, destination);
                }).setOnCancelListener(dialog -> screen.a((byte)38)).show());
    }

    private static boolean isEngland() {
        try { return get(ea.class, "a", int.class).getInt(null) == 0; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private static void chooseChampionshipClub(Activity activity, ea screen, byte destination) {
        try {
            NativeEnglishCareer.Opening opening = NativeEnglishCareer.championshipStart(activity, 0);
            String[] clubs = new String[opening.clubs.size()];
            for (int i = 0; i < clubs.length; i++) clubs[i] = opening.clubs.get(i).name;
            new AlertDialog.Builder(activity).setTitle("Choose a Championship club")
                    .setItems(clubs, (dialog, choice) -> {
                        opening.userId = opening.clubs.get(choice).id;
                        startingChampionship = opening;
                        dc.a = 1;
                        putStartingClubIndex(choice);
                        screen.a(destination);
                    }).setOnCancelListener(dialog -> screen.a((byte)38)).show();
        } catch (IOException error) {
            android.util.Log.e("PFM-English", "Championship squads could not be opened", error);
            new AlertDialog.Builder(activity).setMessage("Championship squads unavailable.")
                    .setPositiveButton("OK", (dialog, which) -> screen.a(destination)).show();
        }
    }

    private static void putStartingClubIndex(int choice) {
        try { get(ch.class, "a", int.class).setInt(null, choice); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    public static dw initializeStartingClub(byte index) {
        NativeEnglishCareer.Opening opening = startingChampionship;
        startingChampionship = null;
        try {
            Context context = AndroidRuntime.context();
            if (opening == null) {
                if (pfm2.getMode() == 1 && isEngland() && EnglishLegacyIds.installed(context, pfm2.getSeason())) {
                    resetStartingIdentity(context);
                }
                cp.a(index);
                return pfmPatch39.initUserTeam();
            }
            pfmLifecycle82.onUserTeamInit();
            resetStartingIdentity(context);
            SharedPreferences ids = EnglishLegacyIds.season(context, pfm2.getSeason());
            install(opening, ids, ids);
            pfmTeams.clearSeedPoints();
            refreshStatsBaseline();
            return pfmPatch38.initUserTeam();
        } catch (Exception error) {
            throw new IllegalStateException("Could not initialize the starting club", error);
        }
    }

    private static void resetStartingIdentity(Context context) throws IOException {
        String key = "slot_" + pfm2.getSlot();
        SharedPreferences world = context.getSharedPreferences("pfm_world_careers", 0);
        int previous = world.getInt(key + "_nonce", 0);
        Random random = new Random();
        int nonce;
        do { nonce = random.nextInt(); } while (nonce == 0 || nonce == previous);
        if (!world.edit().putInt(key + "_nonce", nonce).putInt(key + "_season", pfm2.getSeason())
                .putInt(key + "_played", 0).commit()) throw new IOException("Career identity could not be saved");
    }

    public static Image boardBadge(int legacyIndex) {
        if (PfmSeasonBridge.totalRounds() == 46) {
            try {
                int user = get(cp.class, "b", byte.class).getByte(null) & 255;
                String path = matchBadgePath(clubId(user, ""));
                if (path != null) return Image.fitted(path, 47, 50);
                legacyIndex = 27 + user % 20;
            } catch (Exception error) {
                legacyIndex = 27;
            }
        }
        return (Image) invoke(aq.class, "a", Image.class, legacyIndex);
    }

    public static String startingClubLabel(int index) {
        if (PfmSeasonBridge.totalRounds() == 46) return PfmSeasonBridge.teamName();
        return m.b(index);
    }

    public static void startFirstRound() {
        boolean installed = false;
        try {
            if (pfm2.getMode() == 1 && get(ea.class, "a", int.class).getInt(null) == 0) {
                Context context = AndroidRuntime.context();
                int year = pfm2.getSeason();
                NativeEnglishCareer.Opening opening = NativeEnglishCareer.opening(context, year - 1);
                if (opening != null) {
                    install(opening, EnglishLegacyIds.players(context, year - 1),
                            EnglishLegacyIds.season(context, year));
                    pfmPlayerStats.startNewSeason();
                    pfmTeams.clearSeedPoints();
                    refreshStatsBaseline();
                    installed = true;
                }
            }
        } catch (Exception error) {
            android.util.Log.e("PFM-English", "English season projection failed", error);
        }
        pfmRound.startFirstRound();
        if (installed) pfmLineup83.snapshotAfterLoad();
    }

    public static String clubId(int index, String fallback) {
        return clubId(index, fallback, pfm2.getSeason());
    }

    private static String clubId(int index, String fallback, int year) {
        try {
            Context context = AndroidRuntime.context();
            if (get(ea.class, "a", int.class).getInt(null) == 0 && EnglishLegacyIds.installed(context, year)) {
                return EnglishLegacyIds.season(context, year).getString("club." + index, fallback);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    /** pfm2 increments its year immediately before resetting the old table. */
    public static void beforeSeasonReset() {
        try {
            if (pfm2.getMode() == 1 && get(ea.class, "a", int.class).getInt(null) == 0 &&
                    PfmSeasonBridge.totalRounds() == 38 && cp.pfmPlayedForTable() == 38) {
                int year = pfm2.getSeason() - 1;
                byte[] ranks = (byte[]) get(cp.class, "c", byte[].class).get(null);
                List<String> order = new ArrayList<>();
                for (byte rank : ranks) {
                    int index = rank & 255;
                    order.add(clubId(index, "en:" + index, year));
                }
                NativeEnglishCareer.recordFinalPremier(AndroidRuntime.context(), year, order);
            }
        } catch (Exception error) {
            android.util.Log.e("PFM-English", "Could not preserve the final Premier League table", error);
        }
        pfmSeasonStatsV22.startNewSeason();
    }

    /** Repair v105's seed points without replaying the matches or changing the division. */
    public static void afterLeagueLoad() {
        invoke(cp.class, "b", void.class);
        try {
            PfmDomesticReplay.afterCoreLoad();
        } catch (RuntimeException | LinkageError error) {
            android.util.Log.e("PFM-Replay", "Could not initialize the replay after loading", error);
        }
        try {
            if (pfm2.getMode() == 1 && get(ea.class, "a", int.class).getInt(null) == 0 &&
                    EnglishLegacyIds.installed(AndroidRuntime.context(), pfm2.getSeason()) && repairPoints()) {
                refreshStatsBaseline();
            }
        } catch (Exception error) {
            android.util.Log.e("PFM-English", "Could not repair loaded league points", error);
        }
    }

    static boolean repairPoints() throws Exception {
        dw[] teams = (dw[]) get(cp.class, "a", dw[].class).get(null);
        int played = cp.pfmPlayedForTable();
        Field points = get(dw.class, "h", byte.class), wins = get(dw.class, "i", byte.class);
        Field losses = get(dw.class, "j", byte.class);
        for (dw team : teams) if ((wins.getByte(team) & 255) + (losses.getByte(team) & 255) > played) return false;
        boolean changed = false;
        for (dw team : teams) {
            int won = wins.getByte(team) & 255, lost = losses.getByte(team) & 255;
            int actual = 3 * won + played - won - lost;
            if ((points.getByte(team) & 255) == actual) continue;
            points.setByte(team, (byte) actual);
            changed = true;
        }
        if (changed) invoke(cp.class, "a", void.class);
        return changed;
    }

    private static void refreshStatsBaseline() throws Exception {
        pfmPlayerStats.ensureLoaded();
        for (String name : new String[]{"snapshotTeamsOnly", "save"}) {
            Method method = pfmPlayerStats.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(null);
        }
    }

    public static Image matchBadge(dw team, int offset) {
        int index = 0;
        try {
            index = get(dw.class, "n", byte.class).getByte(team) & 255;
            String path = matchBadgePath(clubId(index, ""));
            if (path != null) return Image.fitted(path, 47, 50);
        } catch (Exception ignored) {}
        return (Image) invoke(aq.class, "a", Image.class, 9 + index + offset);
    }

    static String matchBadgePath(String club) {
        if (club.startsWith("en2:")) return "second_division_badges/en2_" + club.substring(4) + ".png";
        if (club.startsWith("en:")) return "badges/en" + (Integer.parseInt(club.substring(3)) + 1) + ".png";
        return null;
    }

    static void install(NativeEnglishCareer.Opening opening, SharedPreferences previous,
                        SharedPreferences next) throws Exception {
        Field teamsField = get(cp.class, "a", dw[].class), playersField = get(cp.class, "a", ci[].class);
        Field calendarField = get(cp.class, "a", int[][][].class), tableField = get(cp.class, "c", byte[].class);
        Field currentClub = get(cp.class, "a", dw.class), userIndex = get(cp.class, "b", byte.class);
        Field count = get(cp.class, "a", int.class), rounds = get(cp.class, "b", int.class);
        Field matches = get(cp.class, "c", int.class), roundStats = get(bb.class, "a", int[].class);
        Field marketList = get(cp.class, "c", short[].class);
        Field training = get(cp.class, "a", eg[].class);
        dw[] oldTeams = (dw[]) teamsField.get(null);
        List<ci> players = new ArrayList<>(Arrays.asList((ci[]) playersField.get(null)));
        Map<String, dw> oldClubs = new HashMap<>();
        for (dw team : oldTeams) oldClubs.put(pfmAccess50.name(team), team);

        int size = opening.clubs.size(), user = -1;
        if (size != 20 && size != 24) throw new IOException("Invalid English matchday size");
        dw[] teams = new dw[size];
        byte[] order = new byte[size];
        Set<Integer> usedSlots = new HashSet<>();
        SharedPreferences.Editor saved = next.edit().clear();
        for (Map.Entry<String, ?> entry : previous.getAll().entrySet()) {
            if (entry.getValue() instanceof Integer) saved.putInt(entry.getKey(), (Integer) entry.getValue());
        }
        for (int i = 0; i < size; i++) {
            NativeEnglishCareer.Club club = opening.clubs.get(i);
            dw template = oldClubs.get(club.name);
            boolean incoming = template == null;
            if (incoming) template = oldTeams[(club.id.hashCode() & Integer.MAX_VALUE) % oldTeams.length];
            dw team = copy(template);
            teams[i] = team;
            order[i] = (byte) i;
            if (club.id.equals(opening.userId)) user = i;
            if (incoming) {
                put(team, "a", byte[].class, latin(club.name));
                put(team, "b", byte[].class, latin(club.name));
            }
            put(team, "n", byte.class, (byte) i);
            for (String field : new String[]{"h", "i", "j", "k", "l", "f"}) put(team, field, byte.class, (byte) 0);
            put(team, "m", byte.class, (byte) i);
            put(team, "g", int.class, club.budget);
            put(team, "c", byte[].class, new byte[2 * (size - 1)]);
            short[] roster = new short[30];
            if (club.players.size() < 11 || club.players.size() > roster.length) throw new IOException("Invalid squad " + club.id);
            Collections.sort(club.players, (a, b) -> Integer.compare(a.position, b.position));
            int shirt = 0;
            for (NativeEnglishCareer.Player person : club.players) {
                int id = previous.getInt(person.id, person.originalSlot);
                if (id != person.originalSlot && id >= 0 && id < players.size()) {
                    byte[] existing = (byte[]) get(ci.class, "a", byte[].class).get(players.get(id));
                    if (!Arrays.equals(existing, latin(person.name))) id = person.originalSlot;
                }
                if (id < 0 || id >= players.size() || usedSlots.contains(id)) {
                    id = players.size();
                    if (id >= PfmEnglishLedger.SLOTS) throw new IOException("Legacy player index limit");
                    players.add(null);
                }
                usedSlots.add(id);
                roster[shirt++] = (short) id;
                players.set(id, player(person, i, shirt));
                saved.putInt(person.id, id);
            }
            put(team, "a", short[].class, roster);
            put(team, "e", byte.class, (byte) shirt);
            saved.putString("club." + i, club.id);
        }
        if (user < 0) throw new IOException("English user club missing");
        int[][][] calendar = NativeEnglishCareer.calendar(size);
        ci[] roster = players.toArray(new ci[0]);
        short[] offers = new short[(size - 1) * 10];
        int[] scratch = new int[size / 2];
        if (!saved.putString("installed", "yes").commit()) throw new IOException("Legacy slot map could not be saved");
        teamsField.set(null, teams);
        playersField.set(null, roster);
        calendarField.set(null, calendar);
        tableField.set(null, order);
        currentClub.set(null, teams[user]);
        userIndex.setByte(null, (byte) user);
        count.setInt(null, size);
        rounds.setInt(null, calendar.length);
        matches.setInt(null, size / 2);
        roundStats.set(null, scratch);
        marketList.set(null, offers);
        training.set(null, new eg[30]);
        cp.a((byte) user);
        for (int i = 0; i < roster.length; i++) if (!usedSlots.contains(i)) put(roster[i], "j", byte.class, (byte) 0);
    }

    private static dw copy(dw template) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        template.a(new DataOutputStream(buffer));
        return new dw(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));
    }

    static void syncTraining(dw team) throws Exception {
        Field field = get(cp.class, "a", eg[].class);
        eg[] previous = (eg[]) field.get(null), next = new eg[30];
        Map<Short, eg> training = new HashMap<>();
        Field playerId = get(eg.class, "a", short.class);
        for (eg record : previous) if (record != null) training.put(playerId.getShort(record), record);
        short[] ids = (short[]) get(dw.class, "a", short[].class).get(team);
        int count = get(dw.class, "e", byte.class).getByte(team);
        for (int i = 0; i < count; i++) {
            eg record = training.get(ids[i]);
            next[i] = record == null ? new eg(ids[i]) : record;
        }
        field.set(null, next);
    }

    private static ci player(NativeEnglishCareer.Player source, int team, int shirt) throws Exception {
        ci player = new ci();
        put(player, "a", byte[].class, latin(source.name));
        int[] values = {source.age, source.position, source.speed, source.resistance, source.quality, shirt, 0, 55, 0, team};
        for (int i = 0; i < values.length; i++) put(player, String.valueOf((char) ('a' + i)), byte.class, (byte) values[i]);
        put(player, "a", int.class, source.wage);
        put(player, "c", int.class, source.value);
        return player;
    }

    private static byte[] latin(String text) { return text.getBytes(StandardCharsets.ISO_8859_1); }

    private static void put(Object target, String name, Class<?> type, Object value) throws Exception {
        get(target.getClass(), name, type).set(target, value);
    }

    private static Field get(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        for (Field field : owner.getDeclaredFields()) {
            if (field.getName().equals(name) && field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    private static Object invoke(Class<?> owner, String name, Class<?> type, Object... args) {
        Class<?>[] parameters = args.length == 0 ? new Class<?>[0] : new Class<?>[]{int.class};
        try {
            for (Method method : owner.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getReturnType() != type ||
                        !Arrays.equals(method.getParameterTypes(), parameters)) continue;
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(owner.getName() + "." + name, error);
        }
        throw new IllegalStateException(owner.getName() + "." + name);
    }
}
