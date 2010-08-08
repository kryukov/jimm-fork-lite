/*
 * TextListEx.java
 *
 * Created on 17 Июнь 2007 г., 21:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import DrawControls.*;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.search.*;
import jimm.ui.timers.*;
import jimm.util.*;

/**
 *
 * @author vladimir
 */
public class TextListEx extends TextList implements SelectListener {
    /** Creates a new instance of TextListEx */
    public TextListEx(String title) {
        super(title);
        setFontSize(Font.SIZE_SMALL);
    }    

    
    public void copy(boolean all) {
        String text = getCurrText(0, all);
        if (null != text) {
            JimmUI.setClipBoardText(getCaption(), text);
        }
    }
    private Select menu;
    protected Select getMenu() {
        return menu;
    }
    private final void setMenu(Select m) {
        this.menu = m;
    }
    public final void setMenu(Select menu, int backCode) {
        setMenu(menu);
        setMenuCodes(backCode, -1);
    }
    public final void setMenu(Select menu, int backCode, int defCode) {
        setMenu(menu);
        setMenuCodes(backCode, defCode);
    }
    

    //////////////////////////////////////////////////////////////////////////////
    //                                                                          //
    // About                                                                    //
    //                                                                          //
    //////////////////////////////////////////////////////////////////////////////
    // String for recent version
    private static final int MENU_UPDATE   = 0;
    private static final int MENU_LAST     = 1;
    private static final int MENU_BACK     = 2;
    
    public void initAbout() {
        System.gc();
        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        
        lock();
        clear();

        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
        setFontSize(Font.SIZE_MEDIUM);
        // #sijapp cond.else#
        setFontSize(Font.SIZE_SMALL);
        // #sijapp cond.end#
        
        setCaption(ResourceBundle.getString("about"));
        
        final String commaAndSpace = ", ";
        
        StringBuffer str = new StringBuffer();
        str.append(" ").append(ResourceBundle.getString("about_info")).append("\n\n");

        str.append(ResourceBundle.getString("midp_info")).append(": ")
                .append(Jimm.microeditionPlatform);
        if (null != Jimm.microeditionProfiles) {
            str.append(commaAndSpace).append(Jimm.microeditionProfiles);
        }
        
        String locale = System.getProperty("microedition.locale");
        if (null != locale) {
            str.append(commaAndSpace).append(locale);
        }

        str.append("\n\n")
                .append(ResourceBundle.getString("free_heap")).append(": ")
                .append(freeMem).append("kb\n")
                .append(ResourceBundle.getString("total_mem")).append(": ")
                .append(Runtime.getRuntime().totalMemory() / 1024).append("kb\n");
        
        str.append('\n');
        
        addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        addBigText(str.toString(), THEME_TEXT, Font.STYLE_PLAIN, -1);
        
        Select menu = new Select();
        menu.add("back",                 MENU_BACK);
        menu.setActionListener(this);
        setMenu(menu, MENU_BACK);
        unlock();
    }

    ///////////////////////////////////////////////////////////////////////////
    private static final int URL_MENU_GOTO = 10;
    private static final int URL_MENU_ADD = 11;
    // #sijapp cond.if target is "SIEMENS2" | target is "MOTOROLA" | target is "MIDP2"#
    public void gotoURL(String text) {
        Vector urls = Util.parseMessageForURL(text);
        if (null == urls) return;

        boolean goUrlNow = (1 == urls.size());
        // #sijapp cond.if protocols_JABBER is "true" #
        goUrlNow = false;
        // #sijapp cond.end #

        if (goUrlNow) {
	    String url = ((String)urls.elementAt(0));
    	    // #sijapp cond.if protocols_JABBER is "true" #
    	    if (url.startsWith("xmpp:")) {
                Search search = new Search(ContactList.getInstance().getProtocol(), Search.TYPE_NOFORM);
                search.setSearchParam(Search.UIN, getUrlWithoutProtocol(url));
                search.searchUsers();
                return;
            }
            // #sijapp cond.end #
            try {
                Jimm.platformRequestUrl(url);
            } catch (Exception e) {
            }

        } else {
            setCaption(ResourceBundle.getString("goto_url"));
            Select menu = new Select();
            menu.add("select", URL_MENU_GOTO);
            // #sijapp cond.if protocols_JABBER is "true" #
            menu.add("add_user", URL_MENU_ADD);
            // #sijapp cond.end #
            menu.add("back",   MENU_BACK);
            menu.setActionListener(this);
            setMenu(menu, MENU_BACK, URL_MENU_GOTO);
            clear();
            for (int i = 0 ; i < urls.size() ; i++) {
                addBigText((String)urls.elementAt(i), THEME_TEXT, Font.STYLE_PLAIN, i).doCRLF(i);
            }
            show();
        }
    }
    // #sijapp cond.end#

    ///////////////////////////////////////////////////////////////////////////
    public void select(Select select, int cmd) {
        switch (cmd) {
            case MENU_BACK:
                back();
                clear();
                break;

            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            case URL_MENU_GOTO:
                try {
                    Jimm.platformRequestUrl(getCurrText(0, false));
                } catch (Exception e) {
                }
                break;
            // #sijapp cond.end#

            case URL_MENU_ADD:
                Search search = new Search(ContactList.getInstance().getProtocol(), Search.TYPE_NOFORM);
                search.setSearchParam(Search.UIN, getUrlWithoutProtocol(getCurrText(0, false)));
                search.searchUsers();
                break;
        }
    }
    private String getUrlWithoutProtocol(String url) {
        url = (-1 == url.indexOf(':')) ? url : url.substring(url.indexOf(':') + 1);
        return (url.startsWith("\57\57")) ? url.substring(2) : url;
    }
}