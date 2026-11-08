#!/usr/bin/env python3
"""Attach domestic checkpoints to the original save and load calls."""
import os
import struct
import sys
import tempfile
import zipfile

from v31_policy_stats_hooks import add_methodref


TARGETS = {
    "i.class": (
        (85, "add", "(Lpfm/android/compat/rms/RecordStore;[BII)I"),
        (90, "set", "(Lpfm/android/compat/rms/RecordStore;I[BII)V"),
    ),
    "dy.class": ((152, "read", "(Lpfm/android/compat/rms/RecordStore;I)[B"),),
}


def main(path):
    with zipfile.ZipFile(path) as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    fd, target = tempfile.mkstemp(prefix="pfm-v107-", suffix=".jar", dir=os.path.dirname(path) or ".")
    os.close(fd)
    seen = set()
    try:
        with zipfile.ZipFile(target, "w") as output:
            for item, data in entries:
                if item.filename == "dd.class":
                    data = allow_canvas_shutdown(data)
                    seen.add(item.filename)
                if item.filename in TARGETS:
                    for old, name, descriptor in TARGETS[item.filename]:
                        call = b"\xb6" + struct.pack(">H", old)
                        assert data.count(call) == 1, (item.filename, name, "legacy save call changed")
                        data, ref = add_methodref(data, "PfmDomesticReplay", name, descriptor)
                        data = data.replace(call, b"\xb8" + struct.pack(">H", ref), 1)
                    seen.add(item.filename)
                output.writestr(item, data)
        assert seen == set(TARGETS) | {"dd.class"}
        os.replace(target, path)
    finally:
        if os.path.exists(target):
            os.unlink(target)


def allow_canvas_shutdown(data):
    initial = bytes.fromhex("2b b6 00 50 9a 00 10 14 00 12 b8 00 5a a7 ff f3 57 a7 ff ef")
    frame = bytes.fromhex("2a b6 00 50 99 ff fc")
    assert data.count(initial) == data.count(frame) == 1, "legacy game loop changed"
    data, ref = add_methodref(data, "dd", "pfmAwaitVisibleFrame", "()Z")
    call = b"\xb6" + struct.pack(">H", ref)
    # Both false branches target the existing return at the end of dd.h().
    # The original handler at byte 18 remains in place.
    data = data.replace(initial, b"\x2b" + call + bytes.fromhex("99 01 99 a7 00 0d") + b"\x00" * 6 + initial[-4:])
    return data.replace(frame, b"\x2a" + call + bytes.fromhex("99 01 77"))


if __name__ == "__main__":
    main(sys.argv[1])
