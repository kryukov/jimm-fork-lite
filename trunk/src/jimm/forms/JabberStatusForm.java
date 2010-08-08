/*
 * StatusForm.java
 *
 * Created on 10 Июнь 2007 г., 13:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package jimm.forms;

import DrawControls.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import protocol.jabber.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author vladimir
 */
public class JabberStatusForm extends Select implements SelectListener {
    private static final int[] statuses = {
        JabberStatus.I_STATUS_ONLINE,
        JabberStatus.I_STATUS_CHAT,
        JabberStatus.I_STATUS_AWAY,
        JabberStatus.I_STATUS_XA,
        JabberStatus.I_STATUS_DND};
    
    private Jabber protocol;
    
    /** Creates a new instance of StatusForm */
    public JabberStatusForm(Jabber protocol) {
        this.protocol = protocol;
        
        JabberStatus status = new JabberStatus();
        for (int i = 0; i < statuses.length; i++) {
            status.setStatusIndex(statuses[i]);
            add(status.getName(), null, status.getStatusIndex());
        }
        setSelectedItemCode((int)Options.getLong(Options.OPTION_ONLINE_STATUS));
        setActionListener(this);
    }
    
    public void select(Select select, int statusIndex) {
        setStatus(statusIndex, null);
        ContactList.updateMainMenu();
        back();
    }
    private void setStatus(int statusIndex, String str) {
        protocol.setOnlineStatus(statusIndex, str);
    }
}
// #sijapp cond.end #
