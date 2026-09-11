#!/usr/bin/env python3
import sys
from pathlib import Path

p = Path(sys.argv[1])
s = p.read_text(encoding='utf-8')

# v9 converts timed Android key pulses into input that is advanced only on
# PFM's own flush/frame boundary. v15 keeps that mechanism but gives the legacy
# menu several neutral frames to commit a changed selection before FIRE.
old = 'syntheticGapFrames=1;'
new = 'syntheticGapFrames=4;'
if old not in s:
    raise SystemExit('v15: frame-gap insertion point not found')
s = s.replace(old, new, 1)

# The checked-in compatibility layer uses a slightly longer native-pointer
# busy window than the original v9 patch expected. Make native pointer taps obey
# the synthetic queue as well, so Back cannot overlap a queued DOWN/UP/FIRE.
old = '''    private synchronized boolean directPointerTap(final int x,final int y){\n        long now=SystemClock.uptimeMillis();\n        if(now<syntheticBusyUntil) return false;'''
new = '''    private synchronized boolean directPointerTap(final int x,final int y){\n        long now=SystemClock.uptimeMillis();\n        if(now<syntheticBusyUntil || syntheticSequenceActive()) return false;'''
if old not in s:
    raise SystemExit('v15: directPointerTap guard insertion point not found')
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('Applied v15 frame-synced menu input gap to', p)
