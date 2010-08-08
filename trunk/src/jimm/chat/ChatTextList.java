/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/ChatHistory.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

/*
 * ChatTextList.java
 *
 * Created on 19 Апрель 2007 г., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.chat;

import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.comm.message.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import DrawControls.*;
import protocol.Contact;

public final class ChatTextList extends TextList implements SelectListener {
    
    private Contact contact;
    private boolean writable = true;
    private Vector messData = new Vector();
    private int messTotalCounter = 0;
    
    public void setWritable(boolean wr) {
        writable = wr;
    }
    
    public ChatTextList(Contact item) {
        super(null);
        contact = item;
        
        setFontSize(Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT)
        ? TextList.SMALL_FONT : TextList.MEDIUM_FONT);
        setMenuCodes(Contact.USER_MENU_SHOW_CL, -1);
    }
    public static boolean isHighlight(String text, String nick) {
        for (int index = text.indexOf(nick); -1 != index; index = text.indexOf(nick, index + 1)) {
            if (0 < index) {
                char before = text.charAt(index - 1);
                if ((' ' != before) && ('\n' != before) && ('\t' != before)) {
                    continue;
                }
            }
            if (index + nick.length() + 2 < text.length()) {
                // Calculate space char...
                // ' a': min(' ', 'a') is ' '
                // 'a ': min('a', ' ') is ' '
                char after = (char)Math.min(text.charAt(index + nick.length()),
                        text.charAt(index + nick.length() + 1));
                if ((' ' != after) && ('\n' != after) && ('\t' != after)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    
    
    static byte getInOutColor(boolean incoming) {
        return incoming ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG;
    }
    
    public void doKeyReaction(int keyCode, int actionCode, int type) {
        if (type == CanvasEx.KEY_PRESSED) {
            if (!contact.isSingleUserContact()
            && (NativeCanvas.NAVIKEY_FIRE == actionCode) && ('5' == keyCode)) {
                getMenu().go(MENU_REPLY);
            }
            switch(keyCode) {
                case NativeCanvas.CALL_KEY:
                    actionCode = 0;
                    break;
                    
                case NativeCanvas.CLEAR_KEY:
                    contact.doAction(Contact.USER_MENU_DEL_CHAT);
                    return;
            }
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_FIRE:
                    writeMessage(null);
                    return;
                    
                case NativeCanvas.NAVIKEY_LEFT:
                case NativeCanvas.NAVIKEY_RIGHT:
                    ChatHistory.showNextPrevChat(contact, actionCode == NativeCanvas.NAVIKEY_RIGHT);
                    return;
            }
        }
        if (!JimmUI.execHotKey(keyCode, type)) {
            super.doKeyReaction(keyCode, actionCode, type);
        }
    }
    
    
    
    private static Select menu = new Select();
    private static int MENU_REPLY = 999;
    public Select getMenu() {
        menu.clean();
        if (isMessageAvailable(MESSAGE_AUTH_REQUEST)) {
            menu.add("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.add("deny",  Contact.USER_MENU_DENY_AUTH);
        }
        
        if (contact.isSingleUserContact()) {
            menu.add("reply",     Contact.USER_MENU_MESSAGE);
        } else {
            if (writable) {
                menu.add("reply",     MENU_REPLY);
                menu.add("message",   Contact.USER_MENU_MESSAGE);
            }
            menu.add("list_of_users", Contact.USER_MENU_MULTIUSERS_LIST);
        }
        menu.add("copy_text", Contact.USER_MENU_COPY_TEXT);
        if (!JimmUI.clipBoardIsEmpty() && writable) {
            menu.add("paste", Contact.USER_MENU_PASTE);
            menu.add("quote", Contact.USER_MENU_QUOTE);
        }
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        MessData md = getCurrentMsgData();
        if (null != md && md.isURL()) {
            menu.add("goto_url", Contact.USER_MENU_GOTO_URL);
        }
        // #sijapp cond.end#
        
        menu.add("user_menu",   Contact.USER_MENU_SHOW);
        menu.add("delete_chat", Contact.USER_MENU_DEL_CHAT);
        menu.add("close",       Contact.USER_MENU_SHOW_CL);
        menu.setActionListener(this);
        return menu;
    }
    
    private void writeMessage(String initText) {
        if (writable) {
            contact.writeMessage(initText);
        }
    }
    public static final String ADDRESS = Jimm.getAppProperty("Jimm-Address", ",");
    public void select(Select select, int cmd) {
        if (!writable && ((MENU_REPLY == cmd) || (Contact.USER_MENU_MESSAGE == cmd))) {
            return;
        }
        if (MENU_REPLY == cmd) {
            MessData md = getCurrentMsgData();
            String to = (md == null) ? null : md.getNick();
            writeMessage((null == to) ? "" : (to + ADDRESS + " "));
            return;
        }
        contact.select(select, cmd);
    }
    
    
    // #sijapp cond.if target isnot "DEFAULT"#
    private boolean typing = false;
    public void beginTyping(boolean type) {
        typing = type;
        invalidate();
    }
    // #sijapp cond.end#
    
    public static final String CMD_WAKEUP = "/wakeup";
    private synchronized void addTextToForm(Message message) {
        String from = message.getName();
        String messageText = message.getText();
        long time = message.getNewDate();
        boolean incoming = message.isIncoming();
        boolean offline = message.isOffline();
        
        lock();
        int lastSize = getSize();
        int texOffset = 0;
        
        
        if (messageText.startsWith(CMD_WAKEUP) && contact.isSingleUserContact()) {
            if (incoming) {
                messageText = "/me " + ResourceBundle.getString("wake_up_you");
            } else {
                messageText = "/me " + ResourceBundle.getString("wake_up");
            }
        }
        if (messageText.startsWith("/me ")) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return;
            }
            addTextWithEmotions("* " + from + " " + messageText,
                    getInOutColor(incoming), Font.STYLE_PLAIN, messTotalCounter);
            doCRLF(messTotalCounter);
            
        } else {
            addBigText(from + " (" + Util.getDateString(time, !offline) + "): ",
                    getInOutColor(incoming), Font.STYLE_BOLD, messTotalCounter);
            doCRLF(messTotalCounter);
            texOffset = getSize() - lastSize;
            
            // add message
            byte color = THEME_TEXT;
            // #sijapp cond.if protocols_JABBER is "true" #
            if (incoming && !contact.isSingleUserContact()
            && (-1 != messageText.indexOf(contact.getMyName()))) {
                color = GraphicsEx.THEME_CHAT_HIGHLITE_MSG;
            }
            // #sijapp cond.end#
            addTextWithEmotions(messageText, color, Font.STYLE_PLAIN, messTotalCounter);
            doCRLF(messTotalCounter);
            // end add message
            
        }
        
        boolean contains_url = false;
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        contains_url = Util.hasURL(messageText);
        // #sijapp cond.end#
        messData.addElement(new MessData(incoming, time, texOffset, from, contains_url));
        messTotalCounter++;
        
        int messIndex = getCurrTextIndex();
        if (!incoming || ((messTotalCounter - 2 <= messIndex)
        && (contact.isSingleUserContact() || isVisibleChat()))) {
            setCurrentItem(getSize() - 1);
            setCurrentItem(lastSize);
        }
        removeOldMessages();
        unlock();
    }
    
    public void activate() {
        show();
    }
    
    protected void restoring() {
        NativeCanvas.setCommands("menu", "reply", "close");
        resetUnreadMessages();
        updateCaption();
    }
    
    private int historySize = 0;
    
    private void updateCaption() {
        int counter = ChatHistory.calcCounter(contact);
        int total   = ChatHistory.historyTable.size();
        // Calculate the title for the chatdisplay.
        String title = "[" + counter + "/" + total + "] " + contact.getName();
        setCaption(title);
    }
    public MessData getCurrentMsgData() {
        try {
            int messIndex = getCurrTextIndex();
            if (messIndex == -1) return null;
            int startIndex = Math.max(0, getTextIndex(0));
            return (MessData)messData.elementAt(messIndex - startIndex);
        } catch (Exception e) {
            return null;
        }
    }
    public String getCurrentMessage() {
        MessData md = getCurrentMsgData();
        return (null == md) ? "" : getCurrText(md.getOffset(), false);
    }
    public void clear() {
        super.clear();
        messData.removeAllElements();
        messTotalCounter = 0;
    }
    private void removeOldMessages() {
        final int maxHistorySize = 100;
        while (maxHistorySize < messData.size()) {
            messData.removeElementAt(0);
            removeFirstText();
        }
    }
    
    public void copyText() {
        MessData md = getCurrentMsgData();
        if (null == md) return;
        String msg = getCurrText(md.getOffset(), false);
        JimmUI.setClipBoardText(md.isIncoming(),
                Util.getDateString(md.getTime(), false),
                md.getNick(),
                msg);
    }
    
    public boolean empty() {
        return (0 == messData.size()) && (0 == historySize);
    }
    
    public long getLastMessageTime() {
        if (0 == messData.size()) return 0;
        MessData md = (MessData)messData.elementAt(messData.size() - 1);
        return md.getTime();
    }
    
    // Adds a message to the message display
    public void addMessage(Message message) {
        final long time = message.getNewDate();
        int type = -1;
        if (message instanceof PlainMessage) {
            type = MESSAGE_PLAIN;
            
            addTextToForm(message);
            
        } else if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            type = MESSAGE_SYS_NOTICE;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                type = MESSAGE_AUTH_REQUEST;
            }
            
            addTextToForm(message);
        }
        if (!isVisibleChat()) {
            increaseMessageCount(type);
        }
    }
    private boolean isVisibleChat() {
        return (this == Jimm.getCurrentDisplay()) && !Jimm.isPaused();
    }
    
    /* Increases the mesage count */
    private void increaseMessageCount(int type) {
        switch (type) {
            case MESSAGE_PLAIN:        plainMessageCounter++; return;
            case MESSAGE_SYS_NOTICE:   sysNoticeCounter++;    return;
            case MESSAGE_AUTH_REQUEST: authRequestCounter++;  return;
        }
    }
    /* Message types */
    public static final int MESSAGE_PLAIN        = 1;
    public static final int MESSAGE_SYS_NOTICE   = 2;
    public static final int MESSAGE_AUTH_REQUEST = 3;
    private byte plainMessageCounter = 0;
    private byte sysNoticeCounter = 0;
    private byte authRequestCounter = 0;
    
    public void resetAuthRequests() {
        authRequestCounter = 0;
    }
    private void resetUnreadMessages() {
        plainMessageCounter = 0;
        sysNoticeCounter = 0;
    }
    public int getUnreadMessageCount() {
        return plainMessageCounter + sysNoticeCounter + authRequestCounter;
    }
    /* Returns true if the next available message is a message of given type
       Returns false if no message at all is available, or if the next available
       message is of another type */
    public boolean isMessageAvailable(int type) {
        switch (type) {
            case MESSAGE_PLAIN:        return plainMessageCounter > 0;
            case MESSAGE_SYS_NOTICE:   return sysNoticeCounter > 0;
            case MESSAGE_AUTH_REQUEST: return authRequestCounter > 0;
        }
        return false;
    }
    
    
    public void addMyMessage(PlainMessage message) {
        resetUnreadMessages();
        contact.setBooleanValue(Contact.B_AUTOANSWER, false);
        addTextToForm(message);
    }
    
    public Contact getContact() {
        return contact;
    }
}
