/*
 * Contact.java
 *
 * Created on 13 Май 2008 г., 15:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.*;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.chat.*;
import jimm.cl.ContactList;
import jimm.comm.message.*;
import jimm.modules.*;
import jimm.search.*;
import jimm.ui.*;
import jimm.comm.*;
import jimm.ui.timers.UIUpdater;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
abstract public class Contact extends TreeNode implements CommandListener, SelectListener {
    protected static final Select contactMenu = new Select();
    
    abstract public void setOfflineStatus();
    abstract public boolean isMeVisible();
    abstract public void showUserInfo();
    // #sijapp cond.if (modules_CLIENTS is "true") | (modules_FILES is "true") #
    abstract public void showClientInfo();
    // #sijapp cond.end #
    abstract public void showStatus();
    abstract public void sendMessage(String msg, boolean addToChat);
    abstract public boolean isOnline();

    protected String uin;
    public final String getUin() {
        return uin;
    }
    public String getUniqueUin() {
        return uin;
    }
    
    public void setName(String newName) {
        if (!StringConvertor.isEmpty(newName)) {
    	    name = newName;
        }
    }
    private int groupId;
    public final void setGroupId(int id) {
        groupId = id;
    }
    public int getGroupId() {
        return groupId;
    }
    public final Group getGroup() {
        return getProtocol().getGroupById(groupId);
    }
    public final void setGroup(Group group) {
        setGroupId((null == group) ? 0 : group.getId());
    }

    public boolean isVisibleInContactList() {
        if (!Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) return true;
        return isOnline() || hasChat() || isTemp();
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    public void showHistory() {
        if (hasChat()) {
            getChat().getHistory().showHistoryList();
        } else {
            HistoryStorage.getHistory(this).showHistoryList();
        }
    }
    // #sijapp cond.end#

///////////////////////////////////////////////////////////////////////////
    protected Status status;
    public Status getStatus() {
        return status;
    }

///////////////////////////////////////////////////////////////////////////
    protected static boolean capLocked = false;
    protected final void showTopLine(String text) {
        if (capLocked) {
            return;
        }
        Object vis = null;
        if (messageTextbox.isShown()) {
            vis = messageTextbox;
        } else {
            if (hasChat()) {
                vis = getChat();
            }
        }
        UIUpdater.startFlashCaption(vis, text);
    }
    protected void statusChainged(boolean prevOffline, boolean nowOffline) {
        if (prevOffline && !nowOffline) {
            Notify.playSoundNotification(Notify.SOUND_TYPE_ONLINE);  
        }
        if (isCurrent()) {
            showTopLine(status.getName());
        }
    }
///////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_FILES is "true"#  
    abstract public void sendFile(String filename, String description, InputStream fis, int fsize);
    // #sijapp cond.end#
///////////////////////////////////////////////////////////////////////////
    protected static final InputTextBox messageTextbox = new InputTextBox("message", true);
    static {
        messageTextbox.setOkCommandCaption("send");
    }
    private static Contact writeMessageToUIN = null;
    /* Shows new message form */
    public void writeMessage(String initText) {
        /* If user want reply with quotation */ 
        if (null != initText) {
            messageTextbox.setString(initText);

        /* Keep old text if press "cancel" while last edit */ 
        } else if (this != writeMessageToUIN) {
            messageTextbox.setString(null);
        }
        writeMessageToUIN = this;

        /* Display textbox for entering messages */
        messageTextbox.setCaption(ResourceBundle.getString("message") + " " + getName());
        messageTextbox.setCommandListener(this);
        messageTextbox.show();
    }

    /* Activates the contact item menu */
    public void activate() {
        ContactList.getInstance().setCurrentContact(this);
        
        ChatTextList chat = getChat();
        if (hasChat()) {
            chat.activate();
            return;
        }
        deleteChat();
        writeMessage(null);
    }
