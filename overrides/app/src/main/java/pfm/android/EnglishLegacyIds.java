package pfm.android;

import android.content.Context;
import android.content.SharedPreferences;

/** A legacy slot map belongs to a specific season and career identity. */
public final class EnglishLegacyIds {
    private EnglishLegacyIds() {}

    public static int nonce(Context context) {
        return context.getSharedPreferences("pfm_world_careers", 0)
                .getInt("slot_" + NativeWorldCenter.slot() + "_nonce", 0);
    }

    public static SharedPreferences season(Context context, int year) {
        return context.getSharedPreferences("pfm_english_slots_" + nonce(context) + "_" +
                NativeWorldCenter.slot() + "_" + year, 0);
    }

    public static boolean installed(Context context, int year) {
        return nonce(context) != 0 && "yes".equals(season(context, year).getString("installed", ""));
    }

    public static SharedPreferences players(Context context, int year) {
        return installed(context, year) ? season(context, year) :
                context.getSharedPreferences("pfm_native_legacy_ids_v58", 0);
    }
}
