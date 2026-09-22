#!/usr/bin/env python3
"""Reconcile v74 with independently sourced 2010/11 positions/appearances.

The opening-day game snapshot is a curated approximation: season-end tables
include winter moves, so a player is added only when not already owned and
when his identity is unambiguous. Sources remain in roster_facts.tsv.
"""
import argparse
import csv
import difflib
import hashlib
import re
import unicodedata
from collections import Counter, defaultdict
from datetime import date
from pathlib import Path


def key(s):
    return re.sub(r"[^a-z0-9]", "", unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower())


def stable(s, n):
    return int(hashlib.sha256(s.encode()).hexdigest()[:12], 16) % n


def same_person(a, b):
    aa=[key(t) for t in a.split()];bb=[key(t) for t in b.split()]
    if not aa or not bb:return False
    if key(a)==key(b):return True
    if len(aa)==1 or len(bb)==1:
        return min(len(key(a)),len(key(b)))>=6 and (key(a) in key(b) or key(b) in key(a))
    first=difflib.SequenceMatcher(None,aa[0],bb[0]).ratio()
    last=difflib.SequenceMatcher(None,aa[-1],bb[-1]).ratio()
    return first>=.73 and last>=.78 and difflib.SequenceMatcher(None,key(a),key(b)).ratio()>=.78


