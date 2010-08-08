/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/SplashCanvas.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;

import DrawControls.*;
import jimm.comm.*;
import jimm.comm.message.Message;
import protocol.*;
import jimm.cl.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.timers.*;
import jimm.util.*;

import java.io.IOException;
import javax.microedition.lcdui.*;
import java.util.*;
import jimm.comm.*;
import jimm.comm.message.Message;
import protocol.*;
import jimm.cl.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.timers.*;
import jimm.util.*;

import java.io.IOException;
import javax.microedition.lcdui.*;
import java.util.*;
//// #sijapp cond.if target is "RIM"#
//import net.rim.device.api.system.*;
//// #sijapp cond.end#


public final class SplashCanvas extends CanvasEx {	
	
    // True if keylock has been enabled
    static private final short KEY_LOCK_MSG_TIME = 2000 / NativeCanvas.UIUPDATE_TIME;
    private short keyLock = -1;
    static private final short UPDATE_INTERVAL = 20000 / NativeCanvas.UIUPDATE_TIME;
    private short updateTime = UPDATE_INTERVAL;
    static private final short RESET_INTERVAL = 3000 / NativeCanvas.UIUPDATE_TIME;
    private short resetTime = -1;

	// Font used to display the logo (if image is not available)
	private static final Font logoFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
	
	// Font used to display the version nr
	private static final Font versionFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

	// Font used to display informational messages
	private static final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

	/*****************************************************************************/


    // Message to display beneath the splash image
    private String message;
    
    // Progress in percent
    private volatile int progress;
    
    // Number of available messages
    private int availableMessages;
    
    // Time since last key # pressed
    private long poundPressTime;
    

	public static final SplashCanvas instance = new SplashCanvas();

    // Constructor
	private SplashCanvas() {
	}

	// Sets the informational message
	static public void setMessage(String message) {
		instance.message = message;
        instance.progress = 0;
        instance.invalidate();
	}


	public static void setNotifyMessage(String msg) {
	instance.message = msg;
        instance.resetTime = RESET_INTERVAL;
        instance.invalidate();
    }
    public static void showSplash() {
        instance.show();
        instance.protocol = null;
    }
    
    // Sets the current progress in percent (and request screen refresh)
    static public void setProgress(int progress) {
        if (progress == instance.progress) return;
        instance.progress = progress;
        instance.invalidate();
    }
    
    public void lockJimm() {
        keyLock = 0;
        setMessage(ResourceBundle.getString("keylock_enabled"));
        messageAvailable();
        show();
    }


    // Called when message has been received
    public static void messageAvailable() {
        if (Jimm.isLocked()) {
            int unread = ContactList.getInstance().getUnreadMessCount();
            if (unread != instance.availableMessages) {
                instance.availableMessages = unread;
                instance.invalidate();
            }
        }
    }
    
