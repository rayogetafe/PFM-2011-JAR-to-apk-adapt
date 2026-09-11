package pfm.android.compat.lcdui.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import pfm.android.AndroidRuntime;
import pfm.android.compat.lcdui.Displayable;
import pfm.android.compat.lcdui.Graphics;

/**
 * Android-native compatibility surface for the small subset of MIDP GameCanvas
 * used by the PFM 2011 core. No J2ME runtime or emulator is involved.
 */
public class GameCanvas extends Displayable {
    public static final int UP=1, LEFT=2, RIGHT=5, DOWN=6, FIRE=8;
    public static final int UP_PRESSED=0x0002;
    public static final int LEFT_PRESSED=0x0004;
    public static final int RIGHT_PRESSED=0x0020;
    public static final int DOWN_PRESSED=0x0040;
    public static final int FIRE_PRESSED=0x0100;
    public static final int GAME_A_PRESSED=0x0200;
    public static final int GAME_B_PRESSED=0x0400;
    public static final int GAME_C_PRESSED=0x0800;
    public static final int GAME_D_PRESSED=0x1000;

    private static final int LOGICAL_W = 360;
    private static final int LOGICAL_H = 503;
    private static final int SWIPE_THRESHOLD = 24;
    private static final int DRAG_START_THRESHOLD = 13;
    private static final int TAP_MOVE_THRESHOLD = 14;
    private static final long KEY_HOLD_MS = 40L;
    private static final long KEY_STEP_MS = 68L;

    private final Bitmap buffer = Bitmap.createBitmap(LOGICAL_W, LOGICAL_H, Bitmap.Config.ARGB_8888);
    private final GameView view;
    private final int[] pixelScratch = new int[LOGICAL_W * LOGICAL_H];
    private volatile boolean shown;
    private volatile int keyStates;
    private volatile long syntheticBusyUntil;

    protected GameCanvas(boolean suppressKeyEvents) {
        view = new GameView();
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
    }

    public final View androidView() { return view; }
    public final void markShown(boolean v) { shown=v; }
    public final void showNotifyFromDisplay() { showNotify(); }
    public boolean isShown() { return shown; }
    public int getWidth() { return LOGICAL_W; }
    public int getHeight() { return LOGICAL_H; }
    public void setFullScreenMode(boolean full) {}

    public boolean isDoubleBuffered() { return true; }
    public int getKeyStates() { return keyStates; }

    public Graphics getGraphics() { return new Graphics(buffer); }
    public void flushGraphics() { AndroidRuntime.main().post(view::invalidate); }
    public void flushGraphics(int x,int y,int w,int h) { flushGraphics(); }
    public void repaint() { flushGraphics(); }
    public void repaint(int x,int y,int w,int h) { flushGraphics(x,y,w,h); }
    public void serviceRepaints() { flushGraphics(); }
    public boolean hasPointerEvents() { return true; }
    public boolean hasPointerMotionEvents() { return true; }

    public int getKeyCode(int gameAction) {
        switch(gameAction) { case UP:return -1; case DOWN:return -2; case LEFT:return -3; case RIGHT:return -4; case FIRE:return -5; default:return gameAction; }
    }
    public int getGameAction(int keyCode) {
        switch(keyCode) {
            case -1: case '2': return UP;
            case -2: case '8': return DOWN;
            case -3: case '4': return LEFT;
            case -4: case '6': return RIGHT;
            case -5: case '5': return FIRE;
            default: return 0;
        }
    }
    protected void keyPressed(int keyCode) {}
    protected void keyReleased(int keyCode) {}
    protected void keyRepeated(int keyCode) {}
    protected void pointerPressed(int x,int y) {}
    protected void pointerReleased(int x,int y) {}
    protected void pointerDragged(int x,int y) {}

