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
package jimm;

import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.util.*;
import java.util.*;
import javax.microedition.lcdui.*;
import DrawControls.VirtualList;

/* Form for editing option values */
public class OptionsForm implements CommandListener, SelectListener {
    private int currentHour;

    private Select optionsMenu = new Select();
    private Select hotkey = null;
    private Select hotkeyAction = null;
    private FormEx form = new FormEx("options_lng", "save", "back", this);
    private int currentOptionsForm;
    private TextListEx accountList = null;
    
    // Static constants for menu actios
    private static final int OPTIONS_ACCOUNT    = 0;
    private static final int OPTIONS_NETWORK    = 1;
    private static final int OPTIONS_INTERFACE  = 3;
    private static final int OPTIONS_SCHEME     = 4;
    private static final int OPTIONS_HOTKEYS    = 5;
    private static final int OPTIONS_SIGNALING  = 6;
    
    private static final int OPTIONS_TIMEZONE   = 8;
    private static final int OPTIONS_ANTISPAM   = 9;
    private static final int OPTIONS_LIGHT      = 10;
    private static final int OPTIONS_ABSENCE       = 11;
    
    // Exit has to be biggest element cause it also marks the size
    private static final int MENU_EXIT          = 12;

    final private String[] hotkeyActionNames = Util.explode (
                        "ext_hotkey_action_none"
                + "|" + "info"
                + "|" + "send_message"
                + "|" + "open_chats"
                + "|" + "ext_hotkey_action_onoff"
                + "|" + "options_lng"
                + "|" + "menu"
                + "|" + "keylock"
                + "|" + "user_statuses", 
                '|'
    );
    
    final private int [] hotkeyActions = {
        Options.HOTKEY_NONE,
        Options.HOTKEY_INFO,
        Options.HOTKEY_NEWMSG,
        Options.HOTKEY_OPEN_CHATS,
        Options.HOTKEY_ONOFF,
        Options.HOTKEY_OPTIONS,
        Options.HOTKEY_MENU,
        Options.HOTKEY_LOCK,
        Options.HOTKEY_STATUSES            
    };
    
    public OptionsForm() {        
    }

    
    // Initialize the kist for the Options menu
    private void initOptionsList() {
        optionsMenu.clean();
        
        optionsMenu.add("options_account", OPTIONS_ACCOUNT);
        optionsMenu.add("options_network", OPTIONS_NETWORK);
        optionsMenu.add("options_interface", OPTIONS_INTERFACE);
        optionsMenu.add("options_hotkeys", OPTIONS_HOTKEYS);
        optionsMenu.add("options_signaling", OPTIONS_SIGNALING);
        optionsMenu.add("absence", OPTIONS_ABSENCE);
        optionsMenu.add("time_zone", OPTIONS_TIMEZONE);
        optionsMenu.setActionListener(this);
        optionsMenu.setSelectedItemCode(currentOptionsForm);
    }

    private void addHotKey(String keyName, int option) {
        int optionValue = Options.getInt(option);
        String name = null;
        for (int i = 0; i < hotkeyActionNames.length; i++) {
            if (hotkeyActions[i] == optionValue) {
                name = ResourceBundle.getString(keyName) + ": "
                        + ResourceBundle.getString(hotkeyActionNames[i]);  
            }
        }
        if (null == name) {
            name = ResourceBundle.getString(keyName) + ": <???>";
        }
        hotkey.addRaw(name, null, option);
    }
    
