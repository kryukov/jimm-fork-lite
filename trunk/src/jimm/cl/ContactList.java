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
 File: src/jimm/ContactList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm.cl;

import jimm.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.forms.*;
import jimm.forms.ManageContactListForm;
import jimm.modules.*;
import jimm.ui.*;
import jimm.util.*;

import java.util.*;
import java.io.*;
import jimm.comm.message.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import DrawControls.*;
import protocol.*;
import protocol.jabber.*;

//////////////////////////////////////////////////////////////////////////////////
public class ContactList extends VirtualContactList implements CommandListener, SelectListener {
    public static final TextListEx aboutTextList = new TextListEx(null);
    private boolean treeBuilt = false;
    private Protocol protocol = null;
    private final Select mainMenu = new Select();

    /* Constructor */
    private ContactList() {
        super(null);
        mainMenu.setActionListener(this);
    }
    public void setProtocol(Protocol prot) {
        if (protocol != prot) {
            protocol = prot;
        }
        updateMenu();
        optionsChanged();
    }
    public Protocol getProtocol() {
        return protocol;
    }

    private static final ContactList instance = new ContactList();
    public static ContactList getInstance() {
        return instance;
    }

    /* *********************************************************** */
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME   = 2;
    
    /* *********************************************************** */
    // Returns all contact items as array
    private Vector getContactItems() {
        return protocol.getContactItems();
    }
    
    // Returns all group items as array
    private Vector getGroupItems() {
        return protocol.getGroupItems();
    }
    private boolean hasContact(Contact contact) {
        return contact.getProtocol() == protocol;
    }

    // Request display of the main menu
    static public void activate() {
        instance.buildTree();
        instance.show();
    }
    protected void restoring() {
        NativeCanvas.setCommands("menu", "select", "context_menu");
        setFontSize(Options.getBoolean(Options.OPTION_SMALL_FONT)
                ? VirtualList.SMALL_FONT : VirtualList.MEDIUM_FONT);
    }
    protected void beforShow() {
        update();
        updateMetrics();
    }

    public void update() {
        lock();
        updateTitle();
        buildTree();
        unlock();
    }
    
    // is called by options form when options changed
    public void optionsChanged() {
        treeBuilt = false;
    }


    //==================================//
    //                                  //
    //    WORKING WITH CONTACTS TREE    //
    //                                  //  
    //==================================//
    
    // Builds contacts tree
    private void buildTree() {
        if (treeBuilt) {
            return;
        }

        Vector gItems = protocol.getGroupItems();
        int gCount = gItems.size();
        for (int i = 0; i < gCount; ++i) {
            Group gItem = (Group)gItems.elementAt(i);
            gItem.updateContacts();
            gItem.updateGroupData();
        }

        TreeNode currentNode = getCurrentNode();
        TreeBranch root = getRoot();
        clear();
        root.setExpandFlag(false);
        boolean showOffline = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        for (int groupIndex = 0; groupIndex < gCount; ++groupIndex) {
            Group group = (Group)gItems.elementAt(groupIndex);
            boolean isExpanded = group.isExpanded();
            group.setExpandFlag(false);
            cleanNode(group);
            Vector contacts = group.getContacts();
            int contactCount = contacts.size();
            for (int contactIndex = 0; contactIndex < contactCount; ++contactIndex) {
                Contact cItem = (Contact)contacts.elementAt(contactIndex);
                if (cItem.isVisibleInContactList()) {
                    addNode(group, cItem);
                }
            }
            group.setExpandFlag(isExpanded);
            if (showOffline || (0 < group.getSubnodesCount())) {
                addNode(root, group);
            }
        }
        Vector cItems = getContactItems();
        int cCount = cItems.size();
        for (int contactIndex = 0; contactIndex < cCount; ++contactIndex) {
            Contact cItem = (Contact)cItems.elementAt(contactIndex);
            if ((Group.NOT_IN_GROUP == cItem.getGroupId()) && cItem.isVisibleInContactList()) {
                addNode(root, cItem);
            }
        }

        setCurrentNode(currentNode);
        root.setExpandFlag(true);
        updateMetrics();
        treeBuilt = true;
    }
    protected void updateMetrics() {
        Vector cItems = getContactItems();
        int count = Math.min(cItems.size(), 10);
        int height = getDefaultFont().getHeight();
        setItemHeight(height + 2);
    }
    
