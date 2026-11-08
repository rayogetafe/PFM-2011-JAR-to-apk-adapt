import pfm.android.compat.rms.RecordStore;

/** Compatibility reader for the previous fixed-width legacy match ledgers. */
public final class PfmEnglishLedger {
    static final int SLOTS = 8192;
    private static final int OLD_SLOTS = 512;

    private PfmEnglishLedger() {}

    public static byte[] condition(RecordStore store, int record) throws Exception {
        return expand(store.getRecord(record), 24, 2);
    }

    public static byte[] discipline(RecordStore store, int record) throws Exception {
        return expand(store.getRecord(record), 40, 3);
    }

    public static byte[] contributions(RecordStore store, int record) throws Exception {
        return expand(store.getRecord(record), 32, 3);
    }

    static byte[] expand(byte[] original, int header, int fields) {
        if (original == null || original.length != header + fields * OLD_SLOTS) return original;
        byte[] expanded = new byte[header + fields * SLOTS];
        System.arraycopy(original, 0, expanded, 0, header);
        for (int field = 0; field < fields; field++) {
            System.arraycopy(original, header + field * OLD_SLOTS, expanded, header + field * SLOTS, OLD_SLOTS);
        }
        return expanded;
    }
}
