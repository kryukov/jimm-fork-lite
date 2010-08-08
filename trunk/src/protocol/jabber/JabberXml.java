/*
 * JabberXml.java
 *
 * Created on 12 Июль 2008 г., 19:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.Image;
import jimm.search.*;
import jimm.*;
import jimm.comm.*;
import jimm.comm.message.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberXml extends JabberConnectionBase {
    
    private String fullJid_;
    private final String domain_;
    private String resource;
    private final boolean xep0048 = false;

    public static String realJidToJimmJid(String realJid) {
        if (isIrcConference(realJid) && -1 != realJid.indexOf('!')) {
            String res = realJid.substring(0, realJid.indexOf('!'));
            String jid = realJid.substring(realJid.indexOf('!') + 1);
            return jid + '/' + res;
        }
        String resource = getResource(realJid, null);
        String jid = StringConvertor.toLowerCase(getShortJid(realJid));
        return (null == resource) ? jid : (jid + '/' + resource);
    }

    public static String jimmJidToRealJid(String jimmJid) {
        if (isIrcConference(jimmJid)
                && -1 == jimmJid.indexOf('%')
                && -1 != jimmJid.indexOf('/')) {
            return getResource(jimmJid, "") + '!' + getShortJid(jimmJid);
        }
        return jimmJid;
    }

    public static String getDomain(String jid) {
        jid = getShortJid(jid);
        return jid.substring(jid.indexOf("@") + 1);
    }
    public static String getResource(String fullJid, String defResource) {
        int resourceStart = fullJid.indexOf('/') + 1;
        if (0 < resourceStart) {
            return fullJid.substring(resourceStart);
        }
        return defResource;
    }
    public static boolean isConference(String jid) {
        return (-1 != jid.indexOf("@conference."))
    		|| (-1 != jid.indexOf("@chat."))
            || (-1 != jid.indexOf("%conference."))
    		|| isIrcConference(jid);
    }
    public static boolean isGate(String jid) {
        return jid.indexOf('@') < 0;
    }
    public static boolean isIcqGate(String jid) {
        if (!isGate(jid)) {
            return false;
        }
        final String[] transports = {"icq.", "picq.", "pyicq."};
        for (int i = 0; i < transports.length; i++) {
            if (jid.startsWith(transports[i])) {
                return true;
            }
        }
        return false;
    }
    public static boolean isMrim(String jid) {
        return (-1 != jid.indexOf("@mrim."));
    }
    public static boolean isIrcConference(String jid) {
        return (-1 != jid.indexOf("@irc."));
    }
    public static boolean isContactOverTransport(String jid) {
        final String[] transports = {"@mrim.", "@icq.", "@picq.", "@pyicq."};
        for (int i = 0; i < transports.length; i++) {
            if (-1 != jid.indexOf(transports[i])) {
                return true;
            }
        }
        return false;
    }
    
    public static String getShortJid(String fullJid) {
        int resourceStart = fullJid.indexOf("/");
        if (-1 != resourceStart) {
            return StringConvertor.toLowerCase(fullJid.substring(0, resourceStart));
        }
        return StringConvertor.toLowerCase(fullJid);
    }
    public static String getNick(String jid) {
        return jid.substring(0, jid.indexOf('@'));
    }

    static final boolean isTrue(String val) {
        return S_TRUE.equals(val) || "1".equals(val);
    }

    private String getCaps() {
        return "<c xmlns='http://jabber.org/protocol/caps'"
                + " node='http://jimm.net.ru/caps'"
                + " ver='"
                + Util.xmlEscape(jimm.Jimm.VERSION + " (###DATE###)")
                // #sijapp cond.if modules_XSTATUSES is "true" #
                + "' ext='ep-notify"
                // #sijapp cond.end #
                + "'/>";
    }
    
    public final String CAPS = getCaps();
    
    public JabberXml(Jabber jabber) {
        super(jabber);
        resource = jabber.getResource();
        fullJid_ = jabber.getUin() + '/' + resource;
        domain_ = getDomain(fullJid_);
    }
    
    // #sijapp cond.if modules_ZLIB is "true" #
    public void setStreamCompression() {
        SplashCanvas.setProgress(20);
        socket.activateStreamCompression();
        write(getOpenStreamXml(domain_));
        
        readXmlNode(true); // "stream:stream"
        parseAuth(readXmlNode(true));
    }
    // #sijapp cond.end #
    
    public Jabber getJabber() {
        return (Jabber)protocol;
    }
    
    byte getNextByte() throws JimmException {
        try {
            return socket.readByte();
        } catch (Exception e) {
            throw new JimmException(120, 0);
        }
    }
    boolean hasMorePackets() throws JimmException {
        return socket.isConnected() && (0 < available());
    }
    
    private void write(String xml) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        System.out.println("[JABBER OUT XML]:\n  " + xml);
        // #sijapp cond.end #
        write(StringConvertor.stringToByteArray(xml));
    }
    protected void writePacket(Object packet) throws JimmException {
        if (packet instanceof String) {
            write((String)packet);
        }
    }
    private XmlNode readXmlNode(boolean notEmpty) throws JimmException {
        while (hasMorePackets() || notEmpty) {
            XmlNode x = XmlNode.parse(this);
            if (null != x) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
            	System.out.println("IN: " + x.toString());
                // #sijapp cond.end #
                return x;
            }
        }
        return null;
    }
    
    byte[] pingPacket = null;
    private long prevPingTime = 0;
    private long keepAliveInterv = 0;
    protected void ping() throws JimmException {
        if (!Options.getBoolean(Options.OPTION_KEEP_CONN_ALIVE)) {
            return;
        }
        if (null == pingPacket) {
            pingPacket = " ".getBytes();
            long pingTime = Util.strToIntDef(Options.getString(Options.OPTION_CONN_ALIVE_INVTERV), 120);
            pingTime = Math.max(pingTime, 1) * 1000;
            keepAliveInterv = pingTime;
        }
        if (null == pingPacket) {
            return;
        }
        long time = System.currentTimeMillis();
        if (time > (prevPingTime + keepAliveInterv)) {
            prevPingTime = time;
            write(pingPacket);
        }
    }
    
    // -----------------------------------------------------------------------
    private void sendRequest(String request) throws JimmException {
        write(request);
    }
    // -----------------------------------------------------------------------
    
//    private boolean isSasl_ = false;
    private boolean isGTalk_ = false;
    private boolean authorized_ = false;
    
    private void setAuthStatus(boolean authorized) throws JimmException {
        authorized_ = authorized;
        if (!authorized) {
            throw new JimmException(111, 0);
//            disconnect();
        }
    }
    protected void connect() throws JimmException {
        SplashCanvas.connectingTo(getJabber());
        
        String server = getJabber().getServer();
        connectTo((-1 != server.indexOf(":5223") ? "ssl://" : "socket://") + server);
        
        write(getOpenStreamXml(domain_));
        SplashCanvas.setProgress(10);
        /* Authenticate with the server */
        readXmlNode(true); // "stream:stream"
        //parseAuth(hasMorePackets() ? readXmlNode(true) : null);
        parseAuth(readXmlNode(true));
        while (!authorized_) {
            parse(readXmlNode(true));
        }
        SplashCanvas.setProgress(50);
        write(GET_ROSTER_XML);
    }
    protected void processPacket() throws JimmException {
        while (hasMorePackets()) {
            XmlNode x = null;
            try {
                x = readXmlNode(false);
                if (null == x) {
                    return;
                }
                //System.out.println("[JABBER IN XML]:\n" + x.childAt(i).toString());
                parse(x);
            } catch (Exception e){
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                e.printStackTrace();
                if (null != x) {
                    System.out.println("xml: " + x.toString());
                }
                // #sijapp cond.end #
            }
        }
    }
    // -----------------------------------------------------------------------
    
    /**
     * Parse inbound xml for authentication
     *
     * @param x Received xml
     * @param protocol {@link Jabber} instance
     */
    private void parseAuth(XmlNode x) throws JimmException {
        if ((null == x) || !"stream:features".equals(x.name)) {
            nonSaslLogin();
        } else {
            parse(x);
        }
    }
    private void nonSaslLogin() throws JimmException {
        String user = protocol.getUin().substring(0, protocol.getUin().indexOf("@"));
        sendRequest(
                "<iq type='set' to='" + domain_ + "'>"
                + "<query xmlns='jabber:iq:auth'>"
                + "<username>" + Util.xmlEscape(user) + "</username>"
                + "<password>" + Util.xmlEscape(protocol.getPassword()) + "</password>"
                + "<resource>"+ Util.xmlEscape(resource) + "</resource>"
                + "</query>"
                + "</iq>");
    }
    
    /**
     * Parse inbound xml and execute apropriate action
     *
     * @param x Received xml
     * @param protocol {@link Jabber} instance
     */
    private void parse(XmlNode x) throws JimmException {
        if (x.name.equals("stream:stream") &&
                (null != x.getFirstNode("stream:error"))) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            XmlNode err = x.getFirstNode("stream:error").childAt(0);
            System.out.println("[INFO-JABBER] Stream error!: " + err.name + "," + err.value);
            // #sijapp cond.end #
            setAuthStatus(false);
            
        } else if (x.name.equals("stream:features")) {
            parseStreamFeatures(x);
            
        } else if (x.name.equals("compressed")) {
            // #sijapp cond.if modules_ZLIB is "true" #
            setStreamCompression();
            // #sijapp cond.end #
            
            /* Reply to DIGEST-MD5 challenges */
        } else if (x.name.equals("challenge")) {
            parseChallenge(x);
            
        } else if (x.name.equals("failure")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println("[INFO-JABBER] Failed");
            // #sijapp cond.end #
            setAuthStatus(false);
            
        } else if (x.name.equals("success")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println("[INFO-JABBER] Auth success");
            System.out.println("auth " + authorized_);
            // #sijapp cond.end #
            sendRequest(getOpenStreamXml(domain_));
            
        } else if (x.name.equals("iq")) {
            parseIq(x);
            
        } else if (x.name.equals("presence")) {
            parsePresence(x);
            
        } else if (x.name.equals("m" + "essage")) {
            parseMessage(x);
        }
    }
    
    private String generateId() {
        return "jimm" + (System.currentTimeMillis() % 0xFFFF);
    }
    /**
     * Parse the <<lit>iq</lit>> node and launch apropriate action
     * 
     * @param iqNode {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseIq(XmlNode iqNode) throws JimmException {
        String id = iqNode.getAttribute("i" + "d");
        String from = iqNode.getAttribute(S_FROM);
        if (StringConvertor.isEmpty(id)) {
            id = generateId();
        }
        
        if ((S_RESULT).equals(iqNode.getAttribute(S_TYPE))) {
            if ("sess".equals(id)) {
                setAuthStatus(true);
                return;

            } else if ("auth_2".equals(id)) {
                setAuthStatus(true);
                return;
            
            } else if ("reg2".equals(id)) {
                jabberForm.success();
                jabberForm = null;
            }
        }
        XmlNode iqQueryNode = iqNode.childAt(0);
        if (null == iqQueryNode) return;
        
        if (iqNode.contains(S_ERROR)) {
            iqQueryNode = iqNode.getFirstNode(S_ERROR);
            String text = iqQueryNode.getFirstNodeValue(S_TEXT);
            if (null == text) {
                text = iqQueryNode.value;
            }
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + iqQueryNode.getAttribute("co" + "de") + " " +
                    "Value=" + text);
            // #sijapp cond.end #
            XmlNode query = iqNode.getFirstNode("que" + "ry");
            if (null == query) {
            } else if (query.isXmlns("jabber:iq:auth")) {
                setAuthStatus(false);
            } else if (query.isXmlns("jabber:iq:register") && (null != jabberForm)) {
                jabberForm.error(text);
            }
            return;
        }

        if (("que" + "ry").equals(iqQueryNode.name)) {
            if (iqQueryNode.isXmlns("jabber:iq:roster")) {
                String type = iqNode.getAttribute(S_TYPE);
                if ((S_RESULT).equals(type)) {
                    Vector contacts = new Vector();
                    Vector groups = new Vector();
                    while (0 < iqQueryNode.childrenCount()) {
                        XmlNode itemNode = (XmlNode)iqQueryNode.popChildNode();

                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        
                        boolean autorized = !S_NONE.equals(subscription) && !S_FROM.equals(subscription);
                        
                        String name = itemNode.getAttribute(XmlNode.S_NAME);
                        String groupName = itemNode.getFirstNodeValue(S_GROUP);
                        Contact contact = getJabber().getItemByUIN(jid);
                        if (null == contact) {
                            contact = getJabber().createContact(jid, name);
                        }
                        contact.setName(name);
                        Group group = null;
                        
                        if (StringConvertor.isEmpty(groupName) || isConference(jid)) {
                            groupName = ((JabberContact)contact).getDefaultGroupName();
                        }
                        
                        if (!StringConvertor.isEmpty(groupName)) {
                            for (int j = groups.size() - 1; j >= 0; j--) {
                                Group g = (Group)groups.elementAt(j);
                                if (g.getName().equals(groupName)) {
                                    group = g;
                                    break;
                                }
                            }
                            if (null == group) {
                                group = (Group)protocol.getGroup(groupName);
                                if (null == group) {
                                    group = protocol.createGroup(groupName);
                                }
                                groups.addElement(group);
                            }
                        }
                        contact.setGroup(group);
                        contact.setBooleanValue(Contact.CONTACT_IS_TEMP, false);
                        contact.setBooleanValue(Contact.CONTACT_NO_AUTH, !autorized);
                        contacts.addElement(contact);
                    }

                    getJabber().s_updateOnlineStatus();
                    // #sijapp cond.if modules_XSTATUSES is "true" #
                    if (getJabber().getXStatus().isPep()) {
                        setXStatus(getJabber().getXStatus());
                    }
                    // #sijapp cond.end #
                    getBookmarks();

                    if (Options.getBoolean(Options.OPTION_SAVE_TEMP_CONTACT)) {
                        Vector oldContacts = getJabber().getContactItems();
                        for (int i = oldContacts.size() - 1; i >= 0; i--) {
                            Contact o = (Contact)oldContacts.elementAt(i);
                            if (!contacts.contains(o)) {
                                if (o instanceof JabberServiceContact) {
                                    final String groupName = ((JabberContact)o).getDefaultGroupName();
                                    o.setGroup(getJabber().getOrCreateGroup(groupName));
                                    if (!o.isSingleUserContact()) {
                                        continue;
                                    }
                                } else {
                                    o.setGroup(null);
                                }
                                o.setBooleanValue(Contact.CONTACT_IS_TEMP, true);
                                o.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                                contacts.addElement(o);
                            }
                        }
                    }
                    
                    getJabber().setContactList(groups, contacts);
                    Contact selfContact = getJabber().getItemByUIN(getJabber().getUin());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                    }
                    getJabber().getContactList().activate();
                    
                } else if (S_SET.equals(type)) {
                    while (0 < iqQueryNode.childrenCount()) {
                        XmlNode itemNode = (XmlNode)iqQueryNode.popChildNode();
                        
                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        String jid = itemNode.getAttribute(XmlNode.S_JID);

                        if (isConference(jid)) {
                            
                        } else if ((S_REMOVE).equals(subscription)) {
                            getJabber().removeLocalContact(getJabber().getItemByUIN(jid));
                            
                        } else {
                            boolean autorized = !S_NONE.equals(subscription) && !S_FROM.equals(subscription);
                            
                            String name = itemNode.getAttribute(XmlNode.S_NAME);
                            String groupName = itemNode.getFirstNodeValue(S_GROUP);
                            Contact contact = getJabber().createTempContact(jid, name);
                            if (StringConvertor.isEmpty(groupName)) {
                                groupName = ((JabberContact)contact).getDefaultGroupName();
                            }
                            Group group = getJabber().getGroup(groupName);
                            if ((null == group) && !StringConvertor.isEmpty(groupName)) {
                                group = getJabber().getOrCreateGroup(groupName);
                                getJabber().addGroup(group);
                            }
                            contact.setGroup(group);
                            contact.setName(name);
                            contact.setBooleanValue(Contact.CONTACT_IS_TEMP, false);
                            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, !autorized);
                            getJabber().addLocalContact(contact);
                            //getJabber().updateContact(contact, false);
                            //getJabber().getContactList().optionsChanged();
                        }
                    }
                    getJabber().getContactList().update();
                }
                
            } else if (iqQueryNode.isXmlns("http://jabber.org/protocol/disco#info")) {
                if (S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                    String node = StringConvertor.notNull(iqQueryNode.getAttribute("n" + "ode"));
                    StringBuffer sb = new StringBuffer();
                    sb.append("<iq type='result' to='")
                            .append(Util.xmlEscape(from))
                            .append("' id='").append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#info'>");
                    sb.append("<identity category='client' type='phone' name='Jimm'/>");
                    //sb.append("<feature var='urn:xmpp:receipts'/>");
                    sb.append("<feature var='http://jabber.org/protocol/disco#info'/>");
                    sb.append("<feature var='jabber:iq:version'/>");
                    sb.append("<feature var='urn:xmpp:attention:0'/>");
                    sb.append("<feature var='urn:xmpp:time'/>");
                    sb.append("<feature var='bugs'/>");
                    sb.append("</query></iq>");
                    write(sb.toString());
                    return;
                }
                if (!S_RESULT.equals(iqNode.getAttribute(S_TYPE))) {
                    return;
                }
                XmlNode identity = iqQueryNode.getFirstNode("identity");
                String name = (null != identity) ? identity.getAttribute(XmlNode.S_NAME) : "";
                ((JabberServiceContact)getJabber().createTempContact(from)).setConferenceInfo(name);
                
            } else if (iqQueryNode.isXmlns("http://jabber.org/protocol/disco#items")) {
                if (S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<iq type='error' to='")
                            .append(Util.xmlEscape(from)).append("' id='")
                            .append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#items'/>");
                    sb.append("<error type='cancel'><feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>");
                    sb.append("</iq>");
                    putPacketIntoQueue(sb.toString());
                    return;
                }
                ServiceDiscovery disco = getJabber().getServiceDiscovery();
                if (null == disco) {
                    return;
                }
                disco.setTotalCount(iqQueryNode.childrenCount());
                int count = Math.min(30, iqQueryNode.childrenCount());
                for (int i = 0; i < count; i++) {
                    XmlNode item = iqQueryNode.childAt(i);
                    String name = item.getAttribute(XmlNode.S_NAME);
                    String jid = item.getAttribute(XmlNode.S_JID);
                    disco.addItem(name, jid);
                }
                disco.update();
                
            } else if (iqQueryNode.isXmlns("jabber:iq:register")) {
                if (null != jabberForm) {
                    if ("reg1".equals(id)) {
                        jabberForm.loadFromXml(iqQueryNode, iqQueryNode);
                    } else {
                        jabberForm.success();
                        jabberForm = null;
                    }
                }
                
            } else if (iqQueryNode.isXmlns("jabber:iq:private")) {
                XmlNode storage = iqQueryNode.getFirstNode("sto" + "rage");
                if ((null != storage) && storage.isXmlns("storage:bookmarks")) {
                    loadBookmarks(storage);
                }
                
            } else if (iqQueryNode.isXmlns("jabber:iq:version")) {
                if (!S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                    return;
                }
                putPacketIntoQueue("<iq type='result' to='"
                        + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                        + "<query xmlns='jabber:iq:version'><name>Jimm lite</name><version>"
                        + Util.xmlEscape(jimm.Jimm.VERSION + " (###DATE###)")
                        + "</version><os>"
                        + Util.xmlEscape(jimm.Jimm.microeditionPlatform)
                        + "</os></query></iq>");
            } else {
                if (!S_GET.equals(iqNode.getAttribute(S_TYPE))) {
	            return;
    	        }
        	sendRequest("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'  type='error'><"
            	        + iqQueryNode.name + " xmlns='" + Util.xmlEscape(iqQueryNode.getAttribute("xmlns")) + "'/>"
            	        + "<error type='cancel'>"
                        + "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                        + "</error></iq>");
            }
            
        } else if (S_TIME.equals(iqQueryNode.name)) {
            if (!S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                return;
            }
            int gmtOffset = Options.getInt(Options.OPTIONS_GMT_OFFSET);
            putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(id) + "'>"
                    + "<time xmlns='urn:xmpp:time'><tzo>"
                    + (0 <= gmtOffset ? "+" : "-") + Util.makeTwo(Math.abs(gmtOffset)) + ":00"
                    + "</tzo><utc>"
                    + Util.getUtcDateString(Util.createCurrentDate(true))
                    + "</utc></time></iq>");

        } else if (("p" + "ing").equals(iqQueryNode.name)) {
            sendRequest("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' type='result'/>");

        } else if (("pu" + "bsub").equals(iqQueryNode.name)) {
            loadBookmarks(iqQueryNode.getFirstNode("sto" + "rage"));

        } else if (("vCard").equals(iqQueryNode.name)) {
            if (S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                return;
            }
            if (!S_RESULT.equals(iqNode.getAttribute(S_TYPE))) {
                return;
            }
            loadVCard(iqQueryNode, from);

        } else if ("bind".equals(iqQueryNode.name)) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println("[INFO-JABBER] Send open session request");
            // #sijapp cond.end #
            fullJid_ = iqQueryNode.getFirstNode(XmlNode.S_JID).value;
            sendRequest("<iq type='set' id='sess'>"
                    + "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
                    + "</iq>");

        } else {
            if (!S_GET.equals(iqNode.getAttribute(S_TYPE))) {
                return;
            }
            sendRequest("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'  type='error'><"
                    + iqQueryNode.name + " xmlns='" + Util.xmlEscape(iqQueryNode.getAttribute("xmlns")) + "'/>"
                    + "<error type='cancel'>"
                    + "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                    + "</error></iq>");
        }
    }
    private void loadVCard(XmlNode xNode, String from) {
        UserInfo userInfo = singleUserInfo;
        if (null == userInfo) {
            return;
        }
        userInfo.auth = false;
        userInfo.uin = from;

        String name[] = new String[3];
        name[0] = xNode.getFirstNodeValue("N", "GIVEN");
        name[1] = xNode.getFirstNodeValue("N", "MIDDLE");
        name[2] = xNode.getFirstNodeValue("N", "FAMILY");
        if (StringConvertor.isEmpty(Util.implode(name, ""))) {
            userInfo.firstName = xNode.getFirstNodeValue("FN");
            userInfo.lastName = null;
        } else {
            userInfo.lastName = name[2];
            name[2] = null;
            userInfo.firstName = Util.implode(name, " ");
        }
        
        userInfo.nick = xNode.getFirstNodeValue("NICKNAME");
        userInfo.birthDay = xNode.getFirstNodeValue("BDAY");
        userInfo.email = xNode.getFirstNodeValue("EMAIL", "USERID");
        userInfo.about = xNode.getFirstNodeValue("DESC");
        userInfo.homePage = xNode.getFirstNodeValue("URL");
        //userInfo. = xNode.getFirstNodeValue("");
        userInfo.homeAddress = xNode.getFirstNodeValue("ADR", "STREET");
        userInfo.homeCity = xNode.getFirstNodeValue("ADR", "LOCALITY");
        userInfo.homeState = xNode.getFirstNodeValue("ADR", "REGION");
        userInfo.homePhones = xNode.getFirstNodeValue("TEL", "HOME");
        userInfo.cellPhone = xNode.getFirstNodeValue("TEL", "NUMBER");

        userInfo.workCompany = xNode.getFirstNodeValue("ORG", "ORGNAME");
        userInfo.workDepartment = xNode.getFirstNodeValue("ORG", "ORGUNIT");
        userInfo.workPosition = xNode.getFirstNodeValue("TITLE");

        if (isConference(from)) {
            
            Contact c = getJabber().getItemByUIN(getShortJid(from));
            if (c instanceof JabberServiceContact) {
                JabberContact.SubContact sc = ((JabberServiceContact)c).getExistSubContact(getResource(from, null));
                if ((null != sc) && (null != sc.realJid)) {
                    userInfo.uin = sc.realJid;
                }
            }
        }
        if (!isGate(from)) {
            userInfo.setOptimalName();
        }
        userInfo.updateProfileView();
        
        singleUserInfo = null;
    }
    
    private void loadBookmarks(XmlNode xNode) {
    if (null == xNode) {
    	    return;
        }
        Group group = null;
        int autoJoinCount = 7;
        while (0 < xNode.childrenCount()) {
            XmlNode item = xNode.popChildNode();
            
            String jid = item.getAttribute(XmlNode.S_JID);
            String name = item.getAttribute(XmlNode.S_NAME);
            String nick = item.getFirstNodeValue("ni" + "ck");
            String autojoin = item.getAttribute("au" + "tojoin");
            if ((null == jid) || !isConference(jid)) {
                continue;
            }
            JabberServiceContact conference = (JabberServiceContact)getJabber().createTempContact(jid, name);
            if (null == group) {
                String groupName = conference.getDefaultGroupName();
                group = getJabber().getOrCreateGroup(groupName);
            }
            conference.setMyName(nick);
            conference.setBooleanValue(Contact.CONTACT_IS_TEMP, false);
            conference.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            conference.setAutoJoin(isTrue(autojoin));
            conference.setGroup(group);
            getJabber().addLocalContact(conference);
            if (conference.isAutoJoin() && (0 < autoJoinCount)) {
                conference.join();
                autoJoinCount--;
            }
        }
        getJabber().getContactList().optionsChanged();
        getJabber().getContactList().update();
    }

    private static final String S_TEXT = "te" + "xt";
    private static final String S_FROM = "fr" + "om";
    private static final String S_TYPE = "ty" + "pe";
    private static final String S_ERROR = "e" + "rror";
    private static final String S_NONE = "n" + "o" + "ne";
    private static final String S_NODE = "n" + "o" + "de";
    private static final String S_SET = "s" + "e" + "t";
    private static final String S_REMOVE = "r" + "emove";
    private static final String S_RESULT = "r" + "esult";
    private static final String S_GROUP = "g" + "roup";
    private static final String S_ITEM = "i" + "tem";
    private static final String S_ITEMS = "i" + "tems";
    private static final String S_TRUE = "t" + "rue";
    private static final String S_FALSE = "fa" + "lse";
    private static final String S_GET = "g" + "e" + "t";
    private static final String S_TIME = "t" + "ime";
    private static final String S_CODE = "c" + "ode";
    /**
     * Parse the <<lit>presence</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     */
    private void parsePresence(XmlNode x) throws JimmException {
        String fromFull = x.getAttribute(S_FROM);
        String from = getShortJid(fromFull);
        String resource = getResource(fromFull, "");
        
        String type = x.getAttribute(S_TYPE);
        if (S_ERROR.equals(type)) {
            XmlNode xNode = x.getFirstNode(S_ERROR);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + xNode.getAttribute(S_CODE) + " " +
                    "Value=" + xNode.getFirstNodeValue(S_TEXT));
            // #sijapp cond.end #
//            removeContact(from);
            if (isConference(from)) {
                String desc = xNode.getFirstNodeValue(S_TEXT);
                if (null == desc) {
                    desc = "error " + xNode.getAttribute(S_CODE);
                }
                getJabber().addMessage(new SystemNotice(getJabber(),
                        SystemNotice.SYS_NOTICE_ERROR, from,
                        false, desc));
            }
            Contact c = getJabber().getItemByUIN(from);
            if (null != c) {
                c.setOfflineStatus();
            }
            return;
        }
        
        if (("subscr" + "ibe").equals(type)) {
            if (isAutoGateContact(from)) {
                sendSubscribed(from);
                requestSubscribe(from);
            } else {
                getJabber().addMessage(new SystemNotice(getJabber(), SystemNotice.SYS_NOTICE_AUTHREQ, from, false, null));
            }
            return;
        }
        if (("subscr" + "ibed").equals(type) && !isAutoGateContact(from)) {
    	    getJabber().addMessage(new SystemNotice(getJabber(), SystemNotice.SYS_NOTICE_AUTHREQ, from, false, null));
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println("you are granded by " + from);
            // #sijapp cond.end#
            return;
        }
        if (null == type) {
            type = x.getFirstNodeValue("sh" + "ow");
        }
        if (null == type) {
            type = "";
        }
        
        int priority = Util.strToIntDef(x.getFirstNodeValue("prior" + "ity"), 0);
        XmlNode item = x.getFirstNode("ite" + "m");
        if (isConference(from) && (null != item)) {
            String role = item.getAttribute(XmlNode.S_ROLE);
            if (("m" + "oderator").equals(role)) {
                priority = JabberServiceContact.ROLE_MODERATOR;

            } else if (("p" + "articipant").equals(role)) {
                priority = JabberServiceContact.ROLE_PARTICIPANT;
                
            } else {// "visitor"
                priority = JabberServiceContact.ROLE_VISITOR;
            }
        }
        
        JabberContact contact = (JabberContact)getJabber().getItemByUIN(from);
        if (isConference(from)) {
            if ((null != contact) && (null != item) &&
        	    (null != item.getAttribute(XmlNode.S_NICK))) {
                String newNick = item.getAttribute(XmlNode.S_NICK);
                updateLocalContact(from, newNick, null, null, "", "",
                        priority,
                        (null == item) ? null : item.getAttribute(XmlNode.S_JID));
                
                ((JabberServiceContact)contact).nickChainged(resource, newNick);
            }
        } else {
            if ((null != contact) && contact.getUin().equals(contact.getName())) {
                String name = x.getFirstNodeValue("n" + "ickname");
                getJabber().renameContact(contact, name);
            }
        }
        
        updateLocalContact(from, resource, null, null,
                type, x.getFirstNodeValue("st" + "atus"), priority,
                (null == item) ? null : item.getAttribute(XmlNode.S_JID));

        // #sijapp cond.if modules_CLIENTS is "true" #
        if (null != contact) {
            contact.setClient(resource, x.getFirstNodeAttribute("c", S_NODE));
        }
        // #sijapp cond.end #
    }

    private void parseEvent(XmlNode eventNode, String fullJid) {
	// #sijapp cond.if modules_XSTATUSES is "true" #
        JabberContact contact = (JabberContact)getJabber().getItemByUIN(getShortJid(fullJid));
        if (null == contact) {
    	    return;
        }

	final String S_MOOD = "moo" + "d";
	final String S_ACTIVITY = "activ" + "ity";
        XmlNode statusNode = eventNode.getFirstNode(S_MOOD);
        if (null == statusNode) {
            statusNode = eventNode.getFirstNode(S_ACTIVITY);
        }
        if (null == statusNode) {
    	    contact.setXStatus(getResource(fullJid, ""), "", "");
	    return;
        }
        String text = statusNode.getFirstNodeValue(S_TEXT);
        StringBuffer status = new StringBuffer();
        while (null != statusNode) {
    	    status.append(':').append(statusNode.name);
    	    statusNode = statusNode.childAt(0);
        }
        contact.setXStatus(getResource(fullJid, ""), status.toString().substring(1), text);
        // #sijapp cond.end #
    }
    /**
     * Parse the <<lit>message</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseMessage(XmlNode x) throws JimmException {
        String fullJid = realJidToJimmJid(x.getAttribute(S_FROM));
        String from = getShortJid(fullJid);
        String messageType = x.getAttribute(S_TYPE);
        
        String subject = x.popFirstNodeValue("subje" + "ct");
        String text = x.popFirstNodeValue("b" + "ody");
        if ((null != subject) && (null == text)) {
            text = "";
        }
        if (x.contains("atte" + "ntion")) {
            text = jimm.chat.ChatTextList.CMD_WAKEUP;
        }
        if (null == text) {
            XmlNode event = x.getFirstNode("ev" + "ent");
            if (null != event) {
                parseEvent(event, fullJid);
            }
            return;
        }
        if ((null != subject) && (-1 == text.indexOf(subject))) {
            text = subject + "\n\n" + text;
        }
    	XmlNode xnode = x.getFirstNode("x", "jabber:x:oob");
        if (null != xnode) {
            String url = xnode.popFirstNodeValue("u" + "rl");
            String desc = xnode.popFirstNodeValue("d" + "es"+ "c");
            if (null != url) {
                text += "\n\n" + url;
            }
            if (null != desc) {
                text += "\n" + desc;
            }
        }
        if (x.contains(S_ERROR)) {
            XmlNode xNode = x.getFirstNode(S_ERROR);
            String errorText = xNode.getFirstNodeValue(S_TEXT);
            if (null == errorText) {
                errorText = xNode.value;
            }
            if (null == errorText) {
                errorText = "error " + xNode.getAttribute(S_CODE);
            }
            if (null != errorText) {
                text = errorText + "\n-------\n" + text;
            }
        }
        
        boolean isConference = false;
        boolean isGroupchat = ("groupc" + "hat").equals(messageType);
        Contact c = getJabber().getItemByUIN(from);
        
        if ((null != c) && (null != x.getFirstNode("c" + "aptcha"))) {
            JabberForm form = new JabberForm(JabberForm.TYPE_CAPTCHA,
                    getJabber(), from, "captcha");
            form.showCaptcha(x);
            return;
        }

        if (c instanceof JabberServiceContact) {
            isConference = ((JabberServiceContact)c).isConference();
            if (isConference && isGroupchat && (null != subject)) {
                ((JabberServiceContact)c).setSubject(subject);
                subject = null;
            }

        } else if (x.contains("reques" + "t") && (null != x.getId())) {
            putPacketIntoQueue("<message to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(x.getId())
                    + "'><received xmlns='urn:xmpp:receipts'/></message>");
        }
        
        if (isConference && !isGroupchat) {
            from = fullJid;
            isConference = false;
            c = getJabber().getItemByUIN(from);
        }

        XmlNode offline = x.getFirstNode("x");
        String date = (null == offline) ? null : offline.getAttribute("stamp");
        PlainMessage message;
        
        if (null == date) {
            message = new PlainMessage(from, getJabber(),
                    Util.createCurrentDate(false), text, false);
        } else {
            message = new PlainMessage(from, getJabber(),
                    Util.createDate(date, false), text, true);
        }
        

// I don't know why I write it.
//        if (null == c) {
//            c = getJabber().createTempContact(from);
//            getJabber().addTempContact(c);
//        }
        if (null == c) {
        } else if (isConference) {
            JabberServiceContact conf = (JabberServiceContact)c;
            String resource = getResource(fullJid, null);
            if (null != resource) {
                message.setName(conf.getNick(resource));
            }
            
            if (isGroupchat && !message.isOffline()
                    && conf.getMyName().equals(resource)) {
                return;
            }
        } else {
            JabberContact contact = (JabberContact)c;
            contact.setActiveResource(getResource(fullJid, null));
        }
        getJabber().addMessage(message);
        if ((null == c) && isConference(from) && !isGroupchat) {
            JabberServiceContact contact = (JabberServiceContact)getJabber().getItemByUIN(from);
            if (null != contact) {
                contact.setPrivateContactStatus();
            }
        }
    }
    
    private boolean isMechanism(XmlNode list, String myMechanism) {
        for (int i = 0; i < list.childrenCount(); i++) {
            XmlNode mechanism = list.childAt(i);
            if ("mechanism".equals(mechanism.name) && myMechanism.equals(mechanism.value)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse the <<lit>stream:features</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseStreamFeatures(XmlNode x) throws JimmException {
        /* Check for stream compression method */
        XmlNode x2 = x.getFirstNode("compression");
        if ((null != x2) && "zlib".equals(x2.getFirstNodeValue("method"))) {
            // #sijapp cond.if modules_ZLIB is "true" #
            sendRequest("<compress xmlns='http://jabber.org/protocol/compress'><method>zlib</method></compress>");
            return;
            // #sijapp cond.end #
        }
        
        /* Check for authentication mechanisms */
        x2 = x.getFirstNode("mechanisms");
        
        if ((null != x2) && (null != x2.getFirstNode("mechanism"))) {
            //String mechanism = x2.getFirstNodeValue("mechanism");
            
            String auth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' ";
            
            // #sijapp cond.if (target isnot "DEFAULT" & target isnot "SIEMENS1")#
            String googleToken = null;
            /* X-GOOGLE-TOKEN authentication */
            if (isMechanism(x2, "X-GOOGLE-TOKEN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                System.out.println("[INFO-JABBER] Using X-GOOGLE-TOKEN");
                // #sijapp cond.end #
                isGTalk_ = true;
                googleToken = getGoogleToken(getJabber().getUin(), getJabber().getPassword());
            }
            // #sijapp cond.end#
            
            /* DIGEST-MD5 authentication */
            if (isMechanism(x2, "DIGEST-MD5")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                System.out.println("[INFO-JABBER] Using DIGEST-MD5");
                // #sijapp cond.end #
                auth += "mechanism='DIGEST-MD5'/>";
                
                // #sijapp cond.if (target isnot "DEFAULT" & target isnot "SIEMENS1")#
            } else if (null != googleToken) {
                auth += "mechanism='X-GOOGLE-TOKEN'>" + googleToken + "</auth>";
                // #sijapp cond.end#
                
                /* PLAIN authentication */
            } else if (isMechanism(x2, "PLAIN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                System.out.println("[INFO-JABBER] Using PLAIN");
                // #sijapp cond.end #
                auth += "mechanism='PLAIN'>";
                Util data = new Util();
                data.writeString(getJabber().getUin());
                data.writeByte(0);
                data.writeString(getNick(getJabber().getUin()));
                data.writeByte(0);
                data.writeString(getJabber().getPassword());
                auth += JabberMD5.toBase64(data.toByteArray());
                auth += "</auth>";
                
            } else if (isGTalk_) {
                nonSaslLogin();
                return;

            } else {
                /* Unknown authentication method */
                setAuthStatus(false);
                return;
            }
            
            sendRequest(auth);
        }
        
        /* Check for resource bind */
        x2 = x.getFirstNode("bind");
        if (x2 != null) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            System.out.println("[INFO-JABBER] Send bind request");
            // #sijapp cond.end #
            sendRequest("<iq type='set' id='bind'>"
                    + "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                    + "<resource>" + Util.xmlEscape(resource) + "</resource>"
                    + "</bind>"
                    + "</iq>");
        }
    }
    
    /**
     * Parse the <<lit>challenge</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseChallenge(XmlNode x) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        System.out.println("[INFO-JABBER] Received challenge");
        // #sijapp cond.end #
        String challenge = JabberMD5.decodeBase64(x.value);
        String resp = "<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>";
        
        int nonceIndex = challenge.indexOf("nonce=");
        if (nonceIndex >= 0) {
            nonceIndex += 7;
            String nonce = challenge.substring(nonceIndex, challenge.indexOf('\"', nonceIndex));
            String cnonce = "123456789abcd";
            
            int endI = getJabber().getUin().indexOf("@");
            if (endI == -1) endI = getJabber().getUin().length();
            String t = responseMd5Digest(
                    getJabber().getUin().substring(0, endI),
                    getJabber().getPassword(),
                    domain_,// TODO check it
                    "xmpp/" + domain_,
                    nonce,
                    cnonce);
            resp += t;
        }
        
        resp += "</response>";
        sendRequest(resp);
    }
    
    /**
     * This routine generates MD5-DIGEST response via SASL specification
     * (From BOMBUS project)
     *
     * @param user
     * @param pass
     * @param realm
     * @param digest_uri
     * @param nonce
     * @param cnonce
     * @return
     */
    private static String responseMd5Digest(String user, String pass,
            String realm, String digestUri, String nonce, String cnonce) {
        JabberMD5 hUserRealmPass = new JabberMD5();
        hUserRealmPass.init();
        hUserRealmPass.updateASCII(user);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(realm);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(pass);
        hUserRealmPass.finish();
        
        JabberMD5 hA1 = new JabberMD5();
        hA1.init();
        hA1.update(hUserRealmPass.getDigestBits());
        hA1.update((byte) ':');
        hA1.updateASCII(nonce);
        hA1.update((byte) ':');
        hA1.updateASCII(cnonce);
        hA1.finish();
        
        JabberMD5 hA2 = new JabberMD5();
        hA2.init();
        hA2.updateASCII("AUTHENTICATE:");
        hA2.updateASCII(digestUri);
        hA2.finish();
        
        JabberMD5 hResp = new JabberMD5();
        hResp.init();
        hResp.updateASCII(hA1.getDigestHex());
        hResp.update((byte) ':');
        hResp.updateASCII(nonce);
        hResp.updateASCII(":00000001:");
        hResp.updateASCII(cnonce);
        hResp.updateASCII(":auth:");
        hResp.updateASCII(hA2.getDigestHex());
        hResp.finish();
        
        String resp = JabberMD5.toBase64(new StringBuffer()
        .append("username=\"").append(user)
        .append("\",realm=\"").append(realm)
        .append("\",nonce=\"").append(nonce)
        .append("\",nc=00000001,cnonce=\"").append(cnonce)
        .append("\",qop=auth,digest-uri=\"").append(digestUri)
        .append("\",response=\"").append(hResp.getDigestHex())
        .append("\",charset=utf-8").toString().getBytes());
        
        return resp;
    }
    
    
    // #sijapp cond.if (target isnot "DEFAULT" & target isnot "SIEMENS1")#
    /**
     * Generates X-GOOGLE-TOKEN response by communication with
     * http://www.google.com
     * (From mGTalk project)
     *
     * @param userName
     * @param passwd
     * @return
     */
    private String getGoogleToken(String jid, String passwd) {
        int endI = jid.indexOf("@");
        if (endI == -1) endI = jid.length();
        try {
	    String escapedJid = Util.replace(jid, "@", "%40");
            String first = "Email=" + escapedJid + "&Passwd=" + passwd
	            + "&PersistentCookie=false&source=googletalk";

            HttpsConnection c = (HttpsConnection) Connector
                    .open("https://www.google.com:443/accounts/ClientAuth?" + first);

            System.out.println("[INFO-JABBER] Connecting to www.google.com");
            DataInputStream dis = c.openDataInputStream();
            String str = readLine(dis);
            if (str.startsWith("SID=")) {
                String SID = str.substring(4, str.length());
                str = readLine(dis);
                String LSID = str.substring(5, str.length());
                first = "SID=" + SID + "&LSID=" + LSID + "&service=mail&Session=true";
                dis.close();
                c.close();
                c = (HttpsConnection) Connector
                        .open("https://www.google.com:443/accounts/IssueAuthToken?" + first);

                System.out.println("[INFO-JABBER] Next www.google.com connection");
                dis = c.openDataInputStream();
                str = readLine(dis);

                String userName = jid.substring(0, endI);
                String token = JabberMD5.toBase64(("\0" + userName + "\0" + str).getBytes());
                dis.close();
                c.close();
                return token;
            }

            throw new Exception("Invalid response");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("EX: " + ex.toString());
        }
        return null;
    }
    // #sijapp cond.end#
    
    /**
     * Service routine for google token
     * (From mGTalk project)
     *
     * @param dis
     * @return
     */
    private static String readLine(DataInputStream dis) {
        StringBuffer s = new StringBuffer();
        try {
            for (byte ch = dis.readByte(); ch != -1; ch = dis.readByte()) {
                if (ch == '\n') {
                    return s.toString();
                }
                s.append((char)ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s.toString();
    }
    
    /**
     * Updates contacts information. If this contact does not exist, it makes
     * a new instance of {@link Contact}
     *
     * @param jid Contacts jid
     * @param name Contacts screen name
     * @param group Contacts group name
     * @param status_1 Contacts status_1
     * @param statusText Contacts personal message
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void updateLocalContact(String jid, String resource, String name, String group,
            String status, String statusText, int priority, String realJid) {
        Contact c = getJabber().getItemByUIN(jid);
        if (null != c) {
            ((JabberContact)c).setStatus(resource, priority, status, statusText);
            if ((c instanceof JabberServiceContact) && (null != realJid)) {
                JabberServiceContact serviceContact = (JabberServiceContact)c;
                serviceContact.setRealJid(resource, getShortJid(realJid));
            }
            getJabber().updateContact(c, false);
        }
        
        String privateJid = realJidToJimmJid(jid + '/' + resource);
        JabberServiceContact privateContact = (JabberServiceContact)getJabber().getItemByUIN(privateJid);
        if (null == privateContact) {
        } else if (isConference(jid)) {
            privateContact.setPrivateContactStatus();
        } else {
            ((JabberContact)privateContact).setStatus(resource, priority, status, statusText);
        }
    }
    
    public void updateContacts(Vector contacts) {
        StringBuffer xml = new StringBuffer();
        
        int itemCount = 0;
        xml.append("<iq type='set'>");
        xml.append("<query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (isConference(contact.getUin())) {
                continue;
            }
            itemCount++;
            xml.append("<item name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            xml.append("'>");
            Group group = contact.getGroup();
            if (null != group) {
                xml.append("<group>");
                xml.append(Util.xmlEscape(group.getName()));
                xml.append("</group>");
                xml.append("</item>");
            }
        }
        xml.append("</query>");
        xml.append("</iq>");
        if (0 < itemCount) {
            putPacketIntoQueue(xml.toString());
        }
    }
    
    public String getConferenceStorage() {
        StringBuffer xml = new StringBuffer();
        Vector contacts = getJabber().getContactItems();
        xml.append("<storage xmlns='storage:bookmarks'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.isConference() || contact.isTemp()) {
                continue;
            }
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);

            JabberServiceContact conf = (JabberServiceContact)contact;
            xml.append("<conference autojoin='");
            xml.append(conf.isAutoJoin() ? S_TRUE : S_FALSE);
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            if (!StringConvertor.isEmpty(conf.getPassword())) {
                xml.append("' password='");
                xml.append(Util.xmlEscape(conf.getPassword()));
            }
            xml.append("'>");
            xml.append("<nick>");
            xml.append(Util.xmlEscape(conf.getMyName()));
            xml.append("</nick>");
            xml.append("</conference>");
        }
        xml.append("</storage>");
        return xml.toString();
    }
    public void saveConferences() {
        StringBuffer xml = new StringBuffer();
        
        String storage = getConferenceStorage();
        xml.append("<iq type='set'><query xmlns='jabber:iq:private'>");
        xml.append(storage);
        xml.append("</query></iq>");

        // XEP-0048
        if (xep0048) {
            xml.append("<iq type='set'>");
            xml.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
            xml.append("<publish node='storage:bookmarks'><item id='current'>");
            xml.append(storage);
            xml.append("</item></publish></pubsub></iq>");
        }

        putPacketIntoQueue(xml.toString());
    }
    public void removeGateContacts(String gate) {
        if (StringConvertor.isEmpty(gate)) {
            return;
        }
        gate = "@" + gate;
        Vector contacts = getJabber().getContactItems();
        StringBuffer xml = new StringBuffer();
     
        xml.append("<iq type='set'>");
        xml.append("<query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.getUin().endsWith(gate)) {
                continue;
            }
     
            xml.append("<item subscription='remove' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            xml.append("'/>");
        }
        xml.append("</query>");
        xml.append("</iq>");
     
        putPacketIntoQueue(xml.toString());
    }
    
    public void updateContact(JabberContact contact) {
        if (isConference(contact.getUin()) && contact.isConference()) {
            contact.setBooleanValue(Contact.CONTACT_IS_TEMP, false);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            String groupName = ((JabberContact)contact).getDefaultGroupName();
            Group group = getJabber().getOrCreateGroup(groupName);
            contact.setGroup(group);
            getJabber().updateContact(contact, false);
            //getJabber().getContactList().optionsChanged();
            //getJabber().getContactList().update();
            saveConferences();
            return;
        }
        Group g = contact.getGroup();
        if (g.getName().equals(contact.getDefaultGroupName())) {
            g = null;
        }
        putPacketIntoQueue("<iq type='set'>"
                + "<query xmlns='jabber:iq:roster'>"
                + "<item name='" + Util.xmlEscape(contact.getName())
                + "' jid='" + Util.xmlEscape(contact.getUin()) + "'>"
                + (null == g ? "" : "<group>" + Util.xmlEscape(g.getName()) + "</group>")
                + "</item>"
                + "</query>"
                + "</iq>");
    }
    public void removeContact(String jid) {
        if (isConference(jid)) {
            saveConferences();
        }
        putPacketIntoQueue("<iq type='set'>"
                + "<query xmlns='jabber:iq:roster'>"
                + "<item subscription='remove' jid='" + Util.xmlEscape(jid) + "'/>"
                + "</query>"
                + "</iq>");
    }
    
    public void getBookmarks() {
        putPacketIntoQueue("<iq type='get'><query xmlns='jabber:iq:private'><storage xmlns='storage:bookmarks'/></query></iq>");
        // XEP-0048
        if (xep0048) {
            putPacketIntoQueue("<iq type='get'><pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='storage:bookmarks'/></pubsub></iq>");
        }
    }
    /*
    protected void sendStatus(String status) {
        StringBuffer presence = new StringBuffer()
            .append("<presence>")
            .append(  "<priority>1</priority>");
        if (!"".equals(status)) {
            presence.append("<show>").append(status).append("</show>");
        }
        presence.append("</presence>");
        putPacketIntoQueue(presence.toString());
    }
     */
    
    
    /**
     * Get roster request
     */
    private static final String GET_ROSTER_XML = "<iq type='get' id='roster'>"
                + "<query xmlns='jabber:iq:roster'/>"
                + "</iq>";
    
    /**
     * Get open stream request
     */
    private final String getOpenStreamXml(String server) {
        return "<?xml version='1.0'?>"
                + "<stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http:/" + "/etherx.jabber.org/streams' "
                + "version='1.0' "
                + "to='" + server + "'"
                + " xml:lang='" + jimm.util.ResourceBundle.getLanguageCode()+ "'>";
    }
    
    private void getVCard(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(generateId()) + "'>"
                + "<vCard xmlns='vcard-temp' version='2.0' prodid='-/"
                + "/HandGen/" + "/NONSGML vGen v1.0/" + "/EN'/>"
                + "</iq>");
    }
    
    private void sendMessage(String to, String msg, String type, boolean notify, String id) {
        to = jimmJidToRealJid(to);
        boolean buzz = msg.startsWith(jimm.chat.ChatTextList.CMD_WAKEUP) && S_CHAT.equals(type);
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to) + "'"
                + " type='" + type + "' id='" + Util.xmlEscape(id) + "'>"
                + (isGTalk_ ? "<nos:x value='disabled' xmlns:nos='google:nosave'/>" : "")
                + (buzz ? "<attention xmlns='urn:xmpp:attention:0'/>" : "")
                + "<body>" + Util.xmlEscape(msg) + "</body>"
                + (notify ? "<request xmlns='urn:xmpp:receipts'/>" : "")
                + "</message>");
    }
    private static final String S_CHAT = "c" + "hat";
    void sendMessage(String to, String msg) {
        String type = S_CHAT;
        if (isConference(to) && (-1 == to.indexOf('/'))) {
            type = "groupc" + "hat";
        }
        sendMessage(to, msg, type, false, generateId());
    }
    /**
     * Sends a message to a user
     *
     * @param msg Message to send
     * @param to Receivers jid
     */
    void sendMessage(PlainMessage message) {
	JabberContact toContact = (JabberContact)message.getRcvr();
        String to = (null == toContact) ? message.getRcvrUin() : toContact.getReciverJid();
        String type = "c" + "hat";
        if (isConference(to) && (-1 == to.indexOf('/'))) {
            type = "groupc" + "hat";
        }
        sendMessage(to, message.getText(), type, false,
                generateId());
    }
    
    public static final int PRESENCE_UNAVAILABLE = -1;
    void presence(JabberServiceContact conf, String to, int priority, String password) {
        String xml;
        if (0 <= priority) {
            xml = "<presence to='"+ Util.xmlEscape(to) + "'><priority>" + priority + "</priority>";
            String xNode = "";
            if (!StringConvertor.isEmpty(password)) {
                xNode += "<password>" + Util.xmlEscape(password) + "</password>";
            }
            
            if (conf.isConference()) {
                long time = conf.hasChat() ? conf.getChat().getLastMessageTime() : 0;
                time = (0 == time) ? 24*60*60 : (Util.createCurrentDate(false) - time);
                xNode += "<history maxstanzas='20' seconds='" + time + "'/>";
            }
            if (!StringConvertor.isEmpty(xNode)) {
                xml += "<x xmlns='http://jabber.org/protocol/muc'>" + xNode + "</x>";
            }
            
            xml += getCaps() + "</presence>";
            putPacketIntoQueue(xml);
            
        } else {
            putPacketIntoQueue("<presence type='unavailable' to='" + Util.xmlEscape(to)
            + "'><status>I&apos;ll be back</status></presence>");
        }
    }
    
    void setStatus(String status, String msg, int priority) {
        // #sijapp cond.if modules_XSTATUSES is "true" #
        // FIXME
        if (!getJabber().getXStatus().isPep()) {
            msg = getJabber().getXStatus().getText();
        }
        // #sijapp cond.end #
        String xml = "<presence>"
                + (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>")
                + (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>")
                + (0 < priority ? "<priority>" + priority + "</priority>" : "")
                + getCaps()
                + "</presence>";
        putPacketIntoQueue(xml);
    }
    
    public void sendSubscribed(String jid) {
        putPacketIntoQueue("<presence type='subscribed' to='" + Util.xmlEscape(jid) + "'/>");
    }
    public void sendUnsubscribed(String jid) {
        putPacketIntoQueue("<presence type='unsubscribed' to='" + Util.xmlEscape(jid) + "'/>");
    }
    public void requestSubscribe(String jid) {
        putPacketIntoQueue("<presence type='subscribe'  to='" + Util.xmlEscape(jid) + "'/>");
    }
    
    public void requestConferenceInfo(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/disco#info'/></iq>");
    }
    public void requestConferenceUsers(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/disco#items'/></iq>");
    }
    
    public void requestDiscoItems(String server) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(server)
                + "'><query xmlns='http://jabber.org/protocol/disco#items'/></iq>");
    }
    
    void requestRawXml(String xml) {
        putPacketIntoQueue(xml);
    }
    protected void sendClosePacket() {
        try {
            write("<presence type='unavailable'><status>Logged out</status></presence>");
        } catch (Exception e) {
        }
    }
    public void setMucRole(String jid, String nick, String role) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'/></query></iq>");
    }
    public void setMucAffiliation(String jid, String userJid, String affiliation) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='" + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'/></query></iq>");
    }

//    private JabberContact lastContact = null;
    private UserInfo singleUserInfo;
    UserInfo getUserInfo(JabberContact contact) {
        singleUserInfo = new UserInfo(getJabber());
//        lastContact = contact;
        getVCard(contact.getUin());
        return singleUserInfo;
    }
    private String autoSubscribeDomain;
    private JabberForm jabberForm;
    void register2(String rawXml, String jid) {
        autoSubscribeDomain = jid;
        requestRawXml(rawXml);
    }
    private boolean isAutoGateContact(String jid) {
	return !StringConvertor.isEmpty(autoSubscribeDomain)
		&& (jid.equals(autoSubscribeDomain) || jid.endsWith('@' + autoSubscribeDomain));
    }
    void register(String jid) {
        jabberForm = new JabberForm(JabberForm.TYPE_REGISTER, getJabber(), jid, "registration");
        putPacketIntoQueue("<iq type='get' to='" + jid
                + "' id='reg1'><query xmlns='jabber:iq:register'/></iq>");
        jabberForm.show();
    }
    void unregister(String jid) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='unreg1'><query xmlns='jabber:iq:register'><remove/></query></iq>");
    }
    

}
// #sijapp cond.end #