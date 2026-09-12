#!/usr/bin/env python3
"""Tune the existing fatigue-based selection score for broader season rotation."""
import io
import os
import struct
import sys
import tempfile
import zipfile


def patch_class(data):
    buf = bytearray(data)
    pos = 8

    def u1():
        nonlocal pos
        value = buf[pos]
        pos += 1
        return value

    def u2():
        nonlocal pos
        value = struct.unpack_from(">H", buf, pos)[0]
        pos += 2
        return value

    def u4():
        nonlocal pos
        value = struct.unpack_from(">I", buf, pos)[0]
        pos += 4
        return value

    cp = [None] * u2()
    index = 1
    while index < len(cp):
        tag = u1()
        if tag == 1:
            size = u2()
            cp[index] = bytes(buf[pos:pos + size]).decode("utf-8")
            pos += size
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag == 15:
            pos += 3
        else:
            raise RuntimeError("unsupported class constant tag %d" % tag)
        index += 1

    def skip_attributes():
        nonlocal pos
        for _ in range(u2()):
            u2()
            size = u4()
            pos += size

    pos += 6
    interface_count = u2()
    pos += 2 * interface_count
    field_count = u2()
    for _ in range(field_count):
        pos += 6
        skip_attributes()

    found = False
    method_count = u2()
    for _ in range(method_count):
        pos += 2
        name = cp[u2()]
        u2()
        attributes = u2()
        for _ in range(attributes):
            attr_name = cp[u2()]
            attr_len = u4()
            attr_start = pos
            if name == "starts" and attr_name == "Code":
                code_len = struct.unpack_from(">I", buf, pos + 4)[0]
                code_start = pos + 8
                code_end = code_start + code_len
                code = bytes(buf[code_start:code_end])
                if code.count(b"\x10\x19") != 2 or code.count(b"\x10\x06") != 1:
                    raise RuntimeError("unexpected pfmCondition60.starts bytecode")
                code = code.replace(b"\x10\x19", b"\x10\x14")
                code = code.replace(b"\x10\x06", b"\x10\x08")
                buf[code_start:code_end] = code
                found = True
            pos = attr_start + attr_len
    if not found:
        raise RuntimeError("pfmCondition60.starts Code attribute not found")
    return bytes(buf)


def main(path):
    with zipfile.ZipFile(path, "r") as source:
        entries = [(item, source.read(item.filename)) for item in source.infolist()]
    replaced = False
    fd, temporary = tempfile.mkstemp(prefix="pfm-v27-", suffix=".jar", dir=os.path.dirname(path) or ".")
    os.close(fd)
    try:
        with zipfile.ZipFile(temporary, "w") as target:
            for item, content in entries:
                if item.filename == "pfmCondition60.class":
                    content = patch_class(content)
                    replaced = True
                target.writestr(item, content)
        if not replaced:
            raise RuntimeError("pfmCondition60.class not found")
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print("v27 rotation: fatigue threshold 25->20, selection penalty 6->8")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: v27_rotation.py CORE_JAR")
    main(sys.argv[1])
