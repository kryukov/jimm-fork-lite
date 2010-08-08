/*
 * ConferenceParticipants.java
 *
 * Created on 12 Апрель 2009 г., 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.TextList;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import jimm.comm.*;
import protocol.Contact;

/**
 *
 * @author Vladimir Krukov
 */
public class ConferenceParticipants extends TextListEx implements SelectListener {
//    private TextListEx list;
    private JabberServiceContact conference;
    private Vector contacts = new Vector();
    
    /** Creates a new instance of ConferenceParticipants */
    private static final int COMMAND_REPLY = 0;
    private static final int COMMAND_PRIVATE = 1;
    private static final int COMMAND_INFO = 2;
    private static final int COMMAND_COPY = 3;
    private static final int COMMAND_KICK = 4;
    private static final int COMMAND_BAN = 5;
    private static final int COMMAND_DEVOICE = 6;
    private static final int COMMAND_VOICE = 7;
    private static final int COMMAND_BACK = 8;
    
    private int myRole;
    public ConferenceParticipants(JabberServiceContact conf) {
        super(conf.getName());
        conference = conf;
        myRole = getPriority(conference.getMyName());
    }
    
    private final String getCurrentContact() {
        int contactIndex = getCurrTextIndex();
        if (-1 == contactIndex) {
            return null;
        }
        JabberContact.SubContact c = (JabberContact.SubContact)contacts.elementAt(contactIndex);
        return c.resource;
    }
    
    
    protected final Select getMenu() {
        String nick = getCurrentContact();
        if (null == nick) {
            return null;
        }
        Select menu = new Select();
        int defaultCode = -1;
        if (conference.canWrite()) {
            menu.add("reply", COMMAND_REPLY);
            defaultCode = COMMAND_REPLY;
        }
        menu.add("private", COMMAND_PRIVATE);
        menu.add("info", COMMAND_INFO);
        menu.add("copy_text", COMMAND_COPY);
        
        if (JabberServiceContact.ROLE_MODERATOR == myRole) {
            int role = getPriority(nick);
            if (JabberServiceContact.ROLE_MODERATOR != role) {
                if (JabberServiceContact.ROLE_PARTICIPANT == role) {
                    menu.add("devoice", COMMAND_DEVOICE);
                } else {
                    menu.add("voice", COMMAND_VOICE);
                }
                menu.add("kick", COMMAND_KICK);
                menu.add("ban", COMMAND_BAN);
            }
        }
        
        menu.add("back", COMMAND_BACK);
        menu.setActionListener(this);
        setMenu(menu, COMMAND_BACK, defaultCode);
        return menu;
    }
    public void show() {
        update();
        super.show();
    }
    public void clear() {
        super.clear();
        contacts.removeAllElements();
        
    }
    private void update() {
        super.lock();
        int currentIndex = getCurrTextIndex();
        clear();
        addLayerToListOfSubcontacts("Moderators", JabberServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts("Participants", JabberServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts("Visitors", JabberServiceContact.ROLE_VISITOR);
        setCurrTextIndex(currentIndex);
        super.unlock();
    }
    
    private final int getPriority(String nick) {
        JabberContact.SubContact c = getContact(nick);
        return (null == c) ? JabberServiceContact.ROLE_VISITOR : c.priority;
    }
    
    private final JabberContact.SubContact getContact(String nick) {
        if (StringConvertor.isEmpty(nick)) {
            return null;
        }
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); i++) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }
    
    private void addLayerToListOfSubcontacts(String layer, int priority) {
        boolean hasLayer = false;
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); i++) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            hasLayer = (contact.priority == priority);
            if (hasLayer) {
                break;
            }
        }
        if (!hasLayer) {
            return;
        }
        setHeader(layer);
        final int maxLength = 40;
        for (int i = 0; i < subcontacts.size(); i++) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (contact.priority == priority) {
                contacts.addElement(contact);
                String nick = contact.resource;
                if (maxLength < nick.length()) {
                    nick = nick.substring(0, maxLength);
                }
                add(null, nick);
            }
        }
    }
    
    public void select(Select select, int cmd) {
        if (COMMAND_BACK == cmd) {
            super.back();
            return;
        }
        String nick = getCurrentContact();
        if (null == nick) {
            return;
        }
        switch (cmd) {
            case COMMAND_COPY:
                copy(false);
                restore();
                break;
                
            case COMMAND_REPLY:
                conference.writeMessage(nick + ", ");
                break;
                
            case COMMAND_PRIVATE:
                conference.resourceSelected(nick);
                break;
                
            case COMMAND_INFO:
                String jid = conference.getUin() + '/' + nick;
                Contact c = conference.getProtocol().createTempContact(jid);
                c.showUserInfo();
                break;
                
            case COMMAND_KICK:
                conference.setMucRole(nick, "n" + "o" + "ne");
                update();
                break;
                
            case COMMAND_BAN:
                conference.setMucAffiliation(nick, "o" + "utcast");
                update();
                break;
                
            case COMMAND_DEVOICE:
                conference.setMucRole(nick, "v" + "isitor");
                update();
                break;
                
            case COMMAND_VOICE:
                conference.setMucRole(nick, "partic" + "ipant");
                update();
                break;
        }
    }
}
// #sijapp cond.end #
