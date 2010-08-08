/*
 * GraphicsEx.java
 *
 * Created on 15 Ноябрь 2007 г., 0:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import javax.microedition.lcdui.*;
import jimm.Options;

/**
 *
 * @author vladimir
 */
public final class GraphicsEx {
    private Graphics g;
    /** Creates a new instance of GraphicsEx */
    public GraphicsEx() {
    }
    public void setGraphics(Graphics graphics) {
        g = graphics;
    }

    
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
    
    static private int[] theme = Scheme.getScheme();
    public void setThemeColor(byte object) {
        g.setColor(theme[object]);
    }
    public int getThemeColor(byte object) {
        return theme[object];
    }    
    
    // #sijapp cond.if modules_DEBUGLOG is "true" #
    static {
        jimm.modules.DebugLog.assert0("invalid font style: plain", (0 != Font.STYLE_PLAIN));
        jimm.modules.DebugLog.assert0("invalid font style: bold", (1 != Font.STYLE_BOLD));
    }
    // #sijapp cond.end #

    // under constraction...
    private static Font[] smallFontSet;
    private static Font[] mediumFontSet;
    private static Font[] largeFontSet;
    public static Font[] getFontSet(int size) {
        switch (size) {
            case Font.SIZE_MEDIUM:
                if (null == mediumFontSet) {
                    mediumFontSet = createFontSet(size);
                }
                return mediumFontSet;

            case Font.SIZE_LARGE:
                if (null == largeFontSet) {
                    largeFontSet = createFontSet(size);
                }
                return largeFontSet;
        }
        if (null == smallFontSet) {
            smallFontSet = createFontSet(size);
        }
        return smallFontSet;
    }
    private static Font[] createFontSet(int fontSize) {
        Font[] fontSet = new Font[2];
        fontSet[Font.STYLE_PLAIN] = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  fontSize); 
        fontSet[Font.STYLE_BOLD]  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,   fontSize);
        return fontSet;
    }

    
    // change light of color
    public int transformColorLight(int color, int light) {
        int r = (color & 0xFF) + light;
        int g = ((color & 0xFF00) >> 8) + light;
        int b = ((color & 0xFF0000) >> 16) + light;
        r = Math.min(Math.max(r, 0), 255);
        g = Math.min(Math.max(g, 0), 255);
        b = Math.min(Math.max(b, 0), 255);
        return r | (g << 8) | (b << 16);
    }
/*    
    public void drawString(String str, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);
        g.drawString(str, x, y + height, Graphics.BOTTOM + Graphics.LEFT);
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    */
    public void drawBorderedString(String str, int x, int y, int width, int height) {
        final int color = g.getColor();
        final int cr = 255 - (color >> 16) & 0xFF;
        final int cg = 255 - (color >> 8) & 0xFF;
        final int cb = 255 - (color >> 0) & 0xFF;
        final int inversedColor = (cr << 16) | (cg << 8) | (cb << 0);
        g.setColor(inversedColor);
        drawString(str, x - 1, y, width, height);
        drawString(str, x + 1, y, width, height);
        drawString(str, x, y - 1, width, height);
        drawString(str, x, y + 1, width, height);
        g.setColor(color);
        drawString(str, x, y, width, height);
    }
    public void drawString(String str, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }

        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);
        g.drawString(str, x, y + (height - g.getFont().getHeight()) / 2,
                Graphics.LEFT + Graphics.TOP);
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    
    public void drawGradRect(int color1, int color2, int x, int y, int width, int height) {
        int r1 = ((color1 & 0xFF0000) >> 16);
        int g1 = ((color1 & 0x00FF00) >> 8);
        int b1 =  (color1 & 0x0000FF);
        int r2 = ((color2 & 0xFF0000) >> 16);
        int g2 = ((color2 & 0x00FF00) >> 8);
        int b2 =  (color2 & 0x0000FF);
        
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);

        int step = Math.max((height + 8) / 9, 1);
        for (int i = 0; i < 9; i++) {
            g.setColor(i * (r2 - r1) / 8 + r1, i * (g2 - g1) / 8 + g1, i * (b2 - b1) / 8 + b1);
            g.fillRect(x, i * step + y, width, step);
        }

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
    private static Font softBarFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    public int getSoftBarSize(String left, String middle, String right) {
        if (NativeCanvas.isFullScreen() && Options.getBoolean(Options.OPTION_SHOW_SOFTBAR)) {
            if ((null == left) && (null == right)) {
                return 0;
            }
            return softBarFont.getHeight() + 2;
        }
        return 0;
    }
    public void drawSoftBar(String left, String middle, String right, int height) {
        int w = getWidth();
        int x = 0;
        int y = getHeight();
        int h = height;
                
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, w, h);

        int capBkCOlor = getThemeColor(THEME_CAP_BACKGROUND);
        drawGradRect(capBkCOlor, transformColorLight(capBkCOlor, -32), 0, y, w, h);
        setColor(transformColorLight(capBkCOlor, -128));
        drawLine(0, y, w, y);

        int softWidth = w / 2 - 4;
        h -= 2;
        y++;
        setThemeColor(THEME_CAP_TEXT);
        g.setFont(softBarFont);

        int leftWidth = 0;
        if (null != left) {
            leftWidth = Math.min(softBarFont.stringWidth(left),  softWidth);
            drawString(left,  x + 1, y, leftWidth,  h);
        }
        
        int rightWidth = 0;
        if (null != right) {
            rightWidth = Math.min(softBarFont.stringWidth(right), softWidth);
            drawString(right, x + w - rightWidth - 1, y, rightWidth, h);
        }
        
        int criticalWidth = softWidth - 5;
        if ((rightWidth < criticalWidth) && (leftWidth < criticalWidth)) {
            int middleWidth = softBarFont.stringWidth(middle) + 2;
            int start = (w - middleWidth) / 2;
            if ((leftWidth < start) && (rightWidth < start)) {
                drawString(middle, x + start, y, middleWidth, h);
            }
            
        }
        
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    // #sijapp cond.end #

