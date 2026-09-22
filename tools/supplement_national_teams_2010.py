#!/usr/bin/env python3
"""Add facts from a second independent season-roster archive.

Used only where EuroFotbal's historical page is materially incomplete.
National-Football-Teams exposes DOB, detailed position and league matches.
"""
import csv
import re
import unicodedata
from pathlib import Path
from urllib.request import Request, urlopen
from lxml import html

FACTS=Path('tools/eurofotbal_roster_facts.tsv')
OUT=Path('tools/verified_roster_facts_v76.tsv')
SOURCES={
 'eu_azerbaijan_neftci-pfk':'https://www.national-football-teams.com/club/168/2010/Neftci_Baki.html',
 'eu_ukraine_karpaty-lviv':'https://www.national-football-teams.com/old/club/2623/2010_1/Karpaty_Lviv.html',
 'eu_belarus_dinamo-minsk':'https://www.national-football-teams.com/club/196/2010_2/Dynama_Minsk.html',
 'eu_finland_hjk-helsinki':'https://www.national-football-teams.com/club/818/2010_2/Hjk_Helsinki.html',
 'eu_ireland_shamrock-rovers':'https://www.national-football-teams.com/club/2850/2010_2/Shamrock_Rovers.html',
 'eu_portugal_estoril-praia':'https://www.national-football-teams.com/club/1923/2010_1/Estoril_Praia.html',
}

def norm(s):
    return re.sub(r'[^a-z0-9]','',unicodedata.normalize('NFKD',s).encode('ascii','ignore').decode().lower())

def position(s):
    s=s.lower()
    if 'goalkeeper' in s:return 'GK'
    if 'back' in s or 'defender' in s:return 'DEF'
    if 'midfield' in s:return 'MID'
    return 'ATT'

with FACTS.open(encoding='utf-8',newline='') as f:
    rows=list(csv.DictReader(f,delimiter='\t'))
seen={(x['club_id'],x['identity_key']) for x in rows}
added=0
for club,url in SOURCES.items():
    doc=html.fromstring(urlopen(Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=30).read())
    tables=doc.xpath('//table[contains(@class,"player")]')
    if not tables:raise RuntimeError('no player table '+url)
    for tr in tables[0].xpath('.//tr')[1:]:
        cells=[' '.join(x.text_content().split()) for x in tr.xpath('./th|./td')]
        if len(cells)<7:continue
        di=next((i for i,x in enumerate(cells) if re.fullmatch(r'\d{4}-\d{2}-\d{2}',x)),None)
        if di is None or di<1:continue
        name=cells[di-1]
        if ',' in name:
            last,first=[x.strip() for x in name.split(',',1)];name=first+' '+last
        dob=cells[di]
        if not re.fullmatch(r'\d{4}-\d{2}-\d{2}',dob):continue
        ident=norm(name)
        if (club,ident) in seen:continue
        pi=next((i for i in range(di+1,len(cells)) if any(k in cells[i].lower() for k in ('goalkeeper','back','defender','midfield','winger','forward','striker'))),None)
        if pi is None:continue
        si=pi+1
        apps=re.sub(r'\D','',cells[si+1]) if len(cells)>si+1 else '0'
        y,m,d=dob.split('-')
        rows.append(dict(club_id=club,player=name,identity_key=ident,position=position(cells[pi]),
                         dob=f'{d}.{m}.{y}',appearances=apps,source_url=url))
        seen.add((club,ident));added+=1
with OUT.open('w',encoding='utf-8',newline='') as f:
    w=csv.DictWriter(f,fieldnames=['club_id','player','identity_key','position','dob','appearances','source_url'],delimiter='\t',lineterminator='\n')
    w.writeheader();w.writerows(rows)
print('facts',len(rows),'secondary additions',added)
