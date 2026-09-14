#!/usr/bin/env python3
"""Promote real 2010/11 wide/second forwards to FW in shallow squads.

PFM's league files already allocate 25 roster slots and the transfer engine
already accepts squads up to 25.  The shipped database contains exactly 20
players per club, however, and several attacking players are classified as MID,
leaving three-forward formations without a substitute.  This first roster
rebalance changes positions only; it does not invent players or alter ratings,
prices, wages, team ownership, or save serialization.
"""
from __future__ import annotations

import os
from pathlib import Path
import struct
import sys
import tempfile
import zipfile


PROMOTIONS = {
    "resources/england.bin": {
        "Tottenham": ["Bale"],
        "Sunderland": ["Reid"],
        "West Ham": ["Boa Morte"],
        "West Brom": ["Tchoyi", "Thomas"],
    },
    "resources/spain.bin": {
        "Real Madrid": ["Di Maria"],
        "Valencia": ["Mata", "Hernandez"],
        "Deportivo": ["Guardado"],
        "Malaga": ["Quincy"],
        "Racing": ["Munitis"],
        "Ath. Bilbao": ["Susaeta"],
        "Zaragoza": ["Lafita"],
        "Hercules": ["Drenthe"],
    },
    "resources/italy.bin": {
        "Genoa": ["Sculli"],
        "Fiorentina": ["Jovetic", "Cerci"],
        "Catania": ["Gomez"],
        "Cesena": ["Giaccherini", "Schelotto"],
    },
    "resources/germany.bin": {
        "Schalke 04": ["Farfan"],
        "Werder": ["Marin", "Hunt"],
        "Leverkusen": ["Sam"],
        "Dortmund": ["Kagawa"],
        "Hamburg": ["Elia"],
        "Mainz 05": ["Holtby"],
        "M'gladbach": ["Reus"],
        "Freiburg": ["Iashvili", "Caligiuri"],
        "K'lautern": ["Ilicevic", "Amri"],
    },
    "resources/france.bin": {
        "Lorient": ["Monnet-Paquet"],
        "Paris SG": ["Giuly"],
        "Nice": ["Mounier"],
        "Brest": ["Poyet"],
    },
}


class Reader:
    def __init__(self, data: bytearray):
        self.data = data
        self.pos = 0

    def take(self, size: int) -> bytes:
        out = self.data[self.pos:self.pos + size]
        if len(out) != size:
            raise EOFError((self.pos, size, len(out)))
        self.pos += size
        return bytes(out)

    def u8(self) -> int:
        return self.take(1)[0]

    def i8(self) -> int:
        return struct.unpack(">b", self.take(1))[0]

    def i16(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def skip_text(self) -> str:
        return self.take(self.u8()).decode("iso-8859-1").replace("\n", " ").strip()


def patch_league(raw: bytes, expected: dict[str, list[str]]) -> bytes:
    data = bytearray(raw)
    r = Reader(data)
    teams = []
    for _ in range(r.i8()):
        name = r.skip_text()
        r.skip_text()
        r.take(1 + 1 + 2 + 4 + 4 + 2 + 1 + 1)
        roster = [r.i16() & 0xffff for _ in range(r.i8())]
        r.take(2 * r.i8())
        r.take(2 + 4 * 4 + 1 + 4 + 6)
        r.take(r.u8())
        teams.append((name, set(roster)))

    players = []
    for player_id in range(r.i16()):
        name = r.skip_text()
        r.take(1)  # ci.a
        position_offset = r.pos
        position = r.i8()  # ci.b: 0 GK, 1 DEF, 2 MID, 3 FW
        r.take(4 + 12 + 4)
        players.append((name, position, position_offset))

    team_by_name = {name: roster for name, roster in teams}
    changes = []
    for team_name, names in expected.items():
        if team_name not in team_by_name:
            raise RuntimeError(f"team missing: {team_name}")
        roster = team_by_name[team_name]
        for player_name in names:
            matches = [(pid, player) for pid, player in enumerate(players)
                       if pid in roster and player[0] == player_name]
            if len(matches) != 1:
                raise RuntimeError(f"{team_name}/{player_name}: expected one roster match, got {len(matches)}")
            player_id, (_, old_position, offset) = matches[0]
            if old_position != 2:
                raise RuntimeError(f"{team_name}/{player_name}: expected MID(2), got {old_position}")
            data[offset] = 3
            changes.append((team_name, player_name, player_id))

    if len(changes) != sum(len(names) for names in expected.values()):
        raise RuntimeError("not all declared promotions were applied")
    print("  " + ", ".join(f"{team}/{player}" for team, player, _ in changes))
    return bytes(data)


def main(path: str) -> None:
    jar = Path(path)
    with zipfile.ZipFile(jar, "r") as source:
        entries = [(entry, source.read(entry.filename)) for entry in source.infolist()]
    handle, temp_name = tempfile.mkstemp(prefix="pfm-v40-", suffix=".jar", dir=jar.parent)
    os.close(handle)
    seen = set()
    try:
        with zipfile.ZipFile(temp_name, "w") as target:
            for entry, content in entries:
                if entry.filename in PROMOTIONS:
                    print(entry.filename)
                    content = patch_league(content, PROMOTIONS[entry.filename])
                    seen.add(entry.filename)
                target.writestr(entry, content)
        if seen != set(PROMOTIONS):
            raise RuntimeError(f"missing league resources: {sorted(set(PROMOTIONS) - seen)}")
        os.replace(temp_name, jar)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)
    print("v40 roster depth: 36 historical wide/second forwards reclassified as FW")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: v40_roster_depth.py CORE_JAR")
    main(sys.argv[1])