    // Must be called after any changes in contacts
    public synchronized void contactChanged(Contact item, boolean setCurrent) {
        if (!treeBuilt || !hasContact(item)) {
            return;
        }

        // Lock tree repainting
        lock();
        TreeNode currentNode = setCurrent ? item : getCurrentNode();
        final boolean hideEmptyGroups = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        if (item.isVisibleInContactList() && getContactItems().contains(item)) {
            removeNode(item);
            TreeBranch node = item.getGroup();
            if (hideEmptyGroups && (null != node) && (0 == node.getSubnodesCount())) {
                addNode(getRoot(), node);
            }
            addNode((null == node) ? getRoot() : node, item);
        } else {
            removeNode(item);
        }
        if (hideEmptyGroups) {
            Vector gItems = getGroupItems();
            for (int groupIndex = 0; groupIndex < gItems.size(); ++groupIndex) {
                Group group = (Group)gItems.elementAt(groupIndex);
                if (0 == group.getSubnodesCount()) {
                    removeNode(group);
                }
            }
        }

        // Updates the client-side contact list (called when a contact changes status)
        Group group = item.getGroup();
        if (null != group) {
            group.updateGroupData();
        }
        
        updateTitle();

        setCurrentNode(currentNode);
        // unlock tree and repaint
        unlock();

    }

    //Updates the title of the list
    public void updateTitle() {
        String text = protocol.getOnlineCount() + "/" + getContactItems().size();
        if (!Options.getBoolean(Options.OPTION_SHOW_SOFTBAR)) {
            text += "-" + Util.getDateString(true);
        }
        setCaption(text);
    }


    // Adds a contact list item
    public void addContactItem(Contact cItem) {
        if (!hasContact(cItem)) {
            return;
        }
        // Update visual list
        contactChanged(cItem, true);
    }

    public void updateContactItem(Contact cItem) {
        if (!hasContact(cItem)) {
            return;
        }
        // Update visual list
        contactChanged(cItem, false);
    }

    public void removeContactItem(Contact cItem) {
        if (!hasContact(cItem)) {
            return;
        }
        // Update visual list
        contactChanged(cItem, false);
    }

    // Adds new group
    public void addGroup(Group gItem) {
        lock();
        TreeNode currentNode = getCurrentNode();
        gItem.updateGroupData();
        addNode(getRoot(), gItem);
        setCurrentNode(currentNode);
        unlock();
    }

    // removes existing group 
    public void removeGroup(Group gItem) {
        lock();
        TreeNode currentNode = getCurrentNode();
        removeNode(gItem);
        setCurrentNode(currentNode);
        unlock();
    }

    
    // Adds the given message to the message queue of the contact item
    // identified by the given UIN
    public void addMessage(Contact contact, Message message) {
        if (!protocol.isConnected()) {
            return;
        }

        boolean isSingleUserContact = contact.isSingleUserContact();
        boolean isMultiUserNotify = false;
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isSingleUserContact && !message.isOffline()) {
            String msg = message.getText();
            String myName = contact.getMyName();
            // regexp: "^nick. "
            isSingleUserContact = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMultiUserNotify = ChatTextList.isHighlight(msg, myName);
        }
        // #sijapp cond.end #

        boolean isPaused = false;
        // #sijapp cond.if target is "MIDP2" #
		if (isSingleUserContact && Options.getBoolean(Options.OPTION_BRING_UP)) {
            Jimm.setMinimized(false);
        } else {
            isPaused = Jimm.isPaused();
        }
		// #sijapp cond.end #


        if (message.isOffline()) {
            // Offline messages don't play sound
            
        } else if (isSingleUserContact) {
            if (contact.isSingleUserContact()
                    && contact.isAuth()
                    && message.getText().startsWith(jimm.chat.ChatTextList.CMD_WAKEUP)
                    && !contact.isTemp()) {
                Notify.playSoundNotification(Notify.SOUND_TYPE_WAKE_UP);
                
            } else {
                Notify.playSoundNotification(Notify.SOUND_TYPE_MESSAGE);
            }
                
        // #sijapp cond.if protocols_JABBER is "true" #
        } else if (isMultiUserNotify) {
            Notify.playSoundNotification(Notify.SOUND_TYPE_MULTIMESSAGE);
            // #sijapp cond.end #
        }

        // Notify splash canvas
        if (Jimm.isLocked()) {
            SplashCanvas.messageAvailable();
        }

        if (hasContact(contact)) {
            // Update tree
            contactChanged(contact, isSingleUserContact ? true : false);
        }
        if (!isPaused && isSingleUserContact
                && (message instanceof PlainMessage) && !message.isOffline()) {
            contact.showPopupWindow(message.getText());
        }

    }
 
    

    // #sijapp cond.if modules_SOUND is "true" #
    public void beginTyping(Contact item, boolean type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (type && item.getProtocol().isConnected()) {
            Notify.playSoundNotification(Notify.SOUND_TYPE_TYPING);
        }
        // #sijapp cond.end #
        invalidate();
    }
    // #sijapp cond.end#
        
    protected void itemSelected() {
        TreeNode item = getCurrentNode();
        if (null == item) {
            return;
        }
        if (item instanceof Contact) {
            activateContact((Contact)item);

        } else if (item instanceof Group) {
            Group group = (Group)item;
            setExpandFlag(group, !group.isExpanded());
        }
    }
    
    public void doKeyReaction(int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_PRESSED == type) {
            TreeNode item = getCurrentNode();
            setCurrentContact((item instanceof Contact) ? (Contact)item : null);
            switch (keyCode) {
                case NativeCanvas.LEFT_SOFT:
                    getMenu().show();
                    return;
                    
                case NativeCanvas.RIGHT_SOFT:
                    Select menu = (null == item) ? null : item.getContextMenu();
                    if (null != menu) {
                        menu.show();
                    }
                    return;

                case NativeCanvas.CLEAR_KEY:
                    if (item instanceof Contact) {
                        ((Contact)item).doAction(Contact.USER_MENU_DEL_CHAT);
                    }
                    return;
            }
        }
        if (JimmUI.execHotKey(keyCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private Contact currentContact;
    public Contact getCurrentContact() {
        return currentContact;
    }
    public void setCurrentContact(Contact contact) {
        currentContact = contact;
    }
    private void activateContact(Contact contact) {
        contact.activate();
    }
    
    // Returns number of unread messages 
    public int getUnreadMessCount() {
        return protocol.getUnreadMessCount();
    }


	/* Static constants for menu actios */
	private static final int MENU_CONNECT    = 1;
	private static final int MENU_DISCONNECT = 2;
    private static final int MENU_DISCO      = 3;
	private static final int MENU_OPTIONS    = 4;
	private static final int MENU_TRAFFIC    = 5;
	private static final int MENU_KEYLOCK    = 6;
	private static final int MENU_STATUS     = 7;
	private static final int MENU_XSTATUS    = 8;
	private static final int MENU_PRIVATE_STATUS = 9;
	private static final int MENU_GROUPS     = 10;
    private static final int MENU_SEND_SMS   = 11;
	private static final int MENU_ABOUT      = 12;
	private static final int MENU_MINIMIZE   = 13;
	private static final int MENU_SOUND      = 14;
	private static final int MENU_MYSELF     = 15;
	private static final int MENU_CLIENT     = 16;
    private static final int MENU_DEBUGLOG   = 17;
    private static final int MENU_MAGIC_EYE  = 18;
    private static final int MENU_TEST       = 19;
	private static final int MENU_EXIT       = 20;

    /** ************************************************************************* */

    
    /* Builds the main menu (visual list) */
    private Select getMenu() {
        updateMenu();
        mainMenu.setSelectedItemCode(MENU_STATUS);
        return mainMenu;
    }

    protected void updateMenu() {
        int currentCommand = mainMenu.getSelectedItemCode();
        mainMenu.clean();
        if (getProtocol().isConnected()) {
            mainMenu.add("keylock_enable",  MENU_KEYLOCK);
            mainMenu.add("disconnect",  MENU_DISCONNECT);
        } else {
            mainMenu.add("connect",  MENU_CONNECT);
        }
        mainMenu.add("set_status",  MENU_STATUS);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        mainMenu.add("set_xstatus", MENU_XSTATUS);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (getProtocol().isConnected() && (getProtocol() instanceof Jabber)) {
            mainMenu.add("service_discovery", MENU_DISCO);
        }
        // #sijapp cond.end #
        if (getProtocol().isConnected()) {
            mainMenu.add("manage_contact_list", MENU_GROUPS);
            mainMenu.add("myself", MENU_MYSELF);
        }
        if (SmsForm.isSupported()) {
            mainMenu.add("send_sms", MENU_SEND_SMS);
        }
        mainMenu.add("options_lng", MENU_OPTIONS);
        
        // #sijapp cond.if target isnot "DEFAULT"#
        mainMenu.add("#sound_on", MENU_SOUND);
        // #sijapp cond.end#
        
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        mainMenu.add("debug log", MENU_DEBUGLOG);
        // #sijapp cond.end#
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        mainMenu.add("magic eye", MENU_MAGIC_EYE);
        // #sijapp cond.end#
        // #sijapp cond.if modules_TRAFFIC is "true" #
        mainMenu.add("traffic_lng", MENU_TRAFFIC);
        // #sijapp cond.end#
        mainMenu.add("about", MENU_ABOUT);
        // #sijapp cond.if target is "MIDP2" #
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            mainMenu.add("minimize", MENU_MINIMIZE);
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        mainMenu.add("test", MENU_TEST);
        // #sijapp cond.end#
        mainMenu.add("exit", MENU_EXIT);
        


        // #sijapp cond.if target isnot "DEFAULT"#
        boolean isSilent = Options.getBoolean(Options.OPTION_SILENT_MODE);
        mainMenu.setItem(MENU_SOUND, isSilent ? "#sound_on" : "#sound_off", null);
        // #sijapp cond.end#
        mainMenu.setSelectedItemCode(currentCommand);
        mainMenu.invalidate();
    }

    /* Activates the main menu */
    public static void activateMenu() {
        instance.getMenu().show();
    }

    /* Activates the main menu */
    public static void updateMainMenu() {
        instance.updateMenu();
    }

    private static final int TAG_EXIT = 1;

    private void doExit(boolean anyway) {
        if (!anyway && protocol.getUnreadMessCount() > 0) {
            JimmUI.messageBox(ResourceBundle.getString("attention"),
                    ResourceBundle.getString("have_unread_mess"),
                    this, TAG_EXIT);
        } else {
            /* Exit app */
            try {
                Jimm.getJimm().destroyApp(true);
            } catch (Exception e) { 
                /* Do nothing */ 
            }
        }
    }
    
    /* Command listener */
    public void commandAction(Command c, Displayable d) {
        if (JimmUI.isYesCommand(c, TAG_EXIT)) {
            doExit(true);

        } else if ((null != passwordTextBox) && (passwordTextBox.isOkCommand(c))) {
            protocol.setPassword(passwordTextBox.getString());
            passwordTextBox.back();
            if (!StringConvertor.isEmpty(protocol.getPassword())) { 
                protocol.connect();
            }
        }
    }
    private InputTextBox passwordTextBox;
    public void select(Select select, int cmd) {
        switch (cmd) {
            case MENU_CONNECT:
                if (protocol.isEmpty()) {
                    new OptionsForm().showCurrentAccountEditor();

                } else if (StringConvertor.isEmpty(protocol.getPassword())) { 
                    passwordTextBox = new InputTextBox("password", 32, TextField.PASSWORD, false);
                    passwordTextBox.setOkCommandCaption("ok");
                    passwordTextBox.setCommandListener(this);
                    passwordTextBox.show();

                } else {
                    protocol.connect();
                }
                break;
                
            case MENU_DISCONNECT:
                /* Disconnect */
                protocol.disconnect();
                Thread.yield();
                /* Show the main menu */
                ContactList.activate();
                break;

            case MENU_KEYLOCK:
                /* Enable keylock */
                Jimm.lockJimm();
                break;

            case MENU_STATUS:
                // #sijapp cond.if protocols_JABBER is "true" #
                if (getProtocol() instanceof Jabber) {
                    new JabberStatusForm((Jabber)getProtocol()).show();
                }
                // #sijapp cond.end #
                break;

                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_DISCO:
                ((Jabber)getProtocol()).getServiceDiscovery().show();
                break;
                // #sijapp cond.end #
                
            case MENU_OPTIONS:
                /* Options */
                OptionsForm.activate();
                break;

            case MENU_ABOUT:
                aboutTextList.initAbout();
                aboutTextList.show();
                break;

            case MENU_GROUPS:
                new ManageContactListForm(getProtocol()).show();
                break;

                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
            case MENU_MINIMIZE:
                /* Minimize Jimm (if supported) */
                Jimm.setMinimized(true);
                break;
                
            // #sijapp cond.if target isnot "DEFAULT" #
            case MENU_SOUND:
                Notify.changeSoundMode(false);
                ContactList.updateMainMenu();
                break;
            // #sijapp cond.end#
                
            case MENU_MYSELF:
                protocol.createTempContact(protocol.getUin(), protocol.getNick()).showUserInfo();
                break;
                
            case MENU_SEND_SMS:
                if (SmsForm.isSupported()) {
                    new SmsForm(getProtocol(), null).show();
                }
                break;

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            case MENU_TEST:
                for (int i = 0; i < 10; i++) {
                    long m = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024);
                    PopupWindow.showShadowPopup("memory "+ m + "kb",
                            "text" + i + "\nhj:)khgkgfnh\n\n\ngtfn\ngtfn\ngtfn\ngter44fn\n"
                            + "tssfn\ngttwfn\ngtftrn\ngtfn\ngtf54354n\ngt435fn\ng43554tfn\nmenjkhk\n"
                            + ":):D:( :) :) q*WRITE*");
                }
                break;
            // #sijapp cond.end#

            case MENU_EXIT:
                /* Exit */
                doExit(false);
                break;
        }
    }
}