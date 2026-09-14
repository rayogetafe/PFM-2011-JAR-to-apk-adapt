#!/usr/bin/env python3
"""Build the v42 player-nationality sidecar from the patched league databases."""
import argparse, csv, re, sys, unicodedata, zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"patches"))
from v41_roster_expansion import RESOURCE_TO_LEAGUE, parse_league

DEFAULT={"England":"England","Spain":"Spain","Italy":"Italy","Germany":"Germany","France":"France"}
ALIASES={"Korea Republic":"South Korea","Côte d'Ivoire":"Ivory Coast","Bosnia Herzegovina":"Bosnia and Herzegovina"}
MANUAL={
"N'Gog":"France","N'Zonzi":"France","Benjani":"Zimbabwe","Lovenkrands":"Denmark","Valles":"Spain","Briggs":"England","O'Brien":"Ireland",
"Lass":"France","Urreta":"Uruguay","Kris":"Brazil","Quincy":"Ghana","Josemi":"Spain","Bakircioglu":"Sweden","Gurpegui":"Spain","Cases":"Spain","N'Daw":"Senegal","K. Femenia":"Spain","Juanmi":"Spain","Ramon Arcas":"Spain","Boutahar":"Morocco",
"Regini":"Italy","Dumitru":"Italy","Zenoni":"Italy","Leonardi":"Italy","Rajcic":"Croatia","Miskiewicz":"Poland","Bentz":"Germany","Larrivey":"Argentina","Ramirez":"Uruguay","Franceschini":"Italy","Salamon":"Poland","Sokratis":"Greece","Camporese":"Italy","Berny":"France","Laneri":"Italy","Paponi":"Italy","Leali":"Italy",
"Kiessling":"Germany","Grosskreutz":"Germany","Kjaer":"Denmark","Gartner":"Germany","Vrancic":"Germany","Kormaz":"Austria","Rode":"Germany","Wome":"Cameroon","Putsila":"Belarus","Chandler":"United States","Petsos":"Greece","Knaller":"Austria","Hornig":"Germany","Drose-Anpen":"Germany","Bicakcic":"Bosnia and Herzegovina","Mendler":"Germany","Plattenhardt":"Germany","Daube":"Germany",
"Krstic":"Serbia","Pedretti":"France","Boly":"France","Souare":"Senegal","Pionnier":"France","Yanga-Mbiwa":"France","Pinard":"France","Nkoulou":"Cameroon","Bonnart":"France","Theophile-Catherine":"France","Luzi":"France","Brahimi":"Algeria","Aurier":"Ivory Coast","Milovanovic":"Serbia","Veselinovic":"Serbia","Helder":"Portugal","N'Diaye":"Senegal","Erding":"Turkey","Makonda":"France","M'Bengue":"Senegal","Abdennour":"Tunisia","Soukouna":"France","Echouafni":"Morocco","Sae":"France","Bayal":"Senegal","Nery":"France","Bosmel":"France","Boucansaud":"France","Nabab":"Guadeloupe","Bertoli":"France","Martial":"France","Puydebois":"France","Ndiaye":"Senegal","Loutre":"France","Maire":"France","Germain":"France","Gnabouyou":"France","Vandam":"France","Lecomte":"France","Bulot":"Gabon","Isimat-Mirin":"France","Queudrue":"France","Ahamada":"France","Regattin":"Morocco","Poujol":"France","Raineau":"France"
}

def norm(s):
    s=unicodedata.normalize("NFKD",s).encode("ascii","ignore").decode().lower()
    return re.sub(r"[^a-z0-9]","",s)

def position_group(value):
    value=(value or "").upper()
    if "GK" in value:return 0
    if any(x in value for x in ("CB","LB","RB","LWB","RWB")):return 1
    if any(x in value for x in ("CM","CDM","CAM","LM","RM")):return 2
    return 3

def main():
    ap=argparse.ArgumentParser();ap.add_argument("jar",type=Path);ap.add_argument("fifa",type=Path);ap.add_argument("output",type=Path);a=ap.parse_args()
    fifa=[]
    with a.fifa.open(encoding="utf-8-sig",newline="") as f:
        for r in csv.DictReader(f):
            try: age=int(r["age"]); rating=int(r["rating"])
            except: continue
            full=norm(r["name"]); last=norm(r["name"].split()[-1])
            fifa.append((full,last,age,rating,position_group(r.get("preferred_positions")),r["nationality"],r.get("club", "")))
    rows=[];matched=0;fallback=0
    with zipfile.ZipFile(a.jar) as z:
        for resource,league in RESOURCE_TO_LEAGUE.items():
            teams,players,_=parse_league(z.read(resource))
            owners={pid:t["name"] for t in teams for pid in t["roster"]}
            for pid,p in enumerate(players):
                if pid not in owners:continue
                key=norm(p["name"]); age=p["attrs"][0]; pos=p["attrs"][1]; ovr=sum(p["attrs"][2:5])//3
                candidates=[]
                for full,last,fa,fr,fp,nat,club in fifa:
                    # The legacy database's age byte is commonly 2–3 years stale;
                    # nationality matching must not repeat that known source defect.
                    if not (last==key or full.endswith(key) or key.endswith(last)):continue
                    score=(80 if last==key else 50)-min(18,abs(fa-age)*2)-abs(fr-ovr)-(0 if fp==pos else 18)
                    tc=norm(owners[pid]); fc=norm(club)
                    if tc and (tc in fc or fc in tc):score+=25
                    candidates.append((score,nat))
                if p["name"] in MANUAL:
                    nat=MANUAL[p["name"]];matched+=1;source="manual-audit"
                elif candidates:
                    candidates.sort(reverse=True);nat=candidates[0][1];matched+=1;source="fifa11"
                else:nat=DEFAULT[league];fallback+=1;source="league-fallback"
                rows.append((league,owners[pid],p["name"],ALIASES.get(nat,nat),source))
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with a.output.open("w",encoding="utf-8",newline="") as f:
        f.write("league\tclub\tplayer\tnationality\tsource\n")
        for row in rows:f.write("\t".join(row)+"\n")
    print(f"players={len(rows)} matched={matched} fallback={fallback} coverage={matched/len(rows):.1%}")

if __name__=="__main__":main()