///////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    // #sijapp cond.end #
///////////////////////////////////////////////////////////////////////////
    protected boolean isCurrent() {
        return this == ContactList.getInstance().getCurrentContact();
    }
    /* Shows popup window with text of received message */
    public final void showPopupWindow(String text) {
        if (Jimm.isLocked()) return;
        Object win = Jimm.getCurrentDisplay();
        if (!(win instanceof CanvasEx)
                && !Options.getBoolean(Options.OPTION_POPUP_OVER_SYSTEM)) {
            return;
        }
        boolean haveToShow = false;
        boolean chatVisible = (getChat() == win);
        boolean uinEquals = isCurrent();
        
        switch (Options.getInt(Options.OPTION_POPUP_WIN2)) {
            case 0: return;
            case 1:
                haveToShow = chatVisible ? false : uinEquals;
                break;
            case 2:
                haveToShow = chatVisible ? !uinEquals : true;
                break;
        }
        
        if (!haveToShow) return;
        
        // #sijapp cond.if target is "MIDP2"#
        boolean save = Jimm.isPhone(Jimm.PHONE_SE) && messageTextbox.isShown();
        if (save) {
            messageTextbox.saveCurrentPage();
        }
        // #sijapp cond.end#
        PopupWindow.showShadowPopup(getName(), text);
        
        // #sijapp cond.if target is "MIDP2"#
        if (save) {
            messageTextbox.setCurrentScreen();
        } 
        // #sijapp cond.end#
    }
///////////////////////////////////////////////////////////////////////////
    private byte booleanValues;
    public static final byte CONTACT_NO_AUTH       = 1 << 1; /* Boolean */
    public static final byte CONTACT_IS_TEMP       = 1 << 3; /* Boolean */
    public static final byte B_AUTOANSWER          = 1 << 2; /* Boolean */
    public static final byte SL_VISIBLE            = 1 << 4; /* Boolean */
    public static final byte SL_INVISIBLE          = 1 << 5; /* Boolean */
    public static final byte SL_IGNORE             = 1 << 6; /* Boolean */
    public final void setBooleanValue(byte key, boolean value) {
        setBooleanValues((byte)((getBooleanValues() & (~key)) | (value ? key : 0x00)));
    }
    public final boolean is(byte key) {
        return (booleanValues & key) != 0;
    }
    public final boolean isTemp() {
        return (booleanValues & CONTACT_IS_TEMP) != 0;
    }
    public final boolean isAuth() {
        return (booleanValues & CONTACT_NO_AUTH) == 0;
    }
    public final void setBooleanValues(byte vals) {
        booleanValues = vals;
    }
    public final byte getBooleanValues() {
        return booleanValues;
    }

    // #sijapp cond.if modules_SERVERLISTS is "true" #
    protected boolean inVisibleList() {
        return (booleanValues & SL_VISIBLE) != 0;
    }
    protected boolean inInvisibleList() {
        return (booleanValues & SL_INVISIBLE) != 0;
    }
    protected boolean inIgnoreList() {
        return (booleanValues & SL_IGNORE) != 0;
    }
    private void showListOperation() {
        Select sl = new Select();
        String visibleList = inVisibleList()
                ? "rem_visible_list" : "add_visible_list";
        String invisibleList = inInvisibleList()
                ? "rem_invisible_list": "add_invisible_list";
        String ignoreList = inIgnoreList()
                ? "rem_ignore_list": "add_ignore_list";
        
        sl.add(visibleList,   USER_MENU_PS_VISIBLE);
        sl.add(invisibleList, USER_MENU_PS_INVISIBLE);
        sl.add(ignoreList,    USER_MENU_PS_IGNORE);
        sl.setActionListener(this);
        sl.show();
    }
    // #sijapp cond.end #

    public String getMyName() {
        return protocol.getNick();
    }

///////////////////////////////////////////////////////////////////////////

    /* Returns total count of all unread messages (messages, sys notices, urls, auths) */
    public final int getUnreadMessageCount() {
        if (hasChat()) {
            return getChat().getUnreadMessageCount();
        }
        return 0;
    }
    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return true;
    }
    /* Adds a message to the message display */
    public void addMessage(Message message) {
        if (message instanceof SystemNotice) {
			SystemNotice notice = (SystemNotice) message;
			if (SystemNotice.SYS_NOTICE_GRANDED == notice.getSysnoteType()) {
                setBooleanValue(CONTACT_NO_AUTH, false);

			} else if (SystemNotice.SYS_NOTICE_AUTHREPLY == notice.getSysnoteType()) {
				if (notice.isAUTH_granted()) {
					setBooleanValue(CONTACT_NO_AUTH, false);
                }
			}
        }
        getChat().addMessage(message);
        ChatHistory.registerChat(this);
        ChatHistory.updateChatList();
    }

    protected final void resetAuthRequests() {
        getChat().resetAuthRequests();
    }

    /* Returns image index for contact */
    // #sijapp cond.if target isnot "DEFAULT"#
    protected boolean typing = false;
    public void beginTyping(boolean type) {
        typing = type;
        if (hasChat()) {
            getChat().beginTyping(type);
        }
    }
    // #sijapp cond.end#
///////////////////////////////////////////////////////////////////////////
    public final int getFontStyle() {
        return (hasChat() && isOnline()) ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
    }
    /* Returns color for contact name */
    public final byte getTextTheme() {
        if (getUnreadMessageCount() > 0) {
            return CanvasEx.THEME_NEW_MESSAGE;
        }
        if (isTemp()) {
            return CanvasEx.THEME_CONTACT_TEMP;
        }
        if (hasChat()) {
            return CanvasEx.THEME_CONTACT_WITH_CHAT;
        }
        if (isOnline()) {
            return CanvasEx.THEME_CONTACT_ONLINE;
        }
        return CanvasEx.THEME_CONTACT_OFFLINE;
    }
    // node weight declaration
    // -1       - group
    // 10       - contact with message
    // 20 - 49  - normal-contact (status)
    // 50       - offline-contact
    // 60       - temp-contact
    public final int getNodeWeight() {
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && getUnreadMessageCount() > 0) {
            return 10;
        }
        int sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
        if (ContactList.SORT_BY_NAME == sortType) {
            return 20;
        }
        if (isOnline()) {
            switch (sortType) {
                case ContactList.SORT_BY_STATUS:
                    // 29 = 49 - 20 last normal status
                    return 20 + (null == status ? 29 : status.getWidth());
                case ContactList.SORT_BY_ONLINE:
                    return 20;
            }
        }
        
        if (isTemp()) {
            return 60;
        }
        return 50;
    }
///////////////////////////////////////////////////////////////////////////
    private volatile ChatTextList chat = null;
    public final void deleteChat() {
        ChatHistory.unregisterChat(this);
        if (null != chat) {
            chat.clear();
        }
        chat = null;
    }
    public final ChatTextList getChat() {
        if (null == chat) {
            chat = new ChatTextList(this);
            if (!getProtocol().inContactList(this)) {
                setBooleanValue(CONTACT_IS_TEMP, true);
                getProtocol().addTempContact(this);
            }
        }
        return chat;
    }
    
    public final boolean hasChat() {
        if ((null != chat) && !chat.empty()) {
            ChatHistory.registerChat(this);
            return true;
        }
        return false;
    }
///////////////////////////////////////////////////////////////////////////
    protected Protocol protocol;
    public final Protocol getProtocol() {
        return protocol;
    }
///////////////////////////////////////////////////////////////////////////
    public static final int USER_MENU_MESSAGE          = 1001;
    public static final int USER_MENU_PASTE            = 1002;
    public static final int USER_MENU_QUOTE            = 1003;
    public static final int USER_MENU_REQU_AUTH        = 1004;
    public static final int USER_MENU_FILE_TRANS       = 1005;
    public static final int USER_MENU_CAM_TRANS        = 1006;
    public static final int USER_MENU_USER_REMOVE      = 1007;
    public static final int USER_MENU_RENAME           = 1009;
    public static final int USER_MENU_LOCAL_INFO       = 1011;
    public static final int USER_MENU_USER_INFO        = 1012;
    public static final int USER_MENU_MOVE             = 1015;
    public static final int USER_MENU_STATUSES         = 1016;
    public static final int USER_MENU_LIST_OPERATION   = 1017;
    public static final int USER_MENU_COPY_TEXT        = 1024;
    public static final int USER_MENU_HISTORY          = 1025;
    public static final int USER_MENU_ADD_TO_HISTORY   = 1026;
    public static final int USER_MENU_DEL_CHAT         = 1027;
    public static final int USER_MENU_DEL_CURRENT_CHAT = 1028;
    public static final int USER_MENU_DEL_ALL_CHATS_EXCEPT_CUR = 1029;
    public static final int USER_MENU_DEL_ALL_CHATS    = 1030;
    public static final int USER_MENU_SHOW_CL          = 1031;
    public static final int USER_MENU_SHOW             = 1032;
    public static final int USER_MENU_GOTO_URL         = 1033;
    public static final int USER_MENU_ADD_USER         = 1018;
    public static final int USER_MENU_GRANT_FUTURE_AUTH = 1019;

    public static final int USER_MENU_GRANT_AUTH       = 1021;
    public static final int USER_MENU_DENY_AUTH        = 1022;


    protected static final int USER_MENU_PS_VISIBLE       = 1034;
    protected static final int USER_MENU_PS_INVISIBLE     = 1035;
    protected static final int USER_MENU_PS_IGNORE        = 1036;

    public static final int USER_MENU_MULTIUSERS_LIST = 1037;

    private static Select groupList = null;
    private static final InputTextBox renameTextbox = new InputTextBox("rename");
    static {
        renameTextbox.setOkCommandCaption("ok");
    }
    public void doAction(int cmd) {
        switch (cmd) {
            case USER_MENU_MESSAGE: /* Send plain message */
                writeMessage(null);
                break;
                
            case USER_MENU_QUOTE: /* Send plain message with quotation */
            case USER_MENU_PASTE: /* Send plain message without quotation */
                writeMessage(JimmUI.getClipBoardText(USER_MENU_QUOTE == cmd));
                break;
                
            case USER_MENU_COPY_TEXT:
                getChat().copyText();
                activate();
                break;

            case USER_MENU_ADD_USER:
                Search search = new Search(getProtocol(), Search.TYPE_NOFORM);
                search.setSearchParam(Search.UIN, uin);
                search.searchUsers();
                break;

            // #sijapp cond.if (modules_CLIENTS is "true") | (modules_FILES is "true") #
            case USER_MENU_LOCAL_INFO: /* Show Timeing info and DC info */
                showClientInfo();
                break;
            // #sijapp cond.end #
                
            case USER_MENU_STATUSES: /* Show user statuses */
                showStatus();
                break;

            case USER_MENU_USER_REMOVE:
                JimmUI.messageBox(
                        ResourceBundle.getString("remove") + "?",
                        ResourceBundle.getString("remove") + " " + getName() + "?",
                        this, MSGBS_DELETECONTACT);
                break;
                
            case USER_MENU_RENAME:
                /* Rename the contact local and on the server
                   Reset and display textbox for entering name */
                renameTextbox.setString(getName());
                renameTextbox.setCommandListener(this);
                renameTextbox.show();
                break;
                                
            // #sijapp cond.if modules_HISTORY is "true" #
            case USER_MENU_HISTORY: /* Stored history */
                showHistory();
                break;

            case USER_MENU_ADD_TO_HISTORY:
                getChat().addTextToHistory();
                activate();
                break;
            // #sijapp cond.end #
                
            case USER_MENU_DEL_CHAT:
                Select select = new Select();
                select.add("currect_contact",         null, USER_MENU_DEL_CURRENT_CHAT);
                select.add("all_contact_except_this", null, USER_MENU_DEL_ALL_CHATS_EXCEPT_CUR);
                select.add("all_contacts",            null, USER_MENU_DEL_ALL_CHATS);
                select.setActionListener(this);
                select.show();
                break;
                
            case USER_MENU_DEL_CURRENT_CHAT:
                ChatHistory.chatHistoryDelete(this, ChatHistory.DEL_TYPE_CURRENT);
                ContactList.activate();
                break;

            case USER_MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                ChatHistory.chatHistoryDelete(this, ChatHistory.DEL_TYPE_ALL_EXCEPT_CUR);
                ContactList.activate();
                break;
                
            case USER_MENU_DEL_ALL_CHATS:
                ChatHistory.chatHistoryDelete(this, ChatHistory.DEL_TYPE_ALL);
                ContactList.activate();
                break;

            case USER_MENU_SHOW_CL:
                protocol.getContactList().contactChanged(this, true);
                ContactList.activate();
                break;

            case USER_MENU_SHOW:
                Select menu = getContextMenu();
                if (null != menu) {
                    menu.show();
                }
                break;

            case USER_MENU_MOVE:
                /* Show list of groups to select which group to add to */
                Vector groups = getProtocol().getGroupItems();
                Group myGroup = getGroup();
                groupList = new Select();
                for (int i = 0; i < groups.size(); i++) {
                    Group g = (Group)groups.elementAt(i);
                    if ((myGroup != g) && g.hasMode(Group.MODE_NEW_CONTACTS)) {
                        groupList.addRaw(g.getName(), null, g.getId());
                    }
                }
                groupList.setActionListener(this);
                groupList.show();
                break;
                
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            case USER_MENU_GOTO_URL:
                textList.gotoURL(getChat().getCurrentMessage());
                break;
            // #sijapp cond.end#
            
            // #sijapp cond.if modules_FILES is "true"#
            case USER_MENU_FILE_TRANS:
                // Send a filetransfer with a file given by path
                FileTransfer.createFileTransfer(this).startFT(FileTransfer.FT_TYPE_FILE_BY_NAME);
                break;
                
                // #sijapp cond.if target isnot "MOTOROLA" #
            case USER_MENU_CAM_TRANS:
                // Send a filetransfer with a camera image
                FileTransfer.createFileTransfer(this).startFT(FileTransfer.FT_TYPE_CAMERA_SNAPSHOT);
                break;
            // #sijapp cond.end#
            // #sijapp cond.end#

            case USER_MENU_USER_INFO:
                showUserInfo();
                break;

            case USER_MENU_MULTIUSERS_LIST:
                break;

            // #sijapp cond.if modules_SERVERLISTS is "true" #
            case USER_MENU_LIST_OPERATION:
                showListOperation();
                break;
            // #sijapp cond.end #
                
        }
    }

    public void select(Select select, int cmd) {
        if (groupList == select) {
            groupList = null;
            getProtocol().moveContactTo(this, getProtocol().getGroupById(cmd));
            getProtocol().getContactList().activate();
            return;
        }
        doAction(cmd);
        switch (cmd) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                textList.copy(INFO_MENU_COPY_ALL == cmd);
                textList.restore();
                break;
            case INFO_MENU_BACK:
                window = WND_NONE;
                textList.back();
                textList.clear();
                break;

        }
    }
    final public static int MSGBS_DELETECONTACT = 10001;
    public void commandAction(Command c, Displayable d) {
        if (messageTextbox.getOkCommand() == c) {
            sendMessage(messageTextbox.getString(), true);
            if (hasChat()) {
                activate();
            } else {
                ContactList.activate();
            }
            messageTextbox.setString(null);
            return;
        }
        /* Rename contact -> "OK" */
        if (renameTextbox.getOkCommand() == c) {
            getProtocol().renameContact(this, renameTextbox.getString());
            renameTextbox.setString(null);
            ContactList.activate();
            return;
        }
        /* user select Ok in delete contact message box */
        if (JimmUI.isYesCommand(c, MSGBS_DELETECONTACT)) {
            // #sijapp cond.if modules_HISTORY is "true" #
            HistoryStorage.getHistory(this).clearHistory();
            // #sijapp cond.end#
            getProtocol().removeContact(this);
            ContactList.activate();
        }
    }
        

///////////////////////////////////////////////////////////////////////////
    
    public final void setOptimalName(UserInfo info) {
        if (getName().equals(getUin())) {
            String nick = info.getOptimalName();
            if (nick.length() != 0) {
                getProtocol().renameContact(this, nick);
            }
        }
    }

///////////////////////////////////////////////////////////////////////////
    public static final int INFO_MENU_COPY             = 1040;
    public static final int INFO_MENU_COPY_ALL         = 1041;
    public static final int INFO_MENU_BACK             = 1042;
    
    protected static final int WND_NONE      = 0;
    protected static final int WND_STATUS    = 1;
    protected static final int WND_DC_INFO   = 2;
    protected static int window = WND_NONE;
    protected static final TextListEx textList = new TextListEx(null);
    
///////////////////////////////////////////////////////////////////////////
    public void dismiss() {
        deleteChat();
        protocol = null;
    }
}
