import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import pfm.android.NativeEnglishCareer;
import pfm.android.AndroidRuntime;
import pfm.android.compat.rms.RecordStore;

/** Drives the real legacy league reader and writer, with an isolated preference store. */
public final class EnglishCoreHarness {
    public static void main(String[] args) throws Exception {
        field(du.class, "a", cv.class).set(null, new cv());
        field(du.class, "a", cr.class).set(null, new cr(null));
        try (DataInputStream input = new DataInputStream(EnglishCoreHarness.class.getResourceAsStream("/resources/england.bin"))) {
            cp.a(input, true);
        }
        SharedPreferences oldSlots = new SharedPreferences();
        SharedPreferences lowerSlots = new SharedPreferences();
        NativeEnglishCareer.Opening lower = division(24, "en2");
        PfmEnglishSeasonBridge.install(lower, oldSlots, lowerSlots);
        verify(lower, lowerSlots);
        reload();
        verify(lower, lowerSlots);
        verifyLoadedPoints();
        check("badges/en20.png".equals(PfmEnglishSeasonBridge.matchBadgePath("en:19")) &&
                "second_division_badges/en2_12.png".equals(PfmEnglishSeasonBridge.matchBadgePath("en2:12")),
                "badge lookup used a legacy slot");

        SharedPreferences upperSlots = new SharedPreferences();
        NativeEnglishCareer.Opening upper = division(20, "en");
        PfmEnglishSeasonBridge.install(upper, lowerSlots, upperSlots);
        verify(upper, upperSlots);
        reload();
        verify(upper, upperSlots);
        check(lowerSlots.getInt("user-gk", -1) == upperSlots.getInt("user-gk", -2), "lost player identity across divisions");
        checkLedgers();
        checkDomesticCheckpoint();
        System.out.println("PASS: real core saves and reloads both English divisions, 30-player squad, 38/46 rounds, same user club and player slot");
    }

    private static void checkDomesticCheckpoint() throws Exception {
        File directory = java.nio.file.Files.createTempDirectory(new File("artifacts").toPath(), "pfm-replay-test").toFile();
        android.content.Context context = new android.content.Context("", directory);
        field(AndroidRuntime.class, "current", android.content.Context.class).set(null, context);
        field(pfm2.class, "mode", int.class).setInt(null, 1);
        int slot = pfm2.getSlot();
        context.getSharedPreferences("pfm_world_careers", 0).edit().putInt("slot_" + slot + "_nonce", 31).commit();
        String scope = "c31s" + slot + "y" + pfm2.getSeason() + "_";
        SharedPreferences keeper = context.getSharedPreferences("pfm_goalkeepers_v31", 0);
        keeper.edit().putInt(scope + "p4_st", 7).commit();
        RecordStore fatigue = RecordStore.openRecordStore("P2F00" + (slot + 1), true);
        byte[] before = {3, 7, 11}, changed = {8, 9};
        fatigue.setRecord(1, before, 0, before.length);
        fatigue.closeRecordStore();
        byte[] save = {0, 4, 9, 12};
        RecordStore game = RecordStore.openRecordStore("P2S00" + (slot + 1), true);
        int id = game.addRecord(save, 0, save.length);  // v106 save without an attached checkpoint
        check(Arrays.equals(PfmDomesticReplay.read(game, id), save), "older save bytes changed");
        check(PfmDomesticReplay.checkpointStart(game.getRecord(id)) == save.length, "old save was not upgraded");
        fatigue = RecordStore.openRecordStore("P2F00" + (slot + 1), true);
        fatigue.setRecord(1, changed, 0, changed.length);
        fatigue.closeRecordStore();
        keeper.edit().putInt(scope + "p4_st", 8).putInt(scope + "p5_st", 1).commit();
        check(Arrays.equals(PfmDomesticReplay.read(game, id), save), "checkpoint leaked into the core save");
        fatigue = RecordStore.openRecordStore("P2F00" + (slot + 1), true);
        check(Arrays.equals(fatigue.getRecord(1), before) && keeper.getInt(scope + "p4_st", 0) == 7 &&
                keeper.getInt(scope + "p5_st", 0) == 0, "unsaved league effects survived reload");
        fatigue.setRecord(1, changed, 0, changed.length);
        fatigue.closeRecordStore();
        PfmDomesticReplay.set(game, id, save, 0, save.length);
        check(Arrays.equals(PfmDomesticReplay.read(game, id), save), "saved result cannot be read");
        fatigue = RecordStore.openRecordStore("P2F00" + (slot + 1), true);
        check(Arrays.equals(fatigue.getRecord(1), changed), "committed effects were not kept");
        fatigue.closeRecordStore();
        game.closeRecordStore();
        System.out.println("PASS: legacy saves upgrade and restore domestic ledgers and goalkeeper stats with the saved attempt");
    }

    private static void verifyLoadedPoints() throws Exception {
        dw[] clubs = (dw[]) field(cp.class, "a", dw[].class).get(null);
        field(cp.class, "a", byte.class).setByte(null, (byte) 2);
        Field points = field(dw.class, "h", byte.class), wins = field(dw.class, "i", byte.class);
        Field losses = field(dw.class, "j", byte.class);
        for (dw club : clubs) points.setByte(club, (byte) 22);
        losses.setByte(clubs[3], (byte) 1);
        wins.setByte(clubs[5], (byte) 2);
        reload();
        check(PfmEnglishSeasonBridge.repairPoints(), "seeded save was not repaired");
        clubs = (dw[]) field(cp.class, "a", dw[].class).get(null);
        check(points.getByte(clubs[3]) == 1 && points.getByte(clubs[5]) == 6 &&
                !PfmEnglishSeasonBridge.repairPoints(), "played results lost or points repair was not idempotent");
    }

