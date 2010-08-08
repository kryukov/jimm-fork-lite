/*
 * XmlNode.java
 *
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import jimm.JimmException;
import jimm.comm.StringConvertor;
import jimm.comm.Util;

/**
 * Very light-weight xml parser
 *
 * @author Matej Usaj
 * @author Vladimir Kryukov
 */
public class XmlNode {
    public String name;
    public String value;
    private Hashtable attribs = new Hashtable();
    private Vector children = new Vector();
    
    private XmlNode(){}

    public XmlNode childAt(int index) {
        if (children.size() <= index) {
            return null;
        }
        return (XmlNode)children.elementAt(index);
    }
    public int childrenCount() {
        return children.size();
    }
    
    public static XmlNode parse(JabberXml jabberConnection) throws JimmException {
        byte ch = jabberConnection.getNextByte();
        if ('<' != ch) {
            return null;
        }
        XmlNode xml = new XmlNode();
        boolean parsed = xml.parseNode(jabberConnection, removeXmlHeader(jabberConnection));
        return parsed ? xml : null;
    }

    private void setName(String tagName) {
	if (-1 == tagName.indexOf(':') || -1 != tagName.indexOf("stream:")) {
	    name = tagName;
	    return;
	}
	name = tagName.substring(tagName.indexOf(':') + 1);
    }
    private String readCdata(JabberXml jabberConnection) throws JimmException {
        Util body = new Util();
        byte ch = jabberConnection.getNextByte();
        int maxSize = S_BINVAL.equals(name) ? MAX_BIN_VALUE_SIZE : MAX_VALUE_SIZE;
        int size = 0;
        for (int state = 0; state < 3;) {
            ch = jabberConnection.getNextByte();
            if (size < maxSize) {
                body.writeByte(ch);
                size++;
            }
            if (']' == ch) {
                state = Math.min(state + 1, 2);
            } else if ((2 == state) && ('>' == ch)) {
                state++;
            } else {
                state = 0;
            }
            
        }
        String value = body.toUtf8String();
        body.reset();
        return value.substring(7, Math.max(7, value.length() - 7 - 3));
    }
    private void readChar(Util out, JabberXml jabberConnection) throws JimmException {
        StringBuffer buffer = new StringBuffer();
        byte ch = jabberConnection.getNextByte();
        int limit = 32;
        while (';' != ch) {
            if (0 < limit) {
                buffer.append((char)ch);
                limit--;
            }
            ch = jabberConnection.getNextByte();
        }
        if (0 == buffer.length()) {
            return;
        }
        if ('#' == buffer.charAt(0)) {
            try {
                buffer.deleteCharAt(0);
                int radix = 10;
                if ('x' == buffer.charAt(0)) {
                    buffer.deleteCharAt(0);
                    radix = 16;
                }
                int code = Integer.parseInt(buffer.toString(), radix);
                out.writeString(String.valueOf((char)code));
            } catch (Exception e) {
                out.writeByte('?');
            }
            return;
        }
        String code = buffer.toString();
        if ("quot".equals(code)) {
            out.writeByte('\"');
        } else if ("gt".equals(code)) {
            out.writeByte('>');
        } else if ("lt".equals(code)) {
            out.writeByte('<');
        } else if ("apos".equals(code)) {
            out.writeByte('\'');
        } else if ("amp".equals(code)) {
            out.writeByte('&');
        } else {
            out.writeByte('&');
            out.writeString(code);
            out.writeByte(';');
        }
    }
    private String readString(JabberXml jabberConnection, byte endCh, int limit) throws JimmException {
        byte ch = jabberConnection.getNextByte();
        if (endCh == ch) {
            return null;
        }
        Util str = new Util();
        while (endCh != ch) {
            if (str.size() < limit) {
                if ('&' != ch) {
                    str.writeByte(ch);
                } else {
                    readChar(str, jabberConnection);
                }
            }
            ch = jabberConnection.getNextByte();
        }
        return str.toUtf8String();
    }

