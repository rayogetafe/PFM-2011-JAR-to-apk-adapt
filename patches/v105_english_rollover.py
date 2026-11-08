#!/usr/bin/env python3
"""Install the English roster at the existing round-one boundary."""
import os
import struct
import sys
import tempfile
import zipfile

from v31_policy_stats_hooks import add_methodref


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
                    assert data.count(old) == 1 and b"startFirstRound" in data, "legacy rollover call changed"
                    data, ref = add_methodref(data, "PfmEnglishSeasonBridge", "startFirstRound", "()V")
                    data = data.replace(old, b"\xb8" + struct.pack(">H", ref) + old[3:])
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
                    changed.add(item.filename)
                output.writestr(item, data)
        assert changed == {"pfm2.class", "dw.class", "cp.class"}
        os.replace(target, path)
    finally:
        if os.path.exists(target):
            os.unlink(target)


if __name__ == "__main__":
    main(sys.argv[1])
