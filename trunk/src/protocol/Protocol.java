/*
 * Protocol.java
 *
 * Created on 13 Май 2008 г., 12:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.TextBox;
import javax.microedition.rms.*;
import jimm.Jimm;
import jimm.JimmException;
import jimm.Options;
import jimm.SplashCanvas;
import jimm.chat.ChatHistory;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.comm.message.*;
import jimm.modules.*;
import jimm.search.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
abstract public class Protocol {
    protected Vector contacts = new Vector();
    protected Vector groups = new Vector();
    private String[] account;
    private String password;

    
    protected final String getContactListRS() {
//        return "contactlist-" + getUin();
	String rms = "cl-" + getUin();
	return (32 < rms.length()) ? rms.substring(0, 32) : rms;
    }

    public boolean isEmpty() {
        return StringConvertor.isEmpty(getUin());
    }

    public String getUin() {
        return (null == account) ? "" : account[Options.ACCOUNT_UIN];
    }
    
    public final String getNick() {
        String nick = account[Options.ACCOUNT_NICK];
        return (nick.length() == 0) ? ResourceBundle.getString("me") : nick;
    }

    public final String getPassword() {
        return (null == password) ? account[Options.ACCOUNT_PASS] : password;
    }
    public final void setPassword(String pass) {
        password = pass;
    }

    public final void setAccount(String[] account) {
        this.account = account;
    }
    
    /** Creates a new instance of Protocol */
    public Protocol() {
    }
    public void updateContact(Contact contact, boolean setCurrent) {
        getContactList().contactChanged(contact, setCurrent);
    }

    
    
    public void setContactList(Vector groups, Vector contacts) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((contacts.size() > 0) && !(contacts.elementAt(0) instanceof Contact)) {
            DebugLog.panic("contacts is not list of Contact");
        }
        if ((groups.size() > 0) && !(groups.elementAt(0) instanceof Group)) {
            DebugLog.panic("groups is not list of Group");
        }
        // #sijapp cond.end #
        this.contacts = contacts;
        this.groups = groups;
        ChatHistory.restoreContactsWithChat();
        getContactList().optionsChanged();
        //getContactList().update();
    }

    /* ********************************************************************* */
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private byte privateStatus = 0;
    protected abstract void s_setPrivateStatus();
    public void setPrivateStatus(byte status) {
        privateStatus = status;
        if (isConnected()) {
            s_setPrivateStatus();
        }
    }
    public byte getPrivateStatus() {
        return privateStatus;
    }
    // #sijapp cond.end #
    /* ********************************************************************* */
    public final void safeLoad() {
        if ("".equals(getUin())) {
            return;
        }
        if (isConnected()) {
            return;
        }
        try {

            // Check whether record store exists
            String[] recordStores = RecordStore.listRecordStores();
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            if (null == recordStores) {
                recordStores = new String[0];
            }
            // #sijapp cond.end #
            String rmsName = getContactListRS();
            boolean exist = false;
            for (int i = 0; i < recordStores.length; i++) {
                if (rmsName.equals(recordStores[i])) {
                    exist = true;
                    break;
                }
            }
            if (exist) {
                load();
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("contact", e);
            // #sijapp cond.end #
            setContactList(new Vector(), new Vector());
        }
    }
    
    public final void safeSave() {
        // Try to delete the record store
        if ("".equals(getUin())) {
            return;
        }
        synchronized (this) {
            try {
                RecordStore.deleteRecordStore(getContactListRS());
            } catch (Exception e) {
                // Do nothing
            }

            RecordStore cl = null;
            try {
                // Create new record store
                cl = RecordStore.openRecordStore(getContactListRS(), true);
                save(cl);
            } catch (Exception e) {
                // Do nothing
            }
            try {
                // Close record store
                cl.closeRecordStore();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
    // Tries to load contact list from record store
    private void load() throws Exception {
        // Initialize vectors
        Vector cItems = new Vector();
        Vector gItems = new Vector();

        // Open record store
        RecordStore cl = RecordStore.openRecordStore(getContactListRS(), false);
        try {
            // Temporary variables
            byte[] buf;
            ByteArrayInputStream bais;
            DataInputStream dis;

            // Get version info from record store
            buf = cl.getRecord(1);
            bais = new ByteArrayInputStream(buf);
            dis = new DataInputStream(bais);
            if (!dis.readUTF().equals(Jimm.VERSION)) {
                throw new Exception();
            }

            // Get version ids from the record store
            loadProtocolData(cl.getRecord(2));
            
            // Read all remaining items from the record store
            for (int marker = 3; marker <= cl.getNumRecords(); marker++) {
                // Get type of the next item
                buf = cl.getRecord(marker);
                bais = new ByteArrayInputStream(buf);
                dis = new DataInputStream(bais);

                // Loop until no more items are available
                while (dis.available() > 0) {
                    byte type = dis.readByte();
                    switch (type) {
                        case 0:
                            cItems.addElement(loadContact(dis));
                            break;
                        case 1:
                            gItems.addElement(loadGroup(dis));
                            break;
                    }
                    
                }
            }
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.memoryUsage("clload");
            // #sijapp cond.end#
        } finally {
            // Close record store
            cl.closeRecordStore();
        }
        setContactList(gItems, cItems);
    }
    abstract protected Contact loadContact(DataInputStream dis) throws Exception;
    private Group loadGroup(DataInputStream dis) throws Exception {
        int groupId = dis.readInt();
        String name = dis.readUTF();
        Group group = createGroup(name);
        group.setGroupId(groupId);
        group.setExpandFlag(dis.readBoolean());
        return group;
    }
    protected void loadProtocolData(byte[] data) throws Exception {
    }
    protected byte[] saveProtocolData() throws Exception {
        return new byte[0];
    }
    
    // Save contact list to record store
    private void save(RecordStore cl) throws Exception {
        // Temporary variables
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        byte[] buf;

        // Add version info to record store
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeUTF(Jimm.VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);

        // Add version ids to the record store
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);

        // Initialize buffer
        baos.reset();

        // Iterate through all contact items
        int cItemsCount = contacts.size();
        int totalCount  = cItemsCount + groups.size();
        for (int i = 0; i < totalCount; i++) {
            if (i < cItemsCount) {
                dos.writeByte(0);
                saveContact(dos, (Contact)contacts.elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, (Group)groups.elementAt(i - cItemsCount));
            }
            
            // Start new record if it exceeds 4000 bytes
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                // Save record
                buf = baos.toByteArray();
                cl.addRecord(buf, 0, buf.length);

                // Initialize buffer
                baos.reset();
            }
        }
    }
    abstract protected void saveContact(DataOutputStream out, Contact contact) throws Exception;
    private void saveGroup(DataOutputStream out, Group group) throws Exception {
        out.writeInt(group.getId());
        out.writeUTF(group.getName());
        out.writeBoolean(group.isExpanded());
    }
    
    /* ********************************************************************* */

    protected void s_removeContact(Contact contact) {};
    protected void s_removedContact(Contact contact) {};
    public void removeContact(Contact contact) {
        // Check whether contact item is temporary
        boolean exec = false;
        if (contact.isTemp()) {
        } else if (isConnected()) {
            // Request contact item removal
            exec = true;
            s_removeContact(contact);
        } else {
            return;
        }
        cl_removeContact(contact);
        if (exec) {
            s_removedContact(contact);
        }
    }

    abstract protected void s_renameContact(Contact contact, String name);
    public void renameContact(Contact contact, String name) {
        if (StringConvertor.isEmpty(name)) {
            return;
        }
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_renameContact(contact, name);
        } else {
            return;
        }
        contact.setName(name);
        cl_renameContact(contact);
    }
    abstract protected void s_removeGroup(Group group);
    public void removeGroup(Group group) {
        s_removeGroup(group);        
        cl_removeGroup(group);
    }
    abstract protected void s_renameGroup(Group group, String name);
    public void renameGroup(Group group, String name) {
        s_renameGroup(group, name);
        group.setName(name);
        cl_renameGroup(group);
    }
    
    abstract protected void s_moveContact(Contact contact, Group to);
    public void moveContactTo(Contact contact, Group to) {
        s_moveContact(contact, to);
        cl_moveContact(contact, to);
    }
    protected void s_addContact(Contact contact) {};
    protected void s_addedContact(Contact contact) {}
    public void addContact(Contact contact) {
        s_addContact(contact);
        contact.setBooleanValue(Contact.CONTACT_IS_TEMP, false);
        cl_addContact(contact);
        safeSave();
        s_addedContact(contact);
    }

    public void addTempContact(Contact contact) {
        cl_addContact(contact);
    }
    public void addLocalContact(Contact contact) {
        if ((null == contact) || inContactList(contact)) {
            return;
        }
        contacts.addElement(contact);
        getContactList().updateContactItem(contact);
        getContactList().optionsChanged();
    }

    abstract protected void s_addGroup(Group group);
    public void addGroup(Group group) {
        s_addGroup(group);
        cl_addGroup(group);
    }
    
    abstract public boolean isConnected();
    abstract protected void startConnection();
    abstract public void disconnect();

    abstract public Group createGroup(String name);
    abstract protected Contact createContact(String uin, String name);
    public Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUIN(uin);
        if (null != contact) {
            return contact;
        }
        contact = createContact(uin, name);
        if (null != contact) {
            contact.setBooleanValue(Contact.CONTACT_IS_TEMP, true);
            contact.setOfflineStatus();
        }
        return contact;
    }
    public Contact createTempContact(String uin) {
        return createTempContact(uin, uin);
    }

    abstract protected void s_searchUsers(Search cont);
    public void searchUsers(Search cont) {
        s_searchUsers(cont);
    }

    public Vector getContactItems() {
        return contacts;
    }
    public Vector getGroupItems() {
        return groups;
    }
    // #sijapp cond.if modules_SOUND is "true" #
    public void beginTyping(String uin, boolean type) {
        Contact item = getItemByUIN(uin);
        if (null == item) {
            // #sijapp cond.if modules_ANTISPAM is "true" #
            if (Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE)) {
                return;
            }
            // #sijapp cond.end #
            item = createTempContact(uin);
            addTempContact(item);
        }

        if (null == item) {
            return;
        }
        item.beginTyping(type);
        getContactList().beginTyping(item, type);
    }
    // #sijapp cond.end #

    private void resetAutoanswer() {
        boolean away = status.isAway();
        for (int i = contacts.size() - 1; i >= 0; i--) {
            ((Contact)contacts.elementAt(i)).setBooleanValue(Contact.B_AUTOANSWER, away);
        }
    }
    protected void setStatusesOffline() {
        for (int i = contacts.size() - 1; i >= 0; i--) {
            ((Contact)contacts.elementAt(i)).setOfflineStatus();
        }
        for (int i = groups.size() - 1; i >= 0; i--) {
            ((Group)groups.elementAt(i)).updateGroupData();
        }
        getContactList().optionsChanged();
    }

    // Returns number of unread messages 
    public int getUnreadMessCount() {
        int result = 0;
        for (int i = contacts.size() - 1; i >= 0; i--) {
            Contact contact = (Contact)contacts.elementAt(i);
            result += contact.getUnreadMessageCount();
        }
        return result;
    }

    // Returns number of unread messages 
    public int getOnlineCount() {
        int result = 0;
        for (int i = contacts.size() - 1; i >= 0; i--) {
            Contact contact = (Contact)contacts.elementAt(i);
            if (contact.isOnline()) {
                result++;
            }
        }
        return result;
    }

    public Contact getItemByUIN(String uin) {
        for (int i = contacts.size() - 1; i >= 0; i--) {
            Contact contact = (Contact)contacts.elementAt(i); 
            if (contact.getUin().equals(uin)) {
                return contact;
            }
        }
        return null;
    }
    public Group getGroupById(int id) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            Group group = (Group)groups.elementAt(i);
            if (group.getId() == id) {
                return group;
            }
        }
        return null;
    }
    
    public Group getGroup(String name) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            Group group = (Group)groups.elementAt(i);
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    public final ContactList getContactList() {
        return ContactList.getInstance();
    }

    public boolean inContactList(Contact contact) {
        return contacts.contains(contact);
    }
    protected Status status;
    private String lastStatusChangeTime;
    
    public final Status getStatus() {
        return status;
    }

    protected abstract void s_updateOnlineStatus();
    public void setOnlineStatus(int statusIndex, String msg) {
       	if (null != msg) {
            Options.setString(Options.OPTION_STATUS_MESSAGE, msg);
        }
        // Save new online status
        Options.setLong(Options.OPTION_ONLINE_STATUS, statusIndex);
        Options.safeSave();

        setLastStatusChangeTime();
        status.setStatusIndex(statusIndex);
        if (isConnected()) {
            resetAutoanswer();
            s_updateOnlineStatus();
        }
    }
    
    private void cl_addContact(Contact contact) {
        if ((null == contact) || inContactList(contact)) {
            return;
        }
        contacts.addElement(contact);
        getContactList().addContactItem(contact);
        getContactList().optionsChanged();
    }
    private void cl_renameContact(Contact contact) {
        safeSave();
        updateContact(contact, true);
    }
    private void cl_moveContact(Contact contact, Group group) {
        contact.setGroup(group);
        safeSave();
        updateContact(contact, true);
        getContactList().optionsChanged();
    }
    private void cl_removeContact(Contact contact) {
        if (!inContactList(contact)) {
            return;
        }
        contacts.removeElement(contact);
        getContactList().removeContactItem(contact);
        contact.dismiss();
        safeSave();
    }
    private void cl_addGroup(Group group) {
        groups.addElement(group);
        getContactList().addGroup(group);
        safeSave();
    }
    private void cl_renameGroup(Group group) {
        safeSave();
    }
    private void cl_removeGroup(Group group) {
        groups.removeElement(group);
        getContactList().removeGroup(group);
        safeSave();
    }

    public void removeLocalContact(Contact contact) {
        cl_removeContact(contact);
    }

    public String getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }
    private void setLastStatusChangeTime() {
        lastStatusChangeTime = Util.getDateString(true); 
    }
    
    public void addMessage(Message message) {
        Contact cItem = (Contact)getItemByUIN(message.getSndrUin());
        // #sijapp cond.if modules_ANTISPAM is "true" #
        if ((null == cItem) && AntiSpam.isSpam(this, message)) {
            return;
        }
        // #sijapp cond.end #

        // Add message to contact
        if (null == cItem) {
            // Create a temporary contact entry if no contact entry could be found
            // do we have a new temp contact
            cItem = createTempContact(message.getSndrUin());
            addTempContact(cItem);
        }
        if (null == cItem) {
            return;
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        if (cItem.inIgnoreList()) {
            return;
        }
        // #sijapp cond.end #
        boolean autoanswer = Options.getBoolean(Options.OPTION_AUTOANSWER_STATE)
                && cItem.isSingleUserContact()
                && status.isAway() && cItem.isMeVisible()
                && cItem.is(Contact.B_AUTOANSWER) && !message.isOffline()
                && (message instanceof PlainMessage);

        cItem.addMessage(message);
        getContactList().addMessage(cItem, message);

        if (autoanswer) {
            cItem.sendMessage(Options.getString(Options.OPTION_AUTOANSWER), true);
        }
    }

    
    private int reconnect_attempts;
    public void connect() {
        reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
        //setStatusesOffline();
        startConnection();
        setLastStatusChangeTime();
    }
    // Puts the comm. subsystem into STATE_CONNECTED
    public void setConnected() {
        reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_CONNECT);
        // #sijapp cond.end #
    }
    
    public void processException(JimmException e) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("process exception: " + e.getMessage());
        // #sijapp cond.end #
        if (e.isReconnectable()) {
            reconnect_attempts--;
            if (0 < reconnect_attempts) {
                disconnect();
                Options.nextServer();
                startConnection();
                return;
            }
        }
        // Critical exception
        if (e.isCritical()) {
            // Reset comm. subsystem
            // #sijapp cond.if modules_FILES is "true"#
            if (!e.isPeer()) {
                disconnect();
            }
            // #sijapp cond.else#
            disconnect();
            // #sijapp cond.end#
        }
        JimmException.handleException(e);
    }

    /**
     *  Release all resources used by the protocol.
     */
    public void dismiss() {
	disconnect();
        for (int i = contacts.size() - 1; i >= 0; i--) {
            ((Contact)contacts.elementAt(i)).dismiss();
        }
        Vector gItems = getGroupItems();
        for (int i = groups.size() - 1; i >= 0; i--) {
            ((Group)groups.elementAt(i)).dismiss();
        }
        account = null;
        contacts = null;
        groups = null;
    }
    
    public String getUinName() {
	return "UIN";
    }
    
    public void autoDenyAuth(String uin) {
    }
}
