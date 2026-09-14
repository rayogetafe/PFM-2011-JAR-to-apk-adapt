#!/usr/bin/env python3
"""Route core substitution minutes through pfmSubTiming38 and permit pre-45 changes."""
import os
import struct
import sys
import tempfile
import zipfile


def patch_class(data):
    buf = bytearray(data)
    old_count = struct.unpack_from(">H", buf, 8)[0]
    pos = 10
    cp = [None] * old_count
    offsets = [None] * old_count
    index = 1
    while index < old_count:
        offsets[index] = pos
        tag = buf[pos]
        pos += 1
        if tag == 1:
            size = struct.unpack_from(">H", buf, pos)[0]
            pos += 2
            cp[index] = (tag, bytes(buf[pos:pos + size]).decode("utf-8"))
            pos += size
        elif tag in (3, 4):
            cp[index] = (tag,)
            pos += 4
        elif tag in (5, 6):
            cp[index] = (tag,)
            pos += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[index] = (tag, struct.unpack_from(">H", buf, pos)[0])
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[index] = (tag,) + struct.unpack_from(">HH", buf, pos)
            pos += 4
        elif tag == 15:
            cp[index] = (tag,)
            pos += 3
        else:
            raise RuntimeError("unsupported constant tag %d" % tag)
        index += 1
    cp_end = pos

    def utf(i):
        return cp[i][1] if i and cp[i] and cp[i][0] == 1 else None

    target = None
    for i, entry in enumerate(cp):
        if not entry or entry[0] != 10:
            continue
        _, _, nat = entry
        ne = cp[nat]
        if ne and ne[0] == 12 and utf(ne[1]) == "subMinutes" and utf(ne[2]) == "(II)I":
            target = i
            break
    if target is None:
        raise RuntimeError("subMinutes method reference not found")

    utf_index = old_count
    class_index = old_count + 1
    name = b"pfmSubTiming38"
    appended = b"\x01" + struct.pack(">H", len(name)) + name + b"\x07" + struct.pack(">H", utf_index)
    methodref_offset = offsets[target]
    struct.pack_into(">H", buf, methodref_offset + 1, class_index)

    # The core previously forced every outgoing starter to stay until minute 45.
    # Change only the two BIPUSH 45 instructions inside minutesForRosterIndex.
    scan = cp_end + 6
    interfaces = struct.unpack_from(">H", buf, scan)[0]
    scan += 2 + interfaces * 2

    def skip_members(at, patch_methods=False):
        count = struct.unpack_from(">H", buf, at)[0]
        at += 2
        patched = False
        for _ in range(count):
            name_index = struct.unpack_from(">H", buf, at + 2)[0]
            attributes = struct.unpack_from(">H", buf, at + 6)[0]
            at += 8
            for _ in range(attributes):
                attribute_name = utf(struct.unpack_from(">H", buf, at)[0])
                attribute_length = struct.unpack_from(">I", buf, at + 2)[0]
                payload = at + 6
                if patch_methods and utf(name_index) == "minutesForRosterIndex" and attribute_name == "Code":
                    code_length = struct.unpack_from(">I", buf, payload + 4)[0]
                    code_start = payload + 8
                    code_end = code_start + code_length
                    code = bytes(buf[code_start:code_end])
                    if code.count(b"\x10\x2d") != 2:
                        raise RuntimeError("unexpected minute-45 clamp count in minutesForRosterIndex")
                    buf[code_start:code_end] = code.replace(b"\x10\x2d", b"\x10\x01")
                    patched = True
                at = payload + attribute_length
        return at, patched

    scan, _ = skip_members(scan, False)
    _, clamp_patched = skip_members(scan, True)
    if not clamp_patched:
        raise RuntimeError("minutesForRosterIndex Code attribute not found")
    return bytes(buf[:8]) + struct.pack(">H", old_count + 2) + bytes(buf[10:cp_end]) + appended + bytes(buf[cp_end:])


def main(path):
    with zipfile.ZipFile(path, "r") as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    fd, temporary = tempfile.mkstemp(prefix="pfm-v38-", suffix=".jar", dir=os.path.dirname(path) or ".")
    os.close(fd)
    replaced = False
    try:
        with zipfile.ZipFile(temporary, "w") as target:
            for item, content in entries:
                if item.filename == "pfmSubstitutions.class":
                    content = patch_class(content)
                    replaced = True
                target.writestr(item, content)
        if not replaced:
            raise RuntimeError("pfmSubstitutions.class not found")
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print("v38 substitution timing: early 5%, half-time 10%, normal 63%, late 22%")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: v38_sub_timing.py CORE_JAR")
    main(sys.argv[1])
