/*
 * TreeRoot.java
 *
 * Created on 26 Февраль 2010 г., 17:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls;

/**
 *
 * @author Vladimir Krukov
 */
class TreeRoot extends TreeBranch {
    public String getText() {
        return null;
    }
    
    public int getNodeWeight() {
        return 0;
    }
    TreeRoot() {
        setExpandFlag(true);
    }
}
