#!/usr/bin/env python3
"""Export compact five-league metadata used by the native v42 parallel simulator."""
import argparse, csv, sys, zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"patches"))
from v41_roster_expansion import RESOURCE_TO_LEAGUE,parse_league

CODES={"England":"en","Spain":"es","Italy":"it","Germany":"de","France":"fr"}
def safe(s):return str(s).replace("\t"," ").replace("\r"," ").replace("\n"," ")
def main():
    p=argparse.ArgumentParser();p.add_argument("jar",type=Path);p.add_argument("nationalities",type=Path);p.add_argument("output",type=Path);a=p.parse_args()
    nat={(r["league"],r["club"],r["player"]):r["nationality"] for r in csv.DictReader(a.nationalities.open(encoding="utf-8"),delimiter="\t")}
    lines=["type\tleague\tclubIndex\tclub\tplayerIndex\tplayer\tposition\tage\toverall\tnationality\tspeed\tresistance\tquality\tmorale\tstyle\tformation\tvalue\twage"]
    with zipfile.ZipFile(a.jar) as z:
        for resource,league in RESOURCE_TO_LEAGUE.items():
            teams,players,_=parse_league(z.read(resource));code=CODES[league]
            for ti,t in enumerate(teams):
                # dw.c/d bytes: team style and formation consumed by stock bb.b(dw).
                lines.append("\t".join(("T",code,str(ti),safe(t["name"]),"-1","","-1","0","0","","0","0","0","0",str(t["fixed"][14]),str(t["fixed"][15]),"0","0")))
                for pid in t["roster"]:
                    x=players[pid];overall=sum(x["attrs"][2:5])//3
                    lines.append("\t".join(("P",code,str(ti),safe(t["name"]),str(pid),safe(x["name"]),str(x["attrs"][1]),str(x["attrs"][0]),str(overall),safe(nat[(league,t["name"],x["name"])]),str(x["attrs"][2]),str(x["attrs"][3]),str(x["attrs"][4]),str(x["attrs"][7]),"0","0",str(x["money"][2]),str(x["money"][0]))))
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text("\n".join(lines)+"\n",encoding="utf-8")
    print(f"world asset: {sum(x.startswith('T'+chr(9)) for x in lines)} clubs, {sum(x.startswith('P'+chr(9)) for x in lines)} players")
if __name__=="__main__":main()
