#!/usr/bin/env python3
"""Connect the Championship's legacy attendance and settlement to its club data."""
import sys
import zipfile
from pathlib import Path
from struct import pack

from v31_policy_stats_hooks import add_methodref


HOOKS = {
    "bb.class": [
        ("1a 1b b8 00 54 b8 00 56 3d", "aiDemand", "(II)I"),
        ("b8 00 3e b8 00 54 59 59 36 05", "demand", "(II)I"),
    ],
    "ca.class": [("1b b8 02 6d b8 02 6e", "settle", "(I)V")],
}


def main(path):
    path = Path(path)
    with zipfile.ZipFile(path) as source:
        contents = [(item, source.read(item)) for item in source.infolist()]
    temporary = path.with_suffix(".v112.tmp")
    try:
        with zipfile.ZipFile(temporary, "w") as target:
            for item, data in contents:
                for pattern, method, descriptor in HOOKS.get(item.filename, ()):
                    original = bytes.fromhex(pattern)
                    assert data.count(original) == 1, (item.filename, method, pattern)
                    data, index = add_methodref(data, "PfmChampionshipEconomy", method, descriptor)
                    offset = 1 if method == "settle" else 2 if method == "aiDemand" else 3
                    replacement = original[:offset] + pack(">BH", 0xb8, index) + original[offset + 3:]
                    data = data.replace(original, replacement)
                target.writestr(item, data)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    main(sys.argv[1])
