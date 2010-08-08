/*
 * CanvasEx.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.Util;
import jimm.modules.*;

/**
 * Basic class for UI-controls.
 *
 * @author Vladimir Kryukov
 */
abstract public class CanvasEx extends DisplayableEx {
    public static final byte THEME_BACKGROUND           = 0;
    public static final byte THEME_TEXT                 = 1;
    public static final byte THEME_CAP_BACKGROUND       = 2;
    public static final byte THEME_CAP_TEXT             = 3;
    public static final byte THEME_PARAM_VALUE          = 4;
    
    public static final byte THEME_CHAT_INMSG           = 5;
    public static final byte THEME_CHAT_OUTMSG          = 6;
    public static final byte THEME_CHAT_FROM_HISTORY    = 7;
    
    public static final byte THEME_CONTACT_ONLINE       = 8;
    public static final byte THEME_CONTACT_WITH_CHAT    = 9;
    public static final byte THEME_CONTACT_OFFLINE      = 10;
    public static final byte THEME_CONTACT_TEMP         = 11;
    
    public static final byte THEME_SCROLL_BACK          = 12;
    
    public static final byte THEME_SELECTION_RECT       = 13;
    public static final byte THEME_BACK                 = 14;
    
    public static final byte THEME_SPLASH_BACKGROUND    = 15;
    public static final byte THEME_SPLASH_LOGO_TEXT     = 16;
    public static final byte THEME_SPLASH_MESSAGES      = 17;
    public static final byte THEME_SPLASH_DATE          = 18;///
    public static final byte THEME_SPLASH_PROGRESS_BACK = 19;
    public static final byte THEME_SPLASH_PROGRESS_TEXT = 20;
    public static final byte THEME_SPLASH_LOCK_BACK     = 21;
    public static final byte THEME_SPLASH_LOCK_TEXT     = 22;
    
    public static final byte THEME_MAGIC_EYE_NUMBER     = 23;
    public static final byte THEME_MAGIC_EYE_ACTION     = 24;
    public static final byte THEME_MAGIC_EYE_NL_USER    = 25;
    public static final byte THEME_MAGIC_EYE_USER       = 26;
    public static final byte THEME_MAGIC_EYE_TEXT       = 27;
    
    public static final byte THEME_MENU_SHADOW          = 28;
    public static final byte THEME_MENU_BACK            = 29;
    public static final byte THEME_MENU_BORDER          = 30;
    public static final byte THEME_MENU_TEXT            = 31;
    public static final byte THEME_MENU_SEL_BACK        = 32;
    public static final byte THEME_MENU_SEL_BORDER      = 33;
    public static final byte THEME_MENU_SEL_TEXT        = 34;
    
    public static final byte THEME_POPUP_SHADOW         = 35;
    public static final byte THEME_POPUP_BORDER         = 36;
    public static final byte THEME_POPUP_BACK           = 37;
    public static final byte THEME_POPUP_TEXT           = 38;

    public static final byte THEME_GROUP                = 39;
    public static final byte THEME_CHAT_HIGHLITE_MSG    = 40;
    public static final byte THEME_NEW_MESSAGE          = 41;
    
    
    // Width of scroller line
    protected final static int scrollerWidth;
    static {
        int zoom = 2;
        scrollerWidth = Math.max(NativeCanvas.getScreenWidth() * zoom / 100, 4);
    }

    /**
     * UI dinamic update
     */
    public void updateTask() {
    }

    /**
     * Caclulate params
     */
    protected void beforShow() {
    }

    
    /**
     * paint procedure
     */
    public abstract void paint(GraphicsEx g);
    
    protected void paintBack(GraphicsEx g, Object o) {
        if (o instanceof CanvasEx) {
            ((CanvasEx)o).paint(g);
        } else {
            g.setThemeColor(THEME_BACK);
            g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        }
        g.clipRect(0, 0, NativeCanvas.getScreenWidth(), NativeCanvas.getScreenHeight());
    }

    
    protected void restoring() {
        NativeCanvas.setCommands(null, null, null);
    }
    public void restore() {
        repaintLocked = false;
        Jimm.setDisplay(this);
    }
    public void setDisplayableToDisplay() {
        restoring();
        beforShow();
        NativeCanvas.setCanvas(this);
        NativeCanvas instance = NativeCanvas.getInstance();

        if ((Jimm.getCurrentDisplay() instanceof CanvasEx) && instance.isShown()) {
            NativeCanvas.invalidate(this);

        } else {
            Jimm.setDisplayable(instance);
        }
    }
    
    
    // Used by "Invalidate" method to prevent invalidate when locked
    private boolean repaintLocked = false;
    
    // protected void invalidate()
    public void invalidate() {
        if (repaintLocked) return;
        NativeCanvas.invalidate(this);
    }
    
    public final void lock() {
        repaintLocked = true;
    }
    
    protected void afterUnlock() {
    }

    public final void unlock() {
        repaintLocked = false;
        afterUnlock();
        invalidate();
    }
    
    protected final boolean isLocked() {
        return repaintLocked;
    }
    
    // Key event type
    public final static int KEY_PRESSED = 1;
    public final static int KEY_REPEATED = 2;
    public final static int KEY_RELEASED = 3;
    
    public void doKeyReaction(int keyCode, int actionCode, int type) {
    }
}