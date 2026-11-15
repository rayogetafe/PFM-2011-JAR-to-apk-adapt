package pfm.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Read-only handoff to the legacy matchday code just before round one is installed. */
public final class NativeEnglishCareer {
    private NativeEnglishCareer() {}

    public static final class Opening {
        public String league, userId;
        public final ArrayList<Club> clubs = new ArrayList<>();
    }

    public static final class Club {
        public String id, name;
        public int budget;
        public final ArrayList<Player> players = new ArrayList<>();
    }

    public static final class Player {
        public String id, name;
        public int originalSlot = -1, age, position, speed, resistance, quality, wage, value;
    }

    public static int[][][] calendar(int clubs) {
        int[][][] rounds = new int[2 * (clubs - 1)][clubs / 2][3];
        int[] slots = new int[rounds.length];
        for (NativeWorldCenter.M match : NativeWorldCenter.schedule(clubs)) {
            int round = match.r - 1;
            int[] fixture = rounds[round][slots[round]++];
            fixture[0] = match.h;
            fixture[1] = match.a;
        }
        return rounds;
    }

    public static Opening championshipStart(Context context, int clubIndex) throws IOException {
        return secondDivisionStart(context, clubIndex, "en2");
    }

    public static boolean reserveClub(String id) { return NativeSegunda.reserve(id); }

    public static Opening secondDivisionStart(Context context, int clubIndex, String league) throws IOException {
        Map<String, ArrayList<NativeWorldCenter.T>> world = new HashMap<>();
        boolean german = "de2".equals(league);
        if ("es2".equals(league)) NativeSegunda.load(context, world);
        else if ("it2".equals(league)) NativeSerieB.load(context, world);
        else if ("fr2".equals(league)) NativeLigue2.load(context, world);
        else if (german) NativeBundesliga2.load(context, world);
        else NativeChampionship.load(context, world);
        ArrayList<NativeWorldCenter.T> teams = world.get(league);
        Opening opening = new Opening();
        opening.league = league;
        opening.userId = opening.league + ":" + teams.get(clubIndex).id;
        for (NativeWorldCenter.T team : teams) {
            Club club = new Club();
            club.id = opening.league + ":" + team.id;
            club.name = team.name;
            for (NativeWorldCenter.P source : team.ps) {
                Player player = new Player();
                player.id = source.uid;
                player.name = source.name;
                player.age = source.age;
                player.position = source.pos;
                player.speed = source.spe;
                player.resistance = source.res;
                player.quality = source.qua;
                player.wage = source.wage;
                player.value = source.value;
                club.players.add(player);
            }
            ClubEconomy.Profile ground = ClubEconomy.profile(context, club.id);
            club.budget = "es2".equals(league) ? NativeSegunda.openingCash(ground) : "it2".equals(league) ? NativeSerieB.openingCash(ground) : "fr2".equals(league) ? NativeLigue2.openingCash(ground) : german ? NativeBundesliga2.openingCash(ground) : ground.openingCash();
            opening.clubs.add(club);
        }
        return opening;
    }

    public static Opening opening(Context context, int previousYear) throws IOException {
        return opening(context, previousYear, "en");
    }

    public static Opening opening(Context context, int previousYear, String country) throws IOException {
        int nonce = EnglishLegacyIds.nonce(context);
        boolean german = "de".equals(country);
        NativeCareerStore.Career career = !"en".equals(country) ? NativeCareerStore.divisionOpening(context, previousYear, nonce, country)
                : NativeCareerStore.englishOpening(context, previousYear, nonce);
        if (career == null) return null;
        MatchJournal previous = new MatchJournal(new File(context.getFilesDir(), "match-journals"), nonce, previousYear);
        if ("en".equals(career.activeLeague) && previous.metadata(NativeEnglishDivisions.FINAL_PREMIER_ORDER) == null) return null;
        NativeSpanishDivisions.Opening spanish = "es".equals(country) ? NativeSpanishDivisions.opening(previous, career) : null;
        Set<String> premier = "es".equals(country) ? (spanish == null ? Collections.emptySet() : spanish.top)
                : "it".equals(country) ? NativeItalianDivisions.nextTop(previous, career)
                : "fr".equals(country) ? NativeFrenchDivisions.nextTop(previous, career) : german ? NativeGermanDivisions.nextTop(previous, career, nonce)
                : NativeEnglishDivisions.nextPremier(previous, career);
        if (premier.isEmpty()) return null;
        Opening opening = new Opening();
        for (NativeCareerStore.Club club : career.clubs) {
            if (country.equals(EuropeanPlaces.association(club.league)) && club.name.equalsIgnoreCase(career.userClub)) {
                opening.userId = club.id;
                break;
            }
        }
        if (opening.userId == null) throw new IOException("Career club unavailable");
        boolean top = premier.contains(opening.userId);
        opening.league = top ? country : country + "2";
        Map<String, Club> selected = new LinkedHashMap<>();
        for (NativeCareerStore.Club source : career.clubs) {
            if (spanish != null) {
                if (!(top ? spanish.top : spanish.lower).contains(source.id)) continue;
            } else if (!country.equals(EuropeanPlaces.association(source.league)) || premier.contains(source.id) != top) continue;
            Club club = new Club();
            club.id = source.id;
            club.name = source.name;
            club.budget = source.balance;
            selected.put(club.id, club);
        }
        for (NativeCareerStore.Player source : career.players) {
            Club club = selected.get(source.owner);
            if (club == null) continue;
            Player player = new Player();
            player.id = source.id;
            player.name = source.name;
            if (source.id.startsWith(country + ":")) player.originalSlot = source.sourceId;
            player.age = Math.min(50, source.age + 1);
            player.position = source.position;
            player.speed = source.spe;
            player.resistance = source.res;
            player.quality = source.qua;
            player.wage = source.contractWage;
            player.value = source.value;
            club.players.add(player);
        }
        opening.clubs.addAll(selected.values());
        if (opening.clubs.size() != ("es".equals(country) ? (top ? 20 : NativeSegunda.CLUBS) : "it".equals(country) ? (top ? 20 : NativeSerieB.CLUBS) : "fr".equals(country) ? NativeLigue2.CLUBS : german ? NativeBundesliga2.CLUBS : top ? 20 : NativeChampionship.CLUBS)) {
            throw new IOException("Wrong division size");
        }
        return opening;
    }

    public static void recordFinalPremier(Context context, int year, List<String> order) throws IOException {
        int nonce = EnglishLegacyIds.nonce(context);
        if (nonce == 0 || order.size() != 20) return;
        MatchJournal journal = new MatchJournal(new File(context.getFilesDir(), "match-journals"), nonce, year);
        Set<String> members = new HashSet<>(NativeEnglishDivisions.members(journal, "en"));
        if (members.size() != 20 || !members.equals(new HashSet<>(order))) return;
        journal.define(NativeEnglishDivisions.FINAL_PREMIER_ORDER, EuropeanQualification.encode(order));
        journal.save();
    }
}
