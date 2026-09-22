#!/usr/bin/env python3
"""Regression checks for the conservative v77 season-roster reconciliation."""
import csv
from collections import Counter, defaultdict
from pathlib import Path

from build_v75_verified_world import EXPLICIT_ALIASES, identity_match, key

ROOT = Path(__file__).resolve().parents[1]


def rows(path):
    with (ROOT / path).open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


assert not identity_match("Steven Smith", "Steven Naismith")
assert EXPLICIT_ALIASES[("eu_netherlands_feyenoord", "fedorsmolov")] == "fjodorsmolov"

old = [row for row in rows("overrides/app/src/main/assets/europe_world_v76.tsv") if row["type"] == "P"]
new = [row for row in rows("overrides/app/src/main/assets/europe_world_v77.tsv") if row["type"] == "P"]
assert len(old) == 2584 and len(new) == 2583
assert {row["uid"] for row in old} - {row["uid"] for row in new} == {"eu:marioselia"}
assert not ({row["uid"] for row in new} - {row["uid"] for row in old})
assert len({row["uid"] for row in new}) == len(new)
by_club = defaultdict(list)
for row in new:
    by_club[(row["league"], row["clubIndex"])].append(row)
assert len(by_club) == 129
assert all(13 <= len(squad) <= 30 and any(p["position"] == "0" for p in squad)
           for squad in by_club.values())
assert all(len({key(p["player"]) for p in squad}) == len(squad)
           for squad in by_club.values())
timisoara = {row["player"]: row for row in new if row["club"] == "FC Timișoara"}
assert (timisoara["Pedro Taborda"]["age"], timisoara["Pedro Taborda"]["position"]) == ("32", "0")
assert (timisoara["Alexandru Bourceanu"]["age"], timisoara["Alexandru Bourceanu"]["position"]) == ("25", "2")
assert (timisoara["Ianis Zicu"]["age"], timisoara["Ianis Zicu"]["position"]) == ("26", "3")
assert [row["player"] for row in new if row["club"] == "APOEL"].count("Marios Ilia") == 1
audit = rows("tools/v77_roster_audit.tsv")
statuses = Counter(row["status"] for row in audit)
assert statuses["UNVERIFIED_SOURCE_GAP"] == 279
assert statuses["SEASON_ROSTER_CONFIRMED"] == 7
print("v77 roster OK:", len(new), "players;", len(by_club), "clubs;", dict(statuses))
