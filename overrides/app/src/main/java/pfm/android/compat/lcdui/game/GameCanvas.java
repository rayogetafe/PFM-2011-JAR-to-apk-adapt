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

/** Android-native compatibility surface for the PFM 2011 core. */
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

    private static final int LOGICAL_W=360;
    private static final int LOGICAL_H=503;
    private static final int SWIPE_THRESHOLD=24;
    private static final int DRAG_START_THRESHOLD=13;
    private static final int TAP_MOVE_THRESHOLD=14;
    // The original game polls key-state flags from its own loop. Short pulses can
    // be missed on popup/two-item menus, so keep each key alive for > one tick.
    private static final long KEY_HOLD_MS=82L;
    private static final long KEY_STEP_MS=135L;

    private final Bitmap buffer=Bitmap.createBitmap(LOGICAL_W,LOGICAL_H,Bitmap.Config.ARGB_8888);
    private final GameView view;
    private final int[] pixelScratch=new int[LOGICAL_W*LOGICAL_H];
    private volatile boolean shown;
    private volatile int keyStates;
    private volatile long syntheticBusyUntil;

    protected GameCanvas(boolean suppressKeyEvents) {
        view=new GameView();
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
    }

    public final View androidView(){ return view; }
    public final void markShown(boolean v){ shown=v; }
    public final void showNotifyFromDisplay(){ showNotify(); }
    public boolean isShown(){ return shown; }
    public int getWidth(){ return LOGICAL_W; }
    public int getHeight(){ return LOGICAL_H; }
    public void setFullScreenMode(boolean full){}
    public boolean isDoubleBuffered(){ return true; }
    public int getKeyStates(){ return keyStates; }

    public Graphics getGraphics(){ return new Graphics(buffer); }
    public void flushGraphics(){ AndroidRuntime.main().post(view::invalidate); }
    public void flushGraphics(int x,int y,int w,int h){ flushGraphics(); }
    public void repaint(){ flushGraphics(); }
    public void repaint(int x,int y,int w,int h){ flushGraphics(x,y,w,h); }
    public void serviceRepaints(){ flushGraphics(); }
    public boolean hasPointerEvents(){ return true; }
    public boolean hasPointerMotionEvents(){ return true; }

    public int getKeyCode(int gameAction){
        switch(gameAction){
            case UP:return -1; case DOWN:return -2; case LEFT:return -3;
            case RIGHT:return -4; case FIRE:return -5; default:return gameAction;
        }
    }

    public int getGameAction(int keyCode){
        switch(keyCode){
            case -1: case '2': return UP;
            case -2: case '8': return DOWN;
            case -3: case '4': return LEFT;
            case -4: case '6': return RIGHT;
            case -5: case '5': return FIRE;
            default:return 0;
        }
    }

    protected void keyPressed(int keyCode){}
    protected void keyReleased(int keyCode){}
    protected void keyRepeated(int keyCode){}
    protected void pointerPressed(int x,int y){}
    protected void pointerReleased(int x,int y){}
    protected void pointerDragged(int x,int y){}

    private int legacyKey(int androidCode,KeyEvent e){
        switch(androidCode){
            case KeyEvent.KEYCODE_DPAD_UP:return -1;
            case KeyEvent.KEYCODE_DPAD_DOWN:return -2;
            case KeyEvent.KEYCODE_DPAD_LEFT:return -3;
            case KeyEvent.KEYCODE_DPAD_RIGHT:return -4;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:return -5;
            case KeyEvent.KEYCODE_MENU:return -6;
            case KeyEvent.KEYCODE_BACK:return -7;
            default:
                int u=e.getUnicodeChar();
                return u!=0?u:androidCode;
        }
    }

    private int pressedMask(int legacyKey){
        switch(legacyKey){
            case -1:return UP_PRESSED;
            case -2:return DOWN_PRESSED;
            case -3:return LEFT_PRESSED;
            case -4:return RIGHT_PRESSED;
            case -5:return FIRE_PRESSED;
            default:return 0;
        }
    }

    private void pulseKey(final int key,long delayMs){
        final Handler h=AndroidRuntime.main();
        h.postDelayed(() -> {
            keyStates|=pressedMask(key);
            keyPressed(key);
        },delayMs);
        h.postDelayed(() -> {
            keyReleased(key);
            keyStates&=~pressedMask(key);
        },delayMs+KEY_HOLD_MS);
    }

    private synchronized boolean sendSequence(int[] keys){
        long now=SystemClock.uptimeMillis();
        if(now<syntheticBusyUntil) return false;
        long delay=0L;
        for(int key:keys){
            pulseKey(key,delay);
            delay+=KEY_STEP_MS;
        }
        syntheticBusyUntil=now+delay+55L;
        return true;
    }

    private boolean sendSingle(int key){ return sendSequence(new int[]{key}); }

    private boolean sendRepeatedThenFire(int navKey,int count){
        int n=Math.max(0,Math.min(12,count));
        int[] keys=new int[n+1];
        for(int i=0;i<n;i++) keys[i]=navKey;
        keys[n]=-5;
        return sendSequence(keys);
    }

    /**
     * Route an Android-native action through the exact keypad path used by the
     * original game. The caller supplies a delta from the currently selected
     * legacy row, so this also remains correct when New players remembers its
     * previous selection.
     */
    public final boolean pfmNavigateAndFire(int delta){
        if(delta<0) return sendRepeatedThenFire(-1,-delta);
        if(delta>0) return sendRepeatedThenFire(-2,delta);
        return sendSingle(-5);
    }

    private synchronized boolean directPointerTap(final int x,final int y){
        long now=SystemClock.uptimeMillis();
        if(now<syntheticBusyUntil) return false;
        syntheticBusyUntil=now+110L;
        pointerPressed(x,y);
        AndroidRuntime.main().postDelayed(() -> pointerReleased(x,y),48L);
        return true;
    }

    private boolean isGreen(int color){
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return g>70 && g*100>r*112 && g*100>b*115;
    }

    private boolean isYellow(int color){
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return r>120 && g>80 && b<120 && r*10>b*13 && g*10>b*11;
    }

    private boolean isStrongGold(int color){
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return r>165 && g>100 && g<225 && b<100 && r*100>g*106;
    }

    private void captureFrame(){
        buffer.getPixels(pixelScratch,0,LOGICAL_W,0,0,LOGICAL_W,LOGICAL_H);
    }

    /** The lower-left orange arrow has working native pointer handling. */
    private boolean handleCornerBackTap(int x,int y){
        if(x<=78 && y>=452) return directPointerTap(x,y);
        return false;
    }

    private int rowMinX(int y0,int y1){
        int min=LOGICAL_W;
        for(int y=Math.max(0,y0);y<=Math.min(LOGICAL_H-1,y1);y+=2){
            for(int x=0;x<LOGICAL_W;x+=2){
                int c=pixelScratch[y*LOGICAL_W+x];
                if(isGreen(c)||isYellow(c)){ if(x<min) min=x; }
            }
        }
        return min==LOGICAL_W?-1:min;
    }

    private int rowMaxX(int y0,int y1){
        int max=-1;
        for(int y=Math.max(0,y0);y<=Math.min(LOGICAL_H-1,y1);y+=2){
            for(int x=LOGICAL_W-1;x>=0;x-=2){
                int c=pixelScratch[y*LOGICAL_W+x];
                if(isGreen(c)||isYellow(c)){ if(x>max) max=x; break; }
            }
        }
        return max;
    }

    private int rowYellowScore(int y0,int y1,int x0,int x1){
        int yellow=0,green=0,gold=0;
        for(int y=Math.max(0,y0);y<=Math.min(LOGICAL_H-1,y1);y+=2){
            for(int x=Math.max(0,x0);x<=Math.min(LOGICAL_W-1,x1);x+=3){
                int c=pixelScratch[y*LOGICAL_W+x];
                if(isYellow(c)) yellow++;
                if(isGreen(c)) green++;
                if(isStrongGold(c)) gold++;
            }
        }
        // Selected menu items are overwhelmingly yellow/gold while ordinary
        // siblings are green. Penalising green also prevents title bars winning.
        return yellow*4 + gold*2 - green;
    }

    /**
     * Detect classic green/yellow menu rows and translate a tap into keypad
     * navigation. v8 fixes three issues from v7:
     *  - split pieces of one yellow row are merged (Options no longer +1),
     *  - popup title bars are excluded by stricter geometry matching,
     *  - the scan reaches the bottom of the 503px canvas so Quit is hittable.
     */
    private boolean handleClassicMenuTap(int tapX,int tapY){
        try{
            captureFrame();
            int[] ys=new int[28],ye=new int[28],yc=new int[28];
            int[] xs=new int[28],xe=new int[28],score=new int[28];
            int count=0,runStart=-1;

            for(int y=30;y<LOGICAL_H;y++){
                int colored=0;
                for(int x=4;x<LOGICAL_W-4;x+=4){
                    int c=pixelScratch[y*LOGICAL_W+x];
                    if(isGreen(c)||isYellow(c)) colored++;
                }
                boolean active=colored>=19;
                if(active && runStart<0) runStart=y;

                if((!active || y==LOGICAL_H-1) && runStart>=0){
                    int runEnd=active?y:y-1;
                    int height=runEnd-runStart+1;
                    if(height>=4 && height<=42){
                        int minX=rowMinX(runStart,runEnd);
                        int maxX=rowMaxX(runStart,runEnd);
                        if(minX>=0 && maxX-minX>=68){
                            boolean merge=false;
                            if(count>0){
                                int gap=runStart-ye[count-1]-1;
                                int mergedHeight=runEnd-ys[count-1]+1;
                                merge=gap<=10 && mergedHeight<=46 &&
                                        Math.abs(minX-xs[count-1])<=16 &&
                                        Math.abs(maxX-xe[count-1])<=24;
                            }
                            if(merge){
                                ye[count-1]=runEnd;
                                yc[count-1]=(ys[count-1]+runEnd)/2;
                                xs[count-1]=Math.min(xs[count-1],minX);
                                xe[count-1]=Math.max(xe[count-1],maxX);
                                score[count-1]=rowYellowScore(ys[count-1],ye[count-1],xs[count-1],xe[count-1]);
                            } else if(count<ys.length){
                                ys[count]=runStart; ye[count]=runEnd;
                                yc[count]=(runStart+runEnd)/2;
                                xs[count]=minX; xe[count]=maxX;
                                score[count]=rowYellowScore(runStart,runEnd,minX,maxX);
                                count++;
                            }
                        }
                    }
                    runStart=-1;
                }
            }
            if(count<2) return false;

            // Prefer the actual vertical hit-box of a detected row. Only fall
            // back to the nearest centre when the user taps its shadow/edge.
            int target=-1,bestDist=999;
            for(int i=0;i<count;i++){
                if(tapX<xs[i]-20 || tapX>xe[i]+20) continue;
                int margin=9;
                if(tapY>=ys[i]-margin && tapY<=ye[i]+margin){
                    int d=Math.abs(tapY-yc[i]);
                    if(d<bestDist){ bestDist=d; target=i; }
                }
            }
            if(target<0){
                bestDist=28;
                for(int i=0;i<count;i++){
                    if(tapX<xs[i]-20 || tapX>xe[i]+20) continue;
                    int d=Math.abs(tapY-yc[i]);
                    if(d<bestDist){ bestDist=d; target=i; }
                }
            }
            if(target<0) return false;

            // Build a local menu group with almost identical left/right edges.
            // The Options title is ~12 px further right than its four items, so
            // an 8 px left-edge tolerance intentionally excludes that header.
            int first=target,last=target;
            while(first>0){
                int i=first-1,j=first;
                int gap=yc[j]-yc[i];
                if(gap>68) break;
                if(Math.abs(xs[i]-xs[target])>8 || Math.abs(xe[i]-xe[target])>18) break;
                if(Math.abs((xe[i]-xs[i])-(xe[target]-xs[target]))>24) break;
                first=i;
            }
            while(last<count-1){
                int i=last+1,j=last;
                int gap=yc[i]-yc[j];
                if(gap>68) break;
                if(Math.abs(xs[i]-xs[target])>8 || Math.abs(xe[i]-xe[target])>18) break;
                if(Math.abs((xe[i]-xs[i])-(xe[target]-xs[target]))>24) break;
                last=i;
            }
            if(last-first<1) return false;

            int selected=-1,bestScore=Integer.MIN_VALUE;
            for(int i=first;i<=last;i++){
                if(score[i]>bestScore){ bestScore=score[i]; selected=i; }
            }
            if(selected<0) return false;

            int delta=target-selected;
            if(delta<0) return sendRepeatedThenFire(-1,-delta);
            if(delta>0) return sendRepeatedThenFire(-2,delta);
            return sendSingle(-5);
        } catch(Throwable ignored){ return false; }
    }

    private final class GameView extends View {
        private float downRawX,downRawY;
        private int downX,downY;
        private int downControlKey;
        private int gestureMode; // 0=tap, 1=horizontal swipe, 2=vertical pointer drag
        private final Paint controlsPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint controlsText=new Paint(Paint.ANTI_ALIAS_FLAG);

        GameView(){
            super(AndroidRuntime.activity());
            setKeepScreenOn(true);
            controlsText.setTextAlign(Paint.Align.CENTER);
            controlsText.setTextSize(48f);
            controlsText.setFakeBoldText(true);
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            c.drawColor(0xff000000);
            Rect dst=fit(getWidth(),getHeight());
            c.drawBitmap(buffer,null,dst,null);
            drawBottomControls(c,dst);
        }

        private Rect fit(int w,int h){
            float s=Math.min(w/(float)LOGICAL_W,h/(float)LOGICAL_H);
            int dw=Math.round(LOGICAL_W*s),dh=Math.round(LOGICAL_H*s);
            int l=(w-dw)/2,t=(h-dh)/2;
            return new Rect(l,t,l+dw,t+dh);
        }

        private int lx(float x){
            Rect r=fit(getWidth(),getHeight());
            return Math.max(0,Math.min(LOGICAL_W-1,
                    Math.round((x-r.left)*LOGICAL_W/(float)Math.max(1,r.width()))));
        }

        private int ly(float y){
            Rect r=fit(getWidth(),getHeight());
            return Math.max(0,Math.min(LOGICAL_H-1,
                    Math.round((y-r.top)*LOGICAL_H/(float)Math.max(1,r.height()))));
        }

        private float controlHeight(int margin){
            return Math.min(172f,Math.max(128f,margin-16f));
        }

        private float controlTop(Rect game){
            int margin=getHeight()-game.bottom;
            float h=controlHeight(margin);
            return game.bottom+Math.max(5f,(margin-h)/2f);
        }

        private void drawBottomControls(Canvas c,Rect game){
            int margin=getHeight()-game.bottom;
            if(margin<74) return;
            float h=controlHeight(margin);
            float top=controlTop(game);
            float bottom=Math.min(getHeight()-6f,top+h);
            String[] labels={"◀","▲","OK","▼","▶"};
            for(int i=0;i<5;i++){
                float left=i*getWidth()/5f+3f;
                float right=(i+1)*getWidth()/5f-3f;
                controlsPaint.setColor(i==2?0xff3b7729:0xff292929);
                c.drawRoundRect(left,top,right,bottom,18f,18f,controlsPaint);
                controlsText.setColor(0xffeeeeee);
                float yy=(top+bottom)/2f-(controlsText.ascent()+controlsText.descent())/2f;
                c.drawText(labels[i],(left+right)/2f,yy,controlsText);
            }
        }

        private int controlKeyAt(float rawX,float rawY){
            Rect game=fit(getWidth(),getHeight());
            int margin=getHeight()-game.bottom;
            if(margin<74) return 0;
            float h=controlHeight(margin);
            float top=controlTop(game),bottom=Math.min(getHeight()-6f,top+h);
            if(rawY<top||rawY>bottom) return 0;
            int cell=Math.max(0,Math.min(4,(int)(rawX*5f/Math.max(1,getWidth()))));
            switch(cell){
                case 0:return -3;
                case 1:return -1;
                case 2:return -5;
                case 3:return -2;
                case 4:return -4;
                default:return 0;
            }
        }

        private void haptic(){
            try{ performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
            catch(Throwable ignored){}
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            int x=lx(e.getX()),y=ly(e.getY());
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    downRawX=e.getX(); downRawY=e.getY(); downX=x; downY=y;
                    downControlKey=controlKeyAt(downRawX,downRawY);
                    gestureMode=0;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if(downControlKey!=0) return true;
                    int mdx=x-downX,mdy=y-downY;
                    if(gestureMode==0 &&
                       (Math.abs(mdx)>DRAG_START_THRESHOLD||Math.abs(mdy)>DRAG_START_THRESHOLD)){
                        if(Math.abs(mdy)>Math.abs(mdx)*1.10f){
                            gestureMode=2;
                            pointerPressed(downX,downY);
                            pointerDragged(x,y);
                        } else if(Math.abs(mdx)>Math.abs(mdy)*1.05f){
                            gestureMode=1;
                        }
                    } else if(gestureMode==2){
                        pointerDragged(x,y);
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if(gestureMode==2) pointerReleased(x,y);
                    downControlKey=0; gestureMode=0;
                    return true;

                case MotionEvent.ACTION_UP:
                    int upControl=controlKeyAt(e.getX(),e.getY());
                    if(downControlKey!=0){
                        if(upControl==downControlKey){ haptic(); sendSingle(downControlKey); }
                        downControlKey=0; gestureMode=0;
                        return true;
                    }

                    int dx=x-downX,dy=y-downY;
                    if(gestureMode==2){
                        pointerReleased(x,y);
                        gestureMode=0;
                        return true;
                    }
                    if(gestureMode==1 ||
                       (Math.abs(dx)>SWIPE_THRESHOLD&&Math.abs(dx)>Math.abs(dy))){
                        haptic();
                        sendSingle(dx<0?-3:-4);
                        gestureMode=0;
                        return true;
                    }
                    gestureMode=0;

                    if(Math.abs(dx)<=TAP_MOVE_THRESHOLD&&Math.abs(dy)<=TAP_MOVE_THRESHOLD){
                        haptic();
                        if(handleCornerBackTap(x,y)) return true;
                        if(handleClassicMenuTap(x,y)) return true;
                        sendSingle(-5);
                    }
                    return true;
                default:return true;
            }
        }

        @Override public boolean onKeyDown(int code,KeyEvent e){
            int k=legacyKey(code,e);
            keyStates|=pressedMask(k);
            if(e.getRepeatCount()>0) keyRepeated(k); else keyPressed(k);
            return true;
        }

        @Override public boolean onKeyUp(int code,KeyEvent e){
            int k=legacyKey(code,e);
            keyStates&=~pressedMask(k);
            keyReleased(k);
            return true;
        }
    }
}
