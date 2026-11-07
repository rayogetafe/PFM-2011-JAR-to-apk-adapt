package pfm.android;

import android.content.Context;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;
import static pfm.android.JournalHarness.check;

public class EuropeanBanHarness {
    private static void fact(MatchJournal journal, String id, String competition, int clock,
                             String uid, int yellow, int red, boolean played) {
        MatchJournal.Match match = EffectsHarness.fixture(id, competition, clock, uid, yellow, red, 0);
        match.home = "en:0";
        match.away = "en:1";
        match.players.get(0).club = match.home;
        if (!played) match.players.get(0).stats[2] = 0;
        journal.put(match);
    }

    private static void loadAsV4(File root, int nonce, int season) throws IOException {
        File path = new File(root, "match-journals/matches-" + nonce + "-" + season + ".bin");
        ByteBuffer source = ByteBuffer.wrap(Files.readAllBytes(path.toPath()));
        int payloadSize = source.getInt();
        byte[] payload = new byte[payloadSize - 5];
        source.get(payload);
        check(source.get() == 0 && source.getInt() == 0, "fixture must have no defined European bans");
        ByteBuffer.wrap(payload).putInt(4, 4);
        CRC32 crc = new CRC32();
        crc.update(payload);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path))) {
            out.writeInt(payload.length);
            out.write(payload);
            out.writeLong(crc.getValue());
        }
    }

    public static void main(String[] args) throws Exception {
        int nonce = 960;
        File root = new File(args[1]);
        Context context = new Context("overrides/app/src/main/assets", root);
        AndroidRuntime.current = context;
        JournalHarness.world(args[0], nonce);
        NativeCareerStore.Career career = NativeCareerStore.current;
        MatchJournal previous = JournalHarness.open(context, nonce, 2010);
        MatchJournal plan = JournalHarness.open(context, nonce, 2011);
        EuropeanQualification.prepare(plan, previous, career, nonce ^ 2011 * 10007);
        plan.save();
        String[] pair = EuropeanQualification.pairs(plan).get(0);
        NativeCareerStore.Player player = career.players.stream()
                .filter(p -> p.id.startsWith("en:")).findFirst().get();
        String uid = player.id;
        player.owner = pair[0]; // The player changed clubs over the summer.

        fact(previous, "first-yellow", "uel:2010", 29, uid, 1, 0, true);
        fact(previous, "second-yellow", "uel:2010", 31, uid, 1, 0, true);
        fact(previous, "red-with-yellow", "uel:2010", 33, uid, 1, 1, true);
        fact(previous, "domestic-absence", "league:en", 34, uid, 0, 0, false);
        fact(previous, "one-served", "uel:2010", 35, uid, 0, 0, false);
        for (int i = 0; i < 3; i++) fact(previous, "yellow-" + i, "ucl:2010", 29 + i, "yellow-only", 1, 0, true);
        fact(previous, "two-yellows", "ucl:2010", 36, "without-europe", 2, 1, true);
        previous.save();
        loadAsV4(root, nonce, 2010);
        NativeWorldCenter.year = 2011;
        NativeMatchJournal.advance(context, 0);
        MatchJournal opening = JournalHarness.open(context, nonce, 2011);
        Map<String, Integer> expected = new HashMap<>();
        expected.put(uid, 1);
        expected.put("without-europe", 1);
        check(opening.europeanOpeningBans().equals(expected), "v4 archive carry-over, yellow expiry or partial service");
        JournalHarness.restart();
        int[] status = NativeMatchJournal.legacyEffects(player.sourceId);
        check(status[2] == 0 && status[3] == 0 && status[4] == 1, "cold start lost ban or kept old cards");

        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal firstLegs = JournalHarness.open(context, nonce, 2011);
        MatchJournal.Match first = firstLegs.get(UclKnockout.key("uclq:2011", 1, pair[0], pair[1]));
        check(first.players.stream().anyMatch(p -> p.uid.equals(uid) && p.stats[2] == 0),
                "carried suspension did not exclude the transferred player from the qualifier");
        JournalHarness.restart();
        check(NativeMatchJournal.legacyEffects(player.sourceId)[4] == 0, "reopened qualifier did not serve ban exactly once");
        NativeWorldCenter.year = 2012;
        NativeWorldCenter.played = 0;
        NativeMatchJournal.advance(context, 0);
        Map<String, Integer> afterAnotherSummer = JournalHarness.open(context, nonce, 2012).europeanOpeningBans();
        check(!afterAnotherSummer.containsKey(uid) && afterAnotherSummer.getOrDefault("without-europe", 0) == 1,
                "served ban carried forward or unserved ban expired without a European fixture: " + afterAnotherSummer);

        MatchJournal started = JournalHarness.open(context, nonce, 2013);
        fact(started, "old-season", "league:en", 1, uid, 0, 0, true);
        started.save();
        loadAsV4(root, nonce, 2013);
        NativeWorldCenter.year = 2013;
        NativeWorldCenter.played = 1;
        NativeMatchJournal.advance(context, 1);
        MatchJournal upgraded = JournalHarness.open(context, nonce, 2013);
        check(upgraded.europeanOpeningDefined() && upgraded.europeanOpeningBans().isEmpty() &&
                upgraded.get("old-season") != null, "already-started v4 season changed retroactively");
        System.out.println("PASS: v4 upgrade, dismissal vs accumulation, partial service, transferred qualifier player, cold reload and multi-season carry");
    }
}
