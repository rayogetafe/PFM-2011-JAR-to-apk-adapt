#!/usr/bin/env python3
"""Check cup calendar, qualification routes, and upgrades from frozen v94 seasons."""
import sys
from pathlib import Path
from test_v85_long_career import main

ROOT = Path(__file__).resolve().parents[1]
if __name__ == '__main__':
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / 'artifacts/v95-cup-audit.txt'), '1', '1']
    main('pfm.android.DomesticCupHarness', [ROOT / 'tools/tests/v95/DomesticCupHarness.java'])
