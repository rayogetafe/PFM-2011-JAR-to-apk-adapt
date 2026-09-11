#!/usr/bin/env python3
import sys
from pathlib import Path

p=Path(sys.argv[1])
s=p.read_text(encoding='utf-8')

# v10 diagnostics showed that taps on the lower visual half/shadow of bottom
# buttons land 19-20 logical pixels below the colour run (e.g. Continue game:
# tapY=493 while detected row=455..474). Expand the visual hit box.
s=s.replace('int margin=9;', 'int margin=24;', 1)
s=s.replace('bestDist=28;', 'bestDist=42;', 1)

# v10 also proved that the Options popup title is being treated as a menu item:
# rows 2..6 had centres ~172,204,240,277,314; the first gap is compressed
# compared with the regular ~36-37 px item cadence. The title's yellow score
# slightly beat the selected Speed x1 row, producing sel=2 and an extra DOWN.
# Detect this common popup-title pattern from row cadence and exclude only the
# first row from selection/navigation while preserving ordinary menus.
needle='''            if(last-first<1) return false;\n\n            int selected=-1,bestScore=Integer.MIN_VALUE;\n            for(int i=first;i<=last;i++){\n                if(score[i]>bestScore){ bestScore=score[i]; selected=i; }\n            }'''
replacement='''            if(last-first<1) return false;\n\n            int menuFirst=first;\n            if(last-first>=3){\n                int firstGap=yc[first+1]-yc[first];\n                int restSum=0,restCount=0;\n                for(int i=first+1;i<last;i++){\n                    restSum+=yc[i+1]-yc[i];\n                    restCount++;\n                }\n                int restAvg=restCount>0?restSum/restCount:firstGap;\n                // Popup headers sit noticeably closer to the first real row\n                // than the regular item-to-item spacing. In the captured\n                // Options screen this is 32px vs ~36-37px.\n                if(firstGap+4<=restAvg) menuFirst=first+1;\n            }\n            // Tapping the decorative title itself should do nothing, not FIRE.\n            if(target<menuFirst) return true;\n\n            int selected=-1,bestScore=Integer.MIN_VALUE;\n            for(int i=menuFirst;i<=last;i++){\n                if(score[i]>bestScore){ bestScore=score[i]; selected=i; }\n            }'''
if needle not in s:
    raise SystemExit('menu selection block not found')
s=s.replace(needle,replacement,1)

p.write_text(s,encoding='utf-8')
print('Applied v11 diagnostic-driven menu fix to',p)
