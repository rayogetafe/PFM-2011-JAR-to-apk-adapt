#!/usr/bin/env python3
"""Ensure parallel squads rotate without erasing a recognizable first team."""
import csv,sys
from collections import defaultdict

def i32(x):
    x&=0xffffffff
    return x-0x100000000 if x&0x80000000 else x
def h(x):
    x=i32(x^((x&0xffffffff)>>16));x=i32(x*0x7feb352d);x=i32(x^((x&0xffffffff)>>15));x=i32(x*0x846ca68b);return i32(x^((x&0xffffffff)>>16))
def main(path):
    clubs=defaultdict(list)
    with open(path,encoding="utf-8") as f:
        for r in csv.DictReader(f,delimiter="\t"):
            if r["type"]=="P":clubs[r["league"],int(r["clubIndex"])].append({"id":int(r["playerIndex"]),"pos":int(r["position"]),"ovr":int(r["overall"]),"res":int(r["resistance"]),"fat":0,"st":0,"ap":0})
    used_counts=[];max_starts=[];all_apps=[]
    for key,ps in clubs.items():
        for rnd in range(38 if key[0]!="de" else 34):
            seed=rnd*7919+key[1]*101
            score=lambda p,s:p["ovr"]*20-p["fat"]*2+(h(s+p["id"]*97)&31)-(400 if (h(s+p["id"]*193)&255)%100<15 else 0)
            xi=[]
            for po,count,off in ((0,1,0),(1,4,11),(2,4,23),(3,2,37)):
                cand=sorted((p for p in ps if p["pos"]==po and p not in xi),key=lambda p:score(p,seed+off),reverse=True)
                xi.extend(cand[:count])
            ranked=sorted(ps,key=lambda p:score(p,seed+53),reverse=True)
            for p in ranked:
                if len(xi)<11 and p not in xi:xi.append(p)
            used=list(xi)
            target=min(12+(1 if (h(seed+811)&255)%100<70 else 0)+(1 if (h(seed+1231)&255)%100<40 else 0),len(ps))
            for p in ranked:
                if len(used)<target and p not in used:used.append(p)
            for p in used:p["ap"]+=1
            for p in xi:p["st"]+=1
            for p in ps:
                if p in xi:p["fat"]=min(100,p["fat"]+max(7,17-p["res"]//10))
                elif p in used:p["fat"]=min(100,p["fat"]+3)
                else:p["fat"]=max(0,p["fat"]-10)
        used=sum(p["ap"]>0 for p in ps);mx=max(p["st"] for p in ps if p["pos"]!=0);used_counts.append(used);max_starts.append(mx)
        if used<18:raise SystemExit(f"{key}: only {used} players used")
        if mx>37:raise SystemExit(f"{key}: outfield starter still reaches {mx} starts")
        all_apps.extend(p["ap"] for p in ps)
    all_apps.sort();q=lambda n:all_apps[min(len(all_apps)-1,len(all_apps)*n//100)]
    if q(10)>10 or q(90)<28:raise SystemExit(f"flat appearance distribution p10={q(10)} p90={q(90)}")
    print(f"rotation: {len(clubs)} clubs, players used {min(used_counts)}-{max(used_counts)}, max starts {min(max_starts)}-{max(max_starts)}, AP p10/median/p90 {q(10)}/{q(50)}/{q(90)}")
if __name__=="__main__":main(sys.argv[1])
