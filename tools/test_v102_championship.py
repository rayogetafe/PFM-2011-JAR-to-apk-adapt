#!/usr/bin/env python3
"""Exercise the imported Championship, calendar, cup and migration policy."""
import sys
from pathlib import Path
from test_v85_long_career import main

ROOT = Path(__file__).resolve().parents[1]
if __name__ == '__main__':
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / 'artifacts/v102-championship-audit.txt')]
    main('pfm.android.ChampionshipHarness', [ROOT / 'tools/tests/v102/ChampionshipHarness.java'])
