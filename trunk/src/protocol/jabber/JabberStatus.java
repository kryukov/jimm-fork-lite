/*
 * JabberStatus.java
 *
 * Created on 20 Июль 2008 г., 20:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import jimm.util.ResourceBundle;
import protocol.Status;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberStatus extends Status {
//    public static final int STATUS_OFFLINE = 0;
//    private static final int STATUS_ONLINE = 1;
//    private static final int STATUS_CHAT = 2;
//    public static final int I_STATUS_AWAY = 3;
    public static final int I_STATUS_XA = 4;
    public static final int I_STATUS_DND = 5;

    /** Creates a new instance of JabberStatus */
    public JabberStatus() {
    }

    private static final String[] statusCodes = {
            "u" + "navailable",
            "o" + "nline",
            "a" + "way",
            "c" + "h" + "a" + "t",
            "x" + "a",
            "d" + "nd"};
    private final int[] statusWidth = {5, 1, 2, 0, 3, 4};
    private static final String[] statusStrings = {
            "status_offline",
            "status_online",
            "status_away",
            "status_chat",
            "status_na",
            "status_dnd"};

    public void setStatusIndex(int index) {
        if ((0 <= index) && (index < statusStrings.length)) {
            statusIndex = (byte)index;
        }
    }

    public String getName() {
        return ResourceBundle.getString(statusStrings[statusIndex]);
    }


    public boolean isAway() {
        return is(I_STATUS_AWAY) || is(I_STATUS_XA) || is(I_STATUS_DND);
    }

    private boolean is(final int s) {
        return s == statusIndex;
    }

    public void setNativeStatus(String rawStatus) {
        for (byte i = 0; i < statusCodes.length; i++) {
            if (statusCodes[i].equals(rawStatus)) {
                statusIndex = i;
                return;
            }
        }
        statusIndex = I_STATUS_ONLINE;
    }
    public String getNativeStatus() {
        return statusCodes[statusIndex];
    }

    public static final JabberStatus offlineStatus = new JabberStatus();
    public static JabberStatus createStatus(JabberStatus prev, String nativeStatus, String text) {
        if (statusCodes[I_STATUS_OFFLINE].equals(nativeStatus)) {
            return offlineStatus;
        }
        JabberStatus result = ((prev == null) || (prev == offlineStatus))
                ? new JabberStatus() : prev;
        result.setNativeStatus(nativeStatus);
        result.text = text;
        return result;
    }
    

    public int getWidth() {
        return statusWidth[statusIndex];
    }    
    private String text;
    public String getText() {
        return text;
    }
}
// #sijapp cond.end #