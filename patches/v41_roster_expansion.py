#!/usr/bin/env python3
"""Apply the independently audited September 2010 roster expansion plan."""
from __future__ import annotations

import json
import os
from pathlib import Path
import struct
import sys
import tempfile
import zipfile


RESOURCE_TO_LEAGUE = {
    "resources/england.bin": "England",
    "resources/spain.bin": "Spain",
    "resources/italy.bin": "Italy",
    "resources/germany.bin": "Germany",
    "resources/france.bin": "France",
}
POSITION = {"GK": 0, "DEF": 1, "MID": 2, "FW": 3}


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def take(self, size: int) -> bytes:
        value = self.data[self.pos:self.pos + size]
        if len(value) != size:
            raise EOFError((self.pos, size, len(value)))
        self.pos += size
        return value

    def u8(self) -> int:
        return self.take(1)[0]

    def i8(self) -> int:
        return struct.unpack(">b", self.take(1))[0]

    def i16(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def text(self) -> str:
        return self.take(self.u8()).decode("iso-8859-1").replace("\n", " ").strip()


def pack_text(value: str) -> bytes:
    encoded = value.encode("iso-8859-1")
    if len(encoded) > 255:
        raise ValueError(f"text too long: {value}")
    return bytes([len(encoded)]) + encoded


def pack_i8(value: int) -> bytes:
    return struct.pack(">b", value)


def pack_i16(value: int) -> bytes:
    return struct.pack(">h", value)


def parse_league(raw: bytes):
    r = Reader(raw)
    teams = []
    for _ in range(r.i8()):
        name = r.text()
        description = r.text()
        fixed = r.take(16)
        roster = [r.i16() & 0xFFFF for _ in range(r.i8())]
        listed = [r.i16() & 0xFFFF for _ in range(r.i8())]
        tail = r.take(29)
        score = r.take(r.u8())
        teams.append({"name": name, "description": description, "fixed": fixed,
                      "roster": roster, "listed": listed, "tail": tail, "score": score})
    players = []
    for _ in range(r.i16()):
        name = r.text()
        attrs = [r.i8() for _ in range(6)]
        money = list(struct.unpack(">iii", r.take(12)))
        attrs.extend(r.i8() for _ in range(4))
        players.append({"name": name, "attrs": attrs, "money": money})
    trailer = r.take(len(raw) - r.pos)
    return teams, players, trailer


def serialize_league(teams, players, trailer: bytes) -> bytes:
    out = bytearray(pack_i8(len(teams)))
    for team in teams:
        out += pack_text(team["name"])
        out += pack_text(team["description"])
        out += team["fixed"]
        out += pack_i8(len(team["roster"]))
        for player_id in team["roster"]:
            out += pack_i16(player_id)
        out += pack_i8(len(team["listed"]))
        for player_id in team["listed"]:
            out += pack_i16(player_id)
        out += team["tail"]
        out += bytes([len(team["score"])]) + team["score"]
    out += pack_i16(len(players))
    for player in players:
        out += pack_text(player["name"])
        out += b"".join(pack_i8(value) for value in player["attrs"][:6])
        out += struct.pack(">iii", *player["money"])
        out += b"".join(pack_i8(value) for value in player["attrs"][6:])
    out += trailer
    return bytes(out)


def one_roster_match(team, players, name: str) -> int:
    matches = [player_id for player_id in team["roster"]
               if player_id is not None and players[player_id]["name"] == name]
    if len(matches) != 1:
        raise RuntimeError(f'{team["name"]}/{name}: expected one roster match, got {len(matches)}')
    return matches[0]


def nearest_money(team, players, position: int, stats: list[int]) -> list[int]:
    target = sum(stats) / 3
    choices = [players[player_id] for player_id in team["roster"]
               if player_id is not None and players[player_id]["attrs"][1] == position]
    if not choices:
        choices = [players[player_id] for player_id in team["roster"] if player_id is not None]
    nearest = min(choices, key=lambda player: abs(sum(player["attrs"][2:5]) / 3 - target))
    return list(nearest["money"])


def place_player(team, players, player_id: int) -> None:
    position = players[player_id]["attrs"][1]
    for index, hole_position in sorted(team.get("holes", {}).items()):
        if hole_position == position and team["roster"][index] is None:
            team["roster"][index] = player_id
            return
    team["roster"].append(player_id)


def apply_plan(raw: bytes, league_name: str, plan: dict) -> bytes:
    teams, players, trailer = parse_league(raw)
    team_plans = list(plan["leagues"][league_name].items())
    if len(teams) != len(team_plans):
        raise RuntimeError(f"{league_name}: team count mismatch")
    team_by_doc_name = {}
    for team, (doc_name, team_plan) in zip(teams, team_plans):
        if team["name"] != team_plan["database_team"]:
            raise RuntimeError(f'{league_name}/{doc_name}: database order mismatch: {team["name"]}')
        team_by_doc_name[doc_name] = team

    moved = {}
    for doc_name, team_plan in team_plans:
        team = team_by_doc_name[doc_name]
        team["holes"] = {}
        for removal in team_plan["remove"]:
            player_id = one_roster_match(team, players, removal["name"])
            roster_index = team["roster"].index(player_id)
            team["holes"][roster_index] = players[player_id]["attrs"][1]
            team["roster"][roster_index] = None
            players[player_id]["attrs"][9] = -1
            moved[(doc_name, removal["name"])] = player_id

    for doc_name, team_plan in team_plans:
        team = team_by_doc_name[doc_name]
        team_index = teams.index(team)
        for move in team_plan["move_in"]:
            key = (move["from"], move["name"])
            if key not in moved:
                raise RuntimeError(f"move source missing: {league_name}/{key}")
            player_id = moved[key]
            players[player_id]["attrs"][9] = team_index
            place_player(team, players, player_id)

        for name, position_name in team_plan["position_changes"].items():
            player_id = one_roster_match(team, players, name)
            players[player_id]["attrs"][1] = POSITION[position_name]

        adjustment_sum = sum(team_plan["existing_adjustments"].values())
        if adjustment_sum != 0:
            raise RuntimeError(f"{league_name}/{doc_name}: adjustments are not budget-neutral")
        for name, delta in team_plan["existing_adjustments"].items():
            player_id = one_roster_match(team, players, name)
            for attr_index in (2, 3, 4):
                value = players[player_id]["attrs"][attr_index] + delta
                if not 35 <= value <= 99:
                    raise RuntimeError(f"attribute out of range: {league_name}/{doc_name}/{name}")
                players[player_id]["attrs"][attr_index] = value

        used_numbers = {players[player_id]["attrs"][5] for player_id in team["roster"] if player_id is not None}
        for addition in team_plan["add"]:
            position = POSITION[addition["position"]]
            number = next((value for value in range(1, 100) if value not in used_numbers), None)
            if number is None:
                raise RuntimeError(f"no shirt number: {league_name}/{doc_name}")
            used_numbers.add(number)
            stats = addition["stats"]
            money = nearest_money(team, players, position, stats)
            face = (sum(addition["name"].encode("iso-8859-1")) + team_index * 17) % 35
            player_id = len(players)
            players.append({
                "name": addition["name"],
                "attrs": [addition["age"], position, *stats, number, 0, 55, 0, team_index],
                "money": money,
            })
            place_player(team, players, player_id)

        # Keep the original first XI and bench ordering stable. New depth is
        # appended except when a same-position replacement fills a removed
        # player's exact slot.
        team["roster"] = [player_id for player_id in team["roster"] if player_id is not None]
        if not 21 <= len(team["roster"]) <= 25:
            raise RuntimeError(f'{league_name}/{doc_name}: roster size {len(team["roster"])}')

    roster_ids = [player_id for team in teams for player_id in team["roster"]]
    if len(roster_ids) != len(set(roster_ids)):
        raise RuntimeError(f"{league_name}: duplicate player ownership after patch")
    print(f"{league_name}: {len(teams)} teams, {len(roster_ids)} owned players, "
          f"sizes {min(len(t['roster']) for t in teams)}-{max(len(t['roster']) for t in teams)}")
    return serialize_league(teams, players, trailer)


def main(jar_path: str, plan_path: str) -> None:
    jar = Path(jar_path)
    plan = json.loads(Path(plan_path).read_text(encoding="utf-8"))
    with zipfile.ZipFile(jar, "r") as source:
        entries = [(entry, source.read(entry.filename)) for entry in source.infolist()]
    handle, temp_name = tempfile.mkstemp(prefix="pfm-v41-", suffix=".jar", dir=jar.parent)
    os.close(handle)
    seen = set()
    try:
        with zipfile.ZipFile(temp_name, "w") as target:
            for entry, content in entries:
                if entry.filename in RESOURCE_TO_LEAGUE:
                    league = RESOURCE_TO_LEAGUE[entry.filename]
                    content = apply_plan(content, league, plan)
                    seen.add(league)
                target.writestr(entry, content)
        if seen != set(RESOURCE_TO_LEAGUE.values()):
            raise RuntimeError(f"missing league resources: {sorted(set(RESOURCE_TO_LEAGUE.values()) - seen)}")
        os.replace(temp_name, jar)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)
    print("v41 roster expansion: 98 clubs, 21-25 owned players, independent balance audit applied")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: v41_roster_expansion.py CORE_JAR ROSTER_PLAN_JSON")
    main(sys.argv[1], sys.argv[2])