def read(p):
    with p.open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def age(dob, calendar=False):
    d, m, y = map(int, dob.split("."))
    start = date(2010, 3 if calendar else 8, 1)
    return start.year-y-((start.month,start.day)<(m,d))


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--old",type=Path,default=Path("overrides/app/src/main/assets/europe_world_v74.tsv"))
    ap.add_argument("--facts",type=Path,default=Path("tools/eurofotbal_roster_facts.tsv"))
    ap.add_argument("--clubs",type=Path,default=Path("overrides/app/src/main/assets/europe_clubs_v72.tsv"))
    ap.add_argument("--fifa",type=Path,default=Path("../fifa11-fut-source.csv"))
    ap.add_argument("--output",type=Path,default=Path("overrides/app/src/main/assets/europe_world_v75.tsv"))
    args=ap.parse_args()
    old=read(args.old); facts=read(args.facts); clubs=read(args.clubs)
    owners={key(x['player']):x['league']+':'+x['clubIndex'] for x in old if x['type']=='P'}
    topfive={key(x['player']) for x in read(Path('overrides/nationalities_v42.tsv'))}
    by_fact=defaultdict(list)
    for f in facts: by_fact[f['club_id']].append(f)
    by_club={c['id']:c for c in clubs}
    by_name={(c['country'],c['name']):c for c in clubs}
    fifa=defaultdict(list)
    if args.fifa.exists():
        with args.fifa.open(encoding='utf-8-sig',newline='') as f:
            for c in csv.DictReader(f):fifa[key(c['NAME'])].append(c)
    additions=Counter(); changes=Counter(); unmatched=[]; result=[]
    groups=defaultdict(list)
    for x in old:
        if x['type']=='P':groups[(x['league'],x['clubIndex'])].append(x)
    for t in old:
        if t['type']!='T':continue
        c=by_name[(t['nationality'],t['club'])]; rows=groups[(t['league'],t['clubIndex'])]
        available=by_fact[c['id']]; used=set()
        for p in rows:
            identity=key(p['player'])
            exact=[f for f in available if f['identity_key']==identity]
            if len(exact)==1: fact=exact[0]
            else:
                near=sorted(((difflib.SequenceMatcher(None,identity,f['identity_key']).ratio(),f)
                             for f in available if f['identity_key'] not in used),key=lambda x:x[0],reverse=True)
                fact=near[0][1] if near and near[0][0]>=.86 and (len(near)==1 or near[0][0]-near[1][0]>=.06) else None
            if fact and fact['identity_key'] not in used:
                used.add(fact['identity_key'])
                pos={'GK':0,'DEF':1,'MID':2,'ATT':3}[fact['position']]
                if int(p['position'])!=pos:changes['position']+=1
                p['position']=str(pos)
                a=age(fact['dob'],c['country'] in {'Russia','Norway','Sweden','Finland','Ireland','Belarus'})
                if 16<=a<=45:
                    p['age']=str(a); changes['age']+=1
                appearances=int(fact['appearances'])
                floor=59+2*int(c['rep'])+min(8,appearances//5)
                if fact['position']=='GK':floor+=1
                if int(p['overall'])<floor:
                    p['overall']=str(min(83,floor));changes['rating_floor']+=1
                p['provenance']='SEASON_APPS_FIFA11' if 'FIFA11' in p['provenance'] else 'SEASON_APPS_EST_RATING'
            else: unmatched.append((c['name'],p['player']))
            cards=fifa.get(identity,[])
            # A one-letter transliteration variant at the same club is safe;
            # FUT duplicate boosted cards are *not* independent observations.
            if not cards and fact:
                cards=fifa.get(fact['identity_key'],[])
            if cards and 'FIFA11' in p['provenance']:
                base=min(int(x['RATING']) for x in cards)
                p['overall']=str(max(int(p['overall']),base))
            if c['id']=='eu_russia_zenit-st-petersburg' and identity=='vyacheslavmalafeev':
                p['overall']='78';p['position']='0';p['provenance']='SOFIFA11_SEASON_APPS'
            if c['id']=='eu_russia_zenit-st-petersburg' and identity=='aleksandranyukov':
                p['overall']='80';p['position']='1';p['provenance']='FIFA11_ALIAS_SEASON_APPS'
            if c['id']=='eu_russia_zenit-st-petersburg' and identity=='aleksandrkerzhakov':
                p['overall']='78';p['position']='3';p['provenance']='FIFA11_ALIAS_SEASON_APPS'
            ovr=int(p['overall']);p['value']=str(max(250000,(ovr-50)**2*9000));p['wage']=str(max(50000,(int(p['value'])//18//10000)*10000))
        existing={key(p['player']) for p in rows}
        # Guarantee two real keepers where the archive supports them.
        keepers=sorted((f for f in available if f['position']=='GK'),key=lambda f:int(f['appearances']),reverse=True)
        targets=[]
        for f in keepers:
            if sum(p['position']=='0' for p in rows)+sum(z['position']=='GK' for z in targets)>=2:break
            targets.append(f)
        # Restore regular first-team players omitted from the short research
        # draft; no 0-appearance academy padding and no cross-club duplicates.
        regulars=sorted((f for f in available if f['position']!='GK' and int(f['appearances'])>=18),key=lambda f:int(f['appearances']),reverse=True)
        targets.extend(regulars)
        for f in targets:
            identity=f['identity_key']
            if identity in existing or identity in topfive or identity in owners or any(same_person(f['player'],p['player']) for p in rows):continue
            if len(rows)>=26:break
            pos={'GK':0,'DEF':1,'MID':2,'ATT':3}[f['position']]
            if pos==0 and sum(p['position']=='0' for p in rows)>=2:continue
            if pos!=0 and sum(p['position']==str(pos) for p in rows)>=({1:9,2:10,3:7}[pos]):continue
            a=age(f['dob'],c['country'] in {'Russia','Norway','Sweden','Finland','Ireland','Belarus'})
            if a<16 or a>45:continue
            apps=int(f['appearances']);ovr=min(83,59+int(c['rep'])*2+min(8,apps//5)+(1 if pos==0 else 0))
            cards=fifa.get(identity,[])
            if cards:ovr=max(ovr,min(int(card['RATING']) for card in cards))
            val=max(250000,(ovr-50)**2*9000)
            uid='eu:'+identity+':'+f['dob'].replace('.','')
            p=dict(type='P',league=t['league'],clubIndex=t['clubIndex'],club=t['club'],playerIndex=str(len(rows)),player=f['player'],position=str(pos),age=str(a),overall=str(ovr),nationality='Unknown',speed=str(max(45,min(90,ovr+stable(identity+'speed',15)-7))),resistance=str(max(45,min(90,ovr+stable(identity+'stamina',13)-6))),quality=str(max(45,min(90,ovr+stable(identity+'quality',11)-5))),morale='55',style='0',formation='0',value=str(val),wage=str(max(50000,(val//18//10000)*10000)),uid=uid,provenance='SEASON_ARCHIVE_EST_RATING')
            rows.append(p);owners[identity]=t['league']+':'+t['clubIndex'];existing.add(identity);additions['GK' if pos==0 else 'outfield']+=1
        if not any(p['position']=='0' for p in rows):raise SystemExit('No verified goalkeeper: '+c['name'])
        if len(rows)>30:raise SystemExit('Oversized roster: '+c['name'])
        result.append(t)
        for i,p in enumerate(rows):p['playerIndex']=str(i);result.append(p)
    fields=list(old[0]);args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,fieldnames=fields,delimiter='\t',lineterminator='\n');w.writeheader();w.writerows(result)
    print('players',sum(x['type']=='P' for x in result),'additions',dict(additions),'changes',dict(changes),'unmatched',len(unmatched))
    print('unmatched examples',unmatched[:20])


if __name__=='__main__':main()
