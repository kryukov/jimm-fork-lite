/*
 * Jabber.java
 *
 * Created on 12 Июль 2008 г., 19:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;
import jimm.*;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.search.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public class Jabber extends Protocol {
    
    JabberXml connection;
    JabberXml getConnection() {
        return connection;
    }
    /** Creates a new instance of Jabber */
    public Jabber() {
        status = new JabberStatus();
    }

    public boolean isEmpty() {
        return StringConvertor.isEmpty(getUin()) || (getUin().indexOf('@') <= 0);
    }
    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    protected void startConnection() {
        connection = new JabberXml(this);
        connection.start();
    }

    public void disconnect() {
        JabberXml c = connection;
        if (null == c) {
            return;
        }
        connection = null;
        setStatusesOffline();
        c.disconnect();
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = groups.size() - 1; i >= 0; i--) {
                Group group = (Group)groups.elementAt(i);
                if (group.getId() == id) {
                    id = -1;
                    break;
                }
            }
            if (0 <= id) {
                return id;
            }
        }
    }
    public Group createGroup(String name) {
        Group group = new JabberGroup(this, name, getNextGroupId());
        group.setMode(group.MODE_FULL_ACCESS);
        return group;
    }
    public Group getOrCreateGroup(String groupName) {
        if (StringConvertor.isEmpty(groupName)) {
            return null;
        }
        Group group = getGroup(groupName);
        if (null == group) {
            group = createGroup(groupName);
            addGroup(group);
        }
        return group;
    }

    public Contact createContact(String uin, String name) {
        name = (null == name) ? uin : name;
        uin = JabberXml.realJidToJimmJid(uin);
        
        boolean isGate = (uin.indexOf('@') < 0);
        boolean isConference = JabberXml.isConference(uin);
        if (isGate || isConference) {
            return new JabberServiceContact(this, uin, name, null);
        }
        
        return new JabberContact(this, uin, name, null);
    }
    
    protected void s_searchUsers(Search cont) {
        // FIXME
        UserInfo info = new UserInfo(this);
        info.uin = cont.getSearchParam(Search.UIN);
        if (null != info.uin) {
            cont.addResult(info);
        }
        cont.finished();
    }
    public final static int PRIORITY = 5;
    protected void s_updateOnlineStatus() {
        // FIXME
        JabberStatus s = (JabberStatus)status;
        connection.setStatus(s.getNativeStatus(), "", PRIORITY);
    }

    protected void s_addedContact(Contact contact) {
        connection.updateContact((JabberContact)contact);
    }

    protected void s_addGroup(Group group) {
    }

    protected void s_removeGroup(Group group) {
    }

    protected void s_removedContact(Contact contact) {
        connection.removeContact(contact.getUin());
        if ((-1 == getUin().indexOf('@'))
    		&& !JabberXml.getDomain(getUin()).equals(contact.getUin())) {
           getConnection().unregister(contact.getUin());
           getConnection().removeGateContacts(contact.getUin());
        }
    }

    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateContacts(contacts);
    }

    protected void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        connection.updateContact((JabberContact)contact);
    }

    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((JabberContact)contact);
    }
    public void grandAuth(String uin) {
        connection.sendSubscribed(uin);
    }
    public void denyAuth(String uin) {
        connection.sendUnsubscribed(uin);
    }
    public void autoDenyAuth(String uin) {
        denyAuth(uin);
    }
    public void requestAuth(String uin) {
        connection.requestSubscribe(uin);
    }
    
    private String getDefaultServer() {
        String domain = JabberXml.getDomain(getUin());
        if ("ya.ru".equals(domain)) return "xmpp.yandex.ru";
        if ("rambler.ru".equals(domain)) return "jc.rambler.ru";
        if ("gmail.com".equals(domain)) return "talk.google.com";
        if ("qip.ru".equals(domain)) return "webim.qip.ru";
        if ("livejournal.com".equals(domain)) return "xmpp.services.livejournal.com";
        
        return domain;
    }
    
    String getServer() {
        String server = getDefaultServer();
        return (-1 == server.indexOf(':')) ? server + ":5222" : server;
    }
    public String  getUin() {
        return JabberXml.getShortJid(super.getUin());
    }
    public String getResource() {
        return JabberXml.getResource(super.getUin(),
                jimm.Jimm.getAppProperty("Jimm-Jabber-Resource", "Jimm"));
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        String uin = dis.readUTF();
        String name = dis.readUTF();
        int groupId = dis.readInt();
        byte booleanValues = dis.readByte();
        JabberContact contact = (JabberContact)createContact(uin, name);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        return contact;
    }

    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeUTF(contact.getUin());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
    }

    protected void s_setPrivateStatus() {
    }

    void removeMe(String uin) {
        connection.sendUnsubscribed(uin);
    }

    private ServiceDiscovery disco = new ServiceDiscovery(this);
    public ServiceDiscovery getServiceDiscovery() {
        return disco;
    }
    public String getUinName() {
        return "JID";
    }
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private JabberXStatus xStatus = new JabberXStatus();
    public JabberXStatus getXStatus() {
        return xStatus;
    }
    public void setXStatus(String id, String text) {
        xStatus = JabberXStatus.createXStatus(xStatus, id, text);
        if (null != connection) {
            connection.setXStatus(xStatus);
        }
    }
    // #sijapp cond.end #
}
// #sijapp cond.end #
