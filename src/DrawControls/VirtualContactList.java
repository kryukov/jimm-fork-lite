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
 File: src/DrawControls/VirtualTree.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package DrawControls;

import java.util.Vector;
import jimm.ui.GraphicsEx;
import protocol.*;

//! Tree implementation, which allows programmers to store node data themself
/*!
    VirtualContactList is successor of VirtualList. It store tree structure in.
    It shows itself on display and handles user key commands.
    You must inherit new class from VirtualDrawTree and reload next functions:
    \par
    VirtualContactList#getItemDrawData Tree control call this function for request
    of data for tree node to be drawn
 */
public abstract class VirtualContactList extends VirtualList {
    private final TreeBranch root = new TreeRoot();

    private final Vector drawItems = new Vector();

    private int stepSize;

    //! Constructor
    public VirtualContactList(String capt) {
        super(capt);
        stepSize = Math.max(getDefaultFont().getHeight() / 4, 2);
    }

    // private TreeNode getDrawItem(int index)
    private TreeNode getDrawItem(int index) {
        return (TreeNode) drawItems.elementAt(index);
    }

    //! Returns current selected node
    protected final TreeNode getCurrentNode() {
        int cur = getCurrItem();
        if ((cur < 0) || (cur >= drawItems.size())) {
            return null;
        }
        return getDrawItem(cur);
    }

    /**
     * Set node as current. Make autoscroll if needs.
     *
     * Usage:
     * <code>
     * lock();
     * ...
     * setCurrentNode(currentNode);
     * unlock();
     * </code>
     */
    protected final void setCurrentNode(TreeNode node) {
        if (node == null) return;
        currentNode = node;
    }

    /**
     * Build path to node int tree.
     */
    private void expandNodePath(TreeNode node) {
        if (node instanceof Contact) {
            Group group = ((Contact)node).getGroup();
            if (null != group) {
                group.setExpandFlag(true);
            }
        }
    }


    /**
     * Returns root node.
     * Root node is parent for all nodes and never visible.
     */
    protected final TreeBranch getRoot() {
        return root;
    }

    //! Internal function
    /*! Changes node state*/
    protected void itemSelected() {
    }
    
    //! For internal use only
    protected final int getSize() {
        return drawItems.size();
    }

    private synchronized void rebuildFlatItems() {
        if (isLocked()) return;
        drawItems.removeAllElements();
        makeFlatItems(root, false);
    }

    private void makeFlatItems(TreeBranch top, boolean secondLevel) {
        if (top.isExpanded()) {
            int count = top.getSubnodesCount();
            for (int i = 0; i < count; ++i) {
                TreeNode item = top.elementAt(i);
                drawItems.addElement(item);
                item.isSecondLevel = secondLevel;
                if (item instanceof TreeBranch) {
                    makeFlatItems((TreeBranch)item, true);
                }
            }
        }
    }

    // draw + or - before node text
    private int drawNodeRect(GraphicsEx g, TreeNode item,
            int x, int y1, int y2) {

        int height = Math.max((y2 - y1) / 2, 7);
        if (0 == (height & 1)) {
            height--;
        }
        final int result = height + 2;
        if (!(item instanceof TreeBranch)) {
            return result;
        }
        if (((TreeBranch)item).getSubnodesCount() == 0) {
            return result;
        }
        
        final int y = (y1 + y2 - height) / 2;
        final int oldColor = g.getColor();
        
        g.setColor(0x808080);
        g.drawRect(x, y, height - 1, height - 1);
        int my = y + height / 2;
        g.drawLine(x + 2, my, x + height - 3, my);
        if (!item.isExpanded()) {
            int mx = x + height / 2;
            g.drawLine(mx, y + 2, mx, y + height - 3);
        }
        g.setColor(oldColor);
        return result;
    }

    /**
     * Draw a tree node. Called by base class DrawControls.VirtualList 
     */
    protected final void drawItemData(GraphicsEx g, int index,
            int x1, int y1, int x2, int y2) {
        TreeNode node = (TreeNode) drawItems.elementAt(index);
        int x = x1;
        if (node.isSecondLevel) {
            x += stepSize;
             
        } else {
            x += drawNodeRect(g, node, x, y1, y2);
        }
        
        g.setFont(getFontSet()[node.getFontStyle()]);
        g.setThemeColor(node.getTextTheme());
        g.drawString(node.getText(), x, y1, x2 - x, y2 - y1);
    }

    private int itemHeight = 0;
    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    protected final void setItemHeight(int height) {
        itemHeight = height;
    }


    protected final void cleanNode(TreeBranch obj) {
        obj.clear();
    }
    /**
     * Add new node
     * 
     * Method "addNode" insert new item at node root.
     * Function return reference to new node.
     *
     * Usage:
     * <code>
     * lock();
     * TreeNode currentNode = getCurrentNode();
     * ...
     * addNode(base, obj);
     * ...
     * setCurrentNode(currentNode);
     * unlock();
     * </code>
     */
    protected final void addNode(TreeBranch base, TreeNode obj) {
        base.addNode(obj);
    }

    /**
     * Removes node from tree. Returns true if removeing is successful.
     *
     * Usage:
     * <code>
     * lock();
     * TreeNode currentNode = getCurrentNode();
     * ...
     * removeNode(node);
     * ...
     * setCurrentNode(currentNode);
     * unlock();
     * </code>
     */
    protected final void removeNode(TreeNode node) {
        root.removeChild(node);
    }

    /**
     * Expand or collapse tree node. NOTE: this is not recursive operation!
     */
    protected final void setExpandFlag(TreeBranch node, boolean value) {
        lock();
        TreeNode currentNode = getCurrentNode();
        node.setExpandFlag(value);
        setCurrentNode(currentNode);
        unlock();
    }

    /**
     * Remove all nodes from tree
     */
    protected final void clear() {
        root.clear();
        setCurrentItem(0);
    }

    private TreeNode currentNode = null;

    protected void afterUnlock() {
        if (null != currentNode) {
            expandNodePath(currentNode);
        }
        rebuildFlatItems();
        if (null != currentNode) {
            int currentIndex = drawItems.indexOf(currentNode);
            if (-1 != currentIndex) {
                setCurrentItem(currentIndex);
            }
            currentNode = null;
        }
    }
}
