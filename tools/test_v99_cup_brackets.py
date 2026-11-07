#!/usr/bin/env python3
"""Run an expanded domestic cup through the production calendar and Europa allocation."""
import sys
from pathlib import Path
from test_v85_long_career import main

ROOT = Path(__file__).resolve().parents[1]
if __name__ == '__main__':
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / 'artifacts/v99-cup-brackets-audit.txt')]
    main('pfm.android.CupBracketHarness', [ROOT / 'tools/tests/v99/CupBracketHarness.java'])
