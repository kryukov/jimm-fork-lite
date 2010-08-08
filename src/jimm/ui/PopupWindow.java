/*
 * PopupWindows.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import DrawControls.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.timers.*;
import jimm.util.*;

/**
 * 
 * @author Vladimir Krukov
 */
public class PopupWindow extends TextList {
    private int x;
    private int y;
    private int width;
    private int height;

    private String text;
    private String title;
    private int hide;

    private int realHeight;
    private int curPos;
    public static final int FOREVER = -1;
    public static final int TIME_NEW_VERSION = -2;
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
    public static PopupWindow createNewVersion() {
        String title = "check_updates";
        String msg = ResourceBundle.getString("new_version_available");
        String lastVer = Options.getString(Options.OPTION_LAST_VERSION);
        msg = Util.replace(msg, "###LASTVER###", lastVer);
        return new PopupWindow(title, msg, TIME_NEW_VERSION);
    }
    // #sijapp cond.end#

    /** Creates a new instance of TextListEx */
    public PopupWindow(String title, String text, int time) {
        super(null);
        //hide = (time == FOREVER) ? -1 : time / NativeCanvas.UIUPDATE_TIME;
        hide = (time < 0) ? time : time / NativeCanvas.UIUPDATE_TIME;
        this.title = ResourceBundle.getString(title);
        this.text  = StringConvertor.notNull(text);
        setFontSize(Font.SIZE_SMALL);
    }
    
    public PopupWindow(String title, String text) {
        this(title, text, FOREVER);
    }
    
    public static void showShadowPopup(String title, String text) {
        PopupWindow win = new PopupWindow(title, text);
        synchronized (NativeCanvas.setDisplaySemaphore) {
            Object o = Jimm.getCurrentDisplay();
            if (o instanceof PopupWindow) {
                PopupWindow top = (PopupWindow) o;
                PopupWindow p = top;
                while (p.prevDisplay instanceof PopupWindow) {
                    p = (PopupWindow)p.prevDisplay;
                }
                win.prevDisplay = p.prevDisplay;
                p.prevDisplay = win;
                top.invalidate();
                return;
            }
        }
        win.show();
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    protected void stylusTap(int x, int y, boolean longTap) {
        back();
    }
    // #sijapp cond.end#
    
    protected int getHeight() {
        return height;
    }
    
    protected int getWidth() {
        return width;
    }

    protected void restoring() {
        String left = (TIME_NEW_VERSION == hide) ? "update" : null;
        NativeCanvas.setCommands(left, null, "close");
    }
    protected void beforShow() {
        width  = NativeCanvas.getScreenWidth();
        width  = (width > 150) ? width * 9 / 10 : width - 10;
        height = NativeCanvas.getScreenHeight();
        height = (height > 200) ? height * 6 / 10 : height - 15;
        x = NativeCanvas.getScreenWidth() - width - 5;
        y = NativeCanvas.getScreenHeight() - height - 5;

        clear();
        formatedText.setWidth(width - 7);
        formatedText.addTextWithEmotions(getFontSet(), text, THEME_POPUP_TEXT, (byte)Font.STYLE_PLAIN, -1);
        realHeight = formatedText.getHeight() + 8;
        realHeight += (getDefaultFont().getHeight() + 1) * 2;

        curPos = 0;
    }

    public void paint(GraphicsEx g) {
        int width = getWidth();
        int height = getHeight();

        Object o = prevDisplay;
        int i = 1;
        while (o instanceof PopupWindow) {
            i++;
            o = ((PopupWindow)o).prevDisplay;
        }
        paintBack(g, o);
        g.setClip(x, y, width, height);
        g.setStrokeStyle(Graphics.SOLID);

        Font font = getDefaultFont();
        g.setFont(font);
        int capHeight = font.getHeight() + 1;
        int contentX = x + 3;
        int contentY = y + 3 + capHeight;

        g.drawShadowRect(x, y, width, height, THEME_POPUP_BACK, THEME_POPUP_BORDER, THEME_POPUP_SHADOW);

        int capBkCOlor = g.getThemeColor(THEME_POPUP_BACK);
        g.drawGradRect(capBkCOlor, g.transformColorLight(capBkCOlor, -32), x + 1, y + 1, width - 1, capHeight - 1);
        g.setThemeColor(THEME_POPUP_BORDER);
        g.drawLine(x, y + capHeight, x + width, y + capHeight);

        g.setThemeColor(THEME_POPUP_TEXT);
        String caption = title;
        if (i > 1) {
            caption = Integer.toString(i) + ") " + caption;
        }
        g.drawString(caption, contentX, y, width - 6, capHeight);
        showText(g, contentX, contentY, width - 7, height - 6 - capHeight, curPos);
    }
	private void showText(GraphicsEx g, int x, int y, int width, int height, int skipHeight) {
        formatedText.paint(getFontSet(), g, x, y, width, height, skipHeight);
	}


    public void doKeyReaction(int keyCode, int actionCode, int type) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        try {
            String strCode = NativeCanvas.getInstance().getKeyName(keyCode);
            DebugLog.println("key = '" + strCode + "'(" + keyCode + ")");
        } catch(IllegalArgumentException e) {
            DebugLog.println("key = 'null'(" + keyCode + ")" + actionCode);
        }
        // #sijapp cond.end #
		switch (actionCode) {
            case NativeCanvas.NAVIKEY_DOWN:
            case NativeCanvas.NAVIKEY_UP:
                curPos += (actionCode == NativeCanvas.NAVIKEY_DOWN) ? STEP : -STEP;
                curPos = Math.max(0, Math.min(curPos, realHeight + EMPTY_HEIGHT - getHeight()));
                invalidate();
                return;
        }
        if (type != KEY_PRESSED) {
            return;
        }
        switch (keyCode) {
            case NativeCanvas.LEFT_SOFT:
                Object o = prevDisplay;
                while (o instanceof PopupWindow) {
                    Object obj = ((PopupWindow)o).prevDisplay;
                    ((PopupWindow)o).prevDisplay = null;
                    o = obj;
                }
                Jimm.setDisplay(o);
                return;
            case NativeCanvas.RIGHT_SOFT:
            case NativeCanvas.CLOSE_KEY:
            case NativeCanvas.CLEAR_KEY:
                back();
                return;
        }
    }
    
    private static final int STEP = 10;
    private static final int EMPTY_HEIGHT = 2 * STEP;
    public void updateTask() {
        if (hide == 0) {
            back();
        } else if (hide > 0) {
            hide--;
        }
    }
}