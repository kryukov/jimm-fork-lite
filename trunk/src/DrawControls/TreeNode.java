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
 * File: src/DrawControls/TreeNode.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package DrawControls;

import javax.microedition.lcdui.Font;
import jimm.ui.CanvasEx;
import jimm.ui.Select;

//! Tree node
/*! This class is used to handle tree nodes (adding, deleting, moveing...) */
public abstract class TreeNode {
    boolean isSecondLevel;
    protected String name;
    public final String getName() {
        return name;
    }
    public String getText() {
        return name;
    }
    
    public abstract int getNodeWeight();

    public Select getContextMenu() {
        return null;
    }

    public byte getTextTheme() {
        return CanvasEx.THEME_TEXT;
    }

    public int getFontStyle() {
        return Font.STYLE_PLAIN;
    }

    
    //! Store object associated with the node
    public TreeNode() {
    }

    //! Returns true if node if expanded
    boolean isExpanded() {
        return false;
    }
}

