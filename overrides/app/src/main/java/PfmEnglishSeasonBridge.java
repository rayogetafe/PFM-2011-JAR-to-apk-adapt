import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import pfm.android.AndroidRuntime;
import pfm.android.EnglishLegacyIds;
import pfm.android.NativeEnglishCareer;

/** Projects the next active English division into the existing, saved matchday state. */
public final class PfmEnglishSeasonBridge {
    private PfmEnglishSeasonBridge() {}

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
        try {
            Context context = AndroidRuntime.context();
            int year = pfm2.getSeason();
            if (get(ea.class, "a", int.class).getInt(null) == 0 && EnglishLegacyIds.installed(context, year)) {
                return EnglishLegacyIds.season(context, year).getString("club." + index, fallback);
            }
        } catch (Exception ignored) {}
        return fallback;
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
                    if (id >= Short.MAX_VALUE) throw new IOException("Legacy player index limit");
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

    private static Field get(Class<?> owner, String name, Class<?> type) throws Exception {
        for (Field field : owner.getDeclaredFields()) {
            if (field.getName().equals(name) && field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }
}
