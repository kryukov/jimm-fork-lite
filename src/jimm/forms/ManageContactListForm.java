/*
 * ManageContactListForm.java
 *
 * Created on 10 Июнь 2007 г., 21:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.search.*;
import jimm.ui.*;
import jimm.util.*;
import protocol.Group;
import protocol.Protocol;

/**
 *
 * @author vladimir
 */
public class ManageContactListForm implements CommandListener, SelectListener {
    private static final int ADD_USER     = 1;
    private static final int SEARCH_USER  = 2;
    private static final int ADD_GROUP    = 3;

    private InputTextBox box = new InputTextBox(null, 32);
    {
        box.setOkCommandCaption("send");
    }

    public static final int G_ADD_GROUP    = 13;
    public static final int G_RENAME_GROUP = 14;
    public static final int G_DEL_GROUP    = 15;
    private Group g;
    private Select manageCL;
	/* Show form for adding user */
	private void showTextBox(String caption, String text) {
        box.setCaption(ResourceBundle.getString(caption));
        box.setString(text);
		box.setCommandListener(this);
		box.show();
	}
    public Select getMenu() {
        return manageCL;
    }

    private InputTextBox groupName =  new InputTextBox("group_name", 16);
    private Protocol protocol;

    /** Creates a new instance of ManageContactListForm */
    public ManageContactListForm(Protocol protocol, Group g) {
        this.g = g;
        manageCL = new Select();
        if (!protocol.isConnected()) {
            return;
        }
        manageCL.clean();
        if (g.hasMode(Group.MODE_NEW_CONTACTS)) {
            manageCL.add("add_user",     ResourceBundle.FLAG_ELLIPSIS, null, ADD_USER);
            manageCL.add("search_user",  ResourceBundle.FLAG_ELLIPSIS, null, SEARCH_USER);
        }
        manageCL.add("add_group",    ResourceBundle.FLAG_ELLIPSIS, null, ADD_GROUP);
        if (g.hasMode(Group.MODE_EDITABLE)) {
            manageCL.add("rename_group", ResourceBundle.FLAG_ELLIPSIS, null, G_RENAME_GROUP);
        }
        if ((g.getContacts().size() == 0) && g.hasMode(Group.MODE_REMOVABLE)) {
            manageCL.add("del_group",    ResourceBundle.FLAG_ELLIPSIS, null, G_DEL_GROUP);
        }
        manageCL.setSelectedItemCode(ADD_USER);
        manageCL.setActionListener(this);
    }
    /** Creates a new instance of ManageContactListForm */
    public ManageContactListForm(Protocol protocol) {
        manageCL = new Select();
        manageCL.add("add_user",    ResourceBundle.FLAG_ELLIPSIS, null, ADD_USER);
        manageCL.add("search_user", ResourceBundle.FLAG_ELLIPSIS, null, SEARCH_USER);
        manageCL.add("add_group",   ResourceBundle.FLAG_ELLIPSIS, null, ADD_GROUP);
        manageCL.setActionListener(this);
        groupName.setCommandListener(this);
        this.protocol = protocol;
        
        groupName.setOkCommandCaption("ok");
    }
    
    public void select(Select select, int cmd) {
        switch (cmd) {
            case ADD_USER: /* Add user */
                Search search = new Search(protocol, Search.TYPE_LITE);
                if (null != g) {
                    search.putToGroup(g);
                }
                search.show();
                break;

            case SEARCH_USER: /* Search for User */
                Search searchUser = new Search(protocol, Search.TYPE_FULL);
                if (null != g) {
                    searchUser.putToGroup(g);
                }
                searchUser.show();
                break;

            case G_RENAME_GROUP: /* Rename group */
                showTextBox("rename_group", g.getName());
                break;

            case G_DEL_GROUP: /* Delete group */
                protocol.removeGroup(g);
                protocol.getContactList().activate();
                break;

            case ADD_GROUP:
                groupName.show();
                break;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == box.getOkCommand()) {
            /* Return to contact list */
            String groupName = box.getString();
            boolean isExist = null != protocol.getGroup(groupName);
            boolean isMyName = g.getName().equals(groupName);
            if (0 == groupName.length()) {
                protocol.getContactList().activate();
                return;
            }
            switch (manageCL.getSelectedItemCode()) {
                case G_RENAME_GROUP:
                    if (isMyName) {
                        protocol.getContactList().activate();

                    } else if (!isExist) {
                        protocol.renameGroup(g, groupName);
                        protocol.getContactList().activate();
                    }
                break;
            }
            return;
        }
        if (groupName.getOkCommand() != c) {
            return;
        }
        String nameOfGroup = groupName.getString();
        if (0 == nameOfGroup.length()) {
            protocol.getContactList().activate();
            return;
        }
        if (null != protocol.getGroup(nameOfGroup)) {
            return;
        }
        protocol.addGroup(protocol.createGroup(nameOfGroup));
        protocol.getContactList().activate();
    }
    public void show() {
        manageCL.show();
    }
}
