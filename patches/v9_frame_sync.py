#!/usr/bin/env python3
import re
import sys
from pathlib import Path

p = Path(sys.argv[1])
s = p.read_text(encoding='utf-8')

# Synthetic keypad input must live for exactly one PFM game frame.  The legacy
# core runs at ~33 ms/frame and polls booleans set by keyPressed(); time-based
# 40-82 ms holds therefore produced 2-3 menu moves from one Android tap.
s = s.replace(
    'private static final long KEY_HOLD_MS=82L;\n    private static final long KEY_STEP_MS=135L;',
    'private static final long KEY_HOLD_MS=0L;\n    private static final long KEY_STEP_MS=0L;'
)
s = s.replace(
    'private volatile long syntheticBusyUntil;',
    '''private volatile long syntheticBusyUntil;\n    private final java.util.Vector syntheticQueue=new java.util.Vector();\n    private int syntheticHeldKey;\n    private int syntheticGapFrames;\n    private long lastSyntheticFramePump;'''
)

s = s.replace(
    'public void flushGraphics(){ AndroidRuntime.main().post(view::invalidate); }',
    'public void flushGraphics(){ pumpSyntheticFrame(); AndroidRuntime.main().post(view::invalidate); }'
)

old = re.compile(r'''    private void pulseKey\(final int key,long delayMs\)\{.*?    private boolean sendSingle\(int key\)\{ return sendSequence\(new int\[\]\{key\}\); \}''', re.S)
new = r'''    /**
     * Advance queued Android keypad input on the legacy game's own frame
     * boundary. A key is visible to exactly one bl.b() update, then released;
     * one blank frame separates consecutive synthetic keys so the old repeat
     * logic can never interpret one tap as a held key.
     */
    private synchronized void pumpSyntheticFrame(){
        boolean busy=syntheticHeldKey!=0 || syntheticGapFrames>0 || !syntheticQueue.isEmpty();
        if(!busy) return;
        long now=SystemClock.uptimeMillis();
        // Guard against an occasional extra flushGraphics() inside one frame.
        if(lastSyntheticFramePump!=0L && now-lastSyntheticFramePump<20L) return;
        lastSyntheticFramePump=now;

        if(syntheticHeldKey!=0){
            int key=syntheticHeldKey;
            keyReleased(key);
            keyStates&=~pressedMask(key);
            syntheticHeldKey=0;
            syntheticGapFrames=1;
            return;
        }
        if(syntheticGapFrames>0){
            syntheticGapFrames--;
            return;
        }
        if(!syntheticQueue.isEmpty()){
            int key=((Integer)syntheticQueue.elementAt(0)).intValue();
            syntheticQueue.removeElementAt(0);
            keyStates|=pressedMask(key);
            keyPressed(key);
            syntheticHeldKey=key;
        }
    }

    private synchronized boolean syntheticSequenceActive(){
        return syntheticHeldKey!=0 || syntheticGapFrames>0 || !syntheticQueue.isEmpty();
    }

    private synchronized boolean sendSequence(int[] keys){
        if(keys==null || keys.length==0) return false;
        if(syntheticSequenceActive()) return false;
        for(int key:keys) syntheticQueue.addElement(Integer.valueOf(key));
        // Let the next legacy flush boundary arm the first key.
        lastSyntheticFramePump=0L;
        return true;
    }

    private boolean sendSingle(int key){ return sendSequence(new int[]{key}); }'''

s, n = old.subn(new, s, count=1)
if n != 1:
    raise SystemExit('Could not replace v8 timed synthetic key block')

# Native pointer Back should not fire while a queued keypad sequence is active.
s = s.replace(
    'if(now<syntheticBusyUntil) return false;\n        syntheticBusyUntil=now+95L;',
    'if(now<syntheticBusyUntil || syntheticSequenceActive()) return false;\n        syntheticBusyUntil=now+95L;'
)

p.write_text(s, encoding='utf-8')
print('Applied v9 frame-synced input patch to', p)
