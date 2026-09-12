#!/usr/bin/env python3
"""Slightly raise the deterministic three-sub-era selection probability."""
import os
import sys
import tempfile
import zipfile


def patch_class(data):
    # The two-byte BIPUSH 52 instruction occurs exactly once in this class and
    # is the first instruction of chance(). Keep the assertion strict so a
    # future core can never be patched ambiguously.
    old = b"\x10\x34"
    if data.count(old) != 1:
        raise RuntimeError("unexpected pfmPatch32 BIPUSH 52 count")
    return data.replace(old, b"\x10\x3a")


def main(path):
    with zipfile.ZipFile(path, "r") as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    fd, temporary = tempfile.mkstemp(prefix="pfm-v28-", suffix=".jar", dir=os.path.dirname(path) or "."); os.close(fd)
    replaced = False
    try:
        with zipfile.ZipFile(temporary, "w") as target:
            for item, content in entries:
                if item.filename == "pfmPatch32.class": content = patch_class(content); replaced = True
                target.writestr(item, content)
        if not replaced: raise RuntimeError("pfmPatch32.class not found")
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary): os.unlink(temporary)
    print("v28 substitutions: base selection chance 52->58 (three-player cap preserved)")


if __name__ == "__main__":
    if len(sys.argv) != 2: raise SystemExit("usage: v28_sub_frequency.py CORE_JAR")
    main(sys.argv[1])