    private int legacyKey(int androidCode, KeyEvent e) {
        switch(androidCode) {
            case KeyEvent.KEYCODE_DPAD_UP:return -1; case KeyEvent.KEYCODE_DPAD_DOWN:return -2;
            case KeyEvent.KEYCODE_DPAD_LEFT:return -3; case KeyEvent.KEYCODE_DPAD_RIGHT:return -4;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:return -5;
            case KeyEvent.KEYCODE_MENU:return -6; case KeyEvent.KEYCODE_BACK:return -7;
            default:
                int u=e.getUnicodeChar(); return u!=0 ? u : androidCode;
        }
    }

    private int pressedMask(int legacyKey) {
        switch (legacyKey) {
            case -1: return UP_PRESSED;
            case -2: return DOWN_PRESSED;
            case -3: return LEFT_PRESSED;
            case -4: return RIGHT_PRESSED;
            case -5: return FIRE_PRESSED;
            default: return 0;
        }
    }

    private void pulseKey(final int key, long delayMs) {
        final Handler h = AndroidRuntime.main();
        h.postDelayed(() -> {
            keyStates |= pressedMask(key);
            keyPressed(key);
        }, delayMs);
        h.postDelayed(() -> {
            keyReleased(key);
            keyStates &= ~pressedMask(key);
        }, delayMs + KEY_HOLD_MS);
    }

    /** Queue one deterministic synthetic sequence and ignore extra taps until it finishes. */
    private synchronized boolean sendSequence(int[] keys) {
        long now=SystemClock.uptimeMillis();
        if (now < syntheticBusyUntil) return false;
        long delay=0L;
        for (int key : keys) {
            pulseKey(key, delay);
            delay += KEY_STEP_MS;
        }
        syntheticBusyUntil=now+delay+35L;
        return true;
    }

    private boolean sendSingle(int key) { return sendSequence(new int[]{key}); }

    private boolean sendRepeatedThenFire(int navKey, int count) {
        int n=Math.max(0,Math.min(8,count));
        int[] keys=new int[n+1];
        for (int i=0;i<n;i++) keys[i]=navKey;
        keys[n]=-5;
        return sendSequence(keys);
    }

    private boolean isGreen(int color) {
        int r=(color>>16)&255, g=(color>>8)&255, b=color&255;
        return g>70 && g*100>r*112 && g*100>b*115;
    }

    private boolean isYellow(int color) {
        int r=(color>>16)&255, g=(color>>8)&255, b=color&255;
        return r>120 && g>80 && b<120 && r*10>b*13 && g*10>b*11;
    }

    private boolean isStrongGold(int color) {
        int r=(color>>16)&255, g=(color>>8)&255, b=color&255;
        return r>165 && g>100 && g<220 && b<95 && r*100>g*108;
    }

    private boolean isOrange(int color) {
        int r=(color>>16)&255, g=(color>>8)&255, b=color&255;
        return r>105 && g>45 && b<105 && r*100>g*104 && r*100>b*135;
    }

    private void captureFrame() {
        buffer.getPixels(pixelScratch,0,LOGICAL_W,0,0,LOGICAL_W,LOGICAL_H);
    }

    /** Special orange back-arrow used in the lower-left corner of many screens. */
    private boolean handleCornerBackTap(int x,int y) {
        if (x<=72 && y>=458) return sendSingle(-7);
        return false;
    }

