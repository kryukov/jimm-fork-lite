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
 File: src/jimm/Jimm.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;


import javax.microedition.io.ConnectionNotFoundException;
import jimm.chat.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

import java.util.Timer;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import protocol.Protocol;


public class Jimm extends MIDlet {

    // Version
    public static final String VERSION = getAppProperty("Jimm-Version", "###VERSION###");
    public static String lastVersion;
    public static String lastDate;

    // Application main object
    private static Jimm jimm = null;
    public static Jimm getJimm() {
	return jimm;
    }

    // Display object
    private Display display = null;


    /****************************************************************************/

    public static final byte PHONE_SE             = 0;
    public static final byte PHONE_SE_SYMBIAN     = 1;
    public static final byte PHONE_NOKIA          = 2;
    public static final byte PHONE_NOKIA_S40      = 3;
    public static final byte PHONE_NOKIA_S60      = 4;
    public static final byte PHONE_NOKIA_N80      = 5;
    public static final byte PHONE_INTENT_JTE     = 6;
    public static final byte PHONE_SIEMENS_SGOLG2 = 7;
    
    public static final String microeditionPlatform = System.getProperty("microedition.platform");
    public static final String microeditionProfiles = System.getProperty("microedition.profiles");
    private static final String device = (microeditionPlatform == null)
            ? null : microeditionPlatform.toLowerCase();

    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
    public static boolean isPhone(byte phone) {
        if (null == device) {
            return false;
        }
        // #sijapp cond.if target is "SIEMENS2"#
        if (PHONE_SIEMENS_SGOLG2 == phone) {
            return !((device.indexOf("65") != -1)
                || (device.indexOf("66") != -1)
                || (device.indexOf("70") != -1)
                || (device.indexOf("72") != -1)
                || ((device.indexOf("75") != -1
                    && device.indexOf("s") < 0)));
        }
        // #sijapp cond.end#
        // #sijapp cond.if target is "MIDP2" #
        switch (phone) {
            case PHONE_SE:         return device.indexOf("ericsson") != -1;
            case PHONE_SE_SYMBIAN: return isPhone(Jimm.PHONE_SE)
                    && (-1 != getSystemProperty("com.sonyericsson.java.platform", "")
                        .toLowerCase().indexOf("sjp"));
            case PHONE_NOKIA:      return device.indexOf("nokia") != -1;
            // Nokia s40 has only one dot into version
            case PHONE_NOKIA_S40:  return isPhone(Jimm.PHONE_NOKIA)
                    && (device.indexOf('.', device.indexOf('.') + 1) == -1);
            case PHONE_NOKIA_S60:  return (-1 == device.indexOf("platform=s60"))
                    || (isPhone(PHONE_NOKIA) && !isPhone(PHONE_NOKIA_S40));
            case PHONE_NOKIA_N80:  return device.indexOf("nokian80") != -1;
            case PHONE_INTENT_JTE: return device.indexOf("intent") != -1;
        }
        // #sijapp cond.end #
        return false;
    }
    // #sijapp cond.end #

