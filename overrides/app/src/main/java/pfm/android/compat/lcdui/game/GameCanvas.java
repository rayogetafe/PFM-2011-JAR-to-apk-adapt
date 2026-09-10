package pfm.android.compat.lcdui.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
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
    private final Bitmap buffer = Bitmap.createBitmap(LOGICAL_W, LOGICAL_H, Bitmap.Config.ARGB_8888);
    private final GameView view;
    private volatile boolean shown;
    private volatile int keyStates;

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

    // MIDP GameCanvas contract. The Alpha 0.84 core calls this during du's
    // constructor; omitting it caused the first Android APK to die at startup
    // with NoSuchMethodError before the first frame was displayed.
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

    private final class GameView extends View {
        GameView() { super(AndroidRuntime.activity()); setKeepScreenOn(true); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(0xff000000);
            Rect dst=fit(getWidth(),getHeight());
            c.drawBitmap(buffer,null,dst,null);
        }
        private Rect fit(int w,int h) {
            float s=Math.min(w/(float)LOGICAL_W,h/(float)LOGICAL_H);
            int dw=Math.round(LOGICAL_W*s), dh=Math.round(LOGICAL_H*s);
            int l=(w-dw)/2, t=(h-dh)/2; return new Rect(l,t,l+dw,t+dh);
        }
        private int lx(float x) { Rect r=fit(getWidth(),getHeight()); return Math.max(0,Math.min(LOGICAL_W-1,Math.round((x-r.left)*LOGICAL_W/(float)Math.max(1,r.width())))); }
        private int ly(float y) { Rect r=fit(getWidth(),getHeight()); return Math.max(0,Math.min(LOGICAL_H-1,Math.round((y-r.top)*LOGICAL_H/(float)Math.max(1,r.height())))); }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int x=lx(e.getX()), y=ly(e.getY());
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: pointerPressed(x,y); return true;
                case MotionEvent.ACTION_MOVE: pointerDragged(x,y); return true;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: pointerReleased(x,y); return true;
                default:return true;
            }
        }
        @Override public boolean onKeyDown(int code, KeyEvent e) {
            int k=legacyKey(code,e);
            keyStates |= pressedMask(k);
            if(e.getRepeatCount()>0) keyRepeated(k); else keyPressed(k);
            return true;
        }
        @Override public boolean onKeyUp(int code, KeyEvent e) {
            int k=legacyKey(code,e);
            keyStates &= ~pressedMask(k);
            keyReleased(k);
            return true;
        }
    }
}
