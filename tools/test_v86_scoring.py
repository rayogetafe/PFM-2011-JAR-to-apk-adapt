#!/usr/bin/env python3
"""Actual journal-driven seasons, independent calibration/validation seeds and league-specific targets."""
import json,re
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    source=ROOT/'tools/tests/v86/ScoringHarness.java'
    targets=[x for x in json.loads((ROOT/'tools/reference/v86_league_goals_2009_12.json').read_text()) if x['season']=='2010/2011']
    java=source.read_text(encoding='utf-8')
    for name,key in [('MATCHES','matches'),('HOME','home_goals'),('AWAY','away_goals')]:
        values=list(map(int,re.search(name+r'=\{([\d,]+)\}',java)[1].split(',')))
        assert values==[x[key] for x in targets],(name,'reference drift')
    main('pfm.android.ScoringHarness',[source])
