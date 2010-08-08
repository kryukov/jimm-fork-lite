/*
 * JabberForm.java
 *
 * Created on 12 Март 2009 г., 0:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.ui.FormEx;
import jimm.ui.PopupWindow;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberForm implements CommandListener {
    private FormEx form;
    private Jabber jabber;
    private String jid;
    private String id;
    public final static byte TYPE_REGISTER = 0;
    public final static byte TYPE_CAPTCHA = 1;
    private byte type = TYPE_CAPTCHA;
    /** Creates a new instance of JabberForm */
    public JabberForm(byte formType, Jabber protocol, String resourceJid, String title) {
        form = new FormEx(title, "ok", "back", this);
        jabber = protocol;
        jid = resourceJid;
        type = formType;
    }
    public String getJid() {
        return jid;
    }
    public void show() {
        form.addString(ResourceBundle.getString("wait"));
        form.show();
    }

    private String getCaptchaXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><captcha xmlns='urn:xmpp:captcha'>"
                + getXmlForm()
                + "</captcha></iq>";
    }
    private String getRegisterXml() {
        return "<iq type='set' to='"
                + Util.xmlEscape(jid)
                + "' id='reg2'><query xmlns='jabber:iq:register'>"
                + getXmlForm()
                + "</query></iq>";
    }

    private void doAction() {
        switch (type) {
            case TYPE_REGISTER:
                jabber.getConnection().register2(getRegisterXml(), getJid());
                break;
            case TYPE_CAPTCHA:
                jabber.getConnection().requestRawXml(getCaptchaXml());
                form.back();
                break;
        }
    }
    public void commandAction(Command command, Displayable displayable) {
        if ((form.saveCommand == command) && (0 < fields.size())) {
            doAction();
        }
        if (form.backCommand == command) {
            form.back();
        }
    }
    private void clearForm() {
        form.clearForm();
    }
    private void addInfo(String title, String instructions) {
        form.addString(title, instructions);
    }
    
    private boolean isXData;
    private Vector fields = new Vector();
    private Vector types = new Vector();
    private Vector values = new Vector();
    private void addField(String name, String type, String label, String value) {
        int num = fields.size();
        name = StringConvertor.notNull(name);
        type = StringConvertor.notNull(type);
        value = StringConvertor.notNull(value);
        fields.addElement(name);
        types.addElement(type);
        values.addElement(value);
        
        if (S_HIDDEN.equals(type)) {
            
        } else if (S_FIXED.equals(type)) {
            form.addString(value);
            
        } else if (S_TEXT_SINGLE.equals(type)) {
            form.addTextField(num, label, value, 64, TextField.ANY);

        } else if (S_TEXT_MULTI.equals(type)) {
            int size = Math.max(512, value.length());
            form.addTextField(num, label, value, size, TextField.ANY);
            
        } else if (S_TEXT_PRIVATE.equals(type)) {
            form.addTextField(num, label, value, 64, TextField.PASSWORD);
            
        } else if (S_BOOLEAN.equals(type)) {
            form.addCheckBox(num, label, JabberXml.isTrue(value));

        } else if ("".equals(type)) {
    	    form.addTextField(num, label, value, 64, TextField.ANY);
        }
    }
    public void showCaptcha(XmlNode baseNode) {
        final String S_CAPTCHA = "c" + "aptcha";
        XmlNode captcha = baseNode.getFirstNode(S_CAPTCHA);
        id = baseNode.getAttribute("i" + "d");
        loadFromXml(captcha, baseNode);
        form.show();
    }
    
    public void loadFromXml(XmlNode xml, XmlNode baseXml) {
        clearForm();
        XmlNode xmlForm = xml.getFirstNode("x");
        isXData = (null != xmlForm);

        if (isXData) {
            addInfo(xmlForm.getFirstNodeValue("ti" + "tle"),
                    xmlForm.getFirstNodeValue("instruct" + "ions"));
            for (int i = 0; i < xmlForm.childrenCount(); i++) {
                XmlNode item = xmlForm.childAt(i);
                if (item.name.equals("fie" + "ld")) {
                    String type = item.getAttribute("ty" + "pe");
                    addField(item.getAttribute("var"),
                            type,
                            item.getAttribute("la" + "bel"),
                            item.getFirstNodeValue("va" + "lue"));
                    if ((null == type) || "".equals(type)) {
                        String bs64img = baseXml.getFirstNodeValue("d" + "ata");
                        if (null != bs64img) {
                            byte[] imgBytes = Util.base64decode(bs64img);
                            bs64img = null;
                            form.addImage(Image.createImage(imgBytes, 0, imgBytes.length));
                        }
                    }
                }
            }
            return;
        }
        addInfo(xml.getFirstNodeValue("ti" + "tle"),
                xml.getFirstNodeValue("instruct" + "ions"));
        final String S_EMAIL = "emai" + "l";
        final String S_USERNAME = "u" + "sername";
        final String S_PASSWORD = "p" + "assword";
        final String S_KEY = "k" + "e" + "y";
        
        for (int i = 0; i < xml.childrenCount(); i++) {
            XmlNode item = xml.childAt(i);
            if (item.name.equals(S_EMAIL)) {
                addField(S_EMAIL, S_TEXT_SINGLE, "e-mail", "");
                
            } else if (item.name.equals(S_USERNAME)) {
                addField(S_USERNAME, S_TEXT_SINGLE, "nick", "");
                
            } else if (item.name.equals(S_PASSWORD)) {
                addField(S_PASSWORD, S_TEXT_PRIVATE, "password", "");
                
            } else if (item.name.equals(S_KEY)) {
                addField(S_KEY, S_HIDDEN, "", "");
            }
        }
    }
    private static final String S_TEXT_SINGLE = "text-single";
    private static final String S_TEXT_PRIVATE = "text-private";
    private static final String S_HIDDEN = "hid" + "den";
    private static final String S_BOOLEAN = "bo" + "olean";
    private static final String S_FIXED = "f" + "ixed";
    private static final String S_TEXT_MULTI = "text-multi";

    protected String getXmlForm() {
        for (int i = 0; i < fields.size(); i++) {
            String type = (String)types.elementAt(i);
            if (type.startsWith("text-")) {
                values.setElementAt(form.getTextFieldValue(i), i);
            } else if (S_BOOLEAN.equals(type)) {
                values.setElementAt(form.getCheckBoxValue(i) ? "1" : "0", i);
            } else if ("".equals(type)) {
                values.setElementAt(form.getTextFieldValue(i), i);
            }
        }
        StringBuffer sb = new StringBuffer();
        if (!isXData) {
            for (int i = 0; i < fields.size(); i++) {
                sb.append("<").append((String)fields.elementAt(i)).append(">");
                sb.append(Util.xmlEscape((String)values.elementAt(i)));
                sb.append("</").append((String)fields.elementAt(i)).append(">");
            }
            return sb.toString();
        }
        sb.append("<x xmlns='jabber:x:data' type='submit'>");
        for (int i = 0; i < fields.size(); i++) {
            sb.append("<field type='");
            sb.append(Util.xmlEscape((String)types.elementAt(i)));
            sb.append("' var='");
            sb.append(Util.xmlEscape((String)fields.elementAt(i)));
            sb.append("'><value>");
            sb.append(Util.xmlEscape((String)values.elementAt(i)));
            sb.append("</value></field>");
        }
        sb.append("</x>");
        
        return sb.toString();
    }

    void error(String description) {
        PopupWindow.showShadowPopup("error", description);
    }

    void success() {
        System.out.println("success()");
        ContactList.activate();
    }
}
// #sijapp cond.end #
