package pfm.android;

import android.content.Context;
import java.io.IOException;
import java.util.*;

final class NativeFrenchDivisions {
    private NativeFrenchDivisions() {}

    static boolean includes(String code) {
        return "fr".equals(code) || "fr2".equals(code);
    }

    static Set<String> nextTop(MatchJournal previous, NativeCareerStore.Career career) throws IOException {
        List<UclSeason.Standing> top = NativeGermanDivisions.finalTable(previous, "fr", career);
        List<UclSeason.Standing> lower = NativeGermanDivisions.finalTable(previous, "fr2", career);
        if (top.isEmpty() || lower.isEmpty()) return Collections.emptySet();
        Set<String> clubs = new LinkedHashSet<>();
        for (int i = 0; i < 17; i++) clubs.add(top.get(i).id);
        for (int i = 0; i < 3; i++) clubs.add(lower.get(i).id);
        if (clubs.size() != 20) throw new IOException("Invalid Ligue 1 membership");
        return clubs;
    }

    static int promotionZone(int rank) {
        return rank <= 3 ? 0xffdcefd8 : 0;
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year) throws IOException {
        if (NativeLigue2.active(career, year - 1)) {
            NativeGermanDivisions.rollover(context, career, year, "fr", 20);
        }
    }
}
