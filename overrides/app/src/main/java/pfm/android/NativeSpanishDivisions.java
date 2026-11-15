package pfm.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

final class NativeSpanishDivisions {
    static final String TOP = "es", LOWER = NativeSegunda.CODE;
    static final String PLAYOFF = "playoff:es2", TABLE = "es2-final-order";
    private static final int TOP_SIZE = 20;

    private NativeSpanishDivisions() {}

    static boolean includes(String league) {
        return TOP.equals(league) || LOWER.equals(league);
    }

    static List<String> eligibleOrder(List<String> table) {
        List<String> eligible = new ArrayList<>();
        for (String club : table) if (!NativeSegunda.reserve(club)) eligible.add(club);
        return eligible;
    }

    static void complete(MatchJournal journal, int nonce, int total, HashMap<String, NativeWorldCenter.T> teams,
                         HashMap<String, NativeWorldCenter.P> players, String source, MatchEffects effects,
                         NativeCareerStore.Career career) {
        List<UclSeason.Standing> lower = NativeItalianDivisions.finalTable(journal, LOWER, career, true);
        if (lower.isEmpty()) return;
        List<String> order = new ArrayList<>();
        for (UclSeason.Standing row : lower) order.add(row.id);
        journal.define(TABLE, EuropeanQualification.encode(order));
        int seed = nonce ^ journal.season() * 10007 ^ 0x455332;
        NativeItalianDivisions.playPromotion(journal, PLAYOFF, eligibleOrder(order), seed, total,
                teams, players, source, effects, career);
    }

    static final class Opening {
        final Set<String> top, lower;

        Opening(Set<String> top, Set<String> lower) {
            this.top = top;
            this.lower = lower;
        }
    }

    static Opening opening(MatchJournal previous, NativeCareerStore.Career career) throws IOException {
        List<UclSeason.Standing> topTable = NativeItalianDivisions.finalTable(previous, TOP, career, true);
        List<String> lowerOrder = EuropeanQualification.ids(previous.metadata(TABLE));
        if (topTable.size() != TOP_SIZE || lowerOrder.size() != NativeSegunda.CLUBS) return null;
        List<String> eligible = eligibleOrder(lowerOrder);
        Set<String> promoted = new LinkedHashSet<>(eligible.subList(0, 2));
        for (MatchJournal.Match match : previous.competition(PLAYOFF)) {
            if (match.round == NativeItalianDivisions.FINAL && eligible.subList(2, 6).contains(match.winner)) {
                promoted.add(match.winner);
            }
        }
        if (promoted.size() != 3) throw new IOException("Missing Segunda play-off result");
        Set<String> top = new LinkedHashSet<>(promoted);
        for (int i = 0; i < TOP_SIZE - 3; i++) top.add(topTable.get(i).id);
        Set<String> lower = new LinkedHashSet<>(lowerOrder);
        lower.removeAll(promoted);
        for (int i = TOP_SIZE - 3; i < TOP_SIZE; i++) lower.add(topTable.get(i).id);
        fillReservePlaces(top, lower);
        return validated(new Opening(top, lower), career);
    }

    static void fillReservePlaces(Set<String> top, Set<String> lower) {
        for (String reserve : NativeSegunda.RESERVES) {
            if (!top.contains(NativeSegunda.parent(reserve))) lower.remove(reserve);
        }
        for (String reserve : NativeSegunda.RESERVES) {
            if (top.contains(NativeSegunda.parent(reserve)) && !lower.contains(reserve)) {
                if (lower.size() == NativeSegunda.CLUBS) {
                    for (String replacement : NativeSegunda.REPLACEMENTS) {
                        if (lower.remove(replacement)) break;
                    }
                }
                if (lower.size() < NativeSegunda.CLUBS) lower.add(reserve);
            }
        }
        for (String replacement : NativeSegunda.REPLACEMENTS) {
            if (lower.size() == NativeSegunda.CLUBS) break;
            if (!top.contains(replacement)) lower.add(replacement);
        }
    }

    private static Opening validated(Opening opening, NativeCareerStore.Career career) throws IOException {
        Set<String> clubs = new HashSet<>();
        for (NativeCareerStore.Club club : career.clubs) if (NativeSegunda.spanishClub(club.id)) clubs.add(club.id);
        if (opening.top.size() != TOP_SIZE || opening.lower.size() != NativeSegunda.CLUBS ||
                !Collections.disjoint(opening.top, opening.lower) ||
                !clubs.containsAll(opening.top) || !clubs.containsAll(opening.lower)) {
            throw new IOException("Invalid Spanish division membership");
        }
        for (String reserve : NativeSegunda.RESERVES) {
            if (opening.top.contains(reserve) ||
                    opening.lower.contains(reserve) && !opening.top.contains(NativeSegunda.parent(reserve))) {
                throw new IOException("Invalid Spanish reserve-team placement");
            }
        }
        return opening;
    }

    static void rollover(Context context, NativeCareerStore.Career career, int year) throws IOException {
        if (!NativeSegunda.active(career, year - 1)) return;
        boolean managed = includes(career.activeLeague);
        if (managed && !EnglishLegacyIds.installed(context, year)) return;
        File dir = new File(context.getFilesDir(), "match-journals");
        int nonce = NativeWorldCenter.identity();
        MatchJournal next = new MatchJournal(dir, nonce, year);
        String savedTop = next.metadata(UclSeason.membersKey(TOP));
        Opening membership;
        if (savedTop != null) {
            membership = validated(new Opening(new LinkedHashSet<>(EuropeanQualification.ids(savedTop)),
                    new LinkedHashSet<>(EuropeanQualification.ids(next.metadata(UclSeason.membersKey(LOWER))))), career);
        } else {
            if (!next.competition("league:" + TOP).isEmpty() || !next.competition("league:" + LOWER).isEmpty()) return;
            membership = opening(new MatchJournal(dir, nonce, year - 1), career);
        }
        if (membership == null) return;
        for (NativeCareerStore.Club club : career.clubs) {
            if (!NativeSegunda.spanishClub(club.id)) continue;
            String league = membership.top.contains(club.id) ? TOP
                    : membership.lower.contains(club.id) ? LOWER : NativeSegunda.INACTIVE;
            if (NativeSegunda.INACTIVE.equals(league)) club.activeFrom = Integer.MAX_VALUE;
            else if (NativeSegunda.INACTIVE.equals(club.league)) club.activeFrom = year;
            club.league = league;
            if (managed && club.name.equalsIgnoreCase(career.userClub)) career.activeLeague = league;
        }
    }
}
