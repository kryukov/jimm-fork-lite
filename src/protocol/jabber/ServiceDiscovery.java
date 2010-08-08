/*
 * ServiceDiscovery.java
 *
 * Created on 9 Февраль 2009 г., 15:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.comm.StringConvertor;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import jimm.comm.*;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ServiceDiscovery implements SelectListener, CommandListener {
    
    private boolean isConferenceList = false;
    private int totalCount = 0;
    
    private TextListEx list;
    private final Jabber jabber;
    private String serverJid;
    private InputTextBox serverBox;
    private InputTextBox searchBox;
    private Vector jids = new Vector();
    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_REGISTER = 1;
    private static final int COMMAND_SEARCH = 2;
    private static final int COMMAND_SET_SERVER = 3;
    private static final int COMMAND_HOME = 4;
    private static final int COMMAND_BACK = 5;
    /** Creates a new instance of ServiceDiscovery */
    public ServiceDiscovery(Jabber protocol) {
        jabber = protocol;
        
        list = new TextListEx(ResourceBundle.getString("service_discovery"));
        list.setMenu(getMenu(), COMMAND_BACK, COMMAND_ADD);
        
        serverBox = new InputTextBox("server", 64);
        serverBox.setOkCommandCaption("ok");
        serverBox.setCommandListener(this);
        
        searchBox = new InputTextBox("search", 64);
        searchBox.setOkCommandCaption("ok");
        searchBox.setCommandListener(this);
    }
    
    private Select getMenu() {
        Select menu = new Select();
        menu.add("service_discovery_add", COMMAND_ADD);
        menu.add("register", COMMAND_REGISTER);
        menu.add("service_discovery_search", COMMAND_SEARCH);
        menu.add("service_discovery_server", COMMAND_SET_SERVER);
        menu.add("service_discovery_home", COMMAND_HOME);
        menu.add("back", COMMAND_BACK);
        menu.setActionListener(this);
        return menu;
    }
    
    private void addServer(boolean active) {
        if (0 < serverJid.length()) {
            final int serverIndex = active ? jids.size() : -1;
            list.addBigText(serverJid, TextList.THEME_TEXT, Font.STYLE_BOLD, serverIndex);
            list.addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, serverIndex);
            if (-1 != serverIndex) {
                jids.addElement(serverJid);
            }
        }
    }
    public void clear() {
        list.clear();
        jids.removeAllElements();
        addServer(false);
    }
    public void setTotalCount(int count) {
        list.clear();
        jids.removeAllElements();
        addServer(true);
        totalCount = count;
    }
    public void addItem(String name, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        int index = jids.size();
        String shortJid = isConferenceList
                ? jid.substring(0, jid.indexOf('@') + 1) : jid;
        name = StringConvertor.isEmpty(name) ? shortJid : name;
        String visibleJid = isConferenceList ? shortJid : Util.replace(jid, "@conference.", "@c.");
        if (totalCount < 400) {
            list.addBigText(visibleJid, TextList.THEME_TEXT, Font.STYLE_BOLD, index);
            if (!StringConvertor.isEmpty(name)) {
                list.addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
                list.addBigText(name, TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
            }
            
        } else {
            list.addBigText(visibleJid, TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
        }
        
        list.doCRLF(index);
        jids.addElement(shortJid);
    }
    public void show() {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("");
        }
        
        list.show();
    }
    public void update() {
        list.invalidate();
    }
    private void addUnique(String text, String jid) {
        if (-1 == jids.indexOf(jid)) {
            addItem(text, jid);
        }
    }
    
    private void addBuildInList() {
        addUnique("Flood", "flood@conference.jabber.ru");
        addUnique("Jimm aspro", "jimm-aspro@conference.jabber.ru");
        list.addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, -1);
        String domain = JabberXml.getDomain(jabber.getUin());
        
        addUnique("My server", domain);
        addUnique("Conferences on " + domain, "conference." + domain);
    }
    private void setServer(String jid) {
        serverJid = jid;
        isConferenceList = jid.startsWith("conference.");
        clear();
        if (0 == jid.length()) {
            addBuildInList();
            return;
        }
        list.addBigText(ResourceBundle.getString("wait"), CanvasEx.THEME_TEXT, Font.STYLE_BOLD, -1);
        jabber.getConnection().requestDiscoItems(serverJid);
    }
    
    private String getJid(int num) {
        String rawJid = (String)jids.elementAt(num);
        if (rawJid.endsWith("@")) {
            return rawJid + serverJid;
        }
        return rawJid;
    }
    
    public void select(Select select, int cmd) {
        int currentIndex = list.getCurrTextIndex();
        String jid = (-1 == currentIndex) ? null : getJid(currentIndex);
        switch (cmd) {
            case COMMAND_ADD:
                if (null == jid) {
                } else if (JabberXml.isConference(jid)) {
                    Contact c = jabber.createTempContact(jid);
                    jabber.addTempContact(c);
                    jabber.getContactList().contactChanged(c, true);
                    jabber.getContactList().activate();
                    
                    
                } else {
                    setServer(jid);
                }
                break;
                
            case COMMAND_REGISTER:
                if (null == jid) {
                } else if (!JabberXml.isConference(jid)) {
                    jabber.getConnection().register(jid);
                }
                break;
                
            case COMMAND_SEARCH:
                searchBox.show();
                break;
                
            case COMMAND_SET_SERVER:
                serverBox.setString(serverJid);
                serverBox.show();
                break;
                
            case COMMAND_HOME:
                setServer("");
                list.restore();
                break;
                
            case COMMAND_BACK:
                list.back();
                break;
        }
    }
    
    public void commandAction(Command command, Displayable displayable) {
        if (serverBox.getOkCommand() == command) {
            setServer(serverBox.getString());
            list.restore();
            
        } else if (searchBox.getOkCommand() == command) {
            String text = searchBox.getString();
            int currentIndex = list.getCurrTextIndex() + 1;
            for (int i = currentIndex; i < jids.size(); i++) {
                String jid = (String)jids.elementAt(i);
                if (-1 != jid.indexOf(text)) {
                    list.setCurrTextIndex(i);
                    break;
                }
            }
            list.restore();
        }
    }
}
// #sijapp cond.end #