    private boolean parseNode(JabberXml jabberConnection, byte ch0) throws JimmException {
        // tag name
        byte ch = ch0;
        if ('!' == ch) {
            readCdata(jabberConnection);
            return false;
        }
        if ('/' == ch) {
            ch = jabberConnection.getNextByte();
            while ('>' != ch) {
                ch = jabberConnection.getNextByte();
            }
            return false;
        }
        StringBuffer tagName = new StringBuffer();
        while (' ' != ch && '>' != ch) {
            tagName.append((char)ch);
            ch = jabberConnection.getNextByte();
            if ('/' == ch) {
                setName(tagName.toString());
                ch = jabberConnection.getNextByte(); // '>'
                return true;
            }
        }
        setName(tagName.toString());
        tagName = null;
        
        // tag attributes
        while ('>' != ch) {
            if ('/' == ch) {
                ch = jabberConnection.getNextByte(); // '>'
                return true;
            }
            StringBuffer attrName = new StringBuffer();
            while ('=' != ch && '>' != ch) {
                if (' ' != ch) {
                    attrName.append((char)ch);
                }
                ch = jabberConnection.getNextByte();
            }
            
            byte startValueCh = jabberConnection.getNextByte(); // '"' or '\''
            String attribValue = readString(jabberConnection, startValueCh, 2*1024);
            if (0 < attrName.length()) {
                if (null == attribValue) {
                    attribValue = "";
                }
                attribs.put(getAttrName(attrName), attribValue);
            }
            ch = jabberConnection.getNextByte();
        }
        if ("stream:stream".equals(name)) {
            return true;
        }
        // tag body
        int maxSize = S_BINVAL.equals(name) ? MAX_BIN_VALUE_SIZE : MAX_VALUE_SIZE;
        value = readString(jabberConnection, (byte)'<', maxSize);
        
        // sub tags
        while (true) {
            ch = jabberConnection.getNextByte();
            if ('!' == ch) {
                value = readCdata(jabberConnection);
                
            } else {
                XmlNode xml = new XmlNode();
                if (!xml.parseNode(jabberConnection, ch)) {
                    break;
                }
                // #sijapp cond.if target is "DEFAULT" | target is "SIEMENS1" | target is "MOTOROLA" #
                if (!S_BINVAL.equals(xml.name)) {
                    children.addElement(xml);
                }
                // #sijapp cond.else#
                children.addElement(xml);
                // #sijapp cond.end#
            }
            
            ch = jabberConnection.getNextByte();
            while ('<' != ch) {
                ch = jabberConnection.getNextByte();
            }
        }
        if (StringConvertor.isEmpty(value)) {
            value = null;
        }
        return true;
    }
    private static final int MAX_BIN_VALUE_SIZE = 0;
    private static final int MAX_VALUE_SIZE = 3 * 1024;
    public static final String S_JID = "j" + "i" + "d";
    public static final String S_NICK = "n" + "ick";
    public static final String S_NAME = "n" + "ame";
    public static final String S_ROLE = "ro" + "le";
    private static final String S_BINVAL = "BINVAL";
    public static final String S_XMLNS = "x" + "mlns";
    private String getAttrName(StringBuffer buffer) {
	String result = buffer.toString();
        if (S_JID.equals(result)) {
    	    return S_JID;
        } else if (S_NAME.equals(result)) {
	    return S_NAME;
        }
        return result;
    }
    
    private static byte removeXmlHeader(JabberXml jabberConnection) throws JimmException {        
        byte ch = jabberConnection.getNextByte();
        if ('?' != ch) {
            return ch;
        }
        while ('?' != ch) {
            ch = jabberConnection.getNextByte();
        }
        
        ch = jabberConnection.getNextByte(); // '>'
        
        ch = jabberConnection.getNextByte();
        while ('<' != ch) {
            ch = jabberConnection.getNextByte();
        }

        return jabberConnection.getNextByte();
    }
    
    public XmlNode popChildNode() {
        XmlNode node = childAt(0);
        children.removeElementAt(0);
        return node;
    }
    
    /**
     * Get first occurance of a node with a specified name.<br>
     * This method goes in-depth first, not level-by-level
     *
     * @param name Name of the requested node
     * @return {@link XmlNode} node or null if the node was not found.
     */
    public XmlNode getFirstNode(String name) {
        for (int i = 0; i < children.size(); i++) {
    	    XmlNode node = childAt(i);
            if (node.name.equals(name)) {
                return node;
            }
            XmlNode result = node.getFirstNode(name);
            if (null != result) {
                return result;
            }
        }
        return null;
    }
    
    public XmlNode getFirstNode(String name, String xmlns) {
        for (int i = 0; i < children.size(); i++) {
    	    XmlNode node = childAt(i);
            if (node.name.equals(name) && node.isXmlns(xmlns)) {
                return node;
            }
            XmlNode result = node.getFirstNode(name);
            if (null != result) {
                return result;
            }
        }
        return null;
    }
    
    public String getFirstNodeValue(String parentNodeName, String nodeName) {
        for (int i = 0; i < children.size(); i++) {
    	    XmlNode parentNode = childAt(i);
            if (parentNode.name.equals(parentNodeName)) {
        	XmlNode node = parentNode.getFirstNode(nodeName);
        	if (null != node) {
            	    return node.value;
                }
            }
        }
        return null;
    }
    
    public String getFirstNodeAttribute(String name, String key) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.getAttribute(key);
    }

    public String getFirstNodeValue(String name) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.value;
    }
    public String popFirstNodeValue(String name) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.popValue();
    }
    
    public String getAttribute(String key) {
        return (String)attribs.get(key);
    }
    public boolean isXmlns(String namespace) {
        return namespace.equals(getAttribute("xmlns"));
    }
    /**
     * Check if the xml contains a node with a specified name
     *
     * @param name Name of the requested node
     * @return true if the node was found, false otherwise.
     */
    public boolean contains(String name) {
        return null != getFirstNode(name);
    }
    
    private void copyVector(Vector from, Vector to) {
        for (int i = 0; i < from.size(); i++)
            to.addElement(from.elementAt(i));
    }
    public String getId() {
        return getAttribute("i" + "d");
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer().append("<").append(name);
        if (0 != attribs.size()) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                Object k = e.nextElement();
                sb.append(" ").append(k).append("='").append(attribs.get(k)).append("'");
            }
        }
        sb.append(">");
        if (0 != childrenCount()) {
            sb.append("\n");
            for (int i = 0; i < childrenCount(); i++) {
                sb.append("  ").append(childAt(i).toString()).append("\n");
            }
        } else if (null != value) {
            sb.append(value);
        }
        sb.append("</").append(name).append(">");
        return sb.toString();
    }

    public String popValue() {
        String result = value;
        value = null;
        return result;
    }

    public byte[] popBinValue() {
        if (null == value) {
            return null;
        }
        return Util.base64decode(popValue());
    }
}
// #sijapp cond.end #