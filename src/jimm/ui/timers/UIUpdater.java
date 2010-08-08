/*
 * UIUpadter.java
 *
 * Created on 22 ���� 2007 �., 23:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.timers;

import DrawControls.VirtualList;
import javax.microedition.lcdui.*;
import java.util.*;
import jimm.*;
import jimm.comm.Util;
import jimm.ui.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public class UIUpdater extends TimerTask {
    private int gcCount = 0;
    private boolean gcNotified = false;
    private boolean gcEnabled = Options.getBoolean(Options.OPTION_CUSTOM_GC);
    private static long time = 0;
    private static final int FLASH_COUNTER = 16;

    private static UIUpdater uiUpdater;
    private static Timer uiTimer;
    public static void startUIUpdater() {
        if (null != uiTimer) {
            uiTimer.cancel();
        }
        uiTimer = new Timer();
        uiUpdater = new UIUpdater();
        uiTimer.schedule(uiUpdater, 0, NativeCanvas.UIUPDATE_TIME);
    }
    
    private Object displ = null;
    private String text = null;
    private int counter;
    
    public static void startFlashCaption(Object disp, String text) {
        if (null == disp) return;
        if (null == text) return;
        Object prevDisplay = uiUpdater.displ;
        if (null != prevDisplay) {
            uiUpdater.displ = null;
            uiUpdater.setTicker(prevDisplay, null);
        }
        uiUpdater.setTicker(disp, text);
        uiUpdater.text  = text;
        uiUpdater.counter = FLASH_COUNTER;
        uiUpdater.flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        uiUpdater.displ = disp;
    }
    private static final int FLASH_CAPTION_INTERVAL = 250;
    private int flashCaptionInterval;
    public void taskFlashCaption() {
        Object curDispay = displ;
        if (null == curDispay) {
            return;
        }
        flashCaptionInterval -= NativeCanvas.UIUPDATE_TIME;
        if (0 < flashCaptionInterval) {
            return;
        }
        flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        if (0 < counter) {
            if (curDispay instanceof VirtualList) {
                setTicker(curDispay, ((counter & 1) == 0) ? text : " ");
            }
            counter--;

        } else {
            setTicker(curDispay, null);
            displ = null;
        }
    }
    private void setTicker(Object displ, String text) {
        if (displ instanceof InputTextBox) {
            ((InputTextBox)displ).setTicker(text);
        } else if (displ instanceof VirtualList) {
            ((VirtualList)displ).setTicker(text);
        }
    }

    
    public void run() {
        // flash caption task
        taskFlashCaption();

        if (!NativeCanvas.getInstance().isShown()) {
            return;
        }

        // UI update task
        try {
            Object d = Jimm.getCurrentDisplay();
            if (d instanceof CanvasEx) {
                try {
                    ((CanvasEx)d).updateTask();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

        // Time update task
        long newTime = System.currentTimeMillis() / 60000;
        if (newTime > time) {
            time = newTime;
            NativeCanvas.refreshClock();
        }
        
        // Memory Control task
        if (gcEnabled) {
            long mem = Runtime.getRuntime().freeMemory();
            final long CRICAL_MEMORY = 20 * 1024;
            final long CRICAL_COUNT = 2;
            if (CRICAL_MEMORY < mem) {
                gcNotified = false;
                gcCount = 0;
            } else if (gcCount < CRICAL_COUNT) {
                System.gc();
                gcCount++;
            } else if (!gcNotified) {
                new jimm.ui.PopupWindow("warning", ResourceBundle.getString("critical_heap_level")).show();
                gcNotified = true;
            }
        }
    }
}
