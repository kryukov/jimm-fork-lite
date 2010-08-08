/*
 * SmsForm.java
 *
 * Created on 12 Август 2008 г., 14:41
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import javax.microedition.io.*;
import javax.microedition.lcdui.*;
// #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
import javax.wireless.messaging.*;
// #sijapp cond.end #
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.FormEx;
import jimm.util.ResourceBundle;
import protocol.Protocol;

/**
 *
 * @author Vladimir Kryukov
 */
public class SmsForm implements CommandListener {
    
    /** Creates a new instance of SmsForm */
    public SmsForm(Protocol protocol, String phones) {
        this.phones = phones;
        this.protocol = protocol;
    }
    private String phones;
    private Protocol protocol;
    private FormEx form;

    private static final int PHONE = 0;
    private static final int TEXT = 1;
    private static final int MODE = 2;
    
    private static final int MAX_SMS_LENGTH = 156;
    private static final int MODE_PHONE = 0;
    private static final int MODE_MRIM = 1;
    private static final int MODE_NONE = 2;
    private static final String modes = "phone" + "|" + "mail.ru";
    
    private int defaultMode = MODE_NONE;
    public void show() {
        form = new FormEx("send_sms", "send", "cancel", this);
        if (null == phones) {
            form.addTextField(PHONE, "phone", "", 20, TextField.PHONENUMBER);
        
        } else {
            form.addSelector(PHONE, "phone", phones.replace(',', '|'), 0);
        }

        int choiseCount = 0;
        defaultMode = MODE_NONE;
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "SIEMENS1" #
        defaultMode = MODE_PHONE;
        choiseCount++;
        // #sijapp cond.end #
        // #sijapp cond.if protocols_MRIM is "true" #
        if ((protocol instanceof Mrim) && protocol.isConnected()) {
            defaultMode = MODE_MRIM;
            choiseCount++;
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_DEBUGLOG is "true" #
         if (MODE_NONE == defaultMode) {
            DebugLog.panic("unknown default mode for SMS");
            return;
         }
        // #sijapp cond.end#
        if (1 < choiseCount) {
            form.addSelector(MODE, "send_via", modes, defaultMode);
        } else {
            form.addString("send_via", ResourceBundle.getString(Util.explode(modes, '|')[defaultMode]));
        }
        form.addTextField(TEXT, "message", "", MAX_SMS_LENGTH, TextField.ANY);
        form.show();
    }
    public static final boolean isSupported() {
        // #sijapp cond.if target is "SIEMENS1" #
        return true;
        // #sijapp cond.elseif target is "MIDP2"| target is "SIEMENS2" #
        // #sijapp cond.if modules_FILES="true"#
        return true;
        // #sijapp cond.elseif protocols_MRIM is "true" #
        Protocol protocol = jimm.cl.ContactList.getInstance().getProtocol();
        return (protocol instanceof Mrim) && protocol.isConnected();
        // #sijapp cond.else #
        return false;
        // #sijapp cond.end#
        // #sijapp cond.elseif protocols_MRIM is "true" #
        Protocol protocol = jimm.cl.ContactList.getInstance().getProtocol();
        return (protocol instanceof Mrim) && protocol.isConnected();
        // #sijapp cond.else #
        return false;
        // #sijapp cond.end #
    }

    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "SIEMENS1" #
    private final void sendSms(String phone, String text) {
        // #sijapp cond.if target is "SIEMENS1" #
        try {
            com.siemens.mp.gsm.SMS.send(phone, text);
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
        // #sijapp cond.if modules_FILES="true"#
        try {
            final MessageConnection conn = (MessageConnection)Connector.open("sms://" + phone + ":5151");
            final TextMessage msg = (TextMessage)conn.newMessage(MessageConnection.TEXT_MESSAGE);
            msg.setPayloadText(text);
            conn.send(msg);
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
    }
    // #sijapp cond.end #
    public void commandAction(Command command, Displayable displayable) {
        if (form.saveCommand == command) {
            final String text = form.getTextFieldValue(TEXT);
            final String phone = (null == phones)
                        ? form.getTextFieldValue(PHONE)
                        : form.getSelectorString(PHONE);
            if ((text.length() > 0) && (phone.length() > 0)) {
                int mode = form.hasControl(MODE)
                        ? form.getSelectorValue(MODE) : defaultMode;
                switch (mode) {
                    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "SIEMENS1" #
                    case MODE_PHONE:
                        sendSms(phone, text);
                        break;
                    // #sijapp cond.end #

                    // #sijapp cond.if protocols_MRIM is "true" #
                    case MODE_MRIM:
                        ((Mrim)protocol).sendSms(phone, text);
                        break;
                    // #sijapp cond.end #
                }
            }
            
        }
        form.back();
    }
}
