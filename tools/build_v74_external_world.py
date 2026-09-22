#!/usr/bin/env python3
"""Build a deterministic, single-owner 2010/11 European player layer.

FIFA 11 FUT ratings/positions are used only for exact club-and-name matches.
Missing gameplay fields are estimates, never represented as historical facts.
"""
import argparse
import csv
import hashlib
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path


def key(text):
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]", "", text)


def stable(text, mod):
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest()[:12], 16) % mod


def group_position(value):
    value = value.upper()
    if value == "GK": return 0
    if value in {"CB", "LB", "RB", "LWB", "RWB"}: return 1
    if value in {"ST", "CF", "LW", "RW", "LF", "RF"}: return 3
    return 2


def main():
    ap = argparse.ArgumentParser()
    for name in ("clubs", "rosters", "topfive", "fifa", "output"):
        ap.add_argument(name, type=Path)
    args = ap.parse_args()
    with args.clubs.open(encoding="utf-8", newline="") as f: clubs = list(csv.DictReader(f, delimiter="\t"))
    with args.rosters.open(encoding="utf-8", newline="") as f: entries = list(csv.DictReader(f, delimiter="\t"))
    with args.topfive.open(encoding="utf-8", newline="") as f: topfive = {key(r["player"]) for r in csv.DictReader(f, delimiter="\t")}
    with args.fifa.open(encoding="utf-8-sig", newline="") as f: fifa = list(csv.DictReader(f))
    by_club = defaultdict(list)
    for entry in entries: by_club[entry["club_id"]].append(entry)
    fifa_clubs = defaultdict(list)
    for card in fifa: fifa_clubs[key(card["CLUB"])].append(card)
    # Name-overlap matching identifies historical aliases such as FC København.
    maps = {}
    for club in clubs:
        identity = key(club["name"])
        candidate = [x for x in fifa_clubs if identity == x or identity in x or x in identity]
        if len(candidate) == 1:
            maps[club["id"]] = candidate[0]
            continue
        draft = {r["identity_key"] for r in by_club[club["id"]]}
        ranked = sorted(((len(draft & {key(card["NAME"]) for card in cards}), name)
                         for name, cards in fifa_clubs.items()), reverse=True)
        if ranked and ranked[0][0] >= 5 and (len(ranked) == 1 or ranked[0][0] >= ranked[1][0] + 3):
            maps[club["id"]] = ranked[0][1]
    cards = defaultdict(list)
    for club_id, fifa_id in maps.items():
        for card in fifa_clubs[fifa_id]: cards[(club_id, key(card["NAME"]))].append(card)
    # One identity has at most one opening-day owner. Ambiguous rows are held out.
    occurrences = defaultdict(list)
    for row in entries: occurrences[row["identity_key"]].append(row)
    chosen = {}
    skipped = Counter()
    for identity, rows in occurrences.items():
        if identity in topfive:
            skipped["top_five_name_collision"] += len(rows)
            continue
        rows = [r for r in rows if r["review_status"] != "DATE_CONFLICT"]
        if not rows:
            skipped["date_conflict"] += 1
            continue
        if len(rows) == 1:
            chosen[identity] = rows[0]
            continue
        supported = [r for r in rows if cards[(r["club_id"], identity)]]
        if len(supported) == 1:
            chosen[identity] = supported[0]
            skipped["cross_club_resolved"] += len(rows) - 1
        else:
            skipped["cross_club_held"] += len(rows)
    selected = defaultdict(list)
    for row in chosen.values(): selected[row["club_id"]].append(row)
    by_country = defaultdict(list)
    for club in clubs: by_country[club["country"]].append(club)
    lines = [["type", "league", "clubIndex", "club", "playerIndex", "player", "position", "age", "overall", "nationality", "speed", "resistance", "quality", "morale", "style", "formation", "value", "wage", "uid", "provenance"]]
    stats = Counter()
    sizes = []
    for country in sorted(by_country):
        code = "eu_" + key(country)
        for ti, club in enumerate(by_country[country]):
            rows = sorted(selected[club["id"]], key=lambda r: entries.index(r))
            sizes.append(len(rows))
            lines.append(["T", code, ti, club["name"], -1, "", -1, club["active_from"], 0, country, 0, 0, 0, 55, 55, 2, 0, 0, code+":"+str(ti), "REGISTRY"])
            for pi, row in enumerate(rows):
                identity = row["identity_key"]
                available = cards[(club["id"], identity)]
                # Multiple FUT cards for the same name/club represent variants, not players.
                regular = [c for c in available if c["TIER"].lower() in {"gold", "silver", "bronze"}]
                card = max(regular or available, key=lambda c: int(c["RATING"])) if available else None
                if card:
                    pos = group_position(card["POSITION"])
                    ovr = int(card["RATING"])
                    spe = max(1, min(99, int(card["PACE"])))
                    res = max(1, min(99, int(card["PHYSICAL"])))
                    qua = max(1, min(99, (int(card["PASSING"])+int(card["DRIBBLING"])+ovr)//3))
                    provenance = "FIFA11_RATING_EST_AGE_ECONOMY"
                    stats["fifa_rating"] += 1
                else:
                    # Research list is normally GK, defenders, midfielders, forwards.
                    pos = 0 if pi < 2 else 1 if pi < 8 else 2 if pi < 15 else 3
                    ovr = min(82, 60 + int(club["rep"])*2 + stable(identity, 9) - 4)
                    spe = max(45, min(90, ovr + stable(identity+"speed", 19) - 9))
                    res = max(45, min(90, ovr + stable(identity+"stamina", 15) - 7))
                    qua = max(45, min(90, ovr + stable(identity+"quality", 13) - 6))
                    provenance = "ESTIMATED"
                    stats["estimated_rating"] += 1
                age = 20 + stable(identity+"age", 14)
                value = max(250000, ((ovr-50)**2)*9000)
                wage = max(50000, (value//18//10000)*10000)
                uid = "eu:"+identity
                lines.append(["P", code, ti, club["name"], pi, row["player_name"], pos, age, ovr, "Unknown", spe, res, qua, 55, 0, 0, value, wage, uid, provenance])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t", lineterminator="\n")
        writer.writerows(lines)
    print(f"clubs={len(clubs)} players={len(chosen)} size_min={min(sizes)} size_max={max(sizes)}")
    print(dict(stats), dict(skipped))
    if len(clubs) != 129 or min(sizes) < 13 or max(sizes) > 30:
        raise SystemExit("External squad-size audit failed")


if __name__ == "__main__": main()