    private void initHotkeyMenuUI() {
	if (null == hotkey) {
	    hotkey = new Select();
	}
        int opt = hotkey.getSelectedItemCode();
        hotkey.clean();
        addHotKey("ext_clhotkey0",     Options.OPTION_EXT_CLKEY0);
        addHotKey("ext_clhotkey4",     Options.OPTION_EXT_CLKEY4);
        addHotKey("ext_clhotkey6",     Options.OPTION_EXT_CLKEY6);
        addHotKey("ext_clhotkeystar",  Options.OPTION_EXT_CLKEYSTAR);
        addHotKey("ext_clhotkeypound", Options.OPTION_EXT_CLKEYPOUND);
        // #sijapp cond.if target is "SIEMENS2"#
        addHotKey("ext_clhotkeycall", Options.OPTION_EXT_CLKEYCALL);
        // #sijapp cond.elseif target is "MIDP2" #
         addHotKey("ext_clhotkeycall", Options.OPTION_EXT_CLKEYCALL);
        // #sijapp cond.end#
        hotkey.setSelectedItemCode(opt);
        hotkey.setActionListener(this);
    }

    private void initHotkeyActionMenuUI() {
	if (null == hotkeyAction) {
	    hotkeyAction = new Select();
	}
        hotkeyAction.clean();
        for (int i=0; i < hotkeyActionNames.length; i++) {
            hotkeyAction.add(hotkeyActionNames[i], hotkeyActions[i]);
        }
        hotkeyAction.setSelectedItemCode(Options.getInt(hotkey.getSelectedItemCode()));
        hotkeyAction.setActionListener(this);
    }
    ///////////////////////////////////////////////////////////////////////////
    
    /* Activate options menu */
    public static void activate() {
        OptionsForm instance = new OptionsForm();
        instance.initOptionsList();
        instance.optionsMenu.show();
    }
    
    private void setChecked(int contrilId, String lngStr, int optValue) {
        form.addChoiceItem(contrilId, lngStr, Options.getBoolean(optValue));
    }
    
    

    /* Helpers for options UI: */
    private void createSelector(String cap, String items, int opt) {
        form.addSelector(opt, cap, items, Options.getInt(opt));
    }
    private void loadOptionString(int opt, String label, int size, int type) {
        form.addTextField(opt, label, Options.getString(opt), size, type);
    }
    private void saveOptionString(int opt) {
        Options.setString(opt, form.getTextFieldValue(opt));
    }
    private void loadOptionInt(int opt, String label, int size, int type) {
        form.addTextField(opt, label, String.valueOf(Options.getInt(opt)), size, type);
    }
    private void saveOptionInt(int opt, int defval) {
        Options.setInt(opt, Util.strToIntDef(form.getTextFieldValue(opt), defval));
    }
    private void saveOptionBoolean(int opt, int controlId, int index) {
        Options.setBoolean(opt, form.getChoiceItemValue(controlId, index));
    }
    private void saveOptionSelector(int opt) {
        Options.setInt(opt, form.getSelectorValue(opt));
    }

    private static final int keepConnAliveChoiceGroup = 1001;
    private static final int autoConnectChoiceGroup = 1002;
    private static final int connPropChoiceGroup = 1003;
    private static final int choiceInterfaceMisc = 1004;
    private static final int choiceContactList = 1005;
    private static final int chrgChat = 1006;
    private static final int chrgStatus = 1007;
    private static final int chrgMessages = 1008;
    private static final int chrgIcons = 1009;
    private static final int chsNotifyAct = 1010;
    private static final int uinField = 1011;
    private static final int passField = 1012;
    private static final int nickField = 1013;
    private static final int chrgAntispam = 1014;

