#!/usr/bin/env python3
"""Apply the audited 2010/11 age, market-value and wage table to all five leagues."""
from __future__ import annotations
import csv, os, sys, tempfile, zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from v41_roster_expansion import RESOURCE_TO_LEAGUE, parse_league, serialize_league

CODE={"England":"en","Spain":"es","Italy":"it","Germany":"de","France":"fr"}

def main(jar_name,table_name):
    jar=Path(jar_name); rows=list(csv.DictReader(Path(table_name).open(encoding="utf-8"),delimiter="\t"))
    lookup={(r["league"],r["club"],int(r["playerIndex"]),r["player"]):r for r in rows}
    if len(lookup)!=2281: raise RuntimeError(f"expected 2281 economy rows, got {len(lookup)}")
    with zipfile.ZipFile(jar) as z: entries=[(e,z.read(e.filename)) for e in z.infolist()]
    handle,tmp=tempfile.mkstemp(prefix="pfm-v51-",suffix=".jar",dir=jar.parent);os.close(handle);changed=0
    try:
        with zipfile.ZipFile(tmp,"w") as out:
            for entry,data in entries:
                if entry.filename in RESOURCE_TO_LEAGUE:
                    league=RESOURCE_TO_LEAGUE[entry.filename];code=CODE[league];teams,players,trailer=parse_league(data)
                    for team in teams:
                        for pid in team["roster"]:
                            p=players[pid];key=(code,team["name"],pid,p["name"])
                            if key not in lookup: raise RuntimeError(f"missing economy row {key}")
                            row=lookup[key];p["attrs"][0]=int(row["age"]);p["money"][2]=int(row["value"]);p["money"][0]=int(row["wage"]);changed+=1
                    data=serialize_league(teams,players,trailer)
                out.writestr(entry,data)
        os.replace(tmp,jar)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    if changed!=2281:raise RuntimeError(f"patched {changed}, expected 2281")
    print("v51 player economy: 2281 ages, database values and wages applied")

if __name__=="__main__":
    if len(sys.argv)!=3:raise SystemExit("usage: v51_player_economy.py CORE_JAR PLAYER_ECONOMY_TSV")
    main(sys.argv[1],sys.argv[2])
