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
 File: src/jimm/ChatHistory.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

package jimm.chat;

import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.comm.message.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import DrawControls.*;
import protocol.Contact;

public class ChatHistory implements SelectListener {
	protected static final Vector historyTable = new Vector();
	
	final public static int DEL_TYPE_CURRENT        = 1;
	final public static int DEL_TYPE_ALL_EXCEPT_CUR = 2;
	final public static int DEL_TYPE_ALL            = 3;
	
	public static void unregisterChat(Contact item) {
        if (null == item) return;
		historyTable.removeElement(item);
	}

	// Delete the chat history for uin
	public static void chatHistoryDelete(Contact item, int delType) {
		switch (delType) {
		case DEL_TYPE_CURRENT:
			item.deleteChat();
			break;
			
		case DEL_TYPE_ALL_EXCEPT_CUR:
		case DEL_TYPE_ALL:
			for (int i = historyTable.size() - 1; i >= 0; i--) {
				Contact key = (Contact)historyTable.elementAt(i);
				if ((delType == DEL_TYPE_ALL_EXCEPT_CUR) && (key == item)) continue;
				key.deleteChat();
			}
			break;
		}
	}
    
	
	// Creates a new chat form
	public static void registerChat(Contact item) {
        if (!historyTable.contains(item)) {
            historyTable.addElement(item);
        }
	}
    
    public static void restoreContactsWithChat() {
        for (int i = 0; i < historyTable.size(); i++) {
            Contact contact = (Contact)historyTable.elementAt(i);
            if (!contact.getProtocol().inContactList(contact)) {
                contact.setBooleanValue(Contact.CONTACT_IS_TEMP, true);
                contact.getProtocol().addTempContact(contact);
            }
        }
    }
    
    private static Select chatList = null;
    private static int chatCount = 0;
    public static void updateChatList() {
        if (null == chatList) {
            return;
        }
        
        Select chats = chatList;
        int chatCountBefore = chatCount;
        chatCount = historyTable.size();

        for (int i = chatCountBefore; i < chatCount; i++) {
            Contact contact = (Contact)historyTable.elementAt(i);
            chats.addRaw(contact.getName(), null, i);
        }

        for (int i = 0; i < chatCount; i++) {
            Contact contact = (Contact)historyTable.elementAt(i);
            chats.setItemRaw(i, contact.getName(), null);
        }

        chats.update();
    }
    public static void showChatList() {
        if (0 == historyTable.size()) {
            return;
        }
        Select chats = new Select();
        int contactWithChatNum = -1;
        int currentContactNum  = -1;
        Contact currentContact = ContactList.getInstance().getCurrentContact();

        chatCount = historyTable.size();
        for (int i = 0; i < chatCount; i++) {
            Contact contact = (Contact)historyTable.elementAt(i);
            chats.addRaw(contact.getName(), null, i);
            if ((0 > contactWithChatNum) && (contact.getUnreadMessageCount() > 0)) {
                contactWithChatNum = i;
            }
            if (currentContact == contact) {
                currentContactNum = i;
            }
        }
        if (0 <= contactWithChatNum) {
            chats.setSelectedItemCode(contactWithChatNum);

        } else if (0 <= currentContactNum) {
            chats.setSelectedItemCode(currentContactNum);
        }
        chats.setActionListener(new ChatHistory());
        chats.show();
        chatList = chats;
    }
	
	// Sets the counter for the ChatHistory
	public static int calcCounter(Contact contact) {
		return (contact == null) ? 0 : (historyTable.indexOf(contact) + 1);
    }
    // shows next or previos chat 
    public static void showNextPrevChat(Contact item, boolean next) {
        int chatNum = historyTable.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int nextChatNum = (chatNum + (next ? 1 : -1) + historyTable.size())
                % historyTable.size();
        Contact nextContact = (Contact)historyTable.elementAt(nextChatNum);
        if (null != nextContact) {
            nextContact.activate();
        }
    }

    public void select(Select select, int cmd) {
        chatList = null;
        if ((0 <= cmd) && (cmd < historyTable.size())) {
            Contact contact = (Contact)historyTable.elementAt(cmd);
            ContactList.activate();
            contact.activate();
        }
    }
}