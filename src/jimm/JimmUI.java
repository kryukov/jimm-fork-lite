/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/JimmUI.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Igor Palkin, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/

package jimm;

import java.io.*;
import java.util.*;

import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.cl.*;
import jimm.util.*;
import DrawControls.*;
import protocol.Contact;

public final class JimmUI implements CommandListener {
    
    /////////////////////////
    //                     //
    //     Message Box     //
    //                     //
    /////////////////////////
    static private FormEx msgForm;
    static private int actionTag = -1;

    static private CommandListener listener;

    private JimmUI() {
    }

    static private JimmUI _this = new JimmUI();
    public static JimmUI getInstance() {
        return _this;
    }

    public void commandAction(Command c, Displayable d) {
        // Message box
        listener.commandAction(c, d);
        msgForm.clearForm();
        JimmUI.actionTag = -1;
        if (msgForm.backCommand == c) {
            msgForm.back();
        }
    }

    public static boolean isYesCommand(Command testCommand, int testTag) {
        return (actionTag == testTag) && (testCommand == msgForm.saveCommand);
    }
    
    static public void messageBox(String cap, String text, CommandListener listener, int tag) {
        actionTag = tag;
        msgForm = new FormEx(cap, "yes", "no", _this);
        msgForm.addString(text);
        JimmUI.listener = listener;
        msgForm.show();
    }
    
    //////////////////////
    //                  //
    //    Clipboard     //
    //                  //
    //////////////////////
    
    static private String clipBoardText;
    static private String clipBoardHeader;
    static private boolean clipBoardIncoming;
    
    static private String insertQuotingChars(String text, String qChars) {
        StringBuffer result = new StringBuffer();
        int size = text.length();
        boolean wasNewLine = true;
        for (int i = 0; i < size; i++) {
            char chr = text.charAt(i);
            if (wasNewLine) result.append(qChars);
            result.append(chr);
            wasNewLine = (chr == '\n');
        }
        
        return result.toString();
    }
    
    static public boolean clipBoardIsEmpty() {
        return null == clipBoardText;
    }
    
    static public String getClipBoardText(boolean quote) {
        if (clipBoardIsEmpty()) {
            return "";
        }
        if (!quote) {
            return clipBoardText;
        }
        StringBuffer sb = new StringBuffer();
        if (null != clipBoardHeader) {
            sb.append('[').append(clipBoardHeader).append(']').append('\n');
        }
        if (null != clipBoardText) {
            sb.append(insertQuotingChars(clipBoardText, clipBoardIncoming ? ">> " : "<< "));
        }
        return sb.toString();
    }
    
    static public void setClipBoardText(String header, String text) {
        clipBoardText     = text + '\n';
        clipBoardHeader   = header;
        clipBoardIncoming = true;
    }
    
    static public void setClipBoardText(boolean incoming, String date, String from, String text) {
        clipBoardText     = text + '\n';
        clipBoardHeader   = from + ' ' + date;
        clipBoardIncoming = incoming;
    }
    
    static public void clearClipBoardText() {
        clipBoardText = null;
    }
    
    
    /************************************************************************/
    /************************************************************************/
    /************************************************************************/
    
    ///////////////////
    //               //
    //    Hotkeys    //
    //               //
    ///////////////////
    
    static public boolean execHotKey(int keyCode, int type) {
        int action = Options.HOTKEY_NONE;
        switch (keyCode) {
            case Canvas.KEY_NUM0:
                action = Options.getInt(Options.OPTION_EXT_CLKEY0);
                break;
            case Canvas.KEY_NUM4:
                action = Options.getInt(Options.OPTION_EXT_CLKEY4);
                break;
                
            case Canvas.KEY_NUM6:
                action = Options.getInt(Options.OPTION_EXT_CLKEY6);
                break;
                
            case Canvas.KEY_STAR:
                action = Options.getInt(Options.OPTION_EXT_CLKEYSTAR);
                break;
                
            case Canvas.KEY_POUND:
                action = Options.getInt(Options.OPTION_EXT_CLKEYPOUND);
                break;
                
            case NativeCanvas.CAMERA_KEY:
            case NativeCanvas.CALL_KEY:
                action = Options.getInt(Options.OPTION_EXT_CLKEYCALL);
                break;
        }
        
        return (action != Options.HOTKEY_NONE) && execHotKeyAction(action, type);
    }
    
    private static long lockPressedTime = -1;
    static private boolean execHotKeyAction(int actionNum, int keyType) {
        if ((CanvasEx.KEY_REPEATED == keyType)
                || (CanvasEx.KEY_RELEASED == keyType)) {
            if (lockPressedTime == -1) return true;
            long diff = System.currentTimeMillis() - lockPressedTime;
            if ((Options.HOTKEY_LOCK == actionNum) && (diff > 900)) {
                lockPressedTime = -1;
                Jimm.lockJimm();
                return true;
            }
            return false;
        }
        if (Options.HOTKEY_LOCK == actionNum) {
            lockPressedTime = System.currentTimeMillis();
        }
        
        // get current contact or group
        Contact contact = ContactList.getInstance().getCurrentContact();
        
        if (null != contact) {
            switch (actionNum) {
                // #sijapp cond.if modules_HISTORY is "true" #
                case Options.HOTKEY_HISTORY:
                    contact.showHistory();
                    return true;
                // #sijapp cond.end#
                    
                case Options.HOTKEY_INFO:
                    contact.showUserInfo();
                    return true;
                    
                case Options.HOTKEY_NEWMSG:
                    contact.writeMessage(null);
                    return true;
                    
                // #sijapp cond.if (modules_CLIENTS is "true") | (modules_FILES is "true") #
                case Options.HOTKEY_CLI_INFO:
                    contact.showClientInfo();
                    return true;
                // #sijapp cond.end #
                    
                case Options.HOTKEY_STATUSES:
                    contact.showStatus();
                    return true;
            }
        }
        switch (actionNum) {
            case Options.HOTKEY_OPTIONS:
                OptionsForm.activate();
                return true;
                
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case Options.HOTKEY_MAGIC_EYE:
                MagicEye.activate();
                return true;
            // #sijapp cond.end#                    
                    
            case Options.HOTKEY_MENU:
                ContactList.activateMenu();
                return true;
                
            case Options.HOTKEY_OPEN_CHATS:
                ChatHistory.showChatList();
                return true;
                
            case Options.HOTKEY_ONOFF:
                boolean hide = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, hide);
                Options.safeSave();
                ContactList.getInstance().optionsChanged();
                ContactList.activate();
                return true;

                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
            case Options.HOTKEY_MINIMIZE:
                Jimm.setMinimized(true);
                return true;
                // #sijapp cond.end#
                
                // #sijapp cond.if modules_SOUND is "true" #
                // #sijapp cond.if target isnot "DEFAULT" #
            case Options.HOTKEY_SOUNDOFF:
                Notify.changeSoundMode(true);
                return true;
                // #sijapp cond.end#
                // #sijapp cond.end#
            // #sijapp cond.if modules_LIGHT is "true" #
            case Options.HOTKEY_LIGHT:
                CustomLight.setLightMode(CustomLight.ACTION_USER);
                return true;
            // #sijapp cond.end#
        }
        return false;
    }
}