#!/usr/bin/env python3
"""Shared quotas, Europa seasons, UCL transfers, discipline and upgrade regression."""
import sys
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    sys.argv=[sys.argv[0],sys.argv[1],str(ROOT/'artifacts/v93-europa-audit.txt'),'1','1']
    main('pfm.android.EuropaHarness',[ROOT/'tools/tests/v93/EuropaHarness.java'])
