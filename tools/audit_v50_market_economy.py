#!/usr/bin/env python3
"""Audit the v50 asking-price inputs and broad market distribution."""
import csv,sys
from collections import defaultdict

def age(a):return 145 if a<=21 else 130 if a<=24 else 110 if a<=28 else 90 if a<=31 else 75
def main(path):
    clubs=defaultdict(list)
    with open(path,encoding="utf-8") as f:
        for r in csv.DictReader(f,delimiter="\t"):
            if r["type"]=="P":clubs[r["league"],int(r["clubIndex"])].append(r)
    asks=[];protected=0
    for players in clubs.values():
        for p in players:
            same=sorted((x for x in players if x["position"]==p["position"]),key=lambda x:int(x["overall"]),reverse=True);role=same.index(p);depth=len(same);po=int(p["position"]);minimum=(2,5,5,4)[po];f_role=150 if role==0 else 125 if role==1 else 100;f_depth=140 if depth<=minimum else 115 if depth==minimum+1 else 100;f_squad=120 if len(players)<=21 else 100;value=int(p["value"]);ask=max(value,value*age(int(p["age"]))*f_role*f_depth*f_squad//100000000);asks.append((value,ask));protected+=(role<2 and depth<=minimum) or len(players)<=20
    if any(a<v for v,a in asks):raise SystemExit("asking price below database value")
    ratios=sorted(a/max(1,v) for v,a in asks if v>0)
    if not 1.0<=ratios[len(ratios)//2]<=1.8:raise SystemExit("implausible median markup")
    if not 100<=protected<=800:raise SystemExit(f"implausible protected pool {protected}")
    print(f"market economy: {len(asks)} players, protected {protected}, markup median {ratios[len(ratios)//2]:.2f}, p90 {ratios[len(ratios)*9//10]:.2f}")
if __name__=="__main__":main(sys.argv[1])