    // Called when a key is pressed
    public void doKeyReaction(int keyCode, int actionCode, int type) {
        if (KEY_PRESSED == type) {
            if (Jimm.isLocked()) {
                if (Canvas.KEY_POUND == keyCode) {
                    if (0 == poundPressTime) {
                        poundPressTime = System.currentTimeMillis();
                    }
                
                } else {
                    poundPressTime = 0;
                    keyLock = KEY_LOCK_MSG_TIME;
                    invalidate();
                }
            
            } else {
                if (NativeCanvas.RIGHT_SOFT == keyCode) {
                    Protocol p = protocol;
                    if (null != p) {
                        p.disconnect();
                        protocol = null;
                        ContactList.activate();
                    }

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else if ((Canvas.KEY_POUND == keyCode)) {
                    ContactList.activate();
                // #sijapp cond.end#
                }
            }

        } else {
            if (!Jimm.isLocked()) return;
            if (Canvas.KEY_POUND != keyCode) return;
            if ((0 != poundPressTime)
                    && ((System.currentTimeMillis() - poundPressTime) > 900)) {
                Jimm.unlockJimm();
                poundPressTime = 0;
            }
        }
    }
    
    public void updateTask() {
        boolean repaintIt = false;
        // icq action
        if (0 <= resetTime) {
            if (0 == resetTime) {
                setMessage(ResourceBundle.getString("keylock_enabled"));
                repaintIt = true;
            }

            resetTime--;
        }
        
        // key lock
        if (0 <= keyLock) {
            if (0 == keyLock) {
                repaintIt = true;
            }
            keyLock--;
        }
        
        // clock
        if (0 <= updateTime) {
            updateTime--;
            if (0 > updateTime) {
                if (Options.getBoolean(Options.OPTION_DISPLAY_DATE)) {
                    updateTime = UPDATE_INTERVAL;
                }
                repaintIt = true;
            }
        }
        if (repaintIt) {
            invalidate();
        }
    }

    // Render the splash image
    public void paint(GraphicsEx g) {
        final int height = NativeCanvas.getScreenHeight();
        final int width  = NativeCanvas.getScreenWidth();
        final int fontHeight = font.getHeight();
        // Do we need to draw the splash image?
        if (g.getClipY() < height - fontHeight - 2) {
            // Draw background
            g.setThemeColor(THEME_SPLASH_BACKGROUND);
            g.fillRect(0, 0, width, height);

            // Display splash image (or text)
                g.setThemeColor(THEME_SPLASH_LOGO_TEXT);
                g.setFont(SplashCanvas.logoFont);
                g.drawString("jimm", width / 2, height / 2 + 5, Graphics.HCENTER | Graphics.BASELINE);
                g.setFont(SplashCanvas.font);

            
            // Draw the date bellow notice if set up to do so
            if (Options.getBoolean(Options.OPTION_DISPLAY_DATE)) {
                g.setThemeColor(THEME_SPLASH_DATE);
                g.setFont(SplashCanvas.font);
                g.drawString(Util.getDateString(false), width / 2, 12, Graphics.TOP | Graphics.HCENTER);
                g.drawString(Util.getCurrentDay(), width / 2, 13 + SplashCanvas.font.getHeight(),
                        Graphics.TOP | Graphics.HCENTER);
            }

            // Display message icon, if keylock is enabled
            if (Jimm.isLocked()) {
                if (0 < availableMessages) {
                    g.setThemeColor(THEME_SPLASH_MESSAGES);
                    g.setFont(SplashCanvas.font);
                    int x = 0 + 4;
                    int y = height-(2 * fontHeight) - 5;
                    g.drawString("# " + availableMessages, x, y, Graphics.LEFT | Graphics.TOP);
                }
            
                // #sijapp cond.if target is "SIEMENS2"#
                String accuLevel = System.getProperty("MPJC_CAP");
                if (null != accuLevel) {
                    accuLevel += "%";
                    int fontX = width -  SplashCanvas.font.stringWidth(accuLevel) - 1;
                    g.setThemeColor(THEME_SPLASH_DATE);
                    g.setFont(SplashCanvas.font);
                    g.drawString(accuLevel, fontX, height - (2 * fontHeight) - 5, Graphics.LEFT | Graphics.TOP);
                }
                // #sijapp cond.end#

                // Display the keylock message if someone hit the wrong key
                if (0 < keyLock) {
                    // Init the dimensions
                    String lockMsg = ResourceBundle.getString("keylock_message");
                    TextList.showMessage(g, lockMsg, width, height);
                }
            }
        }

        int progressHeight = fontHeight;
        int im_width = 0;
        int stringWidth = font.stringWidth(message);
        g.setFont(SplashCanvas.font);

        // Draw white bottom bar
        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.setStrokeStyle(Graphics.DOTTED);
        g.drawLine(0, height - progressHeight - 3, width, height - progressHeight - 3);

        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.drawString(message, (width / 2) + (im_width / 2), height, Graphics.BOTTOM | Graphics.HCENTER);

        // Draw current progress
        int progressPx = width * progress / 100;
        if (progressPx < 1) return;
        g.setClip(0, height - progressHeight - 2, progressPx, progressHeight + 2);

        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.fillRect(0, height - progressHeight - 2, progressPx, progressHeight + 2);

        g.setThemeColor(THEME_SPLASH_PROGRESS_TEXT);
        // Draw the progressbar message
        g.drawString(message, (width / 2) + (im_width / 2), height, Graphics.BOTTOM | Graphics.HCENTER);
    }
    
    protected void restoring() {
        NativeCanvas.setCommands(null, null, null);
    }
    
    private Protocol protocol;
    public static void connectingTo(Protocol p) {
        setMessage(ResourceBundle.getString("connecting"));
        setProgress(0);
        showSplash();
        instance.protocol = p;
    }
}
