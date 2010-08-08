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
 File: src/DrawControls/VirtualList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Igor Palkin, Vladimir Kryukov
 *******************************************************************************/

package DrawControls;
import javax.microedition.lcdui.*;
import jimm.ui.*;

//! This class is base class of owner draw list controls
/*!
    It allows you to create list with different colors and images. 
    Base class of VirtualDrawList if Canvas, so it draw itself when
    paint event is heppen. VirtualList have cursor controlled of
    user
*/

public abstract class VirtualList extends CanvasEx {
    public final static int MEDIUM_FONT = Font.SIZE_MEDIUM;
    public final static int LARGE_FONT = Font.SIZE_LARGE;
    public final static int SMALL_FONT = Font.SIZE_SMALL;
    
    // Commands to react to VL events
    private VirtualListCommands vlCommands;

    // Caption of VL
    private String caption;

    // Index for current item of VL
    private int currItem = 0;
    
    protected int topItem = 0;            // Index of top visilbe item 
    private int fontSize = SMALL_FONT;  // Current font size of VL

    //! Create new virtual list with default values  
    public VirtualList(String capt) {
        setCaption(capt);
        fontSet = GraphicsEx.getFontSet(fontSize);
    }

    //! Request number of list elements to be shown in list
    /*! You must return number of list elements in successtor of
        VirtualList. Class calls method "getSize" each time before it drawn */
    abstract protected int getSize();

    // Set of fonts for quick selecting
    private Font[] fontSet;

    public final Font getQuickFont(int style) {
         if (style < fontSet.length) {
            return fontSet[style];
        }
        return Font.getFont(Font.FACE_SYSTEM, style, fontSize);
    }
    
    protected final Font[] getFontSet() {
        return fontSet;
    }
    
    public final Font getDefaultFont() {
        return fontSet[Font.STYLE_PLAIN];
    }
    
    // returns height of draw area in pixels
    private int getDrawHeight() {
        return getHeight() - getCapHeight();
    }

    //! Sets new font size and invalidates items
    protected final void setFontSize(int value) {
        if (fontSize == value) return;
        fontSize = value;
        fontSet = GraphicsEx.getFontSet(fontSize);
        checkTopItem();
        //invalidate();
    }

    public final void setVLCommands(VirtualListCommands vlCommands) {
        this.vlCommands = vlCommands;
    }
    
    /** Returns number of visibled lines of text which fits in screen */
    protected int getVisCount() {
        int size = getSize();
        if (size == 0) return 0;

        int height = getDrawHeight();
        int top = Math.min(Math.max(topItem, 0), size - 1);
        
        int y = 0;
        int counter = 0;
        for (int i = top; i < (size - 1); i++) {
            y += getItemHeight(i);
            if (y > height) return counter;
            counter++;
        }
        
        y = height;
        counter = 0;
        for (int i = size - 1; i >= 0; i--) {
            y -= getItemHeight(i);
            if (y < 0) break;
            counter++;
        }
        
        return counter;
    }
    
    //! Returns height of each item in list
    protected abstract int getItemHeight(int itemIndex);

    // check for position of top element of list and change it, if nesessary
    private void checkTopItem() {
        int size = getSize();
        if (size == 0) {
            topItem = 0;
            currItem = 0;
            return;
        }
        currItem = Math.max(Math.min(currItem, size - 1), 0);

        if (currItem > topItem) {
            int height = getDrawHeight();
            int item = currItem;
            while (item >= topItem) {
                height -= getItemHeight(item--);
                if (height <= 0) {
                    item+=2;
                    break;
                }
            }
            topItem = Math.max(item, topItem);
        } else {
            topItem = currItem;
        }
    }

    // Check does item with index visible
    protected boolean visibleItem(int index) {
        return (index >= topItem) && (index <= (topItem + getVisCount()));
    }

    protected void onCursorMove() {
    }

    // public void setCurrentItem(int index)
    public final void setCurrentItem(int index) {
        int lastCurrItem = currItem;
        currItem = Math.max(Math.min(index, getSize() - 1), 0);
        if (lastCurrItem != currItem) {
            checkTopItem();
            invalidate();
            onCursorMove();
            if (vlCommands != null) {
                vlCommands.onCursorMove(this);
            }
        }
    }

    public final int getCurrItem() {
        return currItem;
    }

    // protected void moveCursor(int step)
    protected void moveCursor(int step) {
        setCurrentItem(getCurrItem() + step);
    }

    protected void itemSelected() {}
    
    // private keyReaction(int keyCode)
    private void keyReaction(int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_DOWN:
                moveCursor(1);
                break;
            case NativeCanvas.NAVIKEY_UP:
                moveCursor(-1);
                break;
            case NativeCanvas.NAVIKEY_FIRE:
                itemSelected();
                if (vlCommands != null) {
                    vlCommands.onItemSelected(this);
                }
                break;
        }
        switch (keyCode) {
        case NativeCanvas.KEY_NUM1:
            setCurrentItem(0);
            break;
            
        case NativeCanvas.KEY_NUM7:
            setCurrentItem(getSize() - 1);
            break;

        case NativeCanvas.KEY_NUM3:
            moveCursor(-getVisCount());
            break;
            
        case NativeCanvas.KEY_NUM9:
            moveCursor(getVisCount());
            break;
        }

    }

    public void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((vlCommands != null) && (KEY_PRESSED == type)) {
            if (vlCommands.onKeyPress(this, keyCode, actionCode)) {
                return;
            }
        }
        switch (type) {
        case KEY_PRESSED:
        case KEY_REPEATED:
            keyReaction(keyCode, actionCode);
            break;
        }
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    protected int getItemByCoord(int x, int y) {
        // is pointing on scroller
        if (x >= (getWidth() - 3 * scrollerWidth)) {
            return -1;
        }
        
        // is pointing on data area
        int itemY1 = getCapHeight();
        int size = getSize();
        for (int i = topItem; i < size; i++) {
            int height = getItemHeight(i);
            int itemY2 = itemY1 + height;
            if ((itemY1 <= y) && (y < itemY2)) {
                return i;
            }
            itemY1 = itemY2;
        }
        return -1;
    }
            
    protected boolean pointerPressedOnUtem(int index, int x, int y) {
        return false;
    }
    
    protected void stylusMoving(int fromX, int fromY, int toX, int toY) {
        stylusMoved(fromX, fromY, toX, toY);
    }
    protected void stylusMoved(int fromX, int fromY, int toX, int toY) {
        if (Math.max(fromX, toX) >= (getWidth() - 2 * scrollerWidth)) {
            setCurrentItem(getSize() * (toY - getCapHeight()) / getDrawHeight());
        } else {
            int item = getItemByCoord(toX, toY);
            if (item >= 0) {
                setCurrentItem(item);
                pointerPressedOnUtem(item, toX, toY);
            }
        }
    }
    protected void stylusTap(int x, int y, boolean longTap) {
        if (x >= (getWidth() - scrollerWidth)) {
            setCurrentItem(getSize() * (y - getCapHeight()) / getDrawHeight());
            return;
        }
        if (y < getCapHeight()) {
            return;
        }
        int item = getItemByCoord(x, y);
        if (item >= 0) {
            setCurrentItem(item);
            if (longTap) {
                itemSelected();
                if (vlCommands != null) {
                    vlCommands.onItemSelected(this);
                }
            } else {
                pointerPressedOnUtem(item, x, y);
            }
        }
    }
    // #sijapp cond.end#

    /**
     * Set caption text for list
     */
    public final void setCaption(String capt) {
        // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
        if ((null != caption) && caption.equals(capt)) return;
        caption = capt;
        if (NativeCanvas.isFullScreen()) {
            invalidate();
        } else {
            NativeCanvas.setCaption(caption);
        }
        // #sijapp cond.end#
    }

    public String getCaption() {
        return caption;
    }
    
    private String ticker;
    public void setTicker(String tickerString) {
        if (ticker == tickerString) {
            return;
        }
        ticker = tickerString;
        if (NativeCanvas.isFullScreen()) {
            invalidate();
        }
    }
    
    private static int captionHeight = -1;
    protected int getCapHeight() {
        captionHeight = Math.max(captionHeight, GraphicsEx.calcCaptionHeight(caption));
        return captionHeight;
    }

    protected boolean isItemSelected(int index) {
        return (currItem == index);
    }

    public void paint(GraphicsEx g) {
        int captionHeight = getCapHeight();
        g.drawCaption((null == ticker) ? caption : ticker, captionHeight);
        g.drawVertScroll(getWidth() - scrollerWidth, captionHeight,
                scrollerWidth, getHeight() - captionHeight,
                topItem, getVisCount(), getSize());
        drawItems(g, captionHeight);
    }

    private int drawItems(GraphicsEx g, int top_y) {
        int height = getHeight();
        int size = getSize();
        int itemWidth = getWidth() - scrollerWidth;
        
        // Fill background
        g.setThemeColor(THEME_BACKGROUND);
        g.fillRect(0, top_y, itemWidth, height - top_y);

        int grCursorY1 = -1;
        int grCursorY2 = -1; 
        // Draw cursor
        int y = top_y;
        for (int i = topItem; i < size; i++) {
            int itemHeight = getItemHeight(i);
            if (isItemSelected(i)) {
                if (grCursorY1 == -1) grCursorY1 = y;
                grCursorY2 = y + itemHeight - 1;
            }
            y += itemHeight;
            if (y >= height) break;
        }
        
        // Draw items
        y = top_y;
        for (int i = topItem; i < size; i++) {
            int itemHeight = getItemHeight(i);
            g.setStrokeStyle(Graphics.SOLID);
            drawItemData(g, i, 2, y, itemWidth - 2, y + itemHeight);
            y += itemHeight;
            if (y >= height) break;
        }

        if (grCursorY1 != -1) {
            g.setStrokeStyle(Graphics.DOTTED);
            g.setThemeColor(THEME_SELECTION_RECT);
            //g.setStrokeStyle(Graphics.SOLID);
            //g.drawRoundRect(0, grCursorY1, itemWidth, grCursorY2 - grCursorY1, 3, 3);
            if (!((topItem >= 1) && isItemSelected(topItem - 1))) {
                g.drawLine(1, grCursorY1, itemWidth - 2, grCursorY1);
            }
            g.drawLine(0, grCursorY1 + 1, 0, grCursorY2 - 1);
            g.drawLine(itemWidth - 1, grCursorY1 + 1, itemWidth - 1, grCursorY2 - 1);
            g.drawLine(1, grCursorY2, itemWidth-2, grCursorY2);
        }

        return y;
    }

    protected abstract void drawItemData(GraphicsEx g, int index,
            int x1, int y1, int x2, int y2);
    
    protected int getHeight() {
        return NativeCanvas.getScreenHeight();
    }
    
    protected int getWidth() {
        return NativeCanvas.getScreenWidth();
    }
}