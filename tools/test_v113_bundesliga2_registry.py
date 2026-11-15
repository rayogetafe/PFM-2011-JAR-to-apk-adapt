#!/usr/bin/env python3
"""Check the historical 2. Bundesliga registry before packaging the Android assets."""
import sys
from collections import Counter
from datetime import date
from pathlib import Path

from test_v100_second_divisions import name_key
from test_v101_championship_squads import read


def main(assets):
    clubs = {c["id"]: c["name"] for c in read(assets / "second_divisions_2010.tsv") if c["division"] == "de2"}
    players = read(assets / "bundesliga2_squads_2010.tsv")
    profiles = read(assets / "club_economy_2010.tsv")
    german = {p["club_id"]: p for p in profiles if p["club_id"].startswith("de2:")}
    assert len(profiles) == len({p["club_id"] for p in profiles})
    assert len(clubs) == 18 and set(clubs) == set(german)
    assert len(players) == len({p["uid"] for p in players}) == 510
    assert len(players) == len({(name_key(p["name"]), p["dob"]) for p in players})
    assert sum(bool(p["fifa11_anchor"]) for p in players) == 387
    assert set(p["club_id"] for p in players) == set(clubs)

    depth = Counter((p["club_id"], p["position"]) for p in players)
    for club, profile in german.items():
        assert profile["stadium"] and 0 < int(profile["average_attendance"]) <= int(profile["capacity"])
        assert depth[club, "GK"] >= 2 and depth[club, "DF"] >= 5 and depth[club, "MF"] >= 5 and depth[club, "FW"] >= 3
    assert german["de2:11"]["stadium"] == "Olympiastadion Berlin"
    assert int(german["de2:11"]["capacity"]) == 74500
    mean_attendance = sum(int(p["average_attendance"]) for p in german.values()) / len(clubs)
    assert abs(mean_attendance - 14539) < 300  # DFL's league-wide average uses a different attendance series.

    for player in players:
        age = (date(2010, 8, 20) - date.fromisoformat(player["dob"])).days / 365.2425
        assert 15 <= age < 44
        assert player["uid"].startswith("dfb:") and player["position"] in {"GK", "DF", "MF", "FW"}
        assert 50 <= int(player["overall"]) <= 85
        assert 0 <= int(player["opening_starts"]) <= int(player["opening_squads"]) <= 4
        if player["fifa11_anchor"]:
            assert 40 <= int(player["fifa11_anchor"]) <= 85

    by_id = {p["uid"]: p for p in players}
    assert by_id["dfb:27049"]["club_id"] == "de2:5"  # Augsburg -> Duisburg in August
    assert {"dfb:41799", "dfb:10660", "dfb:28292"}.isdisjoint(by_id)  # September signings
    assert any(p["name"] == "Nils Petersen" and p["club_id"] == "de2:6" for p in players)
    print("PASS: 18 second Bundesliga clubs with stadium profiles, 510 August squad entries and 387 FIFA 11 ratings")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
