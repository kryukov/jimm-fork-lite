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
*******************************************************************************
File: src/jimm/Options.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
           Vladimir Kryukov
******************************************************************************/


/*******************************************************************************
Current record store format:

Record #1: VERSION               (UTF8)
Record #2: OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           ...

Option key            Option value
  0 -  63 (00XXXXXX)  UTF8
 64 - 127 (01XXXXXX)  INTEGER
128 - 191 (10XXXXXX)  BOOLEAN
192 - 224 (110XXXXX)  LONG
225 - 255 (111XXXXX)  SHORT, BYTE-ARRAY (scrambled String)
******************************************************************************/


package jimm;

import jimm.comm.*;
import jimm.comm.message.Message;
import jimm.forms.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.util.*;

import java.io.*;
import java.util.*;

import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

import DrawControls.VirtualList;
import protocol.Protocol;
import protocol.Status;


public class Options {
    public static final int SLOT_VERSION  = 1;
    public static final int SLOT_OPTIONS  = 2;
    public static final int SLOT_ACCOUNTS = 3;
    public static final int SLOT_XSTATUS  = 4;
    
    /* Option keys */
    static final int OPTION_NICK1                      =  21;   /* String */
    static final int OPTION_UIN1                       =   0;   /* String */
    static final int OPTION_PASSWORD1                  = 228;   /* String  */
    static final int OPTION_NICK2                      =  22;   /* String */
    static final int OPTION_UIN2                       =  14;   /* String  */
    static final int OPTION_PASSWORD2                  = 229;   /* String  */
    static final int OPTION_NICK3                      =  23;   /* String */
    static final int OPTION_UIN3                       =  15;   /* String  */
    static final int OPTION_PASSWORD3                  = 230;   /* String  */
    //static final int OPTIONS_CURR_ACCOUNT              =  86;   /* int     */
    
    // Theese two options are not stored in RMS 
    public static final int OPTION_NICK                = 256;   /* String  */
    public static final int OPTION_UIN                 = 257;   /* String  */
    public static final int OPTION_PASSWORD            = 258;   /* String  */
    
    public static final int OPTION_SRV_HOST            =   1;   /* String  */
    public static final int OPTION_SRV_PORT            =   2;   /* String  */
    public static final int OPTION_KEEP_CONN_ALIVE     = 128;   /* boolean */
    public static final int OPTION_CONN_ALIVE_INVTERV  =  13;   /* String  */
    public static final int OPTION_ASYNC               = 166;   /* boolean */
    public static final int OPTION_CONN_TYPE           =  83;   /* int     */
    public static final int OPTION_AUTO_CONNECT        = 138;   /* boolean */
    // #sijapp cond.if target isnot  "MOTOROLA"#
    public static final int OPTION_SHADOW_CON          = 139;   /* boolean */
    // #sijapp cond.end#
    public static final int OPTION_UPDATE_CHECK_TIME  =  64;   /* int     */
    public static final int OPTION_LAST_VERSION       =  27;   /* String  */
    //public static final int OPTION_CHECK_UPDATES      = 174;   /* boolean */
    
    public static final int OPTION_AUTOANSWER_STATE   = 175;   /* boolean */
    public static final int OPTION_AUTOANSWER         =  28;   /* String  */