    /* Command listener */
    public void commandAction(Command c, Displayable d) {
        /* Look for back command */
        if (form.backCommand == c) {
           back();

        // Look for save command
        } else if (c == form.saveCommand) {
            // Save values, depending on selected option menu item
            switch (currentOptionsForm) {
                case OPTIONS_ACCOUNT:
                    String[] account = new String[3];
                    account[Options.ACCOUNT_UIN] = form.getTextFieldValue(uinField);
                    account[Options.ACCOUNT_PASS] = form.getTextFieldValue(passField);
                    account[Options.ACCOUNT_NICK] = form.getTextFieldValue(nickField);
                    Options.setAccount(account);
                    ContactList.getInstance().setProtocol(Options.getProtocol());
                    ContactList.getInstance().update();
                    break;

                case OPTIONS_NETWORK:
                    saveOptionBoolean(Options.OPTION_KEEP_CONN_ALIVE, keepConnAliveChoiceGroup, 0);
                    saveOptionString(Options.OPTION_CONN_ALIVE_INVTERV);
                    saveOptionBoolean(Options.OPTION_AUTO_CONNECT, autoConnectChoiceGroup, 0);
                    saveOptionBoolean(Options.OPTION_RECONNECT, connPropChoiceGroup, 0);
                    saveOptionInt(Options.OPTION_RECONNECT_NUMBER, 1);
                    initOptionsList();
                    break;

                case OPTIONS_INTERFACE:
                    if (ResourceBundle.langAvailable.length > 1) {
                        int lang = form.getSelectorValue(Options.OPTION_UI_LANGUAGE);
                        Options.setString(Options.OPTION_UI_LANGUAGE, ResourceBundle.langAvailable[lang]);
                    }

                    int idx = 0;
                    saveOptionBoolean(Options.OPTION_DISPLAY_DATE, choiceInterfaceMisc, idx++);
                    saveOptionBoolean(Options.OPTION_SWAP_SOFT_KEY, choiceInterfaceMisc, idx++);
                    saveOptionBoolean(Options.OPTION_SHOW_SOFTBAR,  choiceInterfaceMisc, idx++);
                    
                    idx = 0;
                    //saveOptionBoolean(Options.OPTION_USER_GROUPS, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_SMALL_FONT, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_SAVE_TEMP_CONTACT, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG, choiceContactList, idx++);
                    
                    saveOptionSelector(Options.OPTION_CL_SORT_BY);
                    
                    idx = 0;
                    saveOptionBoolean(Options.OPTION_CHAT_SMALL_FONT, chrgChat, idx++);
                    // #sijapp cond.if modules_SMILES is "true"#
                    saveOptionBoolean(Options.OPTION_USE_SMILES,      chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if modules_HISTORY is "true"#
                    saveOptionBoolean(Options.OPTION_HISTORY,         chrgChat, idx++);
                    saveOptionBoolean(Options.OPTION_SHOW_LAST_MESS,  chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" #
                    //saveOptionBoolean(Options.OPTION_CLASSIC_CHAT, chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
                    saveOptionBoolean(Options.OPTION_SWAP_SEND_AND_BACK, chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    saveOptionBoolean(Options.OPTION_TF_FLAGS, chrgChat, idx++);
                    // #sijapp cond.end#
                    saveOptionBoolean(Options.OPTION_UNTITLED_INPUT, chrgChat, idx++);
                    
                    final String sysLang = ResourceBundle.getCurrUiLanguage();
                    final String newLang = Options.getString(Options.OPTION_UI_LANGUAGE);
                    if (!sysLang.equals(newLang)) {
                        Options.setBoolean(Options.OPTIONS_LANG_CHANGED, true);
                    }
                    ContactList.getInstance().optionsChanged();
                    ContactList.getInstance().update();
                    break;
                    
                case OPTIONS_SIGNALING:
                    // #sijapp cond.if target isnot "DEFAULT" #
                    saveOptionSelector(Options.OPTION_VIBRATOR);
                    saveOptionSelector(Options.OPTION_ONLINE_NOTIF_MODE);
                    saveOptionSelector(Options.OPTION_MESS_NOTIF_MODE);
                    //saveOptionSelector(Options.OPTION_TYPING_MODE);
                    // #sijapp cond.end#
                    saveOptionSelector(Options.OPTION_POPUP_WIN2); 
                    idx = 0;
                    saveOptionBoolean(Options.OPTION_POPUP_OVER_SYSTEM, chsNotifyAct, idx++);
                    // #sijapp cond.if target="MIDP2"#
                    saveOptionBoolean(Options.OPTION_BRING_UP, chsNotifyAct, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #    
                    saveOptionBoolean(Options.OPTION_VOLUME_BUGFIX, chsNotifyAct, idx++);
                    // #sijapp cond.end#
                    saveOptionBoolean(Options.OPTION_CUSTOM_GC, chsNotifyAct, idx++);
                    break;

                // #sijapp cond.if modules_ANTISPAM is "true"#
                case OPTIONS_ANTISPAM:
                    saveOptionString(Options.OPTION_ANTISPAM_MSG);
                    saveOptionString(Options.OPTION_ANTISPAM_ANSWER);
                    saveOptionString(Options.OPTION_ANTISPAM_HELLO);
                    saveOptionString(Options.OPTION_ANTISPAM_KEYWORDS);
                    saveOptionBoolean(Options.OPTION_ANTISPAM_ENABLE,  chrgAntispam, 0);
                    saveOptionBoolean(Options.OPTION_ANTISPAM_OFFLINE, chrgAntispam, 1);
                    break;
                // #sijapp cond.end#

                case OPTIONS_ABSENCE:
                    saveOptionBoolean(Options.OPTION_AUTOANSWER_STATE, Options.OPTION_AUTOANSWER_STATE, 0);
                    saveOptionString(Options.OPTION_AUTOANSWER);
                    break;

                case OPTIONS_TIMEZONE: {
                    /* Set up time zone*/
                    int timeZone = form.getSelectorValue(Options.OPTIONS_GMT_OFFSET) - 12;
                    Options.setInt(Options.OPTIONS_GMT_OFFSET, timeZone);
                    
                    /* Translate selected time to GMT */
                    int selHour = form.getSelectorValue(Options.OPTIONS_LOCAL_OFFSET) - timeZone;
                    selHour = (selHour + 24) % 24;

                    /* Calculate diff. between selected GMT time and phone time */ 
                    int localOffset = selHour - currentHour;
                    while (localOffset >= 12) localOffset -= 24;
                    while (localOffset < -12) localOffset += 24;
                    Options.setInt(Options.OPTIONS_LOCAL_OFFSET, localOffset);
                    break;
                }
                                        
            }

            /* Save options */
            Options.safeSave();
            back();
        }

    }
    
    public void showCurrentAccountEditor() {
        showAccountEditor();
    }
    private void showAccountEditor() {
        currentOptionsForm = OPTIONS_ACCOUNT;
        String[] account = Options.getAccount();
        form.clearForm();
        // #sijapp cond.if protocols_ICQ is "true" #
        //     #sijapp cond.if protocols_MRIM is "true" #
        form.addLatinTextField(uinField, "uin/e-mail", account[Options.ACCOUNT_UIN], 64, TextField.ANY);
        //     #sijapp cond.elseif protocols_JABBER is "true" #
        form.addLatinTextField(uinField, "uin/jid", account[Options.ACCOUNT_UIN], 64, TextField.ANY);
        //     #sijapp cond.else #
        form.addTextField(uinField, "uin", account[Options.ACCOUNT_UIN], 10, TextField.ANY);
        //     #sijapp cond.end #
        // #sijapp cond.elseif protocols_MRIM is "true" #
        form.addTextField(uinField, "e-mail", account[Options.ACCOUNT_UIN], 64, TextField.EMAILADDR);
        // #sijapp cond.elseif protocols_JABBER is "true" #
        form.addTextField(uinField, "jid", account[Options.ACCOUNT_UIN], 64, TextField.ANY);
        // #sijapp cond.else #
        // undefined protocol
        // #sijapp cond.end #
        form.addTextField(passField, "password", account[Options.ACCOUNT_PASS], 32, TextField.PASSWORD);
        form.addTextField(nickField, "nick", account[Options.ACCOUNT_NICK], 20, TextField.ANY);
        show();
    }
    public boolean setCurrentAccount() {
        Options.safeSave();
        ContactList.getInstance().setProtocol(Options.getProtocol());
        ContactList.getInstance().update();
        return true;
    }
    public void select(Select select, int cmd) {
        if (hotkey == select) {
            initHotkeyActionMenuUI();
            hotkeyAction.show();
            return;
        }
        if (hotkeyAction == select) {
            Options.setInt(hotkey.getSelectedItemCode(), hotkeyAction.getSelectedItemCode());
            Options.safeSave();
            initHotkeyMenuUI();
            hotkeyAction.back();
            return;
        }
        
        // Delete all items
        form.clearForm();
        // Add elements, depending on selected option menu item
        currentOptionsForm = cmd;
        switch (currentOptionsForm) {
            case OPTIONS_ACCOUNT:
                showAccountEditor();
                return;
                
            case OPTIONS_NETWORK:
                // #sijapp cond.if protocols_ICQ is "true" #
                // Initialize elements (network section)
                loadOptionString(Options.OPTION_SRV_HOST, "server_host", 512, TextField.ANY);
                loadOptionString(Options.OPTION_SRV_PORT, "server_port", 5, TextField.NUMERIC);
                
                // #sijapp cond.if modules_PROXY is "true"#
                createSelector("conn_type", "socket"+"|"+"http"+"|"+"proxy", Options.OPTION_CONN_TYPE);
                // #sijapp cond.else#
                createSelector("conn_type", "socket"+"|"+"http", Options.OPTION_CONN_TYPE);
                // #sijapp cond.end#
                // #sijapp cond.end#

                form.addChoiceGroup(keepConnAliveChoiceGroup, "keep_conn_alive", Choice.MULTIPLE);
                setChecked(keepConnAliveChoiceGroup, "yes", Options.OPTION_KEEP_CONN_ALIVE);
                
                loadOptionString(Options.OPTION_CONN_ALIVE_INVTERV, "timeout_interv", 3, TextField.NUMERIC);
                
                form.addChoiceGroup(connPropChoiceGroup, "conn_prop", Choice.MULTIPLE);
                setChecked(connPropChoiceGroup, "reconnect", Options.OPTION_RECONNECT);
                // #sijapp cond.if protocols_ICQ is "true" #
                setChecked(connPropChoiceGroup, "md5_login", Options.OPTION_MD5_LOGIN);
                setChecked(connPropChoiceGroup, "async",     Options.OPTION_ASYNC);
                // #sijapp cond.if target isnot "MOTOROLA"#
                setChecked(connPropChoiceGroup, "shadow_con", Options.OPTION_SHADOW_CON);
                // #sijapp cond.end#
                // #sijapp cond.end#
                
                form.addChoiceGroup(autoConnectChoiceGroup, "auto_connect", Choice.MULTIPLE);
                setChecked(autoConnectChoiceGroup, "yes", Options.OPTION_AUTO_CONNECT);
                
                //loadOptionString(Options.OPTION_HTTP_USER_AGENT, "http_user_agent", 256, TextField.ANY);
                //loadOptionString(Options.OPTION_HTTP_WAP_PROFILE, "http_wap_profile", 256, TextField.ANY);
                
                loadOptionInt(Options.OPTION_RECONNECT_NUMBER, "reconnect_number", 2, TextField.NUMERIC);
                break;
                
                
            case OPTIONS_INTERFACE:
                // Initialize elements (interface section)
                if (ResourceBundle.langAvailable.length > 1) {
                    int choiceType = Choice.EXCLUSIVE;
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    choiceType = Choice.POPUP;
                    // #sijapp cond.end#
                    form.addChoiceGroup(Options.OPTION_UI_LANGUAGE, "language", choiceType);
                    for (int j = 0; j < ResourceBundle.langAvailable.length; j++) {
                        boolean selected = ResourceBundle.langAvailable[j].equals(Options.getString(Options.OPTION_UI_LANGUAGE));
                        form.addChoiceItem(Options.OPTION_UI_LANGUAGE, ResourceBundle.langAvailableName[j], selected);
                    }
                }
                
                form.addChoiceGroup(choiceInterfaceMisc, "misc", Choice.MULTIPLE);
                setChecked(choiceInterfaceMisc, "display_date", Options.OPTION_DISPLAY_DATE);
                setChecked(choiceInterfaceMisc, "swap_soft_key", Options.OPTION_SWAP_SOFT_KEY);
                setChecked(choiceInterfaceMisc, "show_softbar",  Options.OPTION_SHOW_SOFTBAR);
                
                createSelector("sort_by",
                        "sort_by_status" + "|" + "sort_by_online" + "|" + "sort_by_name",
                        Options.OPTION_CL_SORT_BY);
                
                form.addChoiceGroup(choiceContactList, "contact_list", Choice.MULTIPLE);
                //setChecked(choiceContactList, "show_user_groups", Options.OPTION_USER_GROUPS);
                setChecked(choiceContactList, "hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                setChecked(choiceContactList, "small_font",   Options.OPTION_SMALL_FONT);
                setChecked(choiceContactList, "save_temp_contacts",   Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked(choiceContactList, "contacts_with_msg_at_top", Options.OPTION_SORT_UP_WITH_MSG);
                
                form.addChoiceGroup(chrgChat, "chat", Choice.MULTIPLE);
                setChecked(chrgChat, "small_font",      Options.OPTION_CHAT_SMALL_FONT);
                // #sijapp cond.if modules_SMILES is "true"#
                setChecked(chrgChat, "use_smiles",      Options.OPTION_USE_SMILES);
                // #sijapp cond.end#
                // #sijapp cond.if modules_HISTORY is "true"#
                setChecked(chrgChat, "use_history",     Options.OPTION_HISTORY);
                setChecked(chrgChat, "show_prev_mess",  Options.OPTION_SHOW_LAST_MESS);
                // #sijapp cond.end#
                // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" #
                //setChecked(chrgChat, "cl_chat",         Options.OPTION_CLASSIC_CHAT);
                // #sijapp cond.end#
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
                setChecked(chrgChat, "swap_send_and_back", Options.OPTION_SWAP_SEND_AND_BACK);
                // #sijapp cond.end#
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                setChecked(chrgChat, "auto_case",       Options.OPTION_TF_FLAGS);
                // #sijapp cond.end#
                setChecked(chrgChat, "untitled_input",  Options.OPTION_UNTITLED_INPUT);
                break;

            case OPTIONS_HOTKEYS:
                initHotkeyMenuUI();
                hotkey.show();
                return;
                
                /* Initialize elements (Signaling section) */
            case OPTIONS_SIGNALING:
                
                /* Vibrator notification controls */
                
                createSelector(
                        "vibration",
                        "no" + "|" + "yes" + "|" + "when_locked",
                        Options.OPTION_VIBRATOR);
                
                /* Message notification controls */
                
                createSelector(
                        "message_notification",
                        "no" + "|" + "beep",
                        Options.OPTION_MESS_NOTIF_MODE);
                
                /* Online notification controls */
                
                createSelector(
                        "onl_notification",
                        "no" + "|" + "beep", Options.OPTION_ONLINE_NOTIF_MODE);
                
                /* Typing notification controls */
                
                /* Popup windows control */
                createSelector("popup_win",
                        "no" + "|" + "pw_forme" + "|" + "pw_all",
                        Options.OPTION_POPUP_WIN2);
                
                
                form.addChoiceGroup(chsNotifyAct, null, Choice.MULTIPLE);
                setChecked(chsNotifyAct, "popup_win_over_system", Options.OPTION_POPUP_OVER_SYSTEM);
                // #sijapp cond.if target="MIDP2"#
                /* Midlet auto bring up controls on MIDP2 */
                setChecked(chsNotifyAct, "bring_up", Options.OPTION_BRING_UP);
                // #sijapp cond.end#
                
                // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #
                /* Sound volume bugfix controls on SIEMENS2 */
                setChecked(chsNotifyAct, "volume_bugfix", Options.OPTION_VOLUME_BUGFIX);
                // #sijapp cond.end #

                setChecked(chsNotifyAct, "show_memory_alert", Options.OPTION_CUSTOM_GC);
                break;
                
                // #sijapp cond.if modules_ANTISPAM is "true"#
            case OPTIONS_ANTISPAM:
                form.addChoiceGroup(chrgAntispam, null, Choice.MULTIPLE);
                form.addChoiceItem(chrgAntispam, "on", Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE));
                form.addChoiceItem(chrgAntispam, "cut_offline", Options.getBoolean(Options.OPTION_ANTISPAM_OFFLINE));
                form.addTextField(Options.OPTION_ANTISPAM_MSG, "antispam_msg", Options.getString(Options.OPTION_ANTISPAM_MSG), 256, TextField.ANY);
                form.addTextField(Options.OPTION_ANTISPAM_ANSWER, "antispam_answer", Options.getString(Options.OPTION_ANTISPAM_ANSWER), 256, TextField.ANY);
                form.addTextField(Options.OPTION_ANTISPAM_HELLO, "antispam_hello", Options.getString(Options.OPTION_ANTISPAM_HELLO), 256, TextField.ANY);
                form.addTextField(Options.OPTION_ANTISPAM_KEYWORDS, "antispam_keywords", Options.getString(Options.OPTION_ANTISPAM_KEYWORDS), 512, TextField.ANY);
                break;
                // #sijapp cond.end#

            case OPTIONS_ABSENCE:
                form.addChoiceGroup(Options.OPTION_AUTOANSWER_STATE, null, Choice.MULTIPLE);
                setChecked(Options.OPTION_AUTOANSWER_STATE, "autoanswer", Options.OPTION_AUTOANSWER_STATE);
                loadOptionString(Options.OPTION_AUTOANSWER, "answer", 256, TextField.ANY);
                break;
                
                /* Initialize elements (cost section) */
                // #sijapp cond.if modules_TRAFFIC is "true"#
            case OPTIONS_TRAFFIC:
                loadOptionDecimal(Options.OPTION_COST_PER_PACKET, "cpp");
                loadOptionDecimal(Options.OPTION_COST_PER_DAY, "cpd");
                form.addTextField(Options.OPTION_COST_PACKET_LENGTH,
                        "plength",
                        String.valueOf(Options.getInt(Options.OPTION_COST_PACKET_LENGTH) / 1024),
                        4, TextField.NUMERIC);
                loadOptionString(Options.OPTION_CURRENCY, "currency", 4, TextField.ANY);
                break;
                // #sijapp cond.end#
                
            case OPTIONS_TIMEZONE: {
                int choiceType = Choice.EXCLUSIVE;
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                choiceType = Choice.POPUP;
                // #sijapp cond.end#
                
                form.addChoiceGroup(Options.OPTIONS_GMT_OFFSET, "time_zone", choiceType);
                int gmtOffset = Options.getInt(Options.OPTIONS_GMT_OFFSET);
                for (int i = -12; i <= 13; i++) {
                    form.addChoiceItem(Options.OPTIONS_GMT_OFFSET,
                            "GMT" + (i < 0 ? "" : "+") + i + ":00", gmtOffset == i);
                }
                
                int[] currDateTime = Util.createDate(Util.createCurrentDate(false));
                form.addChoiceGroup(Options.OPTIONS_LOCAL_OFFSET, "local_time", choiceType);
                int minutes = currDateTime[Util.TIME_MINUTE];
                int hour = currDateTime[Util.TIME_HOUR];
                for (int i = 0; i < 24; i++) {
                    form.addChoiceItem(Options.OPTIONS_LOCAL_OFFSET,
                            i + (minutes < 10 ? ":0" : ":") + minutes,
                            hour == i);
                }
                currentHour = hour - Options.getInt(Options.OPTIONS_LOCAL_OFFSET)
                                   - Options.getInt(Options.OPTIONS_GMT_OFFSET);
                
                break;
            }
        }
        /* Activate options form */
        show();
    }
    private void show() {
        form.show();
    }
    private void back() {
        form.back();
    }
}