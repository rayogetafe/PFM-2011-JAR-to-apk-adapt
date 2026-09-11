#!/usr/bin/env python3
import re
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

if 'if (handleCoreMenuTap(x,y)) return true;' not in s:
    # Formatting of ACTION_UP changed in v14; find the semantic call rather than
    # relying on one exact whitespace layout.
    pat=re.compile(r'(?P<indent>[ \t]*)if\s*\(\s*handleCornerBackTap\s*\(\s*x\s*,\s*y\s*\)\s*\)\s*return\s+true\s*;')
    m=pat.search(s)
    if not m:
        raise SystemExit('ACTION_UP semantic insertion point not found')
    indent=m.group('indent')
    old=m.group(0)
    new=(old+'\n'+indent+'// Preferred Android path: actual active legacy menu and\n'
         +indent+'// its real selectable child hitboxes. Only fall back to image\n'
         +indent+'// recognition for screens that are not v/w menus.\n'
         +indent+'if (handleCoreMenuTap(x,y)) return true;')
    s=s[:m.start()]+new+s[m.end():]

p.write_text(s,encoding='utf-8')
print('Applied v16 active-menu touch bridge to',p)
