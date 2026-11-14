import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Player names for the original in-match bitmap font. */
public final class PfmMatchPlayerLabel {
    private static final int MAX_LENGTH = 18;
    private static final Field NAME = field(ci.class, byte[].class);
    private static final Field TEAMS = field(cp.class, dw[].class);
    private static final Field FIXTURES = field(cp.class, int[][][].class);
    private static final Map<byte[], String> NAMES = new WeakHashMap<>();

    private PfmMatchPlayerLabel() {}

    public static String label(ci player) {
        String full = read(player);
        String label = shortName(full);
        if (!full.equalsIgnoreCase(label)) {
            try {
                int round = pfmRoundAccess60.round();
                int[][] fixtures = ((int[][][]) FIXTURES.get(null))[round];
                dw[] teams = (dw[]) TEAMS.get(null);
                for (int[] fixture : fixtures) {
                    if (fixture[0] != (player.j & 255) && fixture[1] != (player.j & 255)) continue;
                    if (sharesName(player, label, teams[fixture[0]]) || sharesName(player, label, teams[fixture[1]])) {
                        return limit(initial(full, label));
                    }
                    break;
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
                // The label also works while the legacy match state is being initialized.
            }
        }
        return limit(label);
    }

    private static boolean sharesName(ci player, String label, dw team) throws IllegalAccessException {
        for (int i = 0; i < (team.e & 255); i++) {
            ci other = cp.a(team, i);
            if (other == player || other == null) continue;
            String otherLabel = shortName(read(other));
            int initialEnd = otherLabel.indexOf(". ");
            if (initialEnd == 1) otherLabel = otherLabel.substring(3);
            if (label.equalsIgnoreCase(otherLabel)) return true;
        }
        return false;
    }

    private static synchronized String read(ci player) {
        try {
            byte[] bytes = (byte[]) NAME.get(player);
            String name = NAMES.get(bytes);
            if (name == null) {
                String utf8 = new String(bytes, StandardCharsets.UTF_8);
                name = ascii(utf8.indexOf('\ufffd') < 0 ? utf8 : new String(bytes, StandardCharsets.ISO_8859_1));
                NAMES.put(bytes, name);
            }
            return name;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return "Player";
        }
    }

    static String ascii(String name) {
        String text = name.replace("Ł", "L").replace("ł", "l").replace("Ø", "O").replace("ø", "o")
                .replace("Đ", "D").replace("đ", "d").replace("Ð", "D").replace("ð", "d")
                .replace("Æ", "AE").replace("æ", "ae").replace("Œ", "OE").replace("œ", "oe")
                .replace("Þ", "Th").replace("þ", "th").replace("ß", "ss").replace("ẞ", "SS")
                .replace('\u0131', 'i').replace('\u2019', '\'').replace('\u2011', '-').replace('\u2013', '-')
                .replaceAll("\\s+", " ");
        return Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9.\\-' ]+", "").trim().replaceAll(" +", " ");
    }

    static String shortName(String full) {
        if (full.isEmpty()) return "Player";
        String[] words = full.split(" ");
        if (words.length == 1 || (words.length == 2 && words[0].endsWith("."))) return full;
        if ("Cristiano Ronaldo".equalsIgnoreCase(full)) return "Cristiano Ronaldo";
        if (full.equalsIgnoreCase("Joao Pereira") || full.equalsIgnoreCase("Bruno Alves") ||
                full.equalsIgnoreCase("David Luiz") || full.equalsIgnoreCase("Lucas Leiva") ||
                full.equalsIgnoreCase("Javier Hernandez")) return full;
        int first = words.length - 1;
        while (first > 0 && particle(words[first - 1])) first--;
        int offset = 0;
        for (int i = 0; i < first; i++) offset += words[i].length() + 1;
        return full.substring(offset);
    }

    private static boolean particle(String word) {
        switch (word.toLowerCase(Locale.ROOT)) {
            case "da": case "de": case "del": case "den": case "der": case "di":
            case "dos": case "du": case "el": case "la": case "le": case "van": case "von":
                return true;
            default: return false;
        }
    }

    private static String initial(String full, String label) {
        return full.charAt(0) + ". " + label;
    }

    private static String limit(String label) {
        return label.length() <= MAX_LENGTH ? label : label.substring(0, MAX_LENGTH - 1).trim() + ".";
    }

    private static Field field(Class<?> owner, Class<?> type) {
        for (Field field : owner.getDeclaredFields()) {
            if (field.getName().equals("a") && field.getType() == type) return field;
        }
        throw new IllegalStateException("Missing legacy field in " + owner.getName());
    }
}
