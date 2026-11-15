#!/usr/bin/env python3
"""Exercise the 2. Bundesliga season, accounting, cup and save upgrade."""
import sys
from test_v85_long_career import ROOT, main

if __name__ == "__main__":
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / "artifacts/v114-bundesliga2-audit.txt")]
    main("pfm.android.Bundesliga2Harness", [ROOT / "tools/tests/v114/Bundesliga2Harness.java"])