//    public void drawGlassRect(int color1, int color2, int x, int y, int width, int height) {
//        int clipX = g.getClipX();
//        int clipY = g.getClipY();
//        int clipHeight = g.getClipHeight();
//        int clipWidth = g.getClipWidth();
//        g.setClip(x, y, width, height);
//        g.setColor(color1);
//        g.fillRect(x, y, width, height / 2);
//        g.setColor(color2);
//        g.fillRect(x, y + height / 2, width, (height + 1) / 2);
//        g.setClip(clipX, clipY, clipWidth, clipHeight);
//    }
//    
    public void drawVertScroll(int x, int y, int width, int height, int pos, int len, int total) {
        height++;
        boolean haveToShowScroller = ((total > len) && (total > 0));
        pos = Math.min(total - len, pos);
        int color = transformColorLight(transformColorLight(getThemeColor(THEME_SCROLL_BACK), 32), -32);
        if (color == 0) color = 0x808080;
        g.setStrokeStyle(Graphics.SOLID);
        g.setColor(color);
        g.fillRect(x + 1, y, width - 1, height);
        if (haveToShowScroller) {
            final int offset = height % total;
            final int itemHeight = height / total;
            final int sliderSize = Math.max(7, itemHeight * len + offset);
            final int srcollerY1 = itemHeight * pos + y;
            final int srcollerY2 = srcollerY1 + sliderSize;
            //g.setColor(color);
            //g.fillRect(x + 2, srcollerY1 + 2, width - 3, sliderSize - 3);
            g.setColor(transformColorLight(color, -192));
            g.drawRect(x, srcollerY1, width - 1, sliderSize - 1);
            g.setColor(transformColorLight(color, 96));
            g.drawLine(x + 1, srcollerY1 + 1, x + 1, srcollerY2 - 2);
            g.drawLine(x + 1, srcollerY1 + 1, x + width - 2, srcollerY1 + 1);
        }
        g.setColor(transformColorLight(color, -64));
        g.drawLine(x, y, x, y + height - 1);
    }
    private static final int SHADOW_SIZE = 3;
    public void drawShadowRect(int x, int y, int width, int height, byte sback, byte sborder, byte sshadow) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width + SHADOW_SIZE + 1, height + SHADOW_SIZE + 1);

        setThemeColor(sshadow);
        g.fillRect(x + width, y + SHADOW_SIZE, SHADOW_SIZE, height);
        g.fillRect(x + SHADOW_SIZE, y + height, width, SHADOW_SIZE);
        setThemeColor(sback);
        g.fillRect(x, y, width, height);
        setThemeColor(sborder);
        g.drawRect(x, y, width, height);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void setStrokeStyle(int style) {
        g.setStrokeStyle(style);
    }

    public final void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }

    public final void setColor(int color) {
        g.setColor(color);
    }

    public final void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    public final int getColor() {
        return g.getColor();
    }

    public final void drawRect(int x, int y, int width, int height) {
        g.drawRect(x, y, width, height);
    }

    public final void drawString(String str, int x, int y, int anchor) {
        g.drawString(str, x, y, anchor);
    }

    public final int getClipY() {
        return g.getClipY();
    }

    public final int getClipX() {
        return g.getClipX();
    }

    public final int getClipWidth() {
        return g.getClipWidth();
    }

    public final int getClipHeight() {
        return g.getClipHeight();
    }

    public final void setClip(int x, int y, int width, int height) {
        g.setClip(x, y, width, height);
    }
    public final void clipRect(int x, int y, int width, int height) {
        g.clipRect(x, y, width, height);
    }

    public final Graphics getGraphics() {
        return g;
    }

    public final void setFont(Font font) {
        g.setFont(font);
    }

    // #sijapp cond.if target is "MIDP2"#
    public static final int captionOffset;
    static {
        if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA_N80)) {
            captionOffset = 30;
        } else if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA)) {
            captionOffset = 20;
        } else {
            captionOffset = 0;
        }
    }
    // #sijapp cond.end#
    private static Font captionFont;
    static {
        // #sijapp cond.if target="MIDP2"#
        captionFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
        // #sijapp cond.else#
        captionFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        // #sijapp cond.end#
    }
    public static int calcCaptionHeight(String text) {
        if (!NativeCanvas.isFullScreen()) return 0;

        int captionHeight = 0;
        if (null != text) {
            captionHeight = captionFont.getHeight() + 2;
        }
        if (0 != captionHeight) {
            captionHeight++;
        }
        return captionHeight;
    }
    public void drawCaption(String text, int height) {
        if (height <= 0) return;
        
        int width = getWidth();
            int capBkCOlor = getThemeColor(THEME_CAP_BACKGROUND);
            drawGradRect(capBkCOlor, transformColorLight(capBkCOlor, -32), 0, 0, width, height);
            setColor(transformColorLight(capBkCOlor, -128));
            drawLine(0, height - 1, width, height - 1);
        
        int x = 2;
        // #sijapp cond.if target is "MIDP2"#
        x += captionOffset;
        // #sijapp cond.end#
        g.setFont(captionFont);
        setThemeColor(THEME_CAP_TEXT);
        drawString(text, x, 1, width - x, height - 2);
    }
    public int getWidth() {
        return NativeCanvas.getScreenWidth();
    }
    public int getHeight() {
        return NativeCanvas.getScreenHeight();
    }
    public void reset() {
        g = null;
    }

}