    public static final int OPTION_RECONNECT           = 149;   /* boolean */
    public static final int OPTION_RECONNECT_NUMBER    =  91;   /* int */
    //public static final int OPTION_HTTP_USER_AGENT     =  17;   /* String  */
    //public static final int OPTION_HTTP_WAP_PROFILE    =  18;   /* String  */
    public static final int OPTION_UI_LANGUAGE         =   3;   /* String  */
    public static final int OPTION_DISPLAY_DATE        = 129;   /* boolean */
    public static final int OPTION_CL_SORT_BY          =  65;   /* int     */
    public static final int OPTION_CL_HIDE_OFFLINE     = 130;   /* boolean */
    // #sijapp cond.if target isnot  "DEFAULT"#    
    public static final int OPTION_MESS_NOTIF_MODE     =  66;   /* int     */
    public static final int OPTION_MESS_NOTIF_FILE     =   4;   /* String  */
    public static final int OPTION_MESS_NOTIF_VOL      =  67;   /* int     */
    public static final int OPTION_ONLINE_NOTIF_MODE   =  68;   /* int     */
    public static final int OPTION_ONLINE_NOTIF_FILE   =   5;   /* String  */
    public static final int OPTION_ONLINE_NOTIF_VOL    =  69;   /* int     */
    public static final int OPTION_VIBRATOR            =  75;   /* integer */
    public static final int OPTION_TYPING_MODE         =  88;   /* integer */
    public static final int OPTION_TYPING_FILE         =  16;   /* String  */
    public static final int OPTION_TYPING_VOL          =  89;
    // #sijapp cond.end #    
    // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #    
    public static final int OPTION_VOLUME_BUGFIX       = 155; /* boolean */
    // #sijapp cond.end #    
    // #sijapp cond.if modules_TRAFFIC is "true" #
    public static final int OPTION_COST_PER_PACKET     =  70;   /* int     */
    public static final int OPTION_COST_PER_DAY        =  71;   /* int     */
    public static final int OPTION_COST_PACKET_LENGTH  =  72;   /* int     */
    public static final int OPTION_CURRENCY            =   6;   /* String  */
    // #sijapp cond.end #
    public static final int OPTION_ONLINE_STATUS       = 192;   /* long    */
    public static final int OPTION_DETECT_ENCODING     = 153;   /* boolean */
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    public static final int OPTION_TF_FLAGS            = 169;   /* boolean */
//    public static final int OPTION_TF_FLAGS            =  94;   /* int     */
    // #sijapp cond.end#
    //public static final int OPTION_MSGSEND_MODE        =  95;   /* int     */
    public static final int OPTIONS_CLIENT             =  96;   /* int     */
    
    public static final int OPTION_PRIVATE_STATUS      =  93;   /* int     */
    public static final int OPTION_CHAT_SMALL_FONT     = 135;   /* boolean */
    public static final int OPTION_SMALL_FONT          = 157;   /* boolean */
//    public static final int OPTION_USER_GROUPS         = 136;   /* boolean */
    public static final int OPTION_HISTORY             = 137;   /* boolean */
    public static final int OPTION_SHOW_LAST_MESS      = 142;   /* boolean */
    //public static final int OPTION_CLASSIC_CHAT        = 143;   /* boolean */
    public static final int OPTION_COLOR_SCHEME        =  73;   /* int     */
    public static final int OPTION_STATUS_MESSAGE      =   7;   /* String  */
    
    public static final int OPTION_XSTATUS             =  92;   /* int     */
//    public static final int OPTION_XTRAZ_ENABLE        = 156;   /* boolean */
    public static final int OPTION_XTRAZ_TITLE         =  19;   /* String  */
    public static final int OPTION_XTRAZ_DESC          =  20;   /* String  */
    public static final int OPTION_AUTO_STATUS         = 161;   /* boolean */
    public static final int OPTION_AUTO_XTRAZ          = 162;   /* boolean */
    
    public static final int OPTION_ANTISPAM_MSG        =  24;   /* String  */
    public static final int OPTION_ANTISPAM_HELLO      =  25;   /* String  */
    public static final int OPTION_ANTISPAM_ANSWER     =  26;   /* String  */
    public static final int OPTION_ANTISPAM_ENABLE     = 158;   /* boolean */
    public static final int OPTION_ANTISPAM_OFFLINE    = 159;   /* boolean */
    public static final int OPTION_ANTISPAM_KEYWORDS   =  29;   /* String  */

    public static final int OPTION_SAVE_TEMP_CONTACT   = 147;   /* boolean */
    
    public static final int OPTION_USE_SMILES          = 141;   /* boolean */
    public static final int OPTION_MD5_LOGIN           = 144;   /* boolean */
    // #sijapp cond.if modules_PROXY is "true" #
    public static final int OPTION_PRX_TYPE            =  76;   /* int     */
    public static final int OPTION_PRX_SERV            =   8;   /* String  */
    public static final int OPTION_PRX_PORT            =   9;   /* String  */
    public static final int OPTION_AUTORETRY_COUNT     =  10;   /* String  */
    public static final int OPTION_PRX_NAME            =  11;   /* String  */
    public static final int OPTION_PRX_PASS            =  12;   /* String  */
    // #sijapp cond.end#
    
    public static final int OPTIONS_GMT_OFFSET         =  87;   /* int     */
    public static final int OPTIONS_LOCAL_OFFSET       =  90;   /* int     */
    
    //public static final int OPTION_FULL_SCREEN         = 145;   /* boolean */
    public static final int OPTION_SILENT_MODE         = 150;   /* boolean */
    public static final int OPTION_BRING_UP            = 151;   /* boolean */
    
    protected static final int OPTIONS_LANG_CHANGED    = 148;
    
    public static final int OPTION_POPUP_WIN2          =  84;   /* int     */
    public static final int OPTION_EXT_CLKEY0          =  77;   /* int     */
    public static final int OPTION_EXT_CLKEYSTAR       =  78;   /* int     */
    public static final int OPTION_EXT_CLKEY4          =  79;   /* int     */
    public static final int OPTION_EXT_CLKEY6          =  80;   /* int     */
    public static final int OPTION_EXT_CLKEYCALL       =  81;   /* int     */
    public static final int OPTION_EXT_CLKEYPOUND      =  82;   /* int     */
    public static final int OPTION_VISIBILITY_ID       =  85;   /* int     */
    
    public static final int OPTION_UNTITLED_INPUT      = 160;   /* boolean */
    
    public static final int OPTION_LIGHT               = 163;   /* boolean */
    public static final int OPTION_LIGHT_NONE          =  97;   /* int     */
    public static final int OPTION_LIGHT_ONLINE        =  98;   /* int     */
    public static final int OPTION_LIGHT_KEY_PRESS     =  99;   /* int     */
    public static final int OPTION_LIGHT_CONNECT       = 100;   /* int     */
    public static final int OPTION_LIGHT_MESSAGE       = 101;   /* int     */
    public static final int OPTION_LIGHT_ERROR         = 102;   /* int     */
    public static final int OPTION_LIGHT_SYSTEM        = 103;   /* int     */
    public static final int OPTION_LIGHT_TICK          = 104;   /* int     */

    public static final int OPTION_INPUT_MODE          = 105;   /* int     */
    
    public static final int OPTION_SWAP_SOFT_KEY       = 164;   /* boolean */
    public static final int OPTION_SHOW_SOFTBAR        = 167;   /* boolean */
    public static final int OPTION_CUSTOM_GC           = 168;   /* boolean */
    public static final int OPTION_POPUP_OVER_SYSTEM   = 170;   /* boolean */
    public static final int OPTION_SORT_UP_WITH_MSG    = 171;   /* boolean */
    public static final int OPTION_SWAP_SEND_AND_BACK  = 172;   /* boolean */

    //Hotkey Actions
    public static final int HOTKEY_NONE      =  0;
    public static final int HOTKEY_INFO      =  2;
    public static final int HOTKEY_NEWMSG    =  3;
    public static final int HOTKEY_ONOFF     =  4;
    public static final int HOTKEY_OPTIONS   =  5;
    public static final int HOTKEY_MENU      =  6;
    public static final int HOTKEY_LOCK      =  7;
    public static final int HOTKEY_HISTORY   =  8;
    public static final int HOTKEY_MINIMIZE  =  9;
    public static final int HOTKEY_CLI_INFO  = 10;
    public static final int HOTKEY_FULLSCR   = 11;
    public static final int HOTKEY_SOUNDOFF  = 12;
    public static final int HOTKEY_STATUSES  = 13;
    public static final int HOTKEY_MAGIC_EYE = 14;
    public static final int HOTKEY_LIGHT     = 15;
    public static final int HOTKEY_OPEN_CHATS = 16;
            
