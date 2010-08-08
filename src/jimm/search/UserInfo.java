/*
 * UserInfo.java
 *
 * Created on 25 Март 2008 г., 19:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.search;

import javax.microedition.lcdui.Font;
import jimm.comm.Util;
import protocol.Contact;
import jimm.ui.CanvasEx;
import jimm.ui.NativeCanvas;
import jimm.ui.Select;
import jimm.ui.SelectListener;
import jimm.ui.TextListEx;
import jimm.util.ResourceBundle;
import protocol.Protocol;

/**
 *
 * @author vladimir
 */
public class UserInfo implements SelectListener {
    private final Protocol protocol;
    private TextListEx profileView;
    
    /** Creates a new instance of UserInfo */
    public UserInfo(Protocol prot) {
        protocol = prot;
    }
    public void setProfileView(TextListEx view) {
        profileView = view;
    }
    public TextListEx getProfileView() {
        return profileView;
    }

    
    private static final int INFO_MENU_COPY     = 1040;
    private static final int INFO_MENU_COPY_ALL = 1041;
    private static final int INFO_MENU_BACK     = 1042;
    private static final int INFO_MENU_AVATAR   = 1043;
    private static final int INFO_MENU_EDIT     = 1044;
    
    public void setOptimalName() {
        Contact contact = protocol.getItemByUIN(uin);
        if (null != contact) {
            contact.setOptimalName(this);
        }
    }
    public synchronized void updateProfileView() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((null == profileView)) {
            jimm.modules.DebugLog.panic("profileView is null");
            return;
        }
        // #sijapp cond.end#
        profileView.lock();
        profileView.clear();
        
        profileView.setHeader("main_info");
        profileView.add(protocol.getUinName(),    uin);
        profileView.add("nick",   nick);
        profileView.add("name", getName());
        profileView.add("gender", getGenderAsString());
        profileView.add("age",    (0 == age) ? null : Integer.toString(age));
        profileView.add("email",  email);
        if (auth) {
            profileView.add("auth", ResourceBundle.getString("yes"));
        }
        profileView.add("birth_day",  birthDay);
        profileView.add("cell_phone", cellPhone);
        profileView.add("home_page",  homePage);
        profileView.add("notes",      about);
        profileView.add("interests",  interests);
        
        profileView.setHeader("home_info");
        profileView.add("city",  homeCity);
        profileView.add("state", homeState);
        profileView.add("addr",  homeAddress);
        profileView.add("phone", homePhones);
        profileView.add("fax",   homeFax);
        
        profileView.setHeader("work_info");
        profileView.add("title",    workCompany);
        profileView.add("depart",   workDepartment);
        profileView.add("position", workPosition);
        profileView.add("city",     workCity);
        profileView.add("state",    workState);
        profileView.add("addr",     workAddress);
        profileView.add("phone",    workPhone);
        profileView.add("fax",      workFax);
        
        profileView.unlock();

        //profileView.setCaption(getName());
        Select menu = new Select();
        menu.add("copy_text",     INFO_MENU_COPY);
        menu.add("copy_all_text", INFO_MENU_COPY_ALL);
        if (isEditable()) {
            menu.add("edit",      INFO_MENU_EDIT);
        }
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim && (null != uin)) {
            menu.add("get_avatar",    INFO_MENU_AVATAR);
        }
        // #sijapp cond.end #
        menu.add("back", INFO_MENU_BACK);
        menu.setActionListener(this);
        profileView.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);
    }
    public void setProfileViewToWait() {
        Select menu = new Select();
        menu.add("back", INFO_MENU_BACK);
        profileView.clear();
        profileView.addBigText(ResourceBundle.getString("wait"),
                CanvasEx.THEME_TEXT, Font.STYLE_PLAIN, -1);
        menu.setActionListener(this);
        profileView.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);
    }

    private boolean isEditable() {
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            return protocol.getUin().equals(uin) && protocol.isConnected();
        }
        // #sijapp cond.end #
        return false;
    }
    
    public void select(Select select, int cmd) {
        switch (cmd) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                profileView.copy(INFO_MENU_COPY_ALL == cmd);
                profileView.restore();
                break;

            case INFO_MENU_BACK:
                profileView.back();
                profileView.clear();
                break;

            // #sijapp cond.if protocols_MRIM is "true" #
            case INFO_MENU_AVATAR:
                ((Mrim)protocol).getAvatar(this);
                profileView.restore();
                break;
            // #sijapp cond.end #

            // #sijapp cond.if protocols_ICQ is "true" #
            case INFO_MENU_EDIT:
                jimm.EditInfo.showEditForm((Icq)protocol, this);
                break;
            // #sijapp cond.end #
        }
    }

    public String uin;
    public String nick;
    public String email;
    public String homeCity;
    public String firstName;
    public String lastName;

    public String homeState;
    public String homePhones;
    public String homeFax;
    public String homeAddress;
    public String cellPhone;

    public String homePage;
    public String interests;
    
    public String about;

    public String workCity;
    public String workState;
    public String workPhone;
    public String workFax;
    public String workAddress;
    public String workCompany;
    public String workDepartment;
    public String workPosition;
    public String birthDay;
    
    public int age;
    public byte gender;
    //public String auth;
    public boolean auth; // required
    public String status;

    // Convert gender code to string
    public String getGenderAsString() {
        switch (gender) {
            case 1: return ResourceBundle.getString("female");
            case 2: return ResourceBundle.getString("male");
        }
        return "";
    }

    private String packString(String str) {
        return (null == str) ? "" : str.trim();
    }
    public String getName() {
        return packString(packString(firstName) + " " + packString(lastName));
    }
    public String getOptimalName() {
        String optimalName = packString(nick);
        if (optimalName.length() == 0) {
            optimalName = packString(getName());
        }
        if (optimalName.length() == 0) {
            optimalName = packString(firstName);
        }
        if (optimalName.length() == 0) {
            optimalName = packString(lastName);
        }
        return optimalName;
    }
}
