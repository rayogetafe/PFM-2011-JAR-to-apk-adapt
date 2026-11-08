import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import pfm.android.AndroidRuntime;
import pfm.android.EnglishLegacyIds;
import pfm.android.NativeMatchJournal;
import pfm.android.compat.rms.RecordStore;

/** Domestic ledgers travel with the main career save so a replay cannot reuse a later round's state. */
public final class PfmDomesticReplay {
    private static final int MAGIC = 0x50444c31;
    private static final String[] LEDGERS = {"P2P", "P2F", "P2D", "P2X", "P2E", "P2H", "P2UH", "P2K"};
    private static final Class<?>[] CACHES = {pfmPlayerStats.class, pfmCondition60.class, pfmDiscipline70.class, pfmContrib80.class};
    private static boolean loadingCareer;

    private PfmDomesticReplay() {}

    public static int add(RecordStore store, byte[] bytes, int offset, int count) throws IOException {
        if (pfm2.getMode() != 1) return store.addRecord(bytes, offset, count);
        byte[] data = pack(bytes, offset, count);
        return store.addRecord(data, 0, data.length);
    }

    public static void set(RecordStore store, int id, byte[] bytes, int offset, int count) throws IOException {
        if (pfm2.getMode() != 1) {
            store.setRecord(id, bytes, offset, count);
            return;
        }
        byte[] data = pack(bytes, offset, count);
        store.setRecord(id, data, 0, data.length);
    }

    public static byte[] read(RecordStore store, int id) throws IOException {
        loadingCareer = false;
        byte[] data = store.getRecord(id);
        if (pfm2.getMode() != 1) return data;
        int start = checkpointStart(data);
        if (start == data.length) {
            byte[] upgraded = pack(data, 0, data.length);
            store.setRecord(id, upgraded, 0, upgraded.length);
            loadingCareer = true;
            return data;
        }
        restore(new DataInputStream(new ByteArrayInputStream(data, start, data.length - start - 8)));
        loadingCareer = true;
        return Arrays.copyOf(data, start);
    }

    static int checkpointStart(byte[] data) throws IOException {
        if (data.length < 8) return data.length;
        DataInputStream footer = new DataInputStream(new ByteArrayInputStream(data, data.length - 8, 8));
        int length = footer.readInt();
        if (footer.readInt() != MAGIC) return data.length;
        if (length <= 0 || length > data.length - 8) throw new IOException("Invalid domestic save checkpoint");
        return data.length - 8 - length;
    }

    private static byte[] pack(byte[] bytes, int offset, int count) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.write(bytes, offset, count);
        for (String ledger : LEDGERS) {
            byte[] record = RecordStore.checkpoint(storeName(ledger));
            out.writeInt(record == null ? -1 : record.length);
            if (record != null) out.write(record);
        }
        pfmGoalkeeperStats31.writeCheckpoint(out);
        out.writeInt(buffer.size() - count);
        out.writeInt(MAGIC);
        return buffer.toByteArray();
    }

    private static void restore(DataInputStream in) throws IOException {
        byte[][] records = new byte[LEDGERS.length][];
        for (int i = 0; i < records.length; i++) {
            int length = in.readInt();
            if (length < -1 || length > 4 * 1024 * 1024 || length > in.available()) throw new IOException("Invalid domestic ledger length");
            if (length >= 0) {
                records[i] = new byte[length];
                in.readFully(records[i]);
            }
        }
        Map<String, Integer> keepers = pfmGoalkeeperStats31.readCheckpoint(in);
        if (in.available() != 0) throw new IOException("Trailing domestic checkpoint bytes");
        for (int i = 0; i < records.length; i++) {
            RecordStore.restoreCheckpoint(storeName(LEDGERS[i]), records[i]);
        }
        for (Class<?> cache : CACHES) {
            try {
                Field slot = cache.getDeclaredField("loadedSlot");
                slot.setAccessible(true);
                slot.setInt(null, -1);
            } catch (ReflectiveOperationException error) {
                throw new IOException("Cannot reload domestic ledger", error);
            }
        }
        pfmGoalkeeperStats31.restoreCheckpoint(keepers);
    }

    private static String storeName(String ledger) { return ledger + "00" + (pfm2.getSlot() + 1); }

    static void afterCoreLoad() {
        PfmMatchEngineBridge.newLeagueAttempt();
        if (!loadingCareer) return;
        loadingCareer = false;
        try {
            Context context = AndroidRuntime.context();
            int played = cp.pfmPlayedForTable(), slot = pfm2.getSlot(), year = pfm2.getSeason();
            String key = "slot_" + slot;
            SharedPreferences progress = context.getSharedPreferences("pfm_world_careers", 0);
            if (EnglishLegacyIds.nonce(context) != 0 && progress.getInt(key + "_season", year) == year) {
                progress.edit().putInt(key + "_played", played).commit();
            }
            pfmLeagueMatchArchive34.discardAfter(played);
            NativeMatchJournal.restoreActiveLeague(context, PfmSeasonBridge.leagueCode(), played);
        } catch (Exception error) {
            android.util.Log.e("PFM-Replay", "Could not reconcile the loaded domestic season", error);
        }
    }
}
