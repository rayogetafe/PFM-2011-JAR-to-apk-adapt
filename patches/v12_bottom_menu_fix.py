#!/usr/bin/env python3
import sys
from pathlib import Path

p=Path(sys.argv[1])
s=p.read_text(encoding='utf-8')

# Bottom-most menu items (Finalize / Continue game / Quit) visually extend below
# the green/yellow colour run that the detector sees. Treat the last detected row
# as owning the remainder of the logical canvas below it. This avoids NO TARGET
# even when the user taps the visible shadow/lower half of the final button.
old='''                int margin=24;\n                if(tapY>=ys[i]-margin && tapY<=ye[i]+margin){'''
new='''                int margin=24;\n                int hitBottom=(i==count-1 && tapY>=ys[i]-margin)?LOGICAL_H-1:ye[i]+margin;\n                if(tapY>=ys[i]-margin && tapY<=hitBottom){'''
if old not in s:
    raise SystemExit('bottom hit-box insertion point not found')
s=s.replace(old,new,1)

# A manual tap on the Android safety panel naturally leaves several game frames
# between DOWN/UP and OK. The auto-menu sequence was much tighter. Give each
# synthetic legacy key four blank PFM frames before the next one so static action
# menus have time to commit their new highlighted item before FIRE arrives.
old='''            syntheticHeldKey=0;\n            syntheticGapFrames=1;'''
new='''            syntheticHeldKey=0;\n            syntheticGapFrames=4;'''
if old not in s:
    raise SystemExit('synthetic gap insertion point not found')
s=s.replace(old,new,1)

p.write_text(s,encoding='utf-8')
print('Applied v12 bottom-menu hitbox and human-paced input fix to',p)
