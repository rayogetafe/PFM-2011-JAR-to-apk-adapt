#!/usr/bin/env python3
"""Verify Germany's promotion and relegation across active careers and saved seasons."""
import sys
from test_v85_long_career import ROOT, main

if __name__ == "__main__":
    sys.argv = [sys.argv[0], sys.argv[1], str(ROOT / "artifacts/v115-german-career-audit.txt")]
    main("pfm.android.GermanCareerHarness", [ROOT / "tools/tests/v115/GermanCareerHarness.java"])
