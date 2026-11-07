#!/usr/bin/env python3
"""Validate 2010/11 second-division club identities and their bundled crests."""
import csv
import re
import struct
import sys
import unicodedata
from collections import Counter
from pathlib import Path


def name_key(name):
    return re.sub(r"[^a-z0-9]", "", unicodedata.normalize("NFKD", name).lower())


def main(assets):
    assets = Path(assets)
    with (assets / "second_divisions_2010.tsv").open(encoding="utf-8", newline="") as source:
        clubs = list(csv.DictReader(source, delimiter="\t"))
    counts = {"en2": 24, "es2": 22, "it2": 22, "de2": 18, "fr2": 20}
    assert Counter(c["division"] for c in clubs) == counts
    assert {c["id"] for c in clubs} == {f"{league}:{i}" for league, size in counts.items() for i in range(size)}
    assert len({name_key(c["name"]) for c in clubs}) == len({c["source"] for c in clubs}) == len(clubs)
    assert {c["name"]: c["parent_id"] for c in clubs if c["parent_id"]} == {
        "Barcelona B": "es:0",
        "Villarreal B": "es:4",
    }

    current = {}
    for file in ("world_v42.tsv", "europe_world_v78.tsv"):
        with (assets / file).open(encoding="utf-8", newline="") as source:
            for row in csv.reader(source, delimiter="\t"):
                if row[0] == "T":
                    current[f"{row[1]}:{row[2]}"] = row[3]
    assert current["es:0"] == "Barcelona" and current["es:4"] == "Villarreal"
    assert {name_key(name) for name in current.values()}.isdisjoint(name_key(c["name"]) for c in clubs)
    assert current.keys().isdisjoint(c["id"] for c in clubs)

    badges = assets / "second_division_badges"
    assert {p.name for p in badges.iterdir()} == {c["badge"] for c in clubs}
    for c in clubs:
        assert c["id"].startswith(c["division"] + ":")
        assert c["badge"] == c["id"].replace(":", "_") + ".png"
        with (badges / c["badge"]).open("rb") as png:
            assert png.read(16) == b"\x89PNG\r\n\x1a\n\x00\x00\x00\x0dIHDR"
            width, height = struct.unpack(">II", png.read(8))
            assert 0 < width <= 256 and 0 < height <= 256
    print("PASS: 106 clubs and badge assets, five divisions, unique IDs, no active world collisions and two parent links")


if __name__ == "__main__":
    main(sys.argv[1])
