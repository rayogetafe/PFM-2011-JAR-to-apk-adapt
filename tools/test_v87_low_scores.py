#!/usr/bin/env python3
"""Held-out low-score/goal marginal calibration against independently reconciled 2010/11 results."""
import json,re
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    source=ROOT/'tools/tests/v86/ScoringHarness.java';java=source.read_text(encoding='utf-8')
    targets=[x for x in json.loads((ROOT/'tools/reference/v87_low_scores_2009_12.json').read_text()) if x['season']=='2010/2011']
    for name,key in [('MATCHES','matches'),('HOME','home_goals'),('AWAY','away_goals'),('DRAWS','draws'),('ZERO','zero_zero')]:
        values=list(map(int,re.search(name+r'=\{([\d,]+)\}',java)[1].split(',')));assert values==[x[key] for x in targets],(name,'reference drift')
    assert list(map(int,re.search(r'ONE=\{([\d,]+)\}',java)[1].split(',')))==[x['low_scores']['1-1'] for x in targets]
    engine=(ROOT/'overrides/app/src/main/java/pfm/android/MatchEngine.java').read_text(encoding='utf-8')
    profiles={code:tuple(map(int,(g,n,d))) for code,g,n,d in re.findall(r'new Profile\("([a-z]{2})",[\d.]+,[\d.]+,(\d+),(\d+),(\d+),',engine)}
    assert profiles=={x['league']:(x['goals'],x['matches'],x['draws']) for x in targets},'UI/profile reference drift'
    main('pfm.android.ScoringHarness',[source])
