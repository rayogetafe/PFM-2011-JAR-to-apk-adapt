#!/usr/bin/env python3
"""Validate the 2010/11 UCL group manifest and six-round double round-robin."""
import csv
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "overrides/app/src/main/assets"
with (ASSETS / "europe_ucl_groups_2010.tsv").open(encoding="utf-8", newline="") as source:
    entries = list(csv.DictReader(source, delimiter="\t"))
groups = defaultdict(list)
for entry in entries:
    groups[entry["group"]].append(entry["clubId"])
assert list(groups) == list("ABCDEFGH")
assert all(len(clubs) == 4 and len(set(clubs)) == 4 for clubs in groups.values())
assert len({entry["clubId"] for entry in entries}) == 32

with (ASSETS / "europe_world_v78.tsv").open(encoding="utf-8", newline="") as source:
    external = {row["league"] + ":" + row["clubIndex"]: row["club"]
                for row in csv.DictReader(source, delimiter="\t") if row["type"] == "T"}
assert all(id in external for entry in entries if (id := entry["clubId"]).startswith("eu_"))
assert groups["A"] == ["it:0", "de:2", "en:3", "eu_netherlands:2"]
assert groups["D"] == ["es:0", "eu_greece:5", "eu_denmark:2", "eu_russia:5"]
assert external[groups["E"][3]] == "CFR Cluj"
assert external[groups["F"][2]] == "Spartak Moscow"
assert external[groups["G"][2]] == "Ajax"

rounds = ((0, 3, 1, 2), (2, 0, 3, 1), (0, 1, 2, 3),
          (1, 0, 3, 2), (3, 0, 2, 1), (0, 2, 1, 3))
fixtures = Counter((round_[i], round_[i + 1]) for round_ in rounds for i in (0, 2))
assert len(fixtures) == 12
assert all(fixtures[(a, b)] == 1 for a in range(4) for b in range(4) if a != b)
print("v79 UCL groups OK: 32 clubs, 8 groups, 96 scheduled matches")
