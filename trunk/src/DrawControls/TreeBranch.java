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
 * File: src/DrawControls/TreeNodeL.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Vladimir Kryukov
 *******************************************************************************/
/*
 * TreeNodeL.java
 *
 * Created on 7 Февраль 2008 г., 16:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls;

import java.util.*;
import jimm.comm.StringConvertor;

/**
 *
 * @author vladimir
 */
public abstract class TreeBranch extends TreeNode {
	
    public TreeBranch() {
    }

    private boolean expanded = false;
    public boolean isExpanded() {
        return expanded;
    }
    //! Expand or collapse tree node. NOTE: this is not recursive operation!
    public void setExpandFlag(boolean value) {
        expanded = value;
        if (expanded) sort();
    }
    
    final private Vector subnodes = new Vector();
    public int getSubnodesCount() {
        return subnodes.size();
    }
    public TreeNode elementAt(int index) {
        return (TreeNode)subnodes.elementAt(index);
    }

    synchronized void addNode(TreeNode newItem) {
        if (!subnodes.contains(newItem)) {
            subnodes.addElement(newItem);
        }
        if (expanded) sort();
    }
    synchronized void addNodes(Vector nodes) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            subnodes.addElement(nodes.elementAt(i));
        }
        if (expanded) sort();
    }
    
    void clear() {
        subnodes.removeAllElements();
    }

    synchronized boolean removeChild(TreeNode node) {
        int size = getSubnodesCount();
        if ((size > 0) && subnodes.removeElement(node)) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            TreeNode curNode = elementAt(i);
            if ((curNode instanceof TreeBranch)
                    && ((TreeBranch)curNode).removeChild(node)) {
                return true;
            }
        }
        return false;
    }

    private int compareNodes(TreeNode node1, TreeNode node2) {
        int result = node1.getNodeWeight() - node2.getNodeWeight();
        if (result == 0) {
            result = StringConvertor.stringCompare(node1.getText(), node2.getText());
        } 
        return result;
    }

    private void sort() {
        for (int i = 1; i < subnodes.size(); i++) {
            TreeNode currNode = (TreeNode)subnodes.elementAt(i);
            int j = i - 1;
            for (; j >= 0; j--) {
                TreeNode itemJ = (TreeNode)subnodes.elementAt(j);
                if (compareNodes(itemJ, currNode) <= 0) {
                    break;
                }
                subnodes.setElementAt(itemJ, j + 1);
            }
            if (j + 1 != i) {
                subnodes.setElementAt(currNode, j + 1);
            }
        }
    }
}
