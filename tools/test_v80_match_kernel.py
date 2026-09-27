#!/usr/bin/env python3
"""Regression guard: both parallel competitions use one fast-match kernel."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "overrides/app/src/main/java/pfm/android"
kernel = (JAVA / "NativeMatchEngine.java").read_text(encoding="utf-8")
world = (JAVA / "NativeWorldCenter.java").read_text(encoding="utf-8")
ucl = (JAVA / "NativeChampionsLeague.java").read_text(encoding="utf-8")

assert "private static final int BASE=240000" in kernel
assert "segment<14" in kernel
assert "p.qua*125+p.spe*85+p.res*75+p.mor*30+2600" in kernel
assert "NativeMatchEngine.score(seed,home,away,homeSel,awaySel)" in world
assert "NativeMatchEngine.score(seed,home,away,homeXI,awayXI)" in ucl
assert "NativeWorldCenter.selection(home,seed)" in ucl
assert "chance<6" not in ucl
assert "tempo(String code)" not in world
assert "if(prefs.contains(k))continue" in ucl  # Completed v79 games survive.
print("v80 match kernel OK: top-five and UCL share the legacy fast-match model")
