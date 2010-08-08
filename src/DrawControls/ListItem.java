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
 File: src/DrawControls/ListItem.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package DrawControls;

import javax.microedition.lcdui.*;

//! Data for list item
/*! All members of class are made as public 
    in order to easy access. 
 */
public final class ListItem {
	public String text; //!< Text of node

	private short itemWidth = -1;
	private short itemHeigth = -1;

	public byte fontStyle; //!< Font style
	public byte colorType; //!< Color of node text

	ListItem() {
		colorType = 0;
		fontStyle = Font.STYLE_PLAIN;
	}

	ListItem(String text, byte colorType, byte fontStyle) {
		setText(text);
		this.colorType = colorType;
		this.fontStyle = fontStyle;
	}

    public final void setText(String text) {
        // #sijapp cond.if device_configuration is "CLDC-1.1" #
		this.text = text == null ? null : text.intern();
        // #sijapp cond.else#
		this.text = text;
        // #sijapp cond.end#
    }
	//! Set all member to default values
	public void clear() {
		text = "";
		colorType = 0;
		fontStyle = Font.STYLE_PLAIN;
	}

    void calcMetrics(Font[] fontSet) {
        if (null == text) {
            itemHeigth = (short)0;
        
        } else {
            itemHeigth = (short)fontSet[fontStyle].getHeight();
        }

        if (null == text) {
            itemWidth = 0;
        } else {
            itemWidth = (short)fontSet[fontStyle].stringWidth(text);
        }
    }
    int getHeight() {
		return itemHeigth;
	}
	
    int getWidth() {
		return itemWidth;
	}
}