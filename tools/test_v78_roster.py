#!/usr/bin/env python3
"""Check identity and opening-date corrections without erasing distinct players."""
import csv
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    with (ROOT / path).open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


old = {p["uid"]: p for p in read("overrides/app/src/main/assets/europe_world_v77.tsv") if p["type"] == "P"}
new = {p["uid"]: p for p in read("overrides/app/src/main/assets/europe_world_v78.tsv") if p["type"] == "P"}
assert len(old) == 2583 and len(new) == 2558
assert not (new.keys() - old.keys())
assert len(old.keys() - new.keys()) == 25
assert {"eu:ilsinho", "eu:mariuszlewandowski", "eu:oleksandrrybka"} <= old.keys() - new.keys()

by_club = defaultdict(list)
for p in new.values():
    by_club[(p["league"], p["clubIndex"])].append(p)
assert len(by_club) == 129
assert all(13 <= len(players) <= 30 and any(p["position"] == "0" for p in players)
           for players in by_club.values())
assert new["eu:maxipereira"]["position"] == "1"
assert new["eu:maxipereira"]["provenance"] == "SEASON_APPS_UEFA11_ROLE"
for distinct in (("eu:vasiliyberezutskiy", "eu:alexeyberezutskiy"),):
    assert all(uid in new for uid in distinct)

audit = read("tools/v78_roster_audit.tsv")
statuses = Counter(row["status"] for row in audit)
assert statuses["UNVERIFIED_SOURCE_GAP"] == 255
assert statuses["SEASON_APPS_CONFIRMED"] == 1980
assert statuses["SEASON_APPS_ADDED"] == 316
assert statuses["SEASON_ROSTER_CONFIRMED"] == 7
print("v78 roster OK:", len(new), "players;", len(by_club), "clubs;", dict(statuses))