    private static int[] accountKeys =  {OPTION_NICK1, OPTION_UIN1, OPTION_PASSWORD1};
    private static String[] accountList = new String[]{"", "", ""};

    /**************************************************************************/
    public static final int ACCOUNT_UIN  = 0;
    public static final int ACCOUNT_PASS = 1;
    public static final int ACCOUNT_NICK = 2;
    public static void setAccount(String[] account) {
        setString(accountKeys[1], account[ACCOUNT_UIN]); // uin
        setString(accountKeys[2], account[ACCOUNT_PASS]); // pass
        setString(accountKeys[0], account[ACCOUNT_NICK]); // nick
        accountList[ACCOUNT_UIN]  = getString(accountKeys[1]);
        accountList[ACCOUNT_PASS] = getString(accountKeys[2]);
        accountList[ACCOUNT_NICK] = getString(accountKeys[0]);
    }
    public static final String[] getAccount() {
        String[] res = accountList;
        res[ACCOUNT_UIN]  = getString(accountKeys[1]); // uin
        res[ACCOUNT_PASS] = getString(accountKeys[2]); // pass
        res[ACCOUNT_NICK] = getString(accountKeys[0]); // nick
        return res;
    }
    
    //private static Protocol[] protocolList = new Protocol[3];
    private static Protocol protocolInstance;
    private static final int PROTOCOL_JABBER = 2;
    public static final Protocol getProtocol() {
        String[] account = Options.getAccount();
        String uin = account[Options.ACCOUNT_UIN];
        
        //Protocol protocol = protocolList[currentAccount];
        Protocol protocol = protocolInstance;
        if ((null != protocol) && (protocol.getUin().equals(uin))) {
            return protocol;
        }
        if (null != protocol) {
            protocol.disconnect();
            protocol.safeSave();
            protocol.dismiss();
        }
        protocol = new protocol.jabber.Jabber();
        protocol.setAccount(account);
        protocol.safeLoad();

        // #sijapp cond.if modules_SERVERLISTS is "true" #
        protocol.setPrivateStatus((byte)Options.getInt(Options.OPTION_PRIVATE_STATUS));
        // #sijapp cond.end #

        //protocolList[currentAccount] = protocol;
        int statusIndex = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);
        protocol.setOnlineStatus(statusIndex, null);
        protocolInstance = protocol;
        return protocol;
    }
    
    /**************************************************************************/
    
    // Hashtable containing all option key-value pairs
    //static private Hashtable options = new Hashtable(128);
    static private Object[] options = new Object[256];
    
    
    public static final Options instance = new Options();
    
    public void loadOptions() {
        // Try to load option values from record store and construct options form
        try {
            setDefaults();
            load();
        // Use default values if loading option values from record store failed
        } catch (Exception e) {
            setDefaults();
            setBoolean(OPTIONS_LANG_CHANGED, true);
        }
    }
    /* Set default values
       This is done before loading because older saves may not contain all new values */
    private void setDefaults() {
        setString (Options.OPTION_UIN1,               "");
        setString (Options.OPTION_PASSWORD1,          "");
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"# ===>
        setString (Options.OPTION_SRV_HOST,           "login.icq.com");
        // #sijapp cond.else# ===
        // #sijapp cond.if modules_PROXY is "true" #
        setString (Options.OPTION_SRV_HOST,           "64.12.161.185"); //Cannot resolve host IP on MIDP1 devices
        // #sijapp cond.else#
        setString (Options.OPTION_SRV_HOST,           "login.icq.com");
        // #sijapp cond.end#
        // #sijapp cond.end# <===
        setString (Options.OPTION_SRV_PORT,           "5190");
        setBoolean(Options.OPTION_KEEP_CONN_ALIVE,    true);
        setBoolean(Options.OPTION_RECONNECT,          true);
        setInt    (Options.OPTION_RECONNECT_NUMBER,   10);
        setString (Options.OPTION_CONN_ALIVE_INVTERV, "120");
        //setInt    (Options.OPTION_CONN_PROP,          0);
        setBoolean(Options.OPTION_ASYNC,              false);
        setInt    (Options.OPTION_CONN_TYPE,          0);
        // #sijapp cond.if target is "MIDP2"#
        setBoolean(Options.OPTION_SHADOW_CON,         Jimm.isPhone(Jimm.PHONE_NOKIA));
        // #sijapp cond.elseif target isnot "MOTOROLA" #
        setBoolean(Options.OPTION_SHADOW_CON,         false);
        // #sijapp cond.end#
        setBoolean(Options.OPTION_MD5_LOGIN,          true);
        setBoolean(Options.OPTION_AUTO_CONNECT,       false);
        //setString (Options.OPTION_HTTP_USER_AGENT,    "unknown");
        //setString (Options.OPTION_HTTP_WAP_PROFILE,   "unknown");
        setString (Options.OPTION_UI_LANGUAGE,        ResourceBundle.getSystemLanguage());
        setBoolean(Options.OPTION_DISPLAY_DATE,       false);
        setInt    (Options.OPTION_CL_SORT_BY,         0);
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE,    false);
        
        //setBoolean(Options.OPTION_CHECK_UPDATES,      true);

        // #sijapp cond.if target isnot "DEFAULT"#
        setInt    (Options.OPTION_MESS_NOTIF_MODE,    0);
        setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  0);
        setInt    (Options.OPTION_TYPING_MODE,        0);
        // #sijapp cond.if target isnot "RIM"#       
        setInt    (Options.OPTION_MESS_NOTIF_VOL,     50);
        setInt    (Options.OPTION_ONLINE_NOTIF_VOL,   50);
        setInt    (Options.OPTION_TYPING_VOL,         50);
        setString (Options.OPTION_MESS_NOTIF_FILE,    "unknown");
        setString (Options.OPTION_ONLINE_NOTIF_FILE,  "unknown");
        setString (Options.OPTION_TYPING_FILE,        "unknown");
        // #sijapp cond.end #
        // #sijapp cond.end #

        setBoolean(Options.OPTION_DETECT_ENCODING,    true);
        // #sijapp cond.if target is "SIEMENS2"#
        setBoolean(Options.OPTION_TF_FLAGS,           false);
        // #sijapp cond.elseif target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        setBoolean(Options.OPTION_TF_FLAGS,           true);
        // #sijapp cond.end#

        //setInt    (Options.OPTION_MSGSEND_MODE,       0);      
        // #sijapp cond.if target isnot "DEFAULT"#
        setInt    (Options.OPTION_VIBRATOR,           0);
        // #sijapp cond.end#
        // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #    
        setBoolean(Options.OPTION_VOLUME_BUGFIX,      true);
        // #sijapp cond.end #    

        // #sijapp cond.if modules_TRAFFIC is "true" #
        setInt    (Options.OPTION_COST_PER_PACKET,    0);
        setInt    (Options.OPTION_COST_PER_DAY,       0);
        setInt    (Options.OPTION_COST_PACKET_LENGTH, 1024);
        setString (Options.OPTION_CURRENCY,           "$");
        // #sijapp cond.end #
        setLong   (Options.OPTION_ONLINE_STATUS,      Status.I_STATUS_ONLINE);
        
        setBoolean(Options.OPTION_CHAT_SMALL_FONT,    true);
        setBoolean(Options.OPTION_SMALL_FONT,         true);
        setBoolean(Options.OPTION_HISTORY,            false);
        setInt    (Options.OPTION_COLOR_SCHEME,       1);
        setBoolean(Options.OPTION_USE_SMILES,         true);
        setBoolean(Options.OPTION_SHOW_LAST_MESS,     false);
        // #sijapp cond.if modules_PROXY is "true" #
        setInt    (Options.OPTION_PRX_TYPE,           0);
        setString (Options.OPTION_PRX_SERV,           "");
        setString (Options.OPTION_PRX_PORT,           "1080");
        setString (Options.OPTION_AUTORETRY_COUNT,    "1");
        setString (Options.OPTION_PRX_NAME,           "");
        setString (Options.OPTION_PRX_PASS,           "");
        // #sijapp cond.end #
        setInt    (Options.OPTION_VISIBILITY_ID,      0);
        
        // #sijapp cond.if target isnot "DEFAULT" #
        setBoolean(Options.OPTION_SILENT_MODE,        false);
        // #sijapp cond.end #
        setInt    (Options.OPTION_EXT_CLKEYSTAR,      HOTKEY_OPEN_CHATS);
        setInt    (Options.OPTION_EXT_CLKEY0,         HOTKEY_STATUSES);
        setInt    (Options.OPTION_EXT_CLKEY4,         HOTKEY_CLI_INFO);
        setInt    (Options.OPTION_EXT_CLKEY6,         HOTKEY_INFO);
        setInt    (Options.OPTION_EXT_CLKEYCALL,      HOTKEY_HISTORY);
        setInt    (Options.OPTION_EXT_CLKEYPOUND,     HOTKEY_LOCK);
        
        setInt    (Options.OPTION_POPUP_WIN2,         0);
        //setBoolean(Options.OPTION_CLASSIC_CHAT,       false);
        
        setString (Options.OPTION_UIN2,               "");
        setString (Options.OPTION_PASSWORD2,          "");
        setString (Options.OPTION_UIN3,               "");     
        setString (Options.OPTION_PASSWORD3,          "");
        //setInt    (Options.OPTIONS_CURR_ACCOUNT,      0);
        
        // #sijapp cond.if target is "MIDP2"#
        //setBoolean(Options.OPTION_FULL_SCREEN,        !Jimm.isPhone(Jimm.PHONE_INTENT_JTE));
        // #sijapp cond.elseif target is "MOTOROLA" | target is "SIEMENS2"#
        //setBoolean(Options.OPTION_FULL_SCREEN,        true);
        // #sijapp cond.else #
        //setBoolean(Options.OPTION_FULL_SCREEN,        false);
        // #sijapp cond.end #
        // #sijapp cond.if target is "MOTOROLA"#
        setBoolean(Options.OPTION_CUSTOM_GC,          true);
        // #sijapp cond.else #
        setBoolean(Options.OPTION_CUSTOM_GC,          false);
        // #sijapp cond.end #
        
        // #sijapp cond.if target="MIDP2"#
        setBoolean(Options.OPTION_BRING_UP,           true);
        // #sijapp cond.end#
        
        int time = TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60);
        /* Offset (in hours) between GMT time and local zone time 
           GMT_time + GMT_offset = Local_time */
        setInt    (Options.OPTIONS_GMT_OFFSET,        time);
        
        /* Offset (in hours) between GMT time and phone clock 
           Phone_clock + Local_offset = GMT_time */
        setInt    (Options.OPTIONS_LOCAL_OFFSET,      0);
        
        setBoolean(OPTIONS_LANG_CHANGED,              false);
        setBoolean(OPTION_SHOW_SOFTBAR,               true);
        
        // #sijapp cond.if modules_LIGHT is "true" #
        setInt(Options.OPTION_LIGHT_NONE,      0);
        setInt(Options.OPTION_LIGHT_ONLINE,    100);
        setInt(Options.OPTION_LIGHT_KEY_PRESS, 100);
        setInt(Options.OPTION_LIGHT_CONNECT,   101);
        setInt(Options.OPTION_LIGHT_MESSAGE,   101);
        setInt(Options.OPTION_LIGHT_ERROR,     100);
        setInt(Options.OPTION_LIGHT_SYSTEM,    100);
        setInt(Options.OPTION_LIGHT_TICK,      15);
        // #sijapp cond.if target="MOTOROLA"#
        Options.setBoolean(Options.OPTION_LIGHT, true);
        // #sijapp cond.else#
        Options.setBoolean(Options.OPTION_LIGHT, false);
        // #sijapp cond.end#
        // #sijapp cond.end #
            
        // #sijapp cond.if modules_SOUND is "true" #
        // #sijapp cond.if target isnot "DEFAULT" & target isnot "RIM"#
        selectSoundType(OPTION_ONLINE_NOTIF_FILE, "online.");
        selectSoundType(OPTION_MESS_NOTIF_FILE,   "message.");
        selectSoundType(OPTION_TYPING_FILE,       "typing.");
        // #sijapp cond.end#
        // #sijapp cond.end#
    }
    
    public void resetLangDependedOpts() {
        if (!getBoolean(OPTIONS_LANG_CHANGED)) {
            return;
        }
        setString(Options.OPTION_STATUS_MESSAGE, ResourceBundle.getString("status_message_text"));
    }

    /* Load option values from record store */
    private void load() throws IOException, RecordStoreException {
        /* Read all option key-value pairs */
        byte[] buf = loadSlot(SLOT_OPTIONS);
        if (buf == null) {
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        while (dis.available() > 0) {
            int optionKey = dis.readUnsignedByte();
            if (optionKey < 64) {  /* 0-63 = String */
                setString(optionKey, dis.readUTF());
            } else if (optionKey < 128) {  /* 64-127 = int */
                setInt(optionKey, dis.readInt());
            } else if (optionKey < 192) {  /* 128-191 = boolean */
                setBoolean(optionKey, dis.readBoolean());
            } else if (optionKey < 224) {  /* 192-223 = long */
                setLong(optionKey, dis.readLong());
            } else {  /* 226-255 = Scrambled String */
                byte[] optionValue = new byte[dis.readUnsignedShort()];
                dis.readFully(optionValue);
                optionValue = Util.decipherPassword(optionValue);
                setString(optionKey, StringConvertor.utf8beByteArrayToString(optionValue, 0, optionValue.length));
            }
        }
    }


    public static byte[] loadSlot(int slotId) {
        try {
            RecordStore account = RecordStore.openRecordStore("rms-options", false);
            byte[] res = account.getRecord(slotId);
            /* Close record store */
            account.closeRecordStore();
            return res;
        } catch (Exception e) {
            return null;
        }
    }
    public static void saveSlot(int slotId, byte[] buf) {
        try {
            /* Open record store */
            RecordStore account = RecordStore.openRecordStore("rms-options", true);

            /* Add empty records if necessary */
            while (account.getNumRecords() <= slotId) {
                int nextSlotNum = account.getNumRecords() + 1;
                if (nextSlotNum == SLOT_VERSION) {
                    /* Add version info to record store */
                    byte[] ver = StringConvertor.stringToByteArray(Jimm.VERSION);
                    account.addRecord(ver, 0, ver.length);
                } else {
                    account.addRecord(new byte[0], 0, 0);
                }
            }
            
            account.setRecord(slotId, buf, 0, buf.length);
            
            /* Close record store */
            account.closeRecordStore();
        } catch (Exception e) {
        }
    }
    /* Save option values to record store */
    private static void save() throws IOException, RecordStoreException {
        /* Temporary variables */
        
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStart();
        // #sijapp cond.end #

        /* Save all option key-value pairs */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int key = 0; key < options.length; key++) {
            if (null == options[key]) {
                continue;
            }
            dos.writeByte(key);
            if (key < 64) {  /* 0-63 = String */
                dos.writeUTF((String)options[key]);
            } else if (key < 128) {  /* 64-127 = int */
                dos.writeInt(((Integer)options[key]).intValue());
            } else if (key < 192) {  /* 128-191 = boolean */
                dos.writeBoolean(((Boolean)options[key]).booleanValue());
            } else if (key < 224) {  /* 192-223 = long */
                dos.writeLong(((Long)options[key]).longValue());
            } else if (key < 256) {  /* 226-255 = Scrambled String */
                String str = (String)options[key];
                byte[] optionValue = StringConvertor.stringToByteArray(str);
                optionValue = Util.decipherPassword(optionValue);
                dos.writeShort(optionValue.length);
                dos.write(optionValue);
            }
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("make options");
        // #sijapp cond.end #

        /* Close record store */
        saveSlot(SLOT_OPTIONS, baos.toByteArray());
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSlot(OPTIONS)");
        // #sijapp cond.end #
    }

    public static synchronized void safeSave() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        long profiler = DebugLog.profilerStart();
        // #sijapp cond.end #
        try {
            save();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("options: " + e.toString());
            // #sijapp cond.end #
            JimmException.handleException(new JimmException(172, 0, true));
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSave", profiler);
        // #sijapp cond.end #
    }

    /* Option retrieval methods (no type checking!) */
    public static String getString(int key) {
        if (key > 255) {
            int opt = 0;
            switch (key) {
                case OPTION_PASSWORD: opt = 2; break;
                case OPTION_UIN:      opt = 1; break;
                //case OPTION_NICK:     break;
            }
            key = accountKeys[opt];
        }
        String value = (String)options[key];
        return (null == value) ? "" : value;
    }
    
    public static int getInt(int key) {
        Integer value = (Integer) options[key];
        return value == null ? 0 : value.intValue();
    }
    
    public static boolean getBoolean(int key) {
        Boolean value = (Boolean) options[key];
        return value == null ? false : value.booleanValue();
    }
    
    public static long getLong(int key) {
        Long value = (Long) options[key];
        return value == null ? 0 : value.longValue();
    }


    /* Option setting methods (no type checking!) */
    public static void setString(int key, String value) {
        options[key] = value;
    }
    public static void setInt(int key, int value) {
        options[key] = new Integer(value);
    }
    
    public static void setBoolean(int key, boolean value) {
        options[key] = new Boolean(value);
    }
    
    public static void setLong(int key, long value) {
        options[key] = new Long(value);
    }

    /**************************************************************************/
    
    
    
    
    // #sijapp cond.if modules_SOUND is "true" #
    // #sijapp cond.if target isnot "DEFAULT" & target isnot "RIM"#
    public static void selectSoundType(int option, String name) {
        /* Test other extensions */
        String[] exts = Util.explode("mp3|wav|mmf|amr|mid|midi|", '|');
        for (int i = 0; i < exts.length; i++) {
            String testFile = name + exts[i];
            if (Notify.testSoundFile(testFile)) {
                setString(option, testFile);
                return;
            }
        }
    }
    // #sijapp cond.end#    
    // #sijapp cond.end#
    
    public static String getServerHostAndPort() {
        String servers = Options.getString(Options.OPTION_SRV_HOST).replace('\n', ' ');
        String[] serverList = Util.explode(servers, ' ');
        String server = Util.replace(serverList[0], "\r", "");
        if (serverList[0].indexOf(':') >= 0) {
            return server;
        }
        return server + ":" + Options.getString(Options.OPTION_SRV_PORT);
    }
    
    public static void nextServer() {
        String servers = Options.getString(Options.OPTION_SRV_HOST);
        char delim = (servers.indexOf(' ') < 0) ? '\n' : ' ';
        servers = servers.replace('\n', ' ');
        String[] serverList = Util.explode(servers, ' ');
        if (serverList.length < 2) {
            return;
        }
        StringBuffer newSrvs = new StringBuffer();
        for (int i = 1; i < serverList.length; i++) {
            newSrvs.append(serverList[i]);
            newSrvs.append(delim);
        }
        newSrvs.append(serverList[0]);
        Options.setString(Options.OPTION_SRV_HOST, newSrvs.toString());
    }
}
