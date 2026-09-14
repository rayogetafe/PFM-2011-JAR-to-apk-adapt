#!/usr/bin/env python3
"""Guard the isolated port of the stock quick-match formula against regressions."""
import csv,sys
from collections import defaultdict

def i32(x):
    x&=0xffffffff
    return x-0x100000000 if x&0x80000000 else x
def ur(x,n):return (x&0xffffffff)>>n
def h(x):
    x=i32(x^ur(x,16));x=i32(x*0x7feb352d);x=i32(x^ur(x,15));x=i32(x*0x846ca68b);return i32(x^ur(x,16))
FORM=((55,45),(65,35),(50,50),(55,45),(70,30),(60,40),(50,50),(65,35),(60,40))
def pct(style,form):
    fa,fd=FORM[max(0,min(8,form))]
    attack=fa*1000+(100-style)*1000+140*333
    defence=fd*1000+style*1000+160*333
    return attack*100//(attack+defence)
def score(seed,hs,aws,hp,ap,base):
    hs+=50;ha=hs*hp;hd=hs*(100-hp);aa=aws*ap;ad=aws*(100-ap);hg=ag=0
    for s in range(14):
        noise=(h(seed+s*1009)&0x7fffffff)%20000;den=ha+ad+noise+base
        hg+=(h(seed+s*1543+71)&0x7fffffff)%max(1,den)<ha
        noise=(h(seed+s*2017+991)&0x7fffffff)%20000;den=aa+hd+noise+base
        ag+=(h(seed+s*2617+313)&0x7fffffff)%max(1,den)<aa
    return hg,ag
def main(path):
    teams=defaultdict(lambda:defaultdict(list));meta={};names={}
    with open(path,encoding="utf-8") as f:
        for r in csv.DictReader(f,delimiter="\t"):
            if r["type"]=="T":names[(r["league"],int(r["clubIndex"]))]=r["club"];meta[r["league"],int(r["clubIndex"])]=(int(r["style"]),int(r["formation"]))
            else:teams[r["league"]][int(r["clubIndex"])].append((125*int(r["quality"])+85*int(r["speed"])+75*int(r["resistance"])+30*int(r["morale"])+2600)//67)
    for code,clubs in teams.items():
        strength={i:max(1,-3414+sum(v[:11])) for i,v in clubs.items()};n=len(clubs);a=list(range(n));fixtures=[]
        for leg in range(2):
            for rnd in range(n-1):
                for i in range(n//2):fixtures.append((leg*(n-1)+rnd+1,a[i] if leg==0 else a[n-1-i],a[n-1-i] if leg==0 else a[i]))
                a[1:]=[a[-1]]+a[1:-1]
        totals=[]
        for season in range(10):
            total=0
            for rnd,home,away in fixtures:
                seed=season*10007+sum(ord(c) for c in code)*101+rnd*37+home*11+away
                base={"fr":400000,"it":340000,"es":330000}.get(code,320000)
                hg,ag=score(seed,strength[home],strength[away],pct(*meta[code,home]),pct(*meta[code,away]),base);total+=hg+ag
            totals.append(total/len(fixtures))
        avg=sum(totals)/len(totals)
        limits={"fr":(2.20,2.65),"en":(2.55,3.05),"es":(2.55,3.05),"it":(2.45,3.00),"de":(2.55,3.15)}
        lo,hi=limits[code]
        if not lo<=avg<=hi:raise SystemExit(f"{code}: implausible goals/match {avg:.2f} (expected {lo:.2f}-{hi:.2f})")
        print(f"{code}: {n} clubs, goals/match {avg:.2f}, strength {min(strength.values())}-{max(strength.values())}")
if __name__=="__main__":main(sys.argv[1])
