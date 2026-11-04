#!/usr/bin/env python3
"""Recompute aggregate league facts from the first-division tables in public RSSSF archives."""
import hashlib,html,json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PATHS={'en':'tablese/eng','es':'tabless/span','it':'tablesi/ital','de':'tablesd/duit','fr':'tablesf/fran'}

def audit(directory):
    facts=[]
    for code,prefix in PATHS.items():
        for year in (2010,2011,2012):
            raw=(directory/f'{code}-{year}.html').read_bytes();text=raw.decode('latin-1')
            first=re.search(r'<pre\b[^>]*>(.*?)</pre>',text,re.I|re.S).group(1)
            first=html.unescape(re.sub('<[^>]+>','',first))
            rows=re.findall(r'^\s*(\d+)\.\s*.+?\s+(34|38)\s+\d+\s+(\d+)\s+\d+\s+(\d+)\s*[-:]\s*(\d+)',first,re.M)
            n=18 if code=='de' else 20;rows=rows[:n]
            assert [int(r[0]) for r in rows]==list(range(1,n+1)),(code,year,'incomplete table',rows)
            played=sum(int(r[1]) for r in rows)//2;gf=sum(int(r[3]) for r in rows);ga=sum(int(r[4]) for r in rows)
            assert played==n*(n-1) and gf==ga,(code,year,'unbalanced table')
            fact={'league':code,'season':f'{year-1}/{year}','matches':played,'goals':gf,'goals_per_match':gf/played,'draws':sum(int(r[2]) for r in rows)//2,'source':f'https://www.rsssf.org/{prefix}{year}.html','sha256':hashlib.sha256(raw).hexdigest()}
            if year==2011:
                # Detailed match lists precede cups/lower divisions; validate against the independent final table.
                cross=[re.findall(r'(?<!\d)(\d)-(\d)(?!\d)',line) for line in first.splitlines()]
                cross=[line for line in cross if len(line)==n-1]
                scores=[score for line in cross for score in line] if len(cross)==n else re.findall(r'^\s*[^\n]+?\s+(\d)-(\d)\s+[^\n]+$',first,re.M)[:played]
                if len(scores)==played and sum(int(h)+int(a) for h,a in scores)==gf:
                    fact.update(home_goals=sum(int(h) for h,a in scores),away_goals=sum(int(a) for h,a in scores),zero_zero=sum(h==a=='0' for h,a in scores),four_plus=sum(int(h)+int(a)>=4 for h,a in scores))
                    assert sum(h==a for h,a in scores)==fact['draws']
                else:raise AssertionError((code,year,'match list does not reconcile',len(scores)))
            facts.append(fact)
    return facts

if __name__=='__main__':
    facts=audit(Path(sys.argv[1]));print(json.dumps(facts,ensure_ascii=False,indent=2))
