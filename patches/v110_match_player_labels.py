#!/usr/bin/env python3
"""Use display labels for the eight legacy match player-name reads."""
import os
import struct
import sys
import tempfile
import zipfile

from v31_policy_stats_hooks import add_methodref


READS = {
    "bm.class": (176, 181, 5),
    "ca.class": (372, 226, 3),
}


def main(path):
    with zipfile.ZipFile(path) as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    fd, temporary = tempfile.mkstemp(dir=os.path.dirname(path) or ".", suffix=".jar")
    os.close(fd)
    seen = set()
    try:
        with zipfile.ZipFile(temporary, "w") as target:
            for item, data in entries:
                if item.filename in READS:
                    name_field, decoder, expected = READS[item.filename]
                    original = struct.pack(">BHBH", 0xb4, name_field, 0xb8, decoder)
                    assert data.count(original) == expected, (item.filename, "match name reads changed")
                    data, ref = add_methodref(data, "PfmMatchPlayerLabel", "label", "(Lci;)Ljava/lang/String;")
                    data = data.replace(original, struct.pack(">BH", 0xb8, ref) + b"\x00\x00\x00")
                    seen.add(item.filename)
                target.writestr(item, data)
        assert seen == set(READS)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


if __name__ == "__main__":
    main(sys.argv[1])
