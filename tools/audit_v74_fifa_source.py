#!/usr/bin/env python3
"""Read-only coverage audit of the supplied roster names against FIFA 11 FUT data."""
import csv
import re
import sys
import unicodedata
from collections import defaultdict


def key(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]", "", s)


clubs = list(csv.DictReader(open(sys.argv[1], encoding="utf-8"), delimiter="\t"))
names = list(csv.DictReader(open(sys.argv[2], encoding="utf-8"), delimiter="\t"))
fut = list(csv.DictReader(open(sys.argv[3], encoding="utf-8-sig")))
fut_clubs = defaultdict(list)
for row in fut:
    fut_clubs[key(row["CLUB"])].append(row)
roster_by_club = defaultdict(set)
for row in names:
    roster_by_club[row["club_id"]].add(row["identity_key"])
club_map = {}
overlap_matches = []
for club in clubs:
    target = key(club["name"])
    matches = [k for k in fut_clubs if target == k or target in k or k in target]
    if len(matches) == 1:
        club_map[club["id"]] = matches[0]
        continue
    ranked = []
    for fut_name, players in fut_clubs.items():
        intersection = len(roster_by_club[club["id"]] & {key(p["NAME"]) for p in players})
        if intersection:
            ranked.append((intersection, fut_name))
    ranked.sort(reverse=True)
    if ranked and ranked[0][0] >= 5 and (len(ranked) == 1 or ranked[0][0] >= ranked[1][0] + 3):
        club_map[club["id"]] = ranked[0][1]
        overlap_matches.append((club["name"], ranked[0]))

counts = defaultdict(int)
unmatched_clubs = defaultdict(int)
for row in names:
    club = club_map.get(row["club_id"])
    if club is None:
        counts["club_unmatched"] += 1
        unmatched_clubs[row["club_id"]] += 1
        continue
    candidates = [x for x in fut_clubs[club] if key(x["NAME"]) == row["identity_key"]]
    if len(candidates) == 1:
        counts["exact_name"] += 1
    elif len(candidates) > 1:
        counts["exact_duplicate_cards"] += 1
    else:
        counts["name_unmatched"] += 1
print("FUT rows:", len(fut), "clubs:", len(fut_clubs))
print("European clubs matched:", len(club_map), "/", len(clubs))
print("Overlap-based club matches:", overlap_matches)
print("Name coverage:", dict(counts))
print("Unmatched club ids:", sorted(unmatched_clubs))
