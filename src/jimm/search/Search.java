/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/Search.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher
 *******************************************************************************/

package jimm.search;

import java.util.Vector;

import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.util.*;
import DrawControls.*;
import protocol.*;
import protocol.jabber.*;

public class Search implements CommandListener, VirtualListCommands, SelectListener {
    final public static int UIN         = 0;
    final public static int NICK        = 1;
    final public static int FIRST_NAME  = 2;
    final public static int LAST_NAME   = 3;
    final public static int EMAIL       = 4;
    final public static int CITY        = 5;
    final public static int KEYWORD     = 6;
    final public static int GENDER      = 7;
    final public static int ONLY_ONLINE = 8;
    final public static int AGE         = 9;
    final public static int LAST_INDEX  = 10;

    /* Forms for results and query */
    private FormEx searchForm;
    
    private TextListEx screen;
    
    /* List for group selection */
    private Select groupList;
    private Group group;
    private Contact contact;
    private boolean waitResults = false;

    /* Results */
    private Vector results = new Vector();

    /* Constructor */
    private Protocol protocol;
    private boolean icqFields;

    public static final byte TYPE_FULL = 0;
    public static final byte TYPE_LITE = 1;
    public static final byte TYPE_NOFORM = 2;
    private byte type;
    public Search(Protocol protocol, byte searchType) {
        type = searchType;
        this.protocol = protocol;

        // #sijapp cond.if protocols_ICQ is "true" #
        icqFields = (protocol instanceof Icq);
        // #sijapp cond.else #
        icqFields = false;
        // #sijapp cond.end #

        createSearchForm();
        /* Result Screen */
        screen = new TextListEx(null);
        screen.setVLCommands(this);
    }
    private final Protocol getProtocol() {
        return protocol;
    }
    public final void putToGroup(Group group) {
        this.group = group;
    }

    /* Add a result to the results vector */
    public void addResult(UserInfo info) {
        results.addElement(info);
    }

    /* Return a result object by given Nr */
    private UserInfo getResult(int nr) {
        return (UserInfo) results.elementAt(nr);
    }
    private int getResultCount() {
        return results.size();
    }

    
    
    public String getSearchParam(int param) {
        return searchParams[param];
    }
    public void setSearchParam(int param, String value) {
        if ((null == value) || (value.length() == 0)) {
            searchParams[param] = null;

        } else {
            searchParams[param] = value;
        }
    }
    public String[] getSearchParams() {
        return searchParams;
    }
    private String[] searchParams = new String[Search.LAST_INDEX];
    
    public void searchUsers() {
        results.removeAllElements();
        waitResults = true;
        showWaitScreen();
        protocol.searchUsers(this);
    }

    public void finished() {
        if (waitResults) {
            activate();
        }
        waitResults = false;
    }
    private void showForm() {
        if (TYPE_NOFORM == type) {
            screen.back();
        } else {
            searchForm.show();
        }
    }
    public void canceled() {
        if (waitResults) {
            showForm();
        }
        waitResults = false;
    }
    public void show() {
        searchForm.show();
    }


    /* Textboxes for search */
    private static final int uinSearchTextBox = 1000;
    private static final int nickSearchTextBox = 1001;
    private static final int firstnameSearchTextBox = 1002;
    private static final int lastnameSearchTextBox = 1003;
    private static final int emailSearchTextBox = 1004;
    private static final int citySearchTextBox = 1005;
    private static final int keywordSearchTextBox = 1006;
    private static final int chgrAge = 1007;
    private static final int gender = 1008;
    private static final int onlyOnline = 1009;
    
    /* Selectet index in result screen */
    private int selectedIndex;
    private static final int MENU_ADD       = 0;
    private static final int MENU_PREV      = 1;
    private static final int MENU_NEXT      = 2;
    private static final int MENU_SEND_MSG  = 3;
    private static final int MENU_SHOW_INFO = 4;
    private static final int MENU_BACK      = 5;

    private static final String ageList = "-|13-17|18-22|23-29|30-39|40-49|50-59|60-";
    private static final String[] ages = Util.explode(ageList, '|');
    
    private void createSearchForm() {
        /* Form */
        if (TYPE_LITE == type) {
            searchForm = new FormEx("add_user", "search_user", "back", this);
        } else {
            searchForm = new FormEx("search_user", "search_user", "back", this);
        }
        if (TYPE_FULL == type) {
            searchForm.addChoiceGroup(onlyOnline, null, Choice.MULTIPLE);
            searchForm.addChoiceItem(onlyOnline, "only_online", false);
        }
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            searchForm.addTextField(uinSearchTextBox, "e-mail", "", 64, TextField.EMAILADDR);
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            searchForm.addTextField(uinSearchTextBox, "uin", "", 10, TextField.NUMERIC);
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (protocol instanceof Jabber) {
            searchForm.addTextField(uinSearchTextBox, "jid", "", 64, TextField.ANY);
        }
        // #sijapp cond.end #
        if (TYPE_LITE == type) {
            return;
        }
        searchForm.addTextField(nickSearchTextBox, "nick", "", 64, TextField.ANY);
        searchForm.addTextField(firstnameSearchTextBox, "firstname", "", 64, TextField.ANY);
        searchForm.addTextField(lastnameSearchTextBox, "lastname", "", 64, TextField.ANY);
        searchForm.addTextField(citySearchTextBox, "city", "", 64, TextField.ANY);
        searchForm.addSelector(gender, "gender", "female_male" + "|" + "female" + "|" + "male", 0);
        if (icqFields) {
            searchForm.addTextField(emailSearchTextBox, "email", "", 64, TextField.EMAILADDR);
            searchForm.addTextField(keywordSearchTextBox, "keyword", "", 64, TextField.ANY);
        }
        searchForm.addSelector(chgrAge, "age", ageList, 0);
    }
    
    /* Activate search form */
    private void activate() {
        drawResultScreen(Math.min(selectedIndex, getResultCount()));
        screen.show();
    }
    
    private Select menu = new Select();
    private void showWaitScreen() {
        screen.lock();
        screen.clear();
        screen.setCaption(ResourceBundle.getString("search_user"));
        screen.addBigText(ResourceBundle.getString("wait"), CanvasEx.THEME_TEXT, Font.STYLE_BOLD, -1);
        screen.unlock();
        menu.clean();
        menu.add("back", MENU_BACK);
        menu.setActionListener(this);
        screen.setMenu(menu, MENU_BACK, MENU_ADD);
        screen.show();
    }
    private void drawResultScreen(int n) {
        int resultCount = getResultCount();

        if (0 < resultCount) {
            screen.setCaption(ResourceBundle.getString("results") + " " + (n + 1) + "/" + resultCount);
            UserInfo userInfo = getResult(n);
            userInfo.setProfileView(screen);
            userInfo.updateProfileView();

        } else {
            /* Show a result entry */
            screen.lock();
            screen.clear();
            screen.setCaption(ResourceBundle.getString("results") + " 0/0");
            screen.addBigText(ResourceBundle.getString("no_results"), CanvasEx.THEME_TEXT, Font.STYLE_BOLD, -1);
            screen.unlock();
        }

        menu.clean();
        if (0 < resultCount) {
            menu.add("add_to_list",  MENU_ADD);
            menu.add("send_message", MENU_SEND_MSG);
            menu.add("info",         MENU_SHOW_INFO);
        }
        if (1 < resultCount) {
            menu.add("prev", MENU_PREV);
            menu.add("next", MENU_NEXT);
        }
        menu.add("back", MENU_BACK);
        menu.setActionListener(this);
        screen.setMenu(menu, MENU_BACK, MENU_ADD);
    }
    
    private void nextOrPrev(boolean next) {
        int size = getResultCount();
        selectedIndex = ((next ? 1 : size - 1) + selectedIndex) % size;
        activate();
    }
    
