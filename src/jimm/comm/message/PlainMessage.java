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
 File: src/jimm/comm/PlainMessage.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm.comm.message;


import DrawControls.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.util.ResourceBundle;
import protocol.Contact;
import protocol.Protocol;


public class PlainMessage extends Message {
	// Message text
	private String text;

    // unicode message (max len / sizeof char)
    public static final int MESSAGE_LIMIT = 1024;

	// Constructs an incoming message
	public PlainMessage(String contactUin, Protocol protocol, long date, String text, boolean offline) {
		super(date, protocol, contactUin, MESSAGE_TYPE_AUTO, true);
		this.text = text;
		this.offline = offline;
	}

	// Constructs an outgoing message
	public PlainMessage(Protocol protocol, Contact rcvr, int _messageType, long date, String text) {
		super(date, protocol, rcvr, _messageType, false);
		this.text = text;
		this.offline = false;
	}

    private boolean offline;
    public boolean isOffline() {
    	return offline;
    }    
    
    // Returns the message text
	public String getText() {
		return this.text;
	}

    public static final int NOTIFY_OFF = -1;
    public static final int NOTIFY_NONE = ICON_MSG_NONE;
    public static final int NOTIFY_FROM_SERVER = ICON_MSG_FROM_SERVER;
    public static final int NOTIFY_FROM_CLIENT = ICON_MSG_FROM_CLIENT;
}
