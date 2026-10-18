#!/usr/bin/env python3
"""Regression guard: both parallel competitions use one fast-match kernel."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "overrides/app/src/main/java/pfm/android"
kernel = (JAVA / "NativeMatchEngine.java").read_text(encoding="utf-8")
world = (JAVA / "NativeWorldCenter.java").read_text(encoding="utf-8")
ucl = (JAVA / "NativeChampionsLeague.java").read_text(encoding="utf-8")
journal = (JAVA / "NativeMatchJournal.java").read_text(encoding="utf-8")

assert "MatchEngine.goal(seed,minute,0,h,a)" in kernel
assert "MatchEngine.goal(seed,minute,1,h,a)" in kernel
assert "NativeMatchEngine.score(seed,home,away,homeSel,awaySel)" in world
assert "NativeMatchJournal.advance" in ucl
assert "NativeMatchEngine.score(seed,home,away,hs,as)" in journal
assert "eligible(home,homeId,competition,seed,effects,players)" in journal
assert "chance<6" not in ucl
assert "tempo(String code)" not in world
assert "ucl-score-import" in journal  # Completed v79-v82 games survive.
print("Competition routing OK: top-five and UCL use the shared minute model")
