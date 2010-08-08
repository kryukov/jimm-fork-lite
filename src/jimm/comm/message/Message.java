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
 File: src/jimm/comm/Message.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm.comm.message;

import protocol.Contact;
import protocol.Protocol;

public abstract class Message {
    public static final int ICON_SYSREQ = 0;
    public static final int ICON_SYS_OK = 1;
    public static final int ICON_TYPE = 2;
    public static final int ICON_MSG_NONE = 3;
    public static final int ICON_MSG_FROM_SERVER = 4;
    public static final int ICON_MSG_FROM_CLIENT = 5;
    //public static final int ICON_ERROR = 6;

    // Static variables for message type;
    public static final int MESSAGE_TYPE_AUTO     = 0x0000;
    public static final int MESSAGE_TYPE_NORM     = 0x0001;
    public static final int MESSAGE_TYPE_EXTENDED = 0x001a;
    public static final int MESSAGE_TYPE_AWAY     = 0x03e8;
    public static final int MESSAGE_TYPE_OCC      = 0x03e9;
    public static final int MESSAGE_TYPE_NA       = 0x03ea;
    public static final int MESSAGE_TYPE_DND      = 0x03eb;
    public static final int MESSAGE_TYPE_FFC      = 0x03ec;


//    public static final int MESSAGE_TYPE_UNKNOWN  = 0x0000; // Unknown message, only used internally by this plugin
    public static final int MESSAGE_TYPE_PLAIN    = 0x0001; // Plain text (simple) message
//    public static final int MESSAGE_TYPE_CHAT     = 0x0002; // Chat request message
    public static final int MESSAGE_TYPE_FILEREQ  = 0x0003; // File request / file ok message
    public static final int MESSAGE_TYPE_URL      = 0x0004; // URL message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHREQ  = 0x0006; // Authorization request message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHDENY = 0x0007; // Authorization denied message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHOK   = 0x0008; // Authorization given message (empty)
//    public static final int MESSAGE_TYPE_SERVER   = 0x0009; // Message from OSCAR server (0xFE formatted)
    public static final int MESSAGE_TYPE_ADDED    = 0x000C; // "You-were-added" message (0xFE formatted)
//    public static final int MESSAGE_TYPE_WWP      = 0x000D; // Web pager message (0xFE formatted)
//    public static final int MESSAGE_TYPE_EEXPRESS = 0x000E; // Email express message (0xFE formatted)
//    public static final int MESSAGE_TYPE_CONTACTS = 0x0013; // Contact list message
    public static final int MESSAGE_TYPE_PLUGIN   = 0x001A; // Plugin message described by text string
//    public static final int MESSAGE_TYPE_AWAY     = 0x03E8; // Auto away message
//    public static final int MESSAGE_TYPE_OCC      = 0x03E9; // Auto occupied message
//    public static final int MESSAGE_TYPE_NA       = 0x03EA; // Auto not available message
//    public static final int MESSAGE_TYPE_DND      = 0x03EB; // Auto do not disturb message
//    public static final int MESSAGE_TYPE_FFC      = 0x03EC; // Auto free for chat message

    // Message type
    private int messageType;
    
    
    protected boolean isIncoming;
    protected String contactUin;
    protected Contact contact;
    protected Protocol protocol;
    private String senderName;

    // Date of dispatch
    private long newDate;
    
    protected Message(long date, Protocol protocol, String contactUin, int messageType, boolean isIncoming) {
    	newDate          = date;
    	this.protocol = protocol;
    	this.contactUin = contactUin;
        this.isIncoming = isIncoming;
    	this.messageType = messageType;
    }
    protected Message(long date, Protocol protocol, Contact contact, int messageType, boolean isIncoming) {
    	newDate          = date;
    	this.protocol = protocol;
    	this.contact = contact;
        this.isIncoming = isIncoming;
    	this.messageType = messageType;
    }
    
    public void setName(String name) {
        senderName = name;
    }
    private String getContactUin() {
        return (null == contact) ? contactUin : contact.getUin();
    }
    // Returns the senders UIN
    public String getSndrUin() {
        return isIncoming ? getContactUin() : protocol.getUin();
    }

    // Returns the receivers UIN
    public String getRcvrUin() {
        return isIncoming ? protocol.getUin() : getContactUin();
    }
    public boolean isIncoming() {
        return isIncoming;
    }
    
    // Returns the message type
    public int getMessageType() {
        return this.messageType;
    }

    // Returns the receiver
    public Contact getRcvr() {
        return (null == contact) ? protocol.getItemByUIN(contactUin) : contact;
    }

    public boolean isOffline() {
    	return false;
    }
    
    public long getNewDate() {
    	return newDate;
    }
    
    public String getName() {
        if (null == senderName) {
            Contact c = getRcvr();
            if (isIncoming) {
                senderName = (null == c) ? getContactUin() : c.getName();
            } else {
                senderName = (null == c) ? protocol.getNick() : c.getMyName();
            }
        }
        return senderName;
    }
    
    public abstract String getText();
}