/*
 * NativeCanvas.java
 *
 * Midp Canvas wrapper.
 *
 * @author Vladimir Kryukov
 */

package jimm.ui;

import DrawControls.*;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Kryukov
 */
public class NativeCanvas extends Canvas {
    public static final int UIUPDATE_TIME = 250;
    public static final Object setDisplaySemaphore = new Object();

    private static CanvasEx canvas = null;
    private static Image bDIimage = null;

    private static String leftButton = null;
    private static String middleButton = null;
    private static String rightButton = null;
    private static NativeCanvas instance = new NativeCanvas();
    private static boolean fullScreen = false;
    private GraphicsEx graphicsEx = new GraphicsEx();
    
    /** Creates a new instance of NativeCanvas */
    private NativeCanvas() {
    }
    
    
    /**
     * 
     * 
     * @see paint
     */
    protected void paint(Graphics g) {
        if (isDoubleBuffered()) {
            paintAllOnGraphics(g);
        } else {
            try {
                if ((null == bDIimage) || (bDIimage.getHeight() != getHeight())) {
                    bDIimage = Image.createImage(getWidth(), getHeight());
                }
                paintAllOnGraphics(bDIimage.getGraphics());
                g.drawImage(bDIimage, 0, 0, Graphics.TOP | Graphics.LEFT);
            } catch (Exception e) {
                paintAllOnGraphics(g);
            }
        }
    }
    
    // #sijapp cond.if target="MIDP2" #
    protected void showNotify() {
        if (Jimm.isPaused() && Jimm.isPhone(Jimm.PHONE_SE)) {
            Jimm.setMinimized(false);
        }
        updateMetrix();
    }
    // #sijapp cond.end #
//    protected void hideNotify() {
//    }
    
    private void paintAllOnGraphics(Graphics graphics) {

        CanvasEx c = canvas;
        if (null != c) {
            graphicsEx.setGraphics(graphics);

            // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
            if (!NativeCanvas.isFullScreen()) {
                if (c instanceof VirtualList) {
                    setTitle(((VirtualList)c).getCaption());
                } else {
                    setTitle(null);
                }
            }
            // #sijapp cond.end #
            try {
                if (graphicsEx.getClipY() < getScreenHeight()) {
                    c.paint(graphicsEx);
                }
            } catch(Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                //DebugLog.panic("native", e);
                // #sijapp cond.end #
            }
            // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
            graphicsEx.setStrokeStyle(Graphics.SOLID);
            int h = graphicsEx.getSoftBarSize(rightButton, time, leftButton);
            if (0 < h) {
                if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
                    graphicsEx.drawSoftBar(rightButton, time, leftButton, h);
                } else {
                    graphicsEx.drawSoftBar(leftButton, time, rightButton, h);
                }
            }
            // #sijapp cond.end #
            graphicsEx.reset();
        }
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    private int pointerPressY;
    private int pointerPressX;
    private long pointerPressTime;
    private boolean pointerDraged;
    
    protected void pointerReleased(int x, int y) {
        if (getScreenHeight() <= y) {
            int w = getWidth();
            int lsoftWidth = w / 2 - 20;
            int rsoftWidth = w - lsoftWidth;
            int lSoft = LEFT_SOFT;
            int rSoft = RIGHT_SOFT;
            if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
                rSoft = LEFT_SOFT;
                lSoft = RIGHT_SOFT;
            }
            if (x < lsoftWidth) {
                emulateKey(lSoft);

            } else if (rsoftWidth < x) {
                emulateKey(rSoft);

            } else {
                emulateKey(KEY_NUM5);
            }
            pointerPressTime = 0;
            return;
        }
        pointerDraged |= Math.abs(x - pointerPressX) > 10;
        pointerDraged |= Math.abs(y - pointerPressY) > 10;
        CanvasEx c = canvas;
        if (null != c) {
            if (pointerDraged) {
                c.stylusMoved(pointerPressX, pointerPressY, x, y);
            } else {
                c.stylusTap(x, y, (System.currentTimeMillis() - pointerPressTime) > 500);
            }
        }
        pointerPressTime = 0;
    }

    protected void pointerPressed(int x, int y) {
        pointerDraged = false;
        pointerPressX = x;
        pointerPressY = y;
        pointerPressTime = System.currentTimeMillis();
    }

    protected void pointerDragged(int x, int y) {
        pointerDraged |= 10 < Math.abs(x - pointerPressX);
        pointerDraged |= 10 < Math.abs(y - pointerPressY);
        CanvasEx c = canvas;
        if (pointerDraged && (0 < pointerPressTime) && (null != c)) {
            c.stylusMoving(pointerPressX, pointerPressY, x, y);
        }
    }
    // #sijapp cond.end#
    
    static public void setCommands(String left, String middle, String right) {
        leftButton   = ResourceBundle.getString(left);
        middleButton = ResourceBundle.getString(middle);
        rightButton  = ResourceBundle.getString(right);
        instance.setCommandListener(null);
        instance.updateMetrix();
    }

    public static void setCanvas(CanvasEx canvasEx) {
        canvas = canvasEx;
        KeyRepeatTimer.stop();
    }

    public static boolean isFullScreen() {
        return fullScreen;
    }

    // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
    public static void setCaption(String capt) {
        instance.setTitle(capt);
    }
    // #sijapp cond.end#
    
    /**
     * Set fullscreen mode
     */
    public static void setFullScreen(boolean value) {
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        if (fullScreen == value) return;
        fullScreen = value;
        instance.setFullScreenMode(fullScreen);
        instance.resetMetrix();
        instance.updateMetrix();
        CanvasEx c = canvas;
        if (null != c) {
            c.setDisplayableToDisplay();
        }
        // #sijapp cond.end#
    }
    