    /**
     * Handles horizontal groups of orange/gold buttons such as Calendar
     * Back / FW / BW. The bright yellow button is the current selection;
     * tapping a sibling becomes LEFT/RIGHT navigation followed by FIRE.
     */
    private boolean handleMultiGoldButtonTap(int tapX,int tapY) {
        try {
            captureFrame();
            int bestY=-1, bestPixels=0;
            int y0=Math.max(20,tapY-18), y1=Math.min(LOGICAL_H-20,tapY+18);
            for (int y=y0;y<=y1;y++) {
                int n=0;
                for (int x=0;x<LOGICAL_W;x+=2) {
                    int c=pixelScratch[y*LOGICAL_W+x];
                    if (isOrange(c) || isStrongGold(c)) n++;
                }
                if (n>bestPixels) { bestPixels=n; bestY=y; }
            }
            if (bestY<0 || bestPixels<22) return false;

            int[] starts=new int[8], ends=new int[8], centers=new int[8], bright=new int[8], total=new int[8];
            int count=0, start=-1, lastGood=-99;
            for (int x=0;x<LOGICAL_W;x++) {
                int good=0, strong=0;
                for (int yy=Math.max(0,bestY-3);yy<=Math.min(LOGICAL_H-1,bestY+3);yy++) {
                    int c=pixelScratch[yy*LOGICAL_W+x];
                    if (isOrange(c) || isYellow(c)) good++;
                    if (isStrongGold(c)) strong++;
                }
                if (good>=2) {
                    if (start<0) start=x;
                    lastGood=x;
                } else if (start>=0 && x-lastGood>8) {
                    int end=lastGood;
                    if (end-start>=28 && count<8) {
                        starts[count]=start; ends[count]=end; centers[count]=(start+end)/2;
                        for (int xx=start;xx<=end;xx+=2) {
                            for (int yy=Math.max(0,bestY-6);yy<=Math.min(LOGICAL_H-1,bestY+6);yy+=2) {
                                int c=pixelScratch[yy*LOGICAL_W+xx];
                                if (isOrange(c)||isYellow(c)) total[count]++;
                                if (isStrongGold(c)) bright[count]++;
                            }
                        }
                        count++;
                    }
                    start=-1;
                }
            }
            if (start>=0) {
                int end=lastGood;
                if (end-start>=28 && count<8) {
                    starts[count]=start; ends[count]=end; centers[count]=(start+end)/2;
                    for (int xx=start;xx<=end;xx+=2) {
                        for (int yy=Math.max(0,bestY-6);yy<=Math.min(LOGICAL_H-1,bestY+6);yy+=2) {
                            int c=pixelScratch[yy*LOGICAL_W+xx];
                            if (isOrange(c)||isYellow(c)) total[count]++;
                            if (isStrongGold(c)) bright[count]++;
                        }
                    }
                    count++;
                }
            }
            if (count<2) return false;

            int target=-1;
            for (int i=0;i<count;i++) {
                if (tapX>=starts[i]-8 && tapX<=ends[i]+8) { target=i; break; }
            }
            if (target<0) {
                int best=32;
                for (int i=0;i<count;i++) {
                    int d=Math.abs(tapX-centers[i]);
                    if (d<best) { best=d; target=i; }
                }
            }
            if (target<0) return false;

            int selected=0;
            double selectedScore=-1.0;
            for (int i=0;i<count;i++) {
                double score=total[i]==0 ? 0.0 : bright[i]/(double)total[i];
                if (score>selectedScore) { selectedScore=score; selected=i; }
            }
            int delta=target-selected;
            if (delta<0) return sendRepeatedThenFire(-3,-delta);
            if (delta>0) return sendRepeatedThenFire(-4,delta);
            return sendSingle(-5);
        } catch (Throwable ignored) { return false; }
    }

    /**
     * Recognises the original PFM green/yellow horizontal menu rows anywhere
     * on screen. A tap is translated to deterministic UP/DOWN navigation + FIRE.
     */
    private boolean handleClassicMenuTap(int tapY) {
        try {
            captureFrame();
            int[] starts=new int[20], ends=new int[20], centers=new int[20], gold=new int[20];
            int count=0, runStart=-1, runGold=0;

            for (int y=35;y<475;y++) {
                int colored=0, rowGold=0;
                for (int x=6;x<LOGICAL_W-6;x+=4) {
                    int c=pixelScratch[y*LOGICAL_W+x];
                    if (isGreen(c)||isYellow(c)) colored++;
                    if (isStrongGold(c)) rowGold++;
                }
                boolean active=colored>=24;
                if (active) {
                    if (runStart<0) { runStart=y; runGold=0; }
                    runGold+=rowGold;
                }
                if ((!active || y==474) && runStart>=0) {
                    int runEnd=active?y:y-1;
                    int height=runEnd-runStart+1;
                    if (height>=5 && height<=38) {
                        if (count>0 && runStart-ends[count-1]-1<=4 && runEnd-starts[count-1]+1<=38) {
                            ends[count-1]=runEnd;
                            centers[count-1]=(starts[count-1]+runEnd)/2;
                            gold[count-1]+=runGold;
                        } else if (count<centers.length) {
                            starts[count]=runStart; ends[count]=runEnd;
                            centers[count]=(runStart+runEnd)/2; gold[count]=runGold; count++;
                        }
                    }
                    runStart=-1; runGold=0;
                }
            }

            if (count<2) return false;
            int selected=-1, selectedGold=16;
            for (int i=0;i<count;i++) if (gold[i]>selectedGold) { selectedGold=gold[i]; selected=i; }
            if (selected<0) return false;

            int target=-1, bestDist=29;
            for (int i=0;i<count;i++) {
                int d=Math.abs(tapY-centers[i]);
                if (d<bestDist) { bestDist=d; target=i; }
            }
            if (target<0) return false;

            int delta=target-selected;
            if (delta<0) return sendRepeatedThenFire(-1,-delta);
            if (delta>0) return sendRepeatedThenFire(-2,delta);
            return sendSingle(-5);
        } catch (Throwable ignored) { return false; }
    }

    private final class GameView extends View {
        private float downRawX,downRawY;
        private int downX,downY;
        private int downControlKey;
        // 0=tap candidate, 1=horizontal swipe, 2=vertical pointer drag.
        private int gestureMode;
        private final Paint controlsPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint controlsText=new Paint(Paint.ANTI_ALIAS_FLAG);

        GameView() {
            super(AndroidRuntime.activity());
            setKeepScreenOn(true);
            controlsText.setTextAlign(Paint.Align.CENTER);
            controlsText.setTextSize(36f);
            controlsText.setFakeBoldText(true);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(0xff000000);
            Rect dst=fit(getWidth(),getHeight());
            c.drawBitmap(buffer,null,dst,null);
            drawBottomControls(c,dst);
        }

        private Rect fit(int w,int h) {
            float s=Math.min(w/(float)LOGICAL_W,h/(float)LOGICAL_H);
            int dw=Math.round(LOGICAL_W*s), dh=Math.round(LOGICAL_H*s);
            int l=(w-dw)/2, t=(h-dh)/2; return new Rect(l,t,l+dw,t+dh);
        }

        private int lx(float x) {
            Rect r=fit(getWidth(),getHeight());
            return Math.max(0,Math.min(LOGICAL_W-1,Math.round((x-r.left)*LOGICAL_W/(float)Math.max(1,r.width()))));
        }
        private int ly(float y) {
            Rect r=fit(getWidth(),getHeight());
            return Math.max(0,Math.min(LOGICAL_H-1,Math.round((y-r.top)*LOGICAL_H/(float)Math.max(1,r.height()))));
        }

        private float controlHeight(int margin) {
            return Math.min(92f,Math.max(68f,margin-22f));
        }

        private float controlTop(Rect game) {
            int margin=getHeight()-game.bottom;
            float h=controlHeight(margin);
            return game.bottom+Math.max(7f,(margin-h)/2f-3f);
        }

        private void drawBottomControls(Canvas c,Rect game) {
            int margin=getHeight()-game.bottom;
            if (margin<72) return;
            float h=controlHeight(margin);
            float top=controlTop(game);
            float bottom=Math.min(getHeight()-8f,top+h);
            String[] labels={"◀","▲","OK","▼","▶"};
            for (int i=0;i<5;i++) {
                float left=i*getWidth()/5f+3f;
                float right=(i+1)*getWidth()/5f-3f;
                controlsPaint.setColor(i==2?0xff3b7729:0xff292929);
                c.drawRoundRect(left,top,right,bottom,16f,16f,controlsPaint);
                controlsText.setColor(0xffeeeeee);
                float yy=(top+bottom)/2f-(controlsText.ascent()+controlsText.descent())/2f;
                c.drawText(labels[i],(left+right)/2f,yy,controlsText);
            }
        }

        private int controlKeyAt(float rawX,float rawY) {
            Rect game=fit(getWidth(),getHeight());
            int margin=getHeight()-game.bottom;
            if (margin<72) return 0;
            float h=controlHeight(margin);
            float top=controlTop(game), bottom=Math.min(getHeight()-8f,top+h);
            if (rawY<top || rawY>bottom) return 0;
            int cell=Math.max(0,Math.min(4,(int)(rawX*5f/Math.max(1,getWidth()))));
            switch(cell) {
                case 0:return -3;
                case 1:return -1;
                case 2:return -5;
                case 3:return -2;
                case 4:return -4;
                default:return 0;
            }
        }

        private void haptic() {
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int x=lx(e.getX()), y=ly(e.getY());
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX=e.getX(); downRawY=e.getY(); downX=x; downY=y;
                    downControlKey=controlKeyAt(downRawX,downRawY);
                    gestureMode=0;
                    // Do NOT forward pointerPressed yet. A tap must produce exactly
                    // one synthetic action; pointer events are reserved for scroll drags.
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (downControlKey!=0) return true;
                    int mdx=x-downX, mdy=y-downY;
                    if (gestureMode==0 && (Math.abs(mdx)>DRAG_START_THRESHOLD || Math.abs(mdy)>DRAG_START_THRESHOLD)) {
                        if (Math.abs(mdy)>Math.abs(mdx)*1.10f) {
                            gestureMode=2;
                            pointerPressed(downX,downY);
                            pointerDragged(x,y);
                        } else if (Math.abs(mdx)>Math.abs(mdy)*1.05f) {
                            gestureMode=1;
                        }
                    } else if (gestureMode==2) {
                        pointerDragged(x,y);
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if (gestureMode==2) pointerReleased(x,y);
                    downControlKey=0; gestureMode=0;
                    return true;

                case MotionEvent.ACTION_UP:
                    int upControl=controlKeyAt(e.getX(),e.getY());
                    if (downControlKey!=0) {
                        if (upControl==downControlKey) { haptic(); sendSingle(downControlKey); }
                        downControlKey=0; gestureMode=0;
                        return true;
                    }

                    int dx=x-downX, dy=y-downY;
                    if (gestureMode==2) {
                        pointerReleased(x,y);
                        gestureMode=0;
                        return true;
                    }
                    if (gestureMode==1 || (Math.abs(dx)>SWIPE_THRESHOLD && Math.abs(dx)>Math.abs(dy))) {
                        haptic();
                        sendSingle(dx<0?-3:-4);
                        gestureMode=0;
                        return true;
                    }
                    gestureMode=0;

                    if (Math.abs(dx)<=TAP_MOVE_THRESHOLD && Math.abs(dy)<=TAP_MOVE_THRESHOLD) {
                        haptic();
                        // Exact taps never also go through the legacy pointer path.
                        if (handleCornerBackTap(x,y)) return true;
                        if (handleMultiGoldButtonTap(x,y)) return true;
                        if (handleClassicMenuTap(y)) return true;
                        sendSingle(-5);
                    }
                    return true;
                default:return true;
            }
        }

        @Override public boolean onKeyDown(int code,KeyEvent e) {
            int k=legacyKey(code,e);
            keyStates|=pressedMask(k);
            if(e.getRepeatCount()>0) keyRepeated(k); else keyPressed(k);
            return true;
        }
        @Override public boolean onKeyUp(int code,KeyEvent e) {
            int k=legacyKey(code,e);
            keyStates&=~pressedMask(k);
            keyReleased(k);
            return true;
        }
    }
}