    public static String getAppProperty(String key, String defval) {
        String res = null;
        try {
            res = jimm.getAppProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }
    private static String getSystemProperty(String key, String defval) {
        String res = null;
        try {
            res = System.getProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }

    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
    public static void platformRequestUrl(String url) throws ConnectionNotFoundException {
        if (url.equals("jimm:update")) {
            StringBuffer url_ = new StringBuffer();
            url_.append("http://jimm.net.ru/go.xhtml?act=update&lang=");
            url_.append(ResourceBundle.getCurrUiLanguage());
            url_.append("&protocols=###PROTOCOLS###&type=lite");
            url = url_.toString();
        }
        Jimm.getJimm().platformRequest(url.trim());
    }
    public static void platformRequestAndExit(String url) {
        try {
            platformRequestUrl(url);
            Jimm.getJimm().destroyApp(true);
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    // #sijapp cond.end #
    


    private void initialize() {

        ResourceBundle.loadLanguageList();
        Options.instance.loadOptions();
        ResourceBundle.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Options.instance.resetLangDependedOpts();

        UIUpdater.startUIUpdater();
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        NativeCanvas.setFullScreen(true);
        // #sijapp cond.end#

        SplashCanvas.setMessage(ResourceBundle.getString("loading"));
        SplashCanvas.showSplash();

        // Get display object (and update progress indicator)
        SplashCanvas.setProgress(10);

        SplashCanvas.setProgress(20);
        

        // Create and load emotion icons
        // #sijapp cond.if modules_SMILES is "true" #
        Emotions.instance.load();
        SplashCanvas.setProgress(30);
        // #sijapp cond.end#
        
        // Create CL and ICQ objects
        // Load contact list
        ContactList.getInstance().setProtocol(Options.getProtocol());
        SplashCanvas.setProgress(40);
        
        // #sijapp cond.if modules_HISTORY is "true" #
        // Create object for text storage...
        SplashCanvas.setProgress(50);
        // #sijapp cond.end#

        SplashCanvas.setProgress(60);

        // #sijapp cond.if modules_TRAFFIC is "true" #
        // Create traffic Object (and update progress indicator)
        if (null != Traffic.getInstance()) {
            SplashCanvas.setProgress(70);
        }
        // #sijapp cond.end#
        
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.switchOn(Options.getBoolean(Options.OPTION_LIGHT));
        SplashCanvas.setProgress(80);
        // #sijapp cond.end#
        
        System.gc();
        SplashCanvas.setProgress(90);
        
        if (null != Templates.getInstance()) {
            SplashCanvas.setProgress(100);
        }
    }
    private void restore() {
//        UIUpdater.startUIUpdater();
//        // #sijapp cond.if modules_LIGHT is "true" #
//        CustomLight.switchOn(false);
//        CustomLight.switchOn(Options.getBoolean(Options.OPTION_LIGHT));
//        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
//        // #sijapp cond.end#
    }
    
    public Display getDisplay() {
        return display;
    }

    private boolean paused = true;

    // Start Jimm
    public void startApp() throws MIDletStateChangeException {
        if (!paused && (null != Jimm.jimm)) {
            return;
        }
        paused = false;
        if (null == display) {
            display = Display.getDisplay(this);
        }
        // Return if MIDlet has already been initialized
        if (null != Jimm.getJimm()) {
            restore();
            ContactList.activate();
            return;
        }
        locked = false;
        // Save MIDlet reference
        Jimm.jimm = this;
        initialize();
        
        if (Options.getBoolean(Options.OPTION_AUTO_CONNECT)) {
            Options.getProtocol().connect();

        } else {
            // Activate contact list
            ContactList.activate();
            //if (StringConvertor.isEmpty(Options.getProtocol().getUin())) {
            //    ContactList.activateMenu();
            //    new OptionsForm().showCurrentAccountEditor();
            //}
        }
    }

    // Pause
    public void pauseApp() {
        if (currentScreen instanceof FormEx) {
            return;
        }
        if (currentScreen instanceof InputTextBox) {
            return;
        }
        currentScreen = null;
        paused = true;
        locked = false;
    }

    // Destroy Jimm
    public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        Protocol protocol = Options.getProtocol();
        boolean wait = false;
        if (protocol.isConnected()) {
            protocol.disconnect();
            wait = true;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            /* Do nothing */
        }
        protocol.safeSave();
        // #sijapp cond.if modules_TRAFFIC is "true" #
        // Save traffic
        Traffic.safeSave();
        // #sijapp cond.end#
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                /* Do nothing */
            }
        }
        jimm.display.setCurrent(null);
        notifyDestroyed();
    }

    public static void setDisplayable(Displayable d) {
        jimm.display.setCurrent(d);
    }
    private Object currentScreen = null;
    public static void setDisplay(Object d) {
        if (jimm.paused) {
            return;
        }

        if ((jimm.currentScreen instanceof DisplayableEx)) {
            ((DisplayableEx)jimm.currentScreen).closed();

        // #sijapp cond.if modules_LIGHT is "true" #
        } else if ((jimm.currentScreen instanceof Displayable)) {
            CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
        // #sijapp cond.end#
        }

        synchronized (jimm) {
            if (d instanceof DisplayableEx) {                
                ((DisplayableEx)d).setDisplayableToDisplay();
                jimm.currentScreen = d;

            } else if (d instanceof Displayable) {
                // #sijapp cond.if modules_LIGHT is "true" #
                CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
                // #sijapp cond.end#
                setDisplayable((Displayable)d);
                jimm.currentScreen = d;

            // #sijapp cond.if target is "MIDP2" #
            } else if ((null == d) && Jimm.isPhone(Jimm.PHONE_SE)) {
                setMinimized(true);
            // #sijapp cond.end#

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            } else if (null != d) {
                DebugLog.panic("not display object " + d.getClass().getName());
            // #sijapp cond.end#
            }
        }
    }
    public static Object getCurrentDisplay() {
        return jimm.currentScreen;
    }

    public static void setPrevDisplay() {
        Object screen = jimm.currentScreen;
        if (screen instanceof DisplayableEx) {
            ((DisplayableEx)screen).back();
        }
    }
    public static boolean isPaused() {
        if (jimm.paused) {
            return true;
        }
        Displayable d = jimm.display.getCurrent();
        return (null == d) || !d.isShown();
    }

    private static boolean locked = false;
    public static boolean isLocked() {
        return locked;
    }
    
    public static void lockJimm() {
        locked = true;
        SplashCanvas.instance.lockJimm();
    }
    public static void unlockJimm() {
        locked = false;
        ContactList.activate();
        NativeCanvas.refreshClock();
    }
    
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
    // Set the minimize state of midlet
    public static void setMinimized(boolean mini) {
        // #sijapp cond.if target is "MIDP2" #
        if (mini) {
            jimm.pauseApp();
            setDisplayable(null);

        } else {
            if (isPaused()) {
                try{
                    jimm.startApp();
                } catch(Exception exc1) {
                }
            }
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "SIEMENS2"#
        try{
            platformRequestUrl(isPhone(PHONE_SIEMENS_SGOLG2)
                    ? "native://NAT_MAIN_MENU" : "native://ELSE_STR_MYMENU");
        } catch(Exception exc1) {
        }
        // #sijapp cond.end#
    }
    // #sijapp cond.end #
}