/*
 * Select.java
 *
 * Created on 22 Июнь 2007 г., 15:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.modules.*;
import jimm.search.UserInfo;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public class Select extends CanvasEx {
    private static Font menuFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  Font.SIZE_SMALL);
    private static final int BORDER_Y = 1;
    private static final int BORDER_X = 2;
    private static final int MENU_BORDER = 1;
    private static final int MENU_SHADOW = 3;
    private static final int ICON_INTERVAL = 2;
    private static final int WIDTH_SPACE = 6;

    private static final int SLEEP_BEFOR = 2000 / NativeCanvas.UIUPDATE_TIME;
    private static final int SLEEP_AFTER = 1000 / NativeCanvas.UIUPDATE_TIME;
    private static final int STEP = 2;
    private static final int EMPTY_WIDTH = 5 * STEP;
    private static final int UNDEFINED_CODE = -10000;
    public static final int DELIMITER_CODE = -10001;
    
    private int topItem;
    private int selectedItem;
    
    private int selectedItemPosX;
    private int sleep;
    
    private int x;
    private int y;
    private int width;
    private int height;

    private static final class MenuItem {
        public String text;
        public int code;
        public MenuItem(String itemText, int itemCode) {
            text = itemText;
            code = itemCode;
        }
    }
    private Vector items = new Vector();
    private MenuItem itemAt(int i) {
        return (MenuItem)items.elementAt(i);
    }

    private int getItemPerPage() {
        return Math.min(height / itemHeight, items.size());
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    private int getItem(int relativeX, int relativeY) {
        if (relativeX < getItemWidth()) {
            final int size = items.size();
            for (int i = topItem; i < size; i++) {
                if (relativeY < itemHeight) {
                    return i;
                }
                relativeY -= itemHeight;
            }
        }
        return -1;
    }

    private boolean checkRegion(int relativeX, int relativeY) {
        final int size = items.size();
        int itemPerPage = getItemPerPage();
        int curHeight = itemPerPage * itemHeight;
        int curWidth = getItemWidth();
        if (size > itemPerPage) {
            curWidth += scrollerWidth + MENU_BORDER;
        }
        return (relativeX >= 0) && (relativeX < curWidth)
                && (relativeY >= 0) && (relativeY < curHeight);
    }

    protected void stylusMoving(int fromX, int fromY, int toX, int toY) {
        stylusMoved(fromX, fromY, toX, toY);
    }
    protected void stylusMoved(int fromX, int fromY, int toX, int toY) {
        int posX = toX - x - MENU_BORDER;
        int posY = toY - y - MENU_BORDER;
        if (checkRegion(posX, posY)) {
            if (posX < getItemWidth()) {
                int cur = getItem(posX, posY);
                if (cur != -1) {
                    setSelectedItem(cur);
                }
            } else {
                setSelectedItem((items.size() * posY)
                        / (getItemPerPage() * itemHeight));
            }
        }
    }
    
    protected void stylusTap(int x, int y, boolean longTap) {
        int posX = x - this.x - MENU_BORDER;
        int posY = y - this.y - MENU_BORDER;
        if (checkRegion(posX, posY)) {
            if (posX < getItemWidth()) {
                int cur = getItem(posX, posY);
                if (-1 != cur) {
                    setSelectedItem(cur);
                    go(itemAt(cur).code);
                }
            } else {
                setSelectedItem((items.size() * posY) / (items.size() * itemHeight));
            }
        } else {
            back();
        }
    }
    // #sijapp cond.end#

    /**
     * Creates a new instance of Select
     */
    public Select() {
        clean();
    }
/*
    public void setPrefethPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
 */
    protected void restoring() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null == listener) {
            jimm.modules.DebugLog.panic("select listener is null");
            jimm.cl.ContactList.activate();
            return;
        }
        // #sijapp cond.end#
        NativeCanvas.setCommands("select", null, "back");
    }
    
    private int itemHeight;
    private int textWidth;
    private int iconWidth;
    private void calcMetrix() {
        itemHeight = menuFont.getHeight();
        textWidth = 0;
        iconWidth = 0;

        final int size = items.size();
        for (int i = 0; i < size; i++) {
            textWidth = Math.max(textWidth, menuFont.stringWidth(itemAt(i).text));
        }
        itemHeight += BORDER_Y;
    }
    
    private int getItemWidth() {
        int itemWidth = textWidth + iconWidth + ICON_INTERVAL + 2 * BORDER_X;
        return Math.min(itemWidth, NativeCanvas.getScreenWidth()
                - (WIDTH_SPACE + scrollerWidth + MENU_SHADOW + 2 * BORDER_X));
    }

    protected void beforShow() {
        selectedItemPosX = 0;
        sleep = 0;
        calcMetrix();

        width  = Math.min(NativeCanvas.getScreenWidth() - 2 * BORDER_X, getItemWidth());
        height = Math.min(NativeCanvas.getScreenHeight() - (MENU_BORDER + MENU_SHADOW + BORDER_Y),
                itemHeight * items.size());
        
        // x = 1/2
        x = (NativeCanvas.getScreenWidth() - width) / 2 - MENU_BORDER;
        // y = 1/3
        y = (NativeCanvas.getScreenHeight() - height) / 3;
    }

    public void update() {
        beforShow();
        invalidate();
    }

    public final void clean() {
        selectedItem = 0;
        items.removeAllElements();
    }
    
    public void addRaw(String promt, Object icon, int itemCode) {
        items.addElement(new MenuItem(promt, itemCode));
    }
    public void add(String promt, int flags, Object icon, int itemCode) {
        addRaw(ResourceBundle.getString(promt, flags), icon, itemCode);
    }
    public void add(String promt, int itemCode) {
        add(promt, null, itemCode);
    }
    public void add(String promt, Object icon, int itemCode) {
        add(promt, 0, icon, itemCode);
    }

    public void setItemRaw(int itemCode, String promt, Object icon) {
        int i = getIndexByItemCode(itemCode);
        if (i < 0) {
            return;
        }
        itemAt(i).text = promt;
    }
    public void setItem(int itemCode, String promt, Object icon) {
        setItemRaw(itemCode, ResourceBundle.getString(promt), icon);
    }
    
    public void setSelectedItemCode(int itemCode) {
        int inx = getIndexByItemCode(itemCode);
        selectedItem = (0 > inx) ? 0 : inx;
        selectedItemPosX = 0;
        invalidate();
    }
    
    private int getIndexByItemCode(int itemCode) {
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            if (((MenuItem)items.elementAt(i)).code == itemCode) {
                return i;
            }
        }
        return -1;
    }
    
    public int getSelectedItemCode() {
        return ((0 <= selectedItem) && (selectedItem < items.size()))
                ? itemAt(selectedItem).code : UNDEFINED_CODE;
    }
    public String getItemText(int itemCode) {
        int index = getIndexByItemCode(itemCode);
        if (-1 == index) {
            return null;
        }
        return itemAt(index).text;
    }
    
    public void paint(GraphicsEx g) {
        final int size = items.size();
        final int itemPerPage = getItemPerPage();
        final boolean hasScroll = (size > itemPerPage);

        final int itemWidth = getItemWidth();

        // get top item
        int curWidth = itemWidth + 2 * MENU_BORDER;
        int curHeight = itemHeight * itemPerPage;
        int currentIndex = selectedItem;
        if (hasScroll) {
            curWidth += scrollerWidth;
            topItem = Math.max(topItem, getIndex(currentIndex, 1 + 1 - itemPerPage));
            topItem = Math.min(topItem, getIndex(size, -itemPerPage));
            topItem = Math.min(topItem, getIndex(currentIndex, -1));
        } else {
            topItem = 0;
        }
        
        int y = this.y;
        int x = this.x;
        paintBack(g, prevDisplay);
        g.setStrokeStyle(Graphics.SOLID);
        g.drawShadowRect(x, y, curWidth, curHeight + MENU_BORDER + BORDER_Y, 
                THEME_MENU_BACK, THEME_MENU_BORDER, THEME_MENU_SHADOW);
        x += MENU_BORDER;
        y += MENU_BORDER;
        if (hasScroll) {
            g.drawVertScroll(x + MENU_BORDER + itemWidth, y, scrollerWidth, curHeight,
                    topItem, itemPerPage, size);
        }
        paintItems(g, x, y, itemPerPage, currentIndex);
    }
    
    private void paintItems(GraphicsEx g, int baseX, int baseY, int count, int currentIndex) {
        final int itemWidth = getItemWidth();
        final int textWidth = itemWidth - (iconWidth + ICON_INTERVAL + 2 * BORDER_X);

        int iconX  = BORDER_X + baseX + iconWidth / 2;
        int iconY  = BORDER_Y + baseY + (itemHeight - BORDER_Y) / 2;
        int promtX = BORDER_X + baseX + iconWidth + ICON_INTERVAL;
        int promtY = BORDER_Y + baseY + (itemHeight - BORDER_Y) / 2 - menuFont.getHeight() / 2;
        int w = g.getClipWidth();
        int h = g.getClipHeight();

        g.setFont(menuFont);
        for (int i = topItem; count > 0; i++, count--) {
            if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_BACK);
                int capBkCOlor = g.getThemeColor(THEME_MENU_SEL_BACK);
                g.drawGradRect(capBkCOlor, g.transformColorLight(capBkCOlor, -32),
                        baseX, baseY, itemWidth, itemHeight);
                g.setThemeColor(THEME_MENU_SEL_BORDER);
                g.drawRect(baseX, baseY, itemWidth, itemHeight);
            }
            MenuItem item = itemAt(i);
            g.setClip(promtX, BORDER_Y + baseY - 1, textWidth, itemHeight - BORDER_Y + 2);
            if (null == item.text) {
                int posY = baseY + itemHeight / 2;
                int posX = baseX + itemWidth;
                g.setThemeColor(THEME_MENU_SHADOW);
                g.drawLine(baseX + itemWidth - 5, posY, baseX + itemWidth - 3, posY + 1);
                g.drawLine(baseX + 3, posY + 1, baseX + itemWidth - 3, posY + 1);
                g.setThemeColor(THEME_MENU_BORDER);
                g.drawLine(baseX + 2, posY, baseX + itemWidth - 4, posY);

            } else if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_TEXT);
                g.drawString(item.text, promtX - selectedItemPosX, promtY, Graphics.TOP | Graphics.LEFT);

            } else {
                g.setThemeColor(THEME_MENU_TEXT);
                g.drawString(item.text, promtX, promtY, Graphics.TOP | Graphics.LEFT);
            }
            baseY += itemHeight;
            iconY += itemHeight;
            promtY += itemHeight;
            g.setClip(0, 0, w, h);
        }
    }
    private int getIndex(int currentIndex, int moveTo) {
        return Math.max(Math.min(currentIndex + moveTo, items.size() - 1), 0);
    }
    private void setSelectedItem(int index) {
        selectedItem = getIndex(index, 0);
        selectedItemPosX = 0;
        sleep = 0;
        invalidate();
    }
    private void nextPrevItem(boolean next) {
        final int size = items.size();
        setSelectedItem((selectedItem + (next ? 1 : size - 1)) % size);
    }

    private SelectListener listener;
    public void setActionListener(SelectListener listener) {
        this.listener = listener;
    }

    public void doKeyReaction(int keyCode, int gameAct, int type) {
        if (type != KEY_RELEASED) {
            switch (gameAct) {
                case NativeCanvas.NAVIKEY_DOWN:
                case NativeCanvas.NAVIKEY_UP:
                    nextPrevItem(NativeCanvas.NAVIKEY_DOWN == gameAct);
                    return;
            }
        }
        if (type != KEY_PRESSED) return;
        switch (keyCode) {
            case NativeCanvas.LEFT_SOFT:
                go(itemAt(selectedItem).code);
                return;
            case NativeCanvas.RIGHT_SOFT:
            case NativeCanvas.CLOSE_KEY:
            case NativeCanvas.CLEAR_KEY:
                back();
                return;
            case NativeCanvas.KEY_NUM1:
                setSelectedItem(0);
                return;
            case NativeCanvas.KEY_NUM7:
                setSelectedItem(items.size() - 1);
                return;
            case NativeCanvas.KEY_NUM3:
            case NativeCanvas.KEY_NUM9:
                int item = getItemPerPage();
                if (NativeCanvas.KEY_NUM3 == keyCode) {
                    item = -item;
                }
                setSelectedItem(selectedItem + item);
                return;
        }
		switch (gameAct) {
            case NativeCanvas.NAVIKEY_RIGHT: // ????
            case NativeCanvas.NAVIKEY_FIRE:
                go(itemAt(selectedItem).code);
                break;
            case NativeCanvas.NAVIKEY_LEFT:
                back();
                break;
        }
	}

    public void updateTask() {
        sleep++;
        if ((0 == selectedItemPosX) && (sleep < SLEEP_BEFOR)) return;
        int width = menuFont.stringWidth(itemAt(selectedItem).text);
        int textWidth = getItemWidth() - (iconWidth + ICON_INTERVAL + 2 * BORDER_X);
        if (width <= textWidth) return;

        if (selectedItemPosX + textWidth > (width + EMPTY_WIDTH)) {
            if (sleep < SLEEP_AFTER) return;
            selectedItemPosX = 0;
        } else {
            selectedItemPosX += STEP;
        }
        sleep = 0;
        invalidate();
    }

    public void go(int code) {
        if (DELIMITER_CODE == code) {
            back();
            return;
        }
        try {
	    if (0 <= getIndexByItemCode(code)) {
    		listener.select(this, code);
	    }
        } catch (Exception e) {
    	    // #sijapp cond.if modules_DEBUGLOG is "true" #
    	    jimm.modules.DebugLog.panic("select", e);
    	    // #sijapp cond.end #
        }
    }
}