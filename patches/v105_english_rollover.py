#!/usr/bin/env python3
"""Install the English roster at the existing round-one boundary."""
import os
import struct
import sys
import tempfile
import zipfile

from v31_policy_stats_hooks import add_methodref

SLOTS = 8192
LEDGERS = {
    "pfmCondition60.class": ("condition", 116, {512: (SLOTS, 15), 536: (24 + SLOTS, 2), 1048: (24 + 2 * SLOTS, 2)}),
    "pfmDiscipline70.class": ("discipline", 85, {512: (SLOTS, 19), 1576: (40 + 3 * SLOTS, 2)}),
    "pfmContrib80.class": ("contributions", 108, {512: (SLOTS, 20), 1568: (32 + 3 * SLOTS, 2)}),
}


def widen_ledger(data, method, read_ref, limits):
    for old, (new, count) in limits.items():
        opcode = b"\x11" + struct.pack(">H", old)
        assert data.count(opcode) == count, (method, old, "legacy ledger size changed")
        data = data.replace(opcode, b"\x11" + struct.pack(">H", new))
    call = b"\xb6" + struct.pack(">H", read_ref)
    assert data.count(call) == 1, (method, "legacy ledger reader changed")
    data, ref = add_methodref(data, "PfmEnglishLedger", method, "(Lpfm/android/compat/rms/RecordStore;I)[B")
    return data.replace(call, b"\xb8" + struct.pack(">H", ref))


def bridge(data, pattern, method, descriptor="()V", prefix=b"", suffix=b""):
    assert data.count(pattern) == 1, (method, "legacy call site changed")
    data, ref = add_methodref(data, "PfmEnglishSeasonBridge", method, descriptor)
    code = prefix + b"\xb8" + struct.pack(">H", ref) + suffix
    assert len(code) <= len(pattern)
    return data.replace(pattern, code + b"\x00" * (len(pattern) - len(code)), 1)


def main(path):
    with zipfile.ZipFile(path) as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    fd, target = tempfile.mkstemp(prefix="pfm-v105-", suffix=".jar", dir=os.path.dirname(path) or ".")
    os.close(fd)
    changed = set()
    try:
        with zipfile.ZipFile(target, "w") as output:
            for item, data in entries:
                if item.filename == "pfm2.class":
                    old = b"\xb8\x00\x85\xb8\x00\x3f"  # startFirstRound, then updateSeasonStrings
                    data = bridge(data, old, "startFirstRound", suffix=old[3:])
                    old = b"\xb8\x01\x5b\xb8\x00\x7f"  # startNewSeason, then sortSeededTable
                    data = bridge(data, old, "beforeSeasonReset", suffix=old[3:])
                    changed.add(item.filename)
                if item.filename == "dw.class":
                    old = b"\x2a\x10\x19\xbc\x09\xb5"  # constructor roster allocation
                    assert data.count(old) == 1, "legacy roster allocation changed"
                    data = data.replace(old, b"\x2a\x10\x1e\xbc\x09\xb5")
                    changed.add(item.filename)
                if item.filename == "cp.class":
                    old = b"\x10\x19\xbd\x00\x47"  # the saved user squad's training records
                    assert data.count(old) == 1 and b"eg" in data, "legacy training allocation changed"
                    data = data.replace(old, b"\x10\x1e" + old[2:])
                    data = bridge(data, b"\xb8\x00\xcb\xb1", "afterLeagueLoad", suffix=b"\xb1")
                    changed.add(item.filename)
                if item.filename == "ca.class":
                    for team_field in (0x70, 0x76):
                        team = bytes((0x2a, 0xb4, 0, team_field))
                        old = b"\x10\x09" + team + b"\xb4\x00\xd1\x60\x1b\x60\xb8\x00\xd4"
                        data = bridge(data, old, "matchBadge", "(Ldw;I)Lpfm/android/compat/lcdui/Image;", prefix=team + b"\x1b")
                    changed.add(item.filename)
                if item.filename == "pfmRosterAccess50.class":
                    old = b"\x10\x19\xa4\x00\x06\x10\x19"
                    assert data.count(old) == 1, "legacy roster view limit changed"
                    data = data.replace(old, b"\x10\x1e\xa4\x00\x06\x10\x1e")
                    changed.add(item.filename)
                if item.filename in LEDGERS:
                    data = widen_ledger(data, *LEDGERS[item.filename])
                    changed.add(item.filename)
                output.writestr(item, data)
        assert changed == {"pfm2.class", "dw.class", "cp.class", "ca.class", "pfmRosterAccess50.class", *LEDGERS}
        os.replace(target, path)
    finally:
        if os.path.exists(target):
            os.unlink(target)


if __name__ == "__main__":
    main(sys.argv[1])
