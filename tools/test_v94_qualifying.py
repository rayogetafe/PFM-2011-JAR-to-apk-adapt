#!/usr/bin/env python3
"""Qualifying snapshots, group/Europa routing, restart and frozen v93 compatibility."""
import sys
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    sys.argv=[sys.argv[0],sys.argv[1],str(ROOT/'artifacts/v94-qualifying-audit.txt'),'1','1']
    main('pfm.android.QualifyingHarness',[ROOT/'tools/tests/v94/QualifyingHarness.java'])
