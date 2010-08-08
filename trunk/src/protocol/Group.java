/*
 * Group.java
 *
 * Created on 14 Май 2008 г., 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.cl.ContactList;
import jimm.comm.message.Message;
import jimm.forms.ManageContactListForm;
import jimm.search.Search;
import jimm.ui.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public abstract class Group extends TreeBranch {
    
    protected Protocol protocol;
    /** Creates a new instance of Group */
    public Group() {
        setMode(Group.MODE_EDITABLE | Group.MODE_REMOVABLE);
    }
    public static final int NOT_IN_GROUP = -1;
        
    // Sets the group item name
    public void setName(String name) {
        this.name = name;
    }
    
    public static final byte MODE_NONE = 0x00;
    public static final byte MODE_REMOVABLE = 0x01;
    public static final byte MODE_EDITABLE  = 0x02;
    public static final byte MODE_NEW_CONTACTS = 0x04;
    public static final byte MODE_FULL_ACCESS  = 0x7F;
    private byte mode;
    public final void setMode(int newMode) {
        mode = (byte)newMode;
    }
    public boolean hasMode(byte type) {
        return (mode & type) != 0;
    }
    
    public int getNodeWeight() {
        if (!hasMode(MODE_EDITABLE)) return -2;
        if (!hasMode(MODE_REMOVABLE)) return -1;
        return -3;
    }
    
    private int groupId;
    public int getId() {
        return groupId;
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    private String caption = null;
    public String getText() {
        return caption;
    }
    
    public byte getTextTheme() {
        return CanvasEx.THEME_GROUP;
    }
    
    public int getFontStyle() {
        return Font.STYLE_PLAIN;
    }
    
    public int getUnreadMessageCount() {
        int count = 0;
        Vector items = getContacts();
        int size = items.size();
        for (int i = 0; i < size; i++) {
            count += ((Contact)items.elementAt(i)).getUnreadMessageCount();
        }
        return count;
    }
    
    private final Vector contacts = new Vector();
    public void updateContacts() {
        Vector items = protocol.getContactItems();
        contacts.removeAllElements();
        int size = items.size();
        for (int i = 0; i < size; i++) {
            Contact item = (Contact)items.elementAt(i);
            if (item.getGroupId() == groupId) {
                contacts.addElement(item);
            }
        }
    }
    public Vector getContacts() {
        return contacts;
    }
    
    // Calculates online/total values for group
    public void updateGroupData() {
        int onlineCount = 0;
        int total = contacts.size();
        for (int i = 0; i < total; ++i) {
            Contact item = (Contact)contacts.elementAt(i);
            if (item.isOnline()) {
                onlineCount++;
            }
        }
        caption = getName();
        if (0 < total) {
            caption += " (" + onlineCount + "/" + total + ")";
        }
    }
    
    public void dismiss() {
        contacts.removeAllElements();
        protocol = null;
    }
    
    public Select getContextMenu() {
        return new ManageContactListForm(protocol, this).getMenu();
    }
}
