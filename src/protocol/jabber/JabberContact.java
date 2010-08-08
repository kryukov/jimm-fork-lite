/*
 * JabberContact.java
 *
 * Created on 13 Июль 2008 г., 10:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.Vector;
import jimm.*;
import jimm.search.*;
import jimm.comm.*;
import jimm.comm.message.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberContact extends Contact {
    /** Creates a new instance of JabberContact */
    public JabberContact(Jabber jabber, String jid, String name, JabberGroup group) {
        protocol = jabber;
        this.uin = jid;
        setGroup(group);
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }
    
    protected String currentResource;
    // #sijapp cond.if modules_CLIENTS is "true" #
    protected JabberClient client = JabberClient.noneClient;
    // #sijapp cond.end #

    public boolean isMeVisible() {
        return true;
    }

    public void showUserInfo() {
        UserInfo data = null;
        if (getProtocol().isConnected()) {
            data = ((Jabber)getProtocol()).getConnection().getUserInfo(this);
            data.setProfileView(new TextListEx(getName()));
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(getProtocol());
            data.uin = getUin();
            data.nick = getName();
            data.setProfileView(new TextListEx(getName()));
            data.updateProfileView();
        }
        data.getProfileView().show();
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void showClientInfo() {
        window = WND_DC_INFO;
        textList.setCaption(getName());
        Select menu = new Select();
        
        menu.add("copy_text",     INFO_MENU_COPY);
        menu.add("copy_all_text", INFO_MENU_COPY_ALL);
        menu.add("back",          INFO_MENU_BACK);
        menu.setActionListener(this);
        textList.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);

        textList.lock();
        textList.clear();
        textList.setHeader("main_info");
        textList.add(protocol.getUinName(), getUin());
        textList.setHeader("dc_info");
        textList.add("icq_client", client.getIcon(), client.getName());

        textList.show();
    }
    // #sijapp cond.end #

    public void showStatus() {
        window = WND_STATUS;
        textList.setCaption(getName());
        Select menu = new Select();
        
        menu.add("copy_text",     INFO_MENU_COPY);
        menu.add("copy_all_text", INFO_MENU_COPY_ALL);
        menu.add("back",          INFO_MENU_BACK);
        menu.setActionListener(this);
        textList.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);

        String statusMessage = ((JabberStatus)status).getText();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        String xstatusMessage = "";
        if (JabberXStatus.noneXStatus != xstatus) {
            xstatusMessage = xstatus.getText();
            String s = StringConvertor.notNull(statusMessage);
            if (!StringConvertor.isEmpty(xstatusMessage)
                    && s.startsWith(xstatusMessage)) {
                xstatusMessage = statusMessage;
                statusMessage = null;
            }
        }
        // #sijapp cond.end #

        textList.lock();
        textList.clear();
        textList.setHeader("main_info");
        textList.add(protocol.getUinName(), getUin());
        textList.setHeader("common_status");
        textList.add(null, null, status.getName());
        textList.add(null, null, statusMessage);
        
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (JabberXStatus.noneXStatus != xstatus) {
    	    textList.setHeader("extended_status");
    	    textList.add(null, null, xstatus.getName());
    	    textList.add(null, null, xstatusMessage);
        }
        // #sijapp cond.end #
        textList.show();
    }

    public boolean isOnline() {
        return JabberStatus.offlineStatus != status;
    }
    public boolean isConference() {
        return false;
    }

    public void sendFile(String filename, String description, InputStream fis, int fsize) {
    }
    
    public String getDefaultGroupName() {
        return ResourceBundle.getString(JabberGroup.GENERAL_GROUP);
    }
    
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    private static final int USER_MENU_CONNECTIONS = 1;
    public static final int USER_MENU_REMOVE_ME    = 2;

    public Select getContextMenu() {
        contactMenu.clean();
        contactMenu.add("send_message", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_MESSAGE);
        if (!JimmUI.clipBoardIsEmpty()) {
            contactMenu.add("paste", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_PASTE);
            contactMenu.add("quote", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_QUOTE);
        }
        
        if (0 < subcontacts.size()) {
            contactMenu.add("list_of_connections", null, USER_MENU_CONNECTIONS);
        }
        
        contactMenu.add("info", Contact.USER_MENU_USER_INFO);
        // #sijapp cond.if modules_FILES is "true"#
        if (isOnline()) {
            if (jimm.modules.fs.FileSystem.isSupported()) {
                contactMenu.add("ft_name", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_FILE_TRANS);
            }
            // #sijapp cond.if target isnot "MOTOROLA"#
            contactMenu.add("ft_cam", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_CAM_TRANS);
            // #sijapp cond.end#
        }
        // #sijapp cond.end#
        if ((protocol.getGroupItems().size() > 1) && (!isTemp())) {
            contactMenu.add("move_to_group", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_MOVE);
        }
        
        if (protocol.isConnected()) {
            if (isTemp()) {
                contactMenu.add("add_user", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_ADD_USER);
            }
            if (!isAuth()) {
                contactMenu.add("requauth", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_REQU_AUTH);
            }
            contactMenu.add("grand_future_auth", null, USER_MENU_GRANT_FUTURE_AUTH);
        }
        
        if (protocol.isConnected()) {
            contactMenu.add("remove_me", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_REMOVE_ME);
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            contactMenu.add("remove", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_USER_REMOVE);
            contactMenu.add("rename", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_RENAME);
        }
        // #sijapp cond.if modules_HISTORY is "true" #
        contactMenu.add("history", Contact.USER_MENU_HISTORY);
        // #sijapp cond.end#
        if (isOnline()) {
            // #sijapp cond.if (modules_CLIENTS is "true") | (modules_FILES is "true") #
            contactMenu.add("dc_info", USER_MENU_LOCAL_INFO);
            // #sijapp cond.end #
            contactMenu.add("user_statuses", USER_MENU_STATUSES);
        }

        contactMenu.setActionListener(this);
        return contactMenu;
    }
    /////////////////////////////////////////////////////////////////////////
    
    private static final int priority = 5;
    public void doAction(int action) {
        super.doAction(action);
        switch (action) {
            case USER_MENU_REQU_AUTH: /* Request auth */
                ((Jabber)getProtocol()).requestAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;

            case USER_MENU_GRANT_FUTURE_AUTH:
            case USER_MENU_GRANT_AUTH:
                ((Jabber)getProtocol()).grandAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;

            case USER_MENU_DENY_AUTH:
        	((Jabber)getProtocol()).denyAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;
                
            case USER_MENU_CONNECTIONS:
                showListOfSubcontacts();
                break;

            case USER_MENU_REMOVE_ME:
                ((Jabber)getProtocol()).removeMe(getUin());
                break;

        }
    }
    /////////////////////////////////////////////////////////////////////////
    String getReciverJid() {
	if (this instanceof JabberServiceContact) {
        } else if (!StringConvertor.isEmpty(currentResource)) {
            return getUin() + "/" + currentResource;
        }
        return getUin();
    }
    public void sendMessage(String msg, boolean addToChat) {
        if (StringConvertor.isEmpty(msg)) {
            return;
        }
        if (!getProtocol().isConnected()) {
            return;
        }
        
        if (msg.startsWith("/")) {
    	    boolean cmdExecuted = execCommand(msg);
    	    if (cmdExecuted) {
    		return;
    	    }
        }

        capLocked = true;
        PlainMessage plainMsg = new PlainMessage(getProtocol(), this, Message.MESSAGE_TYPE_NORM,
                Util.createCurrentDate(false), msg);
        ((Jabber)getProtocol()).getConnection().sendMessage(plainMsg);
        if (addToChat) {
            getChat().addMyMessage(plainMsg);
        }
        capLocked = false;
    }
    
    private boolean execCommand(String msg) {
	return false;
    }
    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringConvertor.notNull((null == c) ? null : c.realJid);
    }


    protected static class SubContact {
        public String resource;
        public JabberStatus status;
        public int priority;
        public String realJid;
        // #sijapp cond.if modules_CLIENTS is "true" #
        public JabberClient client = JabberClient.noneClient;
        // #sijapp cond.end #
    }
    Vector subcontacts = new Vector();
    private void removeSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; i--) {
            SubContact c = (SubContact)subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                subcontacts.removeElementAt(i);
                return;
            }
        }
    }
    protected SubContact getExistSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; i--) {
            SubContact c = (SubContact)subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                return c;
            }
        }
        return null;
    }
    protected SubContact getSubContact(String resource) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            return c;
        }
        c = new SubContact();
        c.resource = resource;
        subcontacts.addElement(c);
        return c;
    }
    void setRealJid(String resource, String realJid) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.realJid = realJid;
        }
    }
    SubContact getCurrentSubContact() {
        if (0 == subcontacts.size()) {
            return null;
        }
        SubContact currentContact = getExistSubContact(currentResource);
        if (null != currentContact) {
            return currentContact;
        }
        try {
            currentContact = (SubContact)subcontacts.elementAt(0);
            int maxPriority = currentContact.priority;
            for (int i = 1; i < subcontacts.size(); i++) {
                SubContact contact = (SubContact)subcontacts.elementAt(i);
                if (maxPriority < contact.priority) {
                    maxPriority = contact.priority;
                    currentContact = contact;
                }
            }
        } catch (Exception e) {
            // synchronization error
        }
        return currentContact;
    }
    
    
    protected void setMainStatus(JabberStatus s) {
        if (null == status) {
            status = JabberStatus.offlineStatus;
        }
        final int oldStatusIndex = status.getStatusIndex();
        boolean prevPassive = status.isPassive();
        status = s;

        if (oldStatusIndex != status.getStatusIndex()) {
            statusChainged(prevPassive, status.isPassive());
        }
    }
    public void setStatus(String resource, int priority, String statusName, String statusText) {
        SubContact c = getSubContact(resource);
        c.status = JabberStatus.createStatus(c.status, statusName, statusText);
        c.priority = priority;
        if (JabberStatus.offlineStatus == c.status) {
            if (c.resource == currentResource) {
                currentResource = null;
            }
            removeSubContact(resource);
            if (0 == subcontacts.size()) {
                setOfflineStatus();
                return;
            }
        }
        SubContact contact = getCurrentSubContact();
        setMainStatus(contact.status);
    }
    
    // #sijapp cond.if modules_CLIENTS is "true" #
    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = JabberClient.createClient(c.client, caps);
        }
        SubContact contact = getCurrentSubContact();
        if (null == contact) {
            client = JabberClient.noneClient;
        } else {
            client = contact.client;
        }
    }
    // #sijapp cond.end #
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private JabberXStatus xstatus = JabberXStatus.noneXStatus;
    public void setXStatus(String resource, String id, String text) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (!"qip:none".equals(id)) {
            jimm.modules.DebugLog.println("xstatus " + getUin() + " " + id + " " + text);
        }
        // #sijapp cond.end #
        xstatus = JabberXStatus.createXStatus(xstatus, id, text);
    }
    // #sijapp cond.end #

    public final void setOfflineStatus() {
        subcontacts.removeAllElements();
        setMainStatus(JabberStatus.offlineStatus);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatus = JabberXStatus.noneXStatus;
        // #sijapp cond.end #
    }
    protected static final Select sublist = new Select();
    protected void showListOfSubcontacts() {
        sublist.clean();
        int selected = 0;
        for (int i = 0; i < subcontacts.size(); i++) {
            SubContact contact = (SubContact)subcontacts.elementAt(i);
            sublist.addRaw(contact.resource, null, i);
            if (contact.resource == currentResource) {
                selected = i;
            }
        }
        sublist.setSelectedItemCode(selected);
        sublist.setActionListener(this);
        sublist.show();
    }
    public void setActiveResource(String resource) {
        SubContact c = getExistSubContact(resource);
        currentResource = (null == c) ? null : c.resource;

        SubContact contact = getCurrentSubContact();
        // #sijapp cond.if modules_CLIENTS is "true" #
        if (null == contact) {
            client = JabberClient.noneClient;
        } else {
            client = contact.client;
        }
        // #sijapp cond.end #
    }

    protected void resourceSelected(String resource) {
        setActiveResource(resource);
        activate();
    }
    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return true;
    }
    public void select(Select select, int cmd) {
        if (sublist == select) {
            sublist.back();
            resourceSelected(select.getItemText(cmd));
            return;
        }
        super.select(select, cmd);
    }
    
    public String getUniqueUin() {
        String jid = getUin();
        if (JabberXml.isContactOverTransport(jid)) {
            String nick = JabberXml.getNick(jid);
            return nick.replace('%', '@');
        }
        return jid;
    }
}
// #sijapp cond.end #