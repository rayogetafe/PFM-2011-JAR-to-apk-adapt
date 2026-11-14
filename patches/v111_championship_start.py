#!/usr/bin/env python3
"""Connect the English career setup and club header to the Championship."""
import os
import struct
import sys
import tempfile
import zipfile

from v31_policy_stats_hooks import add_methodref


HOOKS = {
    # ea: after loading the country's initial league, route the career club picker.
    "ea.class": ("2a 07 b6 00 67 b1", "chooseStartingClub", "(Lea;B)V", "2a 07", "b1"),
    # dc: enter the normal new-club flow with the selected division already installed.
    "dc.class": ("b8 00 2c b8 00 a6", "initializeStartingClub", "(B)Ldw;", "", "00 00 00"),
    # db: the club header is also built when loading a saved Championship career.
    "db.class": ("10 09 60 1b 60 b8 00 6b b7 00 6f",
                 "boardBadge", "(I)Lpfm/android/compat/lcdui/Image;", "10 09 60 1b 60", "b7 00 6f"),
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
                if item.filename in HOOKS:
                    old, method, descriptor, prefix, suffix = HOOKS[item.filename]
                    original = bytes.fromhex(old)
                    assert data.count(original) == 1, (item.filename, method)
                    data, ref = add_methodref(data, "PfmEnglishSeasonBridge", method, descriptor)
                    replacement = bytes.fromhex(prefix) + struct.pack(">BH", 0xb8, ref) + bytes.fromhex(suffix)
                    assert len(original) == len(replacement), method
                    data = data.replace(original, replacement)
                    seen.add(item.filename)
                target.writestr(item, data)
        assert seen == set(HOOKS)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


if __name__ == "__main__":
    main(sys.argv[1])