//    protected void hideNotify() {
//    }
    
    public static final int LEFT_SOFT  = 0x00100000;

    public static final int RIGHT_SOFT = 0x00100001;
    public static final int CLEAR_KEY  = 0x00100002;
    public static final int CLOSE_KEY  = 0x00100003;
    public static final int CALL_KEY   = 0x00100004;
    public static final int CAMERA_KEY = 0x00100005;
    public static final int ABC_KEY    = 0x00100006;
    public static final int VOLPLUS_KEY  = 0x00100007;
    public static final int VOLMINUS_KEY = 0x00100008;
    public static final int NAVIKEY_RIGHT = 0x00100009;
    public static final int NAVIKEY_LEFT  = 0x0010000A;
    public static final int NAVIKEY_UP    = 0x0010000B;
    public static final int NAVIKEY_DOWN  = 0x0010000C;
    public static final int NAVIKEY_FIRE  = 0x0010000D;
    public static int getKey(int code) {
        int leftSoft  = LEFT_SOFT;
        int rightSoft = RIGHT_SOFT;
        if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
            leftSoft  = RIGHT_SOFT;
            rightSoft = LEFT_SOFT;
        }
        if(code == -6 || code == -21 || code == 21 || code == 105
                || code == -202 || code == 113 || code == 57345
                || code == 0xFFBD) {
            return leftSoft;
        }
        if (code == -7 || code == -22 || code == 22 || code == 106
                || code == -203 || code == 112 || code == 57346
                || code == 0xFFBB) {
            return rightSoft;
        }
        if (-5 == code) {
            return NAVIKEY_FIRE;
        }
        if (code == -8) {
            return CLEAR_KEY;
        }
        if ((-11 == code) || (-12 == code)) {
            return CLOSE_KEY;
        }
        if ((-26 == code) || (-24 == code)) {
            return CAMERA_KEY;
        }
        if (code == -10) {
            return CALL_KEY;
        }
        if (code == -50 || code == 1048582) {
            return ABC_KEY;
        }
        if (code == -36) {
            return VOLPLUS_KEY;
        }
        if (code == -37) {
            return VOLMINUS_KEY;
        }
        return code;
    }

    protected void keyPressed(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        doKeyReaction(keyCode, CanvasEx.KEY_PRESSED);
    }
    
    protected void keyRepeated(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        //doKeyReaction(keyCode, CanvasEx.KEY_REPEATED);
    }
    
    protected void keyReleased(int keyCode) {
        doKeyReaction(keyCode, CanvasEx.KEY_RELEASED);
    }

    public static int getAction(int key, int keyCode) {
        if (key != keyCode) {
            return key;
        }
        try {// getGameAction can raise exception
            int action = instance.getGameAction(keyCode);
            switch (action) {
                case Canvas.RIGHT: return NAVIKEY_RIGHT;
                case Canvas.LEFT:  return NAVIKEY_LEFT;
                case Canvas.UP:    return NAVIKEY_UP;
                case Canvas.DOWN:  return NAVIKEY_DOWN;
                case Canvas.FIRE:  return NAVIKEY_FIRE;
            }
        } catch(Exception e) {
        }
        return key;
    }
    public static void doKeyReaction(int keyCode, int type) {
        CanvasEx c = canvas;
        if (null != c) {
            int key = NativeCanvas.getKey(keyCode);
            int action = getAction(key, keyCode);
            c.doKeyReaction(key, action, type);

            
            if (CanvasEx.KEY_PRESSED == type) { // navigation keys only
                switch (action) {
                    case NAVIKEY_RIGHT:
                    case NAVIKEY_LEFT:
                    case NAVIKEY_UP:
                    case NAVIKEY_DOWN:
                    case NAVIKEY_FIRE:
                    case KEY_NUM1:
                    case KEY_NUM3:
                    case KEY_NUM7:
                    case KEY_NUM9:
                        KeyRepeatTimer.start(key, action, c);
                        break;
                }

            } else {
                KeyRepeatTimer.stop();
            }
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        //try {
        //    String strCode = NativeCanvas.getInstance().getKeyName(keyCode);
        //    DebugLog.println("key = '" + strCode + "'(" + keyCode + ") = " + type);
        //} catch(IllegalArgumentException e) {
        //}
        // #sijapp cond.end #
    }

    private void emulateKey(int key) {
        doKeyReaction(key, CanvasEx.KEY_PRESSED);
        doKeyReaction(key, CanvasEx.KEY_RELEASED);
    }

    public static void invalidate(CanvasEx canvasEx) {
        if (canvas == canvasEx) {
            instance.repaint();
        }
    }

    public static NativeCanvas getInstance() {
        return instance;
    }
    
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    private String time = Util.getDateString(true);
    private int getSoftBarSize() {
        return graphicsEx.getSoftBarSize(leftButton, time, rightButton);
    }
    // #sijapp cond.end #
    private int width = 0;
    private int height = 0;
    private int windowHeight = 0;
    private void resetMetrix() {
        height = 0;
        width = 0;
    }
    private void updateMetrix() {
        if (height < getHeight()) {
            width = getWidth();
            height = getHeight();
        }
        windowHeight = height;
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        windowHeight -= getSoftBarSize();
        // #sijapp cond.end #
    }
    public static int getScreenWidth() {
        return instance.width;
    }

    public static int getScreenHeight() {
        return instance.windowHeight;
    }

    public static void refreshClock() {
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        int h = instance.getSoftBarSize();
        if (0 < h) {
            instance.time = Util.getDateString(true);
            instance.repaint(0, instance.getHeight() - h, getScreenWidth(), h);
        }
        // #sijapp cond.end #
    }
    
    private static class KeyRepeatTimer extends TimerTask {
        private static Timer timer = new Timer();
        private int key;
        private int action;
        private CanvasEx canvas;
        private int slowlyIterations = 8;
        
        
        public static void start(int key, int action, CanvasEx c) {
            stop();
            timer = new Timer();
            KeyRepeatTimer repeater = new KeyRepeatTimer(key, action, c);
            timer.schedule(repeater, 400, 100);
        }
        public static void stop() {
            if (null != timer) {
                timer.cancel();
                timer = null;
            }
        }
        
        private KeyRepeatTimer(int keyCode, int actionCode, CanvasEx c) {
            key = keyCode;
            action = actionCode;
            canvas = c;
        }

        public void run() {
            if (0 < slowlyIterations) {
                slowlyIterations--;
                if (0 != slowlyIterations % 2) {
                    return;
                }
            }
            if (!NativeCanvas.getInstance().isShown()) {
                KeyRepeatTimer.stop();
                return;
            }
            canvas.doKeyReaction(key, action, CanvasEx.KEY_REPEATED);
        }
    }
}
