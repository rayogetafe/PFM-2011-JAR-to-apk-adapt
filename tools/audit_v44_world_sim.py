#!/usr/bin/env python3
"""Guard the parallel engine against the low-scoring/flat-strength v42 regression."""
import csv,sys
from collections import defaultdict

def i32(x):
    x&=0xffffffff
    return x-0x100000000 if x&0x80000000 else x
def ur(x,n):return (x&0xffffffff)>>n
def h(x):
    x=i32(x^ur(x,16));x=i32(x*0x7feb352d);x=i32(x^ur(x,15));x=i32(x*0x846ca68b);return i32(x^ur(x,16))
def goals(seed,a,d,home):
    chance=max(35,min(190,100+(a-d)*3+(12 if home else 0)))
    return min(7,sum(1 for s in range(14) if (h(seed+s*1009)&0x7fffffff)%1000<chance))
def main(path):
    teams=defaultdict(lambda:defaultdict(list));names={}
    with open(path,encoding="utf-8") as f:
        for r in csv.DictReader(f,delimiter="\t"):
            if r["type"]=="T":names[(r["league"],int(r["clubIndex"]))]=r["club"]
            else:teams[r["league"]][int(r["clubIndex"])].append((125*int(r["quality"])+85*int(r["speed"])+75*int(r["resistance"]))//285)
    for code,clubs in teams.items():
        strength={i:sum(v[:11])//len(v[:11]) for i,v in clubs.items()};n=len(clubs);a=list(range(n));fixtures=[]
        for leg in range(2):
            for rnd in range(n-1):
                for i in range(n//2):fixtures.append((leg*(n-1)+rnd+1,a[i] if leg==0 else a[n-1-i],a[n-1-i] if leg==0 else a[i]))
                a[1:]=[a[-1]]+a[1:-1]
        totals=[]
        for season in range(10):
            total=0
            for rnd,home,away in fixtures:
                seed=season*10007+sum(ord(c) for c in code)*101+rnd*37+home*11+away
                total+=goals(seed,strength[home],strength[away],True)+goals(seed+991,strength[away],strength[home],False)
            totals.append(total/len(fixtures))
        avg=sum(totals)/len(totals)
        if not 2.45<=avg<=3.35:raise SystemExit(f"{code}: implausible goals/match {avg:.2f}")
        print(f"{code}: {n} clubs, goals/match {avg:.2f}, strength {min(strength.values())}-{max(strength.values())}")
if __name__=="__main__":main(sys.argv[1])
