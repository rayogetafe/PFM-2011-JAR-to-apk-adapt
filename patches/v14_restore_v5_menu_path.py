#!/usr/bin/env python3
import sys
from pathlib import Path

p = Path(sys.argv[1])
s = p.read_text(encoding='utf-8')

# v5 is the last empirically-known build where the lower two-item match menu
# (Options / Finalize) worked by direct touch. Restore its key pulse timing.
s = s.replace('private static final long KEY_HOLD_MS=82L;',
              'private static final long KEY_HOLD_MS=40L;')
s = s.replace('private static final long KEY_STEP_MS=135L;',
              'private static final long KEY_STEP_MS=68L;')
s = s.replace('syntheticBusyUntil=now+delay+55L;',
              'syntheticBusyUntil=now+delay+35L;')

# Replace one complete Java method without depending on the exact formatting of
# the previous detector revisions.
def replace_method(src: str, signature: str, replacement: str) -> str:
    start = src.find(signature)
    if start < 0:
        raise SystemExit(f'method not found: {signature}')
    brace = src.find('{', start)
    if brace < 0:
        raise SystemExit('opening brace not found')
    depth = 0
    in_string = False
    in_char = False
    escaped = False
    i = brace
    while i < len(src):
        ch = src[i]
        if escaped:
            escaped = False
        elif ch == '\\' and (in_string or in_char):
            escaped = True
        elif ch == '"' and not in_char:
            in_string = not in_string
        elif ch == "'" and not in_string:
            in_char = not in_char
        elif not in_string and not in_char:
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return src[:start] + replacement + src[i+1:]
        i += 1
    raise SystemExit('method closing brace not found')

method = r'''private boolean handleClassicMenuTap(int tapX,int tapY){
        try{
            captureFrame();

            // Row extraction deliberately follows the v5 algorithm: one global
            // list of classic green/yellow rows. Later v6/v7 geometry grouping
            // caused the regression where Finalize/Continue/Quit fell back to
            // FIRE on the already-selected first item.
            int[] ys=new int[28],ye=new int[28],yc=new int[28];
            int[] xs=new int[28],xe=new int[28],gold=new int[28];
            int count=0,runStart=-1,runGold=0;

            for(int y=35;y<LOGICAL_H;y++){
                int colored=0,rowGold=0;
                for(int x=6;x<LOGICAL_W-6;x+=4){
                    int c=pixelScratch[y*LOGICAL_W+x];
                    if(isGreen(c)||isYellow(c)) colored++;
                    if(isStrongGold(c)) rowGold++;
                }
                boolean active=colored>=24;
                if(active){
                    if(runStart<0){ runStart=y; runGold=0; }
                    runGold+=rowGold;
                }
                if((!active||y==LOGICAL_H-1)&&runStart>=0){
                    int runEnd=active?y:y-1;
                    int height=runEnd-runStart+1;
                    if(height>=5&&height<=40){
                        int minX=rowMinX(runStart,runEnd);
                        int maxX=rowMaxX(runStart,runEnd);
                        if(minX>=0&&maxX-minX>=68){
                            // Same small-fragment merge as the stable detector,
                            // so one yellow button cannot become two rows.
                            if(count>0 && runStart-ye[count-1]-1<=4 &&
                               runEnd-ys[count-1]+1<=40 &&
                               Math.abs(minX-xs[count-1])<=16 &&
                               Math.abs(maxX-xe[count-1])<=24){
                                ye[count-1]=runEnd;
                                yc[count-1]=(ys[count-1]+runEnd)/2;
                                xs[count-1]=Math.min(xs[count-1],minX);
                                xe[count-1]=Math.max(xe[count-1],maxX);
                                gold[count-1]+=runGold;
                            } else if(count<ys.length){
                                ys[count]=runStart; ye[count]=runEnd;
                                yc[count]=(runStart+runEnd)/2;
                                xs[count]=minX; xe[count]=maxX;
                                gold[count]=runGold;
                                count++;
                            }
                        }
                    }
                    runStart=-1; runGold=0;
                }
            }
            if(count<2) return false;

            // Target: prefer the actual painted row, but allow the shadow/lower
            // half of bottom buttons such as Finalize and Continue game.
            int target=-1,bestDist=Integer.MAX_VALUE;
            for(int i=0;i<count;i++){
                int top=ys[i]-16;
                int bottom=ye[i]+24;
                if(i==count-1) bottom=LOGICAL_H-1;
                if(tapY>=top && tapY<=bottom){
                    int d=Math.abs(tapY-yc[i]);
                    if(d<bestDist){ bestDist=d; target=i; }
                }
            }
            if(target<0){
                bestDist=46;
                for(int i=0;i<count;i++){
                    int d=Math.abs(tapY-yc[i]);
                    if(d<bestDist){ bestDist=d; target=i; }
                }
            }
            if(target<0) return false;

            // The only place where local geometry is retained is an actual
            // popup menu such as match Options. v10 proved that its decorative
            // title is a yellow row immediately above the real items; v11's
            // title exclusion fixed Strategy/Substitutions/Back on-device.
            int first=target,last=target;
            while(first>0){
                int i=first-1,j=first;
                if(yc[j]-yc[i]>68) break;
                if(Math.abs(xs[i]-xs[target])>12 || Math.abs(xe[i]-xe[target])>46) break;
                first=i;
            }
            while(last<count-1){
                int i=last+1,j=last;
                if(yc[i]-yc[j]>68) break;
                if(Math.abs(xs[i]-xs[target])>12 || Math.abs(xe[i]-xe[target])>46) break;
                last=i;
            }

            boolean popup=false;
            int menuFirst=first;
            if(last-first>=3){
                int firstGap=yc[first+1]-yc[first];
                int restSum=0,restCount=0;
                for(int i=first+1;i<last;i++){
                    restSum+=yc[i+1]-yc[i];
                    restCount++;
                }
                int restAvg=restCount>0?restSum/restCount:firstGap;
                if(firstGap+4<=restAvg){
                    popup=true;
                    menuFirst=first+1;
                }
            }

            if(popup){
                // Decorative popup title is not actionable.
                if(target<menuFirst) return true;
                int selected=-1,selectedGold=10;
                for(int i=menuFirst;i<=last;i++){
                    if(gold[i]>selectedGold){ selectedGold=gold[i]; selected=i; }
                }
                if(selected<0) return false;
                int delta=target-selected;
                if(delta<0) return sendRepeatedThenFire(-1,-delta);
                if(delta>0) return sendRepeatedThenFire(-2,delta);
                return sendSingle(-5);
            }

            // Ordinary menus intentionally use the proven v5 global selection
            // path. Do NOT require a local x-geometry cluster here: that exact
            // requirement was introduced after v5 and broke Finalize/Continue.
            int selected=-1,selectedGold=16;
            for(int i=0;i<count;i++){
                if(gold[i]>selectedGold){ selectedGold=gold[i]; selected=i; }
            }
            if(selected<0) return false;

            int delta=target-selected;
            if(delta<0) return sendRepeatedThenFire(-1,-delta);
            if(delta>0) return sendRepeatedThenFire(-2,delta);
            return sendSingle(-5);
        } catch(Throwable ignored){
            return false;
        }
    }'''

s = replace_method(s, 'private boolean handleClassicMenuTap(', method)

p.write_text(s, encoding='utf-8')
print('Applied v14 regression-based menu restoration to', p)
