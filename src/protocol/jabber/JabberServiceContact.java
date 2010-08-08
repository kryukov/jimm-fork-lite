/*
 * JabberServiceContact.java
 *
 * Created on 4 Январь 2009 г., 19:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.JimmUI;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.comm.message.Message;
import jimm.comm.message.PlainMessage;
import jimm.ui.FormEx;
import jimm.ui.Select;
import jimm.ui.SelectListener;
import jimm.chat.*;
import jimm.util.ResourceBundle;
import protocol.Contact;
import protocol.Group;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberServiceContact extends JabberContact {
    private static final int GATE_CONNECT = 0;
    private static final int GATE_DISCONNECT = 1;
    private static final int GATE_REGISTER = 3;
    private static final int GATE_UNREGISTER = 4;
    public static final int CONFERENCE_CONNECT = 5;
    public static final int CONFERENCE_DISCONNECT = 6;
    public static final int CONFERENCE_OPTIONS = 7;
    private static final int CONFERENCE_ADD = 8;
    
    private boolean isPrivate;
    private boolean isConference;
    private boolean isGate;
    
    private boolean autojoin;
    public void setAutoJoin(boolean auto) {
        autojoin = auto;
    }
    public boolean isAutoJoin() {
        return autojoin;
    }
    
    public void setMucRole(String nick, String role) {
        ((Jabber)getProtocol()).getConnection().setMucRole(getUin(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        SubContact c = getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        ((Jabber)getProtocol()).getConnection().setMucAffiliation(getUin(),
                c.realJid, affiliation);
    }
    
    private String myName;
    private String password;
    public void setPassword(String pass) {
        password = pass;
    }
    public String getPassword() {
        return password;
    }
    
    /** Creates a new instance of JabberContact */
    public JabberServiceContact(Jabber jabber, String jid, String name, JabberGroup group) {
        super(jabber, jid, name, group);
        isPrivate = -1 != getUin().indexOf('/');
        
        isConference = !isPrivate && JabberXml.isConference(getUin());
        isGate = JabberXml.isGate(getUin());
        if (isGate) {
            isPrivate = false;
        }
        if (isPrivate) {
            String resource = JabberXml.getResource(getUin(), "");
            setName(resource + "@" + JabberXml.getNick(getUin()));
        }
        if (isConference) {
            setMyName(getDefaultName());
            String groupName = getDefaultGroupName();
            Group g = protocol.getGroup(groupName);
            if (null != g) {
                setGroup(g);
            }
        }
    }
    
    /////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void setXStatus(String resource, String id, String text) {
    }
    // #sijapp cond.end #
    
    /////////////////////////////////////////////////////////////////////////
    
    private final String getDefaultName() {
        return protocol.getUin().substring(0, protocol.getUin().indexOf("@"));
    }
    public final void setMyName(String nick) {
        myName = nick;
        if (StringConvertor.isEmpty(myName)) {
            myName = getDefaultName();
        }
    }
    public String getMyName() {
        return isConference ? myName : super.getMyName();
    }
    
    public boolean isConference() {
        return isConference;
    }
    public boolean isVisibleInContactList() {
        return true;
    }
    
    void nickChainged(String oldNick, String newNick) {
        if (isConference) {
            if (getMyName().equals(oldNick)) {
                setMyName(newNick);
            }
            String jid = JabberXml.realJidToJimmJid(getUin() + "/" + oldNick);
            JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
            System.out.println(oldNick+"    "+ newNick + " " + c);
            if (null != c) {
                c.nickChainged(oldNick, newNick);
            }
            
        } else if (isPrivate) {
            uin = JabberXml.getShortJid(uin) + "/" + newNick;
            setName(newNick + "@" + JabberXml.getNick(getUin()));
            setOfflineStatus();
        }
    }
    void nickOnline(String nick) {
        
    }
    void nickOffline(String nick) {
        
    }
    
    // #sijapp cond.if modules_HISTORY is "true" #
    public void showHistory() {
    }
    // #sijapp cond.end#
    public void showUserInfo() {
        if (isConference) {
            doAction(USER_MENU_MULTIUSERS_LIST);

        } else {
            super.showUserInfo();
        }
    }
    
    public final String getDefaultGroupName() {
        if (isConference) {
            return ResourceBundle.getString(JabberGroup.CONFERENCE_GROUP);
        }
        return ResourceBundle.getString(JabberGroup.GATE_GROUP);
    }
    // #sijapp cond.if modules_CLIENTS is "true" #
    public void showClientInfo() {
        if (!isConference) {
            super.showClientInfo();
        }
    }
    // #sijapp cond.end #
    public void setSubject(String subject) {
        JabberStatus s = (JabberStatus)getStatus();
        if (isConference && (JabberStatus.offlineStatus != s)) {
            JabberStatus.createStatus(s, "", subject);
        }
    }
    
    /** Creates a new instance of JabberServiceContact */
    public Select getContextMenu() {
        if (!protocol.isConnected()) {
            return null;
        }
        contactMenu.clean();
        if (isGate) {
            if (isOnline()) {
                contactMenu.add("disconnect", null, GATE_DISCONNECT);
            } else {
                contactMenu.add("connect", null, GATE_CONNECT);
                contactMenu.add("register", null, GATE_REGISTER);
                contactMenu.add("unregister", null, GATE_UNREGISTER);
            }
        }
        if (isConference) {
            if (isOnline()) {
                contactMenu.add("disconnect", null, CONFERENCE_DISCONNECT);
                contactMenu.add("list_of_users", null, USER_MENU_MULTIUSERS_LIST);
            } else {
                contactMenu.add("connect", null, CONFERENCE_CONNECT);
                contactMenu.add("options", null, CONFERENCE_OPTIONS);
            }
        }
        if ((isOnline() && isConference && canWrite()) || isPrivate) {
            contactMenu.add("send_message", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_MESSAGE);
            if (!JimmUI.clipBoardIsEmpty()) {
                contactMenu.add("paste", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_PASTE);
                contactMenu.add("quote", ResourceBundle.FLAG_ELLIPSIS, null, Contact.USER_MENU_QUOTE);
            }
        }
        if (isPrivate || isGate) {
            contactMenu.add("info", Contact.USER_MENU_USER_INFO);
        }
        // #sijapp cond.if modules_FILES is "true"#
        if (isOnline() && !isGate) {
            if (jimm.modules.fs.FileSystem.isSupported()) {
                contactMenu.add("ft_name", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_FILE_TRANS);
            }
            // #sijapp cond.if target isnot "MOTOROLA"#
            contactMenu.add("ft_cam", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_CAM_TRANS);
            // #sijapp cond.end#
        }
        // #sijapp cond.end#
        if ((protocol.getGroupItems().size() > 1) && (!isTemp() && isGate)) {
            contactMenu.add("move_to_group", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_MOVE);
        }
        if (protocol.isConnected() && (isTemp() && isConference)) {
            contactMenu.add("add_user", null, CONFERENCE_ADD);
        }
        if (isGate) {
            contactMenu.add("remove_me", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_REMOVE_ME);
        }
        if (protocol.inContactList(this)) {
            contactMenu.add("remove", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_USER_REMOVE);
        }
        if (protocol.inContactList(this) && !isPrivate) {
            contactMenu.add("rename", ResourceBundle.FLAG_ELLIPSIS, null, USER_MENU_RENAME);
        }
        if (isOnline() && !isGate) {
            contactMenu.add("user_statuses", USER_MENU_STATUSES);
        }
        contactMenu.setActionListener(this);
        return contactMenu;
    }
    protected void setMainStatus(JabberStatus s) {
        boolean isOnline = (JabberStatus.offlineStatus != s);
        if (isConference && isOnline) {
            if (null == getExistSubContact(getMyName())) {
                setOfflineStatus();
                return;
            }
            s = (JabberStatus)getStatus();
            s = JabberStatus.createStatus(s, "", s.getText());
        }
        super.setMainStatus(s);
    }
    
    public String getNick(String resource) {
        SubContact c = getExistSubContact(resource);
        return (null == c) ? resource : c.resource;
    }
    
    protected void resourceSelected(String resource) {
        String jid = JabberXml.realJidToJimmJid(getUin() + "/" + resource);
        JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
        if (null == c) {
            c = (JabberServiceContact)protocol.createTempContact(jid);
            protocol.addTempContact(c);
        }
        c.setPrivateContactStatus();
        c.activate();
    }
    
    boolean canWrite() {
        SubContact sc = getExistSubContact(getMyName());
        return (null != sc) && (ROLE_VISITOR != sc.priority);
    }
    public void activate() {
        if (isOnline() || isPrivate || hasChat()) {
            super.activate();
            
        } else if (isConference && protocol.isConnected()) {
            doAction(CONFERENCE_CONNECT);
        }
    }
    
    public boolean isSingleUserContact() {
        return isPrivate || isGate;
    }
    public boolean hasHistory() {
        return false;
    }
    
    public static final int ROLE_MODERATOR = 2;
    public static final int ROLE_PARTICIPANT = 1;
    public static final int ROLE_VISITOR = 0;
    
    private static final int priority = 5;
    public void doAction(int action) {
        super.doAction(action);
        Jabber jabber = (Jabber)getProtocol();
        
        switch (action) {
            case GATE_CONNECT:
                jabber.getConnection().presence(this, getUin(), priority, null);
                getProtocol().getContactList().activate();
                break;
                
            case GATE_DISCONNECT:
                jabber.getConnection().presence(this, getUin(), JabberXml.PRESENCE_UNAVAILABLE, null);
                getProtocol().getContactList().activate();
                break;
                
            case GATE_REGISTER:
                jabber.getConnection().register(getUin());
                break;
                
            case GATE_UNREGISTER:
                jabber.getConnection().unregister(getUin());
                jabber.getConnection().removeGateContacts(getUin());
                getProtocol().getContactList().activate();
                break;
                
            case USER_MENU_MULTIUSERS_LIST:
                new ConferenceParticipants(this).show();
                break;
                
            case CONFERENCE_CONNECT:
                join();
                ChatTextList chat = getChat();
                ChatHistory.registerChat(this);
                chat.activate();
                break;
                
            case CONFERENCE_OPTIONS:
                showOptionsForm();
                break;
                
            case CONFERENCE_DISCONNECT:
                jabber.getConnection().presence(this, getUin() + "/" + getMyName(), JabberXml.PRESENCE_UNAVAILABLE, null);
                setOfflineStatus();
                removePrivateContacts();
                getProtocol().getContactList().activate();
                break;
                
            case CONFERENCE_ADD:
                protocol.addContact(this);
                getProtocol().getContactList().activate();
                break;
        }
    }
    private void showOptionsForm() {
        enterData = new FormEx("conference", "ok", "cancel", this);
        enterData.addTextField(NICK, "nick", getMyName(), 32, TextField.ANY);
        enterData.addTextField(PASSWORD, "password", "", 32, TextField.ANY);
        if (!isTemp()) {
            enterData.addCheckBox(AUTOJOIN, "autojoin", isAutoJoin());
        }
        enterData.show();
        if (!JabberXml.isIrcConference(getUin())) {
            Jabber jabber = (Jabber)getProtocol();
            jabber.getConnection().requestConferenceInfo(getUin());
        }
    }
    public void dismiss() {
        if (isOnline() && isConference) {
            Jabber jabber = (Jabber)getProtocol();
            jabber.getConnection().presence(this, getUin(),
                    JabberXml.PRESENCE_UNAVAILABLE, null);
        }
        super.dismiss();
    }
    
    void setConferenceInfo(String description) {
        if (null != enterData) {
            enterData.addString("description", description);
        }
    }
    
    private static FormEx enterData = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;
    private static final int OLD_GATE = 3;
    private static final int NEW_GATE = 4;
    private static final int ACC_ID = 5;
    private static final int ACC_PASSWD = 6;
    
    public void join() {
        Jabber jabber = (Jabber)getProtocol();
        setStatus(getMyName(), 0, "", "");
        jabber.getConnection().presence(this, getUin() + "/" + getMyName(), priority, password);
        if (JabberXml.isIrcConference(getUin()) && !StringConvertor.isEmpty(password)) {
            String nickserv = getUin().substring(getUin().indexOf('%') + 1) + "/NickServ";
            jabber.getConnection().sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            jabber.getConnection().sendMessage(nickserv, "IDENTIFY " + password);
        }
    }
    
    public void setStatus(String resource, int priority, String statusName, String statusText) {
        SubContact sc = isConference ? getExistSubContact(resource) : null;
        super.setStatus(resource, priority, statusName, statusText);
        if (isConference) {
            getChat().setWritable(canWrite());

            SubContact newSc = getExistSubContact(resource);
            if (isCurrent() && (newSc != sc)) {
                JabberStatus status = (null == newSc) ? JabberStatus.offlineStatus : newSc.status;
                showTopLine(resource + ": " + status.getName());
            }
        }
    }
    public void commandAction(Command c, Displayable d) {
        if (null == enterData) {
            super.commandAction(c, d);
            
        } else if (enterData.backCommand == c) {
            enterData.back();
            enterData = null;
            
        } else if (enterData.saveCommand == c) {
            if (isConference) {
                setMyName(enterData.getTextFieldValue(NICK));
                setAutoJoin(!isTemp() && enterData.getChoiceItemValue(AUTOJOIN, 0));
                setPassword(enterData.getTextFieldValue(PASSWORD));
                
                boolean needUpdate = !isTemp();
                if (needUpdate) {
                    ((Jabber)getProtocol()).getConnection().saveConferences();
                }
                if (isOnline()) {
                    join();
                }
            }
            getProtocol().getContactList().activate();
            enterData = null;
        }
    }
    private final void removePrivateContacts() {
        Vector contacts = protocol.getContactItems();
        String conferenceJid = getUin() + '/';
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            JabberContact c = (JabberContact)contacts.elementAt(i);
            if (c.getUin().startsWith(conferenceJid) && 0 == c.getUnreadMessageCount()) {
                protocol.removeContact(c);
            }
        }
    }
    public final void setPrivateContactStatus() {
        if (!isPrivate) {
            return;
        }
        String jid = getUin();
        String resource = JabberXml.getResource(jid, "");
        JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(JabberXml.getShortJid(jid));
        SubContact sc = (null == c) ? null : c.getExistSubContact(resource);
        SubContact mysc = getSubContact(resource);
        if (null == sc) {
            mysc.status = JabberStatus.offlineStatus;
        } else {
            mysc.status = sc.status;
        }
        status = mysc.status;
        // #sijapp cond.if modules_CLIENTS is "true" #
        if (null == sc) {
            mysc.client = JabberClient.noneClient;
        } else {
            mysc.client = sc.client;
        }
        client = mysc.client;
        // #sijapp cond.end #
    }
}
// #sijapp cond.end #