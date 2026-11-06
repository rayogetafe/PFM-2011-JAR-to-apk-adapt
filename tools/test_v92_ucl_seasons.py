#!/usr/bin/env python3
"""Frozen qualification, season isolation, migration and recurring UCL integration."""
import sys
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    sys.argv=[sys.argv[0],sys.argv[1],str(ROOT/'artifacts/v92-ucl-seasons-audit.txt'),'1','1']
    main('pfm.android.UclSeasonHarness',[ROOT/'tools/tests/v92/UclSeasonHarness.java'])