    public boolean onKeyPress(VirtualList sender, int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_LEFT:
                nextOrPrev(false);
                return true;
                
            case NativeCanvas.NAVIKEY_RIGHT:
                nextOrPrev(true);
                return true;
        }
        return false;
    }
    
    public void onCursorMove(VirtualList sender) {
    }
    
    public void onItemSelected(VirtualList sender) {
    }
    
    public void commandAction(Command c, Displayable d) {
        if (searchForm.backCommand == c) {
            ContactList.activate();

        } else if (searchForm.saveCommand == c) {
            selectedIndex = 0;
            
            setSearchParam(Search.UIN, searchForm.getTextFieldValue(uinSearchTextBox));
            if (TYPE_FULL == type) {
                setSearchParam(Search.NICK,        searchForm.getTextFieldValue(nickSearchTextBox));
                setSearchParam(Search.FIRST_NAME,  searchForm.getTextFieldValue(firstnameSearchTextBox));
                setSearchParam(Search.LAST_NAME,   searchForm.getTextFieldValue(lastnameSearchTextBox));
                setSearchParam(Search.CITY,        searchForm.getTextFieldValue(citySearchTextBox));
                setSearchParam(Search.GENDER,      Integer.toString(searchForm.getSelectorValue(gender)));
                setSearchParam(Search.ONLY_ONLINE, searchForm.getChoiceItemValue(onlyOnline, 0) ? "1" : "0");
                setSearchParam(Search.AGE,         ages[searchForm.getSelectorValue(chgrAge)]);
                if (icqFields) {
                    setSearchParam(Search.EMAIL, searchForm.getTextFieldValue(emailSearchTextBox));
                    setSearchParam(Search.KEYWORD, searchForm.getTextFieldValue(keywordSearchTextBox));
                }
            }
            searchUsers();
        }
    }
    
    public void selectGroup(Contact contact) {
        this.contact = contact;
        if (null == group) {
            Vector groups = getProtocol().getGroupItems();
            if (groups.size() == 0) {
                new PopupWindow("warning", JimmException.getErrDesc(161, 0)).show();

            } else {
                /* Show list of groups to select which group to add to */
                groupList = new Select();
                for (int i = 0; i < groups.size(); i++) {
                    Group g = (Group)groups.elementAt(i);
                    if (g.hasMode(Group.MODE_NEW_CONTACTS)) {
                        groupList.addRaw(g.getName(), null, g.getId());
                    }
                }
                groupList.setActionListener(this);
                groupList.show();
            }
        } else {
            addContact();
        }
    }
    private Contact getCurrentContact() {
        UserInfo resultData = getResult(selectedIndex);
        final String uin = resultData.uin;
        Contact contact = getProtocol().getItemByUIN(uin);
        if (null == contact) {
            contact = getProtocol().createTempContact(uin);
            getProtocol().addTempContact(contact);
            contact.setOfflineStatus();
        }
        contact.setOptimalName(resultData);
        if (contact.isTemp()) {
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, resultData.auth);
        }
        return contact;
    }
    private void addContact() {
        if (contact.isTemp()) {
            contact.setGroup(group);
            getProtocol().addContact(contact);
            getProtocol().getContactList().activate();
        } else {
            getProtocol().getContactList().activate();
        }
    }
    public void select(Select select, int cmd) {
        if (select == groupList) {
            group = getProtocol().getGroupById(cmd);
            addContact();
            return;
        }
        UserInfo resultData;
        Contact cItem;
        switch (cmd) {
            case MENU_ADD:
                selectGroup(getCurrentContact());
                break;
                
            case MENU_PREV:
            case MENU_NEXT:
                nextOrPrev(MENU_NEXT == cmd);
                break;
                
            case MENU_SEND_MSG:
                resultData = getResult(selectedIndex);
                cItem = getProtocol().createTempContact(resultData.uin);
                cItem.setOptimalName(resultData);
                getProtocol().addTempContact(cItem);
                cItem.writeMessage(null);
                break;
                
            case MENU_SHOW_INFO:
                resultData = getResult(selectedIndex);
                cItem = getProtocol().createTempContact(resultData.uin);
                cItem.setOptimalName(resultData);
                cItem.showUserInfo();
                break;
                
            case MENU_BACK:
                showForm();
                break;
        }
    }
}
