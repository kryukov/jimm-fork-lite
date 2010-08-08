/*
 * Status.java
 *
 * Created on 29 Май 2008 г., 17:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

/**
 *
 * @author Vladimir Krukov
 */
abstract public class Status {
    public static final int I_STATUS_OFFLINE = 0;
    public static final int I_STATUS_ONLINE  = 1;
    public static final int I_STATUS_AWAY    = 2;
    public static final int I_STATUS_CHAT    = 3;
//    public static final int I_STATUS_AWAY    = 2;
//    public static final int I_STATUS_XA      = 3;
//    public static final int I_STATUS_DND     = 4;
    
    public abstract String getName();
    public abstract int getWidth();
    public abstract void setStatusIndex(int statusIndex);
    protected byte statusIndex = 0;
    public int getStatusIndex() {
        return statusIndex;
    }

    public abstract boolean isAway();
    public boolean isPassive() {
        return isAway() || (I_STATUS_OFFLINE == statusIndex);
    }
}
