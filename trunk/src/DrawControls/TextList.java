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
 File: src/DrawControls/TextList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis
 *******************************************************************************/


package DrawControls;

import javax.microedition.lcdui.*;
import jimm.ui.CanvasEx;
import jimm.ui.GraphicsEx;
import jimm.ui.NativeCanvas;
import jimm.ui.Select;
import jimm.util.ResourceBundle;


//! Text list
/*! This class store text and data of lines internally
    You may use it to show text with colorised lines :) */
public class TextList extends VirtualList {
    protected FormatedText formatedText = new FormatedText();

	// protected int getSize()
	public int getSize() {
		int size = formatedText.getSize();
		if (0 == size) return 0;
		return (formatedText.getLine(size - 1)).items.isEmpty() ? size - 1 : size;
	}

	protected boolean isItemSelected(int index) {
		int selIndex = getCurrItem();
		int textIndex = (selIndex >= formatedText.getSize())
                ? -1 : formatedText.getLine(selIndex).bigTextIndex;
		if (textIndex == -1) return false;
		return (formatedText.getLine(index).bigTextIndex == textIndex);
	}

	//! Remove all lines form list
	public void clear() {
        formatedText.clear();
		setCurrentItem(0);
		invalidate();
        uiBigTextIndex = 0;
        setHeader(null);
	}
    public void removeFirstText() {
        int size = formatedText.getSize();
        formatedText.removeFirstText();
        // TODO: save cursor position
        int delta = formatedText.getSize() - size;
        topItem = Math.max(0, topItem + delta);
        setCurrentItem(Math.max(0, getCurrItem() + delta));
    }

	//! Construct new text list with default values of colors, font size etc...
	public TextList(String capt) {
		super(capt);
		formatedText.setWidth(getWidth() - scrollerWidth - 3);
	}
	
	public int getItemHeight(int itemIndex) {
		if (itemIndex >= formatedText.getSize()) return 1;
		return formatedText.getLine(itemIndex).getHeight();
	}
	
	// Overrides VirtualList.drawItemData
	protected void drawItemData(
            GraphicsEx g,
            int index,
            int x1, int y1,
            int x2, int y2) {
		formatedText.getLine(index).paint(getFontSet(), 1, y1, g);
	}
	

	// Overrides VirtualList.moveCursor
	protected void moveCursor(int step) {
        int currItem = getCurrItem();

		switch (step) {
		case -1:
		case 1:
			int currTextIndex = getCurrTextIndex();
			int size = formatedText.getSize();
            int halfSize = getVisCount() / 2;
            int changeCounter = 0;
			for (int i = 0; i < halfSize;) {
				currItem += step;
				if ((currItem < 0) || (currItem >= size)) break;
				TextLine item = formatedText.getLine(currItem);
				if (currTextIndex != item.bigTextIndex) {
					currTextIndex = item.bigTextIndex;
					changeCounter++;
					if ((changeCounter == 2) || (!visibleItem(currItem) && (i > 0))) {
						currItem -= step;
						break;
					}
				}
				
				if (!visibleItem(currItem) || (changeCounter != 0)) i++;
			}

            setCurrentItem(currItem);
			break;

		default:
            setCurrentItem(currItem + step);
			return;
		}
	}

	// Returns lines of text which were added by 
	// methon addBigText in current selection
	public String getCurrText(int offset, boolean wholeText) {
        return formatedText.getText(getCurrTextIndex(), offset, wholeText);
	}

    protected int getTextIndex(int lineIndex) {
		if ((formatedText.getSize() <= lineIndex) || (lineIndex < 0)) {
            return -1;
        }
		return formatedText.getLine(lineIndex).bigTextIndex;
    }
	public int getCurrTextIndex() {
		return getTextIndex(getCurrItem());
	}
    public void setCurrTextIndex(int textIndex) {
        for (int i = 0; i < formatedText.getSize(); i++) {
            if (textIndex == formatedText.getLine(i).bigTextIndex) {
                setCurrentItem(i);
                return;
            }
        }
        return;
    }

	public TextList doCRLF(int blockTextIndex) {
        formatedText.doCRLF(blockTextIndex);
		return this;
	}
	
	public TextList addBigText(String text, byte colorType, int fontStyle, int textIndex) {
        formatedText.addBigText(getFontSet(), text, colorType, (byte)fontStyle, textIndex);
		invalidate();
		return this;
	}

