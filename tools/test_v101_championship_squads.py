#!/usr/bin/env python3
"""Verify Championship squad depth, player identities and source evidence."""
import csv
import re
import sys
from collections import Counter
from datetime import date
from pathlib import Path

from test_v100_second_divisions import name_key


def read(path):
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source, delimiter="\t"))


def main(assets):
    assets = Path(assets)
    clubs = {c["id"] for c in read(assets / "second_divisions_2010.tsv") if c["division"] == "en2"}
    players = read(assets / "championship_squads_2010.tsv")
    world = {}
    for file in ("world_v42.tsv", "europe_world_v78.tsv"):
        for p in read(assets / file):
            if p["type"] == "P":
                uid = p.get("uid") or ":".join(p[k] for k in ("league", "clubIndex", "playerIndex"))
                world[uid] = p
    assert len(players) == len({p["uid"] for p in players}) == len({p["espn_id"] for p in players}) == 518
    assert len({(name_key(p["name"]), p["dob"]) for p in players}) == 518
    by_club = Counter(p["club_id"] for p in players)
    keepers = Counter(p["club_id"] for p in players if p["position"] == "GK")
    assert set(by_club) == clubs and set(keepers) == clubs
    assert min(by_club.values()) >= 18 and min(keepers.values()) >= 2
    linked = []
    for p in players:
        birth_date, first_seen = (date.fromisoformat(p[k]) for k in ("dob", "first_seen"))
        assert date(2010, 8, 6) <= first_seen <= date(2010, 8, 28)
        assert 16 <= (first_seen - birth_date).days // 365 <= 43
        assert p["position"] in {"GK", "DF", "MF", "FW"} and p["nationality"]
        assert 45 <= int(p["overall"]) <= 85 and re.fullmatch(r"\d+", p["match_id"])
        if p["uid"].startswith("espn:"):
            assert p["uid"] == "espn:" + p["espn_id"] and p["uid"] not in world
        else:
            assert p["uid"] in world
            linked.append(p)
    assert len(linked) == 12 and sum(bool(p["fifa11_anchor"]) for p in players) == 412
    expected = {"22480": ("Jon Stead", "en2:9"), "71578": ("Ross McCormack", "en2:3"),
                "32915": ("Seyi Olofinjana", "en2:8")}
    for p in players:
        if p["espn_id"] in expected:
            assert (p["name"], p["club_id"]) == expected.pop(p["espn_id"])
    assert not expected
    print("PASS: 518 August squad records, 24 clubs, at least two keepers per club, 12 reused player IDs")


if __name__ == "__main__":
    main(sys.argv[1])