    private static void checkLedgers() throws Exception {
        for (int header : new int[]{24, 32, 40}) {
            int fields = header == 24 ? 2 : 3;
            byte[] old = new byte[header + fields * 512];
            old[4] = 9;
            for (int field = 0; field < fields; field++) old[header + field * 512 + 511] = (byte) (field + 1);
            byte[] expanded = PfmEnglishLedger.expand(old, header, fields);
            check(expanded[4] == 9 && expanded.length == header + fields * PfmEnglishLedger.SLOTS, "ledger header lost");
            for (int field = 0; field < fields; field++) {
                check(expanded[header + field * PfmEnglishLedger.SLOTS + 511] == field + 1 &&
                        expanded[header + field * PfmEnglishLedger.SLOTS + 512] == 0, "ledger fields were interleaved");
            }
        }
        Class<?>[] ledgers = {pfmCondition60.class, pfmDiscipline70.class, pfmContrib80.class};
        String[] values = {"fatigue", "suspension", "assists"};
        for (int i = 0; i < ledgers.length; i++) {
            java.lang.reflect.Method fresh = ledgers[i].getDeclaredMethod("fresh", int.class, int.class);
            fresh.setAccessible(true);
            fresh.invoke(null, pfm2.getSlot(), pfm2.getSeason());
            byte[] ledger = (byte[]) field(ledgers[i], values[i], byte[].class).get(null);
            check(ledger.length == PfmEnglishLedger.SLOTS, "real core ledger still capped at 512");
            ledger[650] = 3;
        }
        check(pfmDiscipline70.roundsOut(650) == 3 && pfmContrib80.assists(650) == 3, "high player IDs ignored");
    }

    private static NativeEnglishCareer.Opening division(int count, String league) {
        NativeEnglishCareer.Opening division = new NativeEnglishCareer.Opening();
        division.league = league;
        division.userId = "en:19";
        for (int i = 0; i < count; i++) {
            NativeEnglishCareer.Club club = new NativeEnglishCareer.Club();
            club.id = i == 3 ? division.userId : league + ":" + (i + 40);
            club.name = i == 3 ? "User club" : league + " club " + i;
            club.budget = 2000000 + i;
            for (int j = 0; j < (i == 3 ? 30 : 18); j++) {
                NativeEnglishCareer.Player player = new NativeEnglishCareer.Player();
                player.id = i == 3 && j == 0 ? "user-gk" : club.id + ":" + j;
                player.name = i == 3 && j == 0 ? "User keeper" : "Player " + j;
                player.position = j < 2 ? 0 : j < 8 ? 1 : j < 14 ? 2 : 3;
                player.age = 25;
                player.speed = player.resistance = player.quality = 70;
                player.wage = 200000;
                player.value = 1000000;
                club.players.add(player);
            }
            division.clubs.add(club);
        }
        return division;
    }

    private static void verify(NativeEnglishCareer.Opening division, SharedPreferences slots) throws Exception {
        pfmRound.startFirstRound();
        pfmLineup83.snapshotAfterLoad();
        dw[] clubs = (dw[]) field(cp.class, "a", dw[].class).get(null);
        int[][][] calendar = (int[][][]) field(cp.class, "a", int[][][].class).get(null);
        int count = division.clubs.size();
        check(clubs.length == count && calendar.length == 2 * (count - 1), "wrong calendar size");
        check(PfmSeasonBridge.leagueCode().equals(division.league), "wrong active competition bridge");
        java.lang.reflect.Method selectRound = Arrays.stream(cp.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("b") && m.getReturnType() == void.class &&
                        Arrays.equals(m.getParameterTypes(), new Class<?>[]{int.class})).findFirst().get();
        Field userFixture = field(cp.class, "d", byte.class);
        for (int round = 0; round < calendar.length; round++) {
            selectRound.invoke(null, round);
            int[] fixture = calendar[round][userFixture.getByte(null)];
            check(fixture[0] == 3 || fixture[1] == 3, "user fixture is missing in round " + (round + 1));
        }
        field(du.class, "t", boolean.class).setBoolean(null, true);
        check(cp.pfmPlayedForTable() == calendar.length, "final round is truncated");
        pfmRound.startFirstRound();
        boolean[][] fixtures = new boolean[count][count];
        for (int[][] round : calendar) {
            Set<Integer> teams = new HashSet<>();
            for (int[] fixture : round) {
                int home = fixture[0], away = fixture[1];
                check(teams.add(home) && teams.add(away) && !fixtures[home][away], "fixture duplicated");
                fixtures[home][away] = true;
            }
            check(teams.size() == count, "club missing from round");
        }
        dw user = pfmPlayerHistoryV2.userTeam();
        check(user == clubs[3] && pfmAccess50.name(user).equals("User club"), "user club identity changed");
        short[] roster = (short[]) field(dw.class, "a", short[].class).get(user);
        check(field(dw.class, "e", byte.class).getByte(user) == 30 && roster[0] == slots.getInt("user-gk", -1), "user squad lost");
        check(pfmRosterAccess50.count(user) == 30, "roster view hides players");
        check(field(dw.class, "g", int.class).getInt(user) == 2000003, "user budget lost");
        check(slots.getString("club.3", "").equals(division.userId), "stable team ID lost");
        for (dw team : clubs) check(((byte[]) field(dw.class, "c", byte[].class).get(team)).length == calendar.length, "score ledger length");
    }

    private static void reload() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        check(cp.a(new DataOutputStream(buffer)), "legacy save failed");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            cp.a(input, false);
            check(input.available() == 0, "save layout left unread bytes");
        }
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws Exception {
        for (Field f : owner.getDeclaredFields()) if (f.getName().equals(name) && f.getType() == type) {
            f.setAccessible(true);
            return f;
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
}
