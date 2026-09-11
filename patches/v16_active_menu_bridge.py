#!/usr/bin/env python3
import sys
from pathlib import Path

p=Path(sys.argv[1])
s=p.read_text(encoding='utf-8')

marker='''    /** The lower-left orange arrow has working native pointer handling. */'''
helper=r'''    /**
     * Ask the default-package bridge to hit-test the game's real active v/w
     * menu model.  This bypasses image-row -> N x UP/DOWN translation entirely.
     */
    private boolean handleCoreMenuTap(int x,int y){
        try{
            Class<?> bridge=Class.forName("PfmTouchBridge");
            java.lang.reflect.Method m=bridge.getMethod("tapActiveMenu",Integer.TYPE,Integer.TYPE);
            Object r=m.invoke(null,Integer.valueOf(x),Integer.valueOf(y));
            return Boolean.TRUE.equals(r);
        }catch(Throwable ignored){
            return false;
        }
    }

'''
if 'handleCoreMenuTap(int x,int y)' not in s:
    if marker not in s:
        raise SystemExit('corner-back marker not found')
    s=s.replace(marker,helper+marker,1)

needle='''                        if (handleCornerBackTap(x,y)) return true;'''
replacement='''                        if (handleCornerBackTap(x,y)) return true;\n                        // Preferred Android path: actual active legacy menu and\n                        // its real selectable child hitboxes.  Only fall back to\n                        // image recognition for screens that are not v/w menus.\n                        if (handleCoreMenuTap(x,y)) return true;'''
if 'if (handleCoreMenuTap(x,y)) return true;' not in s:
    if needle not in s:
        raise SystemExit('ACTION_UP insertion point not found')
    s=s.replace(needle,replacement,1)

p.write_text(s,encoding='utf-8')
print('Applied v16 active-menu touch bridge to',p)
