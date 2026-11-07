#!/usr/bin/env python3
"""Check European suspension carry-over against the career journal and match engine."""
import sys
from pathlib import Path
from test_v85_long_career import main

ROOT = Path(__file__).resolve().parents[1]
if __name__ == '__main__':
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / 'artifacts/v96-european-bans-audit.txt')]
    main('pfm.android.EuropeanBanHarness', [ROOT / 'tools/tests/v96/EuropeanBanHarness.java'])
