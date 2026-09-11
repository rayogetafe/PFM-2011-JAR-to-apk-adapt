#!/usr/bin/env python3
import sys
from pathlib import Path

p=Path(sys.argv[1])
s=p.read_text(encoding='utf-8')

# Instead of simulating UP/DOWN to move the highlight, use the actual legacy
# menu controller. The active PFM state (a bl subclass) is stored in dd.a; most
# menu states own one private field of runtime type v. v inherits dv.e(int),
# which is the game's own selection setter: it updates d and notifies the menu
# container so the highlight is committed exactly as native navigation would.
needle='''    /** The lower-left orange arrow has working native pointer handling. */\n    private boolean handleCornerBackTap(int x,int y){\n        if(x<=78 && y>=452) return directPointerTap(x,y);\n        return false;\n    }\n'''
insert='''    /** The lower-left orange arrow has working native pointer handling. */\n    private boolean handleCornerBackTap(int x,int y){\n        if(x<=78 && y>=452) return directPointerTap(x,y);\n        return false;\n    }\n\n    /** Locate the currently active legacy bl state through dd.a. */\n    private Object currentLegacyState(){\n        try{\n            for(java.lang.reflect.Field f:getClass().getFields()){\n                if(java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;\n                if(!"bl".equals(f.getType().getName())) continue;\n                Object state=f.get(this);\n                if(state!=null) return state;\n            }\n        } catch(Throwable ignored){}\n        return null;\n    }\n\n    /** Locate the state's live v menu controller (ca, main menu states, etc.). */\n    private Object currentLegacyMenu(){\n        Object state=currentLegacyState();\n        if(state==null) return null;\n        try{\n            for(Class<?> c=state.getClass();c!=null;c=c.getSuperclass()){\n                for(java.lang.reflect.Field f:c.getDeclaredFields()){\n                    if(!"v".equals(f.getType().getName())) continue;\n                    f.setAccessible(true);\n                    Object menu=f.get(state);\n                    if(menu!=null) return menu;\n                }\n            }\n        } catch(Throwable ignored){}\n        return null;\n    }\n\n    /**\n     * Select one menu item through PFM's own dv.e(index) method, then press only\n     * FIRE. This bypasses the fragile Android UP/DOWN emulation entirely.\n     */\n    private synchronized boolean directLegacyMenuSelectAndFire(final int index){\n        if(index<0) return false;\n        long now=SystemClock.uptimeMillis();\n        if(now<syntheticBusyUntil) return false;\n        final Object menu=currentLegacyMenu();\n        if(menu==null) return false;\n        try{\n            java.lang.reflect.Method setter=null;\n            for(java.lang.reflect.Method m:menu.getClass().getMethods()){\n                Class<?>[] pt=m.getParameterTypes();\n                if("e".equals(m.getName()) && pt.length==1 && pt[0]==Integer.TYPE){\n                    setter=m; break;\n                }\n            }\n            if(setter==null) return false;\n            setter.invoke(menu,Integer.valueOf(index));\n            syntheticBusyUntil=now+420L;\n            AndroidRuntime.main().post(view::invalidate);\n\n            // FIRE is safe to hold longer: unlike directional flags, v.b()\n            // consumes dd.i once and clears it. No repeated navigation occurs.\n            AndroidRuntime.main().postDelayed(() -> {\n                keyStates|=FIRE_PRESSED;\n                keyPressed(-5);\n                AndroidRuntime.main().postDelayed(() -> {\n                    keyReleased(-5);\n                    keyStates&=~FIRE_PRESSED;\n                },180L);\n            },70L);\n            return true;\n        } catch(Throwable ignored){\n            return false;\n        }\n    }\n'''
if needle not in s:
    raise SystemExit('corner back insertion point not found')
s=s.replace(needle,insert,1)

# v11 creates menuFirst after excluding popup title rows. Use that exact visible
# row index as the legacy v selection index. Keep the old colour-score navigation
# only as a compatibility fallback if reflection cannot find a v controller.
needle='''            // Tapping the decorative title itself should do nothing, not FIRE.\n            if(target<menuFirst) return true;\n\n            int selected=-1,bestScore=Integer.MIN_VALUE;'''
replacement='''            // Tapping the decorative title itself should do nothing, not FIRE.\n            if(target<menuFirst) return true;\n\n            int directIndex=target-menuFirst;\n            if(directLegacyMenuSelectAndFire(directIndex)) return true;\n\n            int selected=-1,bestScore=Integer.MIN_VALUE;'''
if needle not in s:
    raise SystemExit('direct select insertion point not found')
s=s.replace(needle,replacement,1)

p.write_text(s,encoding='utf-8')
print('Applied v13 direct legacy menu selection bridge to',p)
