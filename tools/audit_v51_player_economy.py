#!/usr/bin/env python3
import csv,statistics,sys
from collections import defaultdict

rows=list(csv.DictReader(open(sys.argv[1],encoding="utf-8"),delimiter="\t")); players=[r for r in rows if r["type"]=="P"]
if len(players)!=2281:raise SystemExit(f"expected 2281 players, got {len(players)}")
clubs=defaultdict(list)
for p in players:
    p.update({k:int(p[k]) for k in ("position","age","overall","value","wage")});clubs[(p["league"],p["club"])].append(p)
ratios=[];not_sale=0
for squad in clubs.values():
    for p in squad:
        same=sorted((x for x in squad if x["position"]==p["position"]),key=lambda x:x["overall"],reverse=True);role=same.index(p);minimum=(2,5,5,4)[p["position"]]
        nxt=same[role+1]["overall"] if role+1<len(same) else p["overall"]-10;gap=max(0,p["overall"]-nxt)
        seller=min(175,100+(25 if role==0 else 10 if role==1 else 0)+(15 if len(same)<=minimum else 7 if len(same)==minimum+1 else 0)+(8 if len(squad)<=21 else 0)+min(15,gap))
        ratios.append(seller/100);not_sale+=role==0 and len(same)<=minimum and gap>=8
if any(not 16<=p["age"]<=40 for p in players):raise SystemExit("age outside 16..40")
if any(p["value"]<100000 or p["wage"]<50000 for p in players):raise SystemExit("non-positive economy data")
for a,b in zip(sorted(players,key=lambda x:(x["age"],x["position"],x["overall"])),sorted(players,key=lambda x:(x["age"],x["position"],x["overall"]))[1:]):
    if a["age"]==b["age"] and a["position"]==b["position"] and b["overall"]>=a["overall"]+5 and b["value"]<a["value"]*.65:raise SystemExit("severe value inversion")
print(f"v51 economy: {len(players)} players, value GBP {min(p['value'] for p in players)/1e6:.1f}m-{max(p['value'] for p in players)/1e6:.1f}m, seller median {statistics.median(ratios):.2f}, max {max(ratios):.2f}, not-for-sale {not_sale}")
