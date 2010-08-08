/*
 * Notify.java
 *
 * Created on 22 ������ 2007 �., 17:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.modules;

// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.InputStream;
// #sijapp cond.end#
import jimm.*;
import jimm.cl.*;
import jimm.ui.PopupWindow;
import jimm.util.*;
import javax.microedition.lcdui.*;

// #sijapp cond.if target is "SIEMENS1"#
import com.siemens.mp.game.*;
import com.siemens.mp.media.*;
import com.siemens.mp.media.control.*;
import java.io.InputStream;
// #sijapp cond.end#

/**
 *
 * @author vladimir
 */
public class Notify {
    
    /* Notify notification typs */
    public static final int SOUND_TYPE_MESSAGE = 1;
    public static final int SOUND_TYPE_ONLINE  = 2;
    public static final int SOUND_TYPE_TYPING  = 3;
    public static final int SOUND_TYPE_MULTIMESSAGE = 4;
    public static final int SOUND_TYPE_WAKE_UP = 5;

    
    /**
     * Creates a new instance of Notify
     */
    private Notify() {
    }

    private static Notify _this = new Notify();
    public static Notify getSound() {
        return _this;
    }
    
    // #sijapp cond.if target is "SIEMENS2"#
    private int volume = 0;
    // #sijapp cond.end#
    
    // #sijapp cond.if target is "MIDP2" | target is"SIEMENS1" | target is "MOTOROLA" | target is "SIEMENS2"#
    private void vibrate(int duration) {
        // #sijapp cond.if target is "SIEMENS1"#
        Vibrator.triggerVibrator(duration);
        // #sijapp cond.elseif target is "RIM"#
        // had to use full path since import already contains another Alert object
        net.rim.device.api.system.Alert.startVibrate(duration);
        // #sijapp cond.else#
        Jimm.getJimm().getDisplay().vibrate(duration);
        // #sijapp cond.end#
    }
    // #sijapp cond.if target isnot  "DEFAULT"#        
    // Play a sound notification
    private int getNotificationMode(int notType) {
        switch (notType) {
            case SOUND_TYPE_MESSAGE:
                return Options.getInt(Options.OPTION_MESS_NOTIF_MODE);
                
            case SOUND_TYPE_ONLINE:
                return Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);
                
            case SOUND_TYPE_TYPING:
                return Options.getInt(Options.OPTION_TYPING_MODE) - 1;

            case SOUND_TYPE_MULTIMESSAGE:
                return 0;
        }
        return 0;
    }
    long nextTime = 0;
    private void playNotification(int notType) {
        long now = System.currentTimeMillis();
        if (now < nextTime) {
            return;
        }
        if (SOUND_TYPE_WAKE_UP == notType) {
            if (Options.getBoolean(Options.OPTION_SILENT_MODE)) return;
            vibrate(1000);
            nextTime = now + 20 * 1000;
            return;
        }
        nextTime = now + 2 * 1000;

        int vibraKind = Options.getInt(Options.OPTION_VIBRATOR);
        if (vibraKind == 2) {
            vibraKind = Jimm.isLocked() ? 1 : 0;
        }
        if ((vibraKind > 0) 
                && ((SOUND_TYPE_MESSAGE == notType) || (SOUND_TYPE_MULTIMESSAGE == notType))) {
            vibrate(500);
        }
        
        if (Options.getBoolean(Options.OPTION_SILENT_MODE)) return;
        // #sijapp cond.if target is "SIEMENS1" | target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        if (1 == getNotificationMode(notType)) {
            try {
                switch(notType) {
                    case SOUND_TYPE_MESSAGE:
                        Manager.playTone(ToneControl.C4, 750, Options.getInt(Options.OPTION_MESS_NOTIF_VOL));
                        break;
                    case SOUND_TYPE_ONLINE:
                    case SOUND_TYPE_TYPING:
                        Manager.playTone(ToneControl.C4 + 7, 750, Options.getInt(Options.OPTION_ONLINE_NOTIF_VOL));
                }
            } catch (Exception e) {
            }
        }
        // #sijapp cond.end#
    }
    public static synchronized void playSoundNotification(int notType) {
        getSound().playNotification(notType);
    }
    // #sijapp cond.end#
    
    // #sijapp cond.if target isnot "DEFAULT"#
    static public void changeSoundMode(boolean showPopup) {
        boolean newValue = !Options.getBoolean(Options.OPTION_SILENT_MODE);
        if (showPopup) {
            PopupWindow.showShadowPopup("Jimm",
                    ResourceBundle.getString(newValue ? "#sound_is_off" : "#sound_is_on"));
        }
        if (!newValue) {
            getSound().vibrate(100);
        }
        Options.setBoolean(Options.OPTION_SILENT_MODE, newValue);
        Options.safeSave();
    }
    // #sijapp cond.end#
}