	public TextList addTextWithEmotions(String text, byte colorType, int fontStyle, int textIndex) {
        // #sijapp cond.if modules_SMILES is "true" #
        formatedText.addTextWithEmotions(getFontSet(), text, colorType, (byte)fontStyle, textIndex);
		invalidate();
        // #sijapp cond.else#
        addBigText(text, colorType, fontStyle, textIndex);
        // #sijapp cond.end#
		return this;
	}

    public void addItem(String str, int code, boolean active) {
        int type = active ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
        addBigText(str, THEME_TEXT, type, code);
        doCRLF(code);
    }
    private String header = null;
    public void setHeader(String header) {
        this.header = header;
    }
    protected int uiBigTextIndex = 0;
    
    public void add(Object icon1, Object icon2, String str, Object icon3) {
        if ((null == str) || (0 == str.length())) {
            return;
        }
        if (null != header) {
            addBigText(ResourceBundle.getString(header), THEME_TEXT, Font.STYLE_BOLD, -1);
            doCRLF(-1);
            header = null;
        }
        addBigText(str, CanvasEx.THEME_PARAM_VALUE, Font.STYLE_PLAIN, uiBigTextIndex);

        doCRLF(uiBigTextIndex);
        uiBigTextIndex++;
    }

    public void add(String langStr, Object img, String alt, String str) {
        add(langStr, str);
    }
    
    public void add(String langStr, Object img, String str) {
        add(langStr, str);
    }
    
    public void add(String langStr, String str) {
        if ((null == str) || (str.length() == 0)) {
            return;
        }

        if (null != header) {
            addBigText(ResourceBundle.getString(header), THEME_TEXT, Font.STYLE_BOLD, -1);
            doCRLF(-1);
            header = null;
        }

        if ((null != langStr) && (langStr.length() > 0)) {
            addBigText(ResourceBundle.getString(langStr) + ": ", THEME_TEXT, Font.STYLE_PLAIN, uiBigTextIndex);
        }
        if ((null != str) && (str.length() > 0)) {
            addBigText(str, CanvasEx.THEME_PARAM_VALUE, Font.STYLE_PLAIN, uiBigTextIndex);
        }
        doCRLF(uiBigTextIndex);
        uiBigTextIndex++;
    }

    private int backCode = -1;
    private int defaultCode = -1;
    
    protected void restoring() {
        NativeCanvas.setCommands("menu", null, "back");
    }
    public final void setMenuCodes(int backCode, int defCode) {
        this.backCode = backCode;
        this.defaultCode = defCode;
    }
    protected Select getMenu() {
        return null;
    }

    public void doKeyReaction(int keyCode, int actionCode, int type) {
        if (type == KEY_PRESSED) {
            switch (actionCode) {
                case NativeCanvas.LEFT_SOFT:
	            Select menu = getMenu();
                    if (null != menu) {
                        menu.show();
                    }
                    return;
                    
                case NativeCanvas.RIGHT_SOFT:
                case NativeCanvas.CLOSE_KEY:
                    backAct();
                    return;

                case NativeCanvas.NAVIKEY_FIRE:
	            Select defaultActionMenu = getMenu();
                    if ((-1 != defaultCode) && (null != defaultActionMenu)) {
                        defaultActionMenu.go(defaultCode);
                        return;
                    }
                    break;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private void backAct() {
        if (backCode == -1) {
            back();
        } else {
            getMenu().go(backCode);
        }
    }    
    

    
    public static void showMessage(GraphicsEx g, String msg, int width, int height) {
        final int size_x = width / 10 * 8;
        final int textWidth = size_x - 8;

        Font[] fontSet = GraphicsEx.getFontSet(Font.SIZE_MEDIUM);
        FormatedText formatedText = new FormatedText();
        formatedText.setWidth(textWidth);
        formatedText.addBigText(fontSet, msg, THEME_SPLASH_LOCK_TEXT, (byte)Font.STYLE_PLAIN, -1);

        final int textHeight = formatedText.getHeight();
        final int size_y = textHeight + 8;
        final int x = width / 2 - (width / 10 * 4);
        final int y = height / 2 - (size_y / 2);
        g.setThemeColor(THEME_SPLASH_LOCK_BACK);
        g.fillRect(x, y, size_x, size_y);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        g.drawRect(x + 2, y + 2, size_x - 5, size_y - 5);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        formatedText.paint(fontSet, g, x + 4, y + 4, size_x - 8, textHeight);
    }
}