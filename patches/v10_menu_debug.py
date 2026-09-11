#!/usr/bin/env python3
import sys
from pathlib import Path

p=Path(sys.argv[1])
s=p.read_text(encoding='utf-8')

needle='    private long lastSyntheticFramePump;'
repl='''    private long lastSyntheticFramePump;\n    private volatile String touchDebug1="";\n    private volatile String touchDebug2="";\n    private volatile String touchDebug3="";\n    private volatile long touchDebugUntil;\n\n    private void setTouchDebug(String a,String b,String c){\n        touchDebug1=a==null?"":a;\n        touchDebug2=b==null?"":b;\n        touchDebug3=c==null?"":c;\n        touchDebugUntil=SystemClock.uptimeMillis()+12000L;\n        AndroidRuntime.main().post(view::invalidate);\n    }'''
if needle not in s:
    raise SystemExit('debug field insertion point not found')
s=s.replace(needle,repl,1)

# Show target failure too.
old='''            if(target<0) return false;\n\n            int targetW=xe[target]-xs[target];'''
new='''            if(target<0){\n                StringBuilder rr=new StringBuilder();\n                for(int q=0;q<count && q<8;q++) rr.append(q).append(':').append(ys[q]).append('-').append(ye[q]).append("g").append(gold[q]).append(' ');\n                setTouchDebug("NO TARGET tap="+tapX+","+tapY+" rows="+count,rr.toString(),"");\n                return false;\n            }\n\n            int targetW=xe[target]-xs[target];'''
if old not in s:
    raise SystemExit('target failure insertion point not found')
s=s.replace(old,new,1)

old='''            int delta=target-selected;\n            if(delta<0) return sendRepeatedThenFire(-1,-delta);\n            if(delta>0) return sendRepeatedThenFire(-2,delta);\n            return sendSingle(-5);'''
new='''            int delta=target-selected;\n            StringBuilder rr=new StringBuilder();\n            for(int q=0;q<count && q<8;q++) rr.append(q).append(':').append(ys[q]).append('-').append(ye[q]).append("g").append(gold[q]).append(' ');\n            String cmd=delta<0?("UPx"+(-delta)+"+OK"):(delta>0?("DOWNx"+delta+"+OK"):"OK");\n            setTouchDebug("tap="+tapX+","+tapY+" tgt="+target+" sel="+selected+" d="+delta,\n                    "group="+first+".."+last+" cmd="+cmd,rr.toString());\n            if(delta<0) return sendRepeatedThenFire(-1,-delta);\n            if(delta>0) return sendRepeatedThenFire(-2,delta);\n            return sendSingle(-5);'''
if old not in s:
    raise SystemExit('delta insertion point not found')
s=s.replace(old,new,1)

# Draw persistent diagnostics in the top black letterbox.
old='''            c.drawBitmap(buffer,null,dst,null);\n            drawBottomControls(c,dst);'''
new='''            c.drawBitmap(buffer,null,dst,null);\n            drawBottomControls(c,dst);\n            drawTouchDebug(c,dst);'''
if old not in s:
    raise SystemExit('onDraw insertion point not found')
s=s.replace(old,new,1)

needle='''        private void drawBottomControls(Canvas c,Rect game){'''
method='''        private void drawTouchDebug(Canvas c,Rect game){\n            if(SystemClock.uptimeMillis()>touchDebugUntil) return;\n            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);\n            p.setColor(0xff00ff66);\n            p.setTextSize(18f);\n            p.setTypeface(android.graphics.Typeface.MONOSPACE);\n            float y=24f;\n            c.drawText(touchDebug1,8f,y,p); y+=22f;\n            c.drawText(touchDebug2,8f,y,p); y+=22f;\n            // The row dump can be wider than the phone; split near the middle.\n            String z=touchDebug3;\n            if(z.length()>48){\n                int cut=z.lastIndexOf(' ',48); if(cut<20) cut=48;\n                c.drawText(z.substring(0,cut),8f,y,p); y+=22f;\n                c.drawText(z.substring(Math.min(z.length(),cut+1)),8f,y,p);\n            } else c.drawText(z,8f,y,p);\n        }\n\n        private void drawBottomControls(Canvas c,Rect game){'''
if needle not in s:
    raise SystemExit('drawBottomControls insertion point not found')
s=s.replace(needle,method,1)

p.write_text(s,encoding='utf-8')
print('Applied v10 menu diagnostics to',p)
