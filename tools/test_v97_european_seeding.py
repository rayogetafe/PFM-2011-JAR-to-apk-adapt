#!/usr/bin/env python3
"""Check rolling European seeding and its persisted use in the career draw."""
import sys
from pathlib import Path
from test_v85_long_career import main

ROOT = Path(__file__).resolve().parents[1]
if __name__ == '__main__':
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / 'artifacts/v97-european-seeding-audit.txt')]
    main('pfm.android.EuropeanSeedingHarness', [ROOT / 'tools/tests/v97/EuropeanSeedingHarness.java'])
