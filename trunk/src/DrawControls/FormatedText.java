package DrawControls;

import java.util.Vector;
import javax.microedition.lcdui.Font;
import jimm.comm.StringConvertor;
import jimm.modules.*;
import jimm.ui.GraphicsEx;
/*
 * FormatedText.java
 *
 * Created on 25 Октябрь 2008 г., 16:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Vladimir Krukov
 */
public final class FormatedText {
	// Vector of lines. Each line contains cols. Col can be text or image
	private Vector lines = new Vector();

    private int width;
    public void setWidth(int w) {
        width = w;
    }
    private int getWidth() {
        return width;
    }
    public int getSize() {
        return lines.size();
	}

    public TextLine getLine(int index) {
		return (TextLine)lines.elementAt(index);
	}
	public void clear() {
		lines.removeAllElements();
	}
    public void remove(int textIndex) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            TextLine line = (TextLine)lines.elementAt(i);
            if (textIndex == line.bigTextIndex) {
                lines.removeElementAt(i);
            }
        }
    }
    public void removeFirstText() {
        if (lines.isEmpty()) {
            return;
        }
        final int textIndex = getLine(0).bigTextIndex;
        while (!lines.isEmpty() && (textIndex == getLine(0).bigTextIndex)) {
            lines.removeElementAt(0);
        }
    }

	private void internAdd(Font[] fontSet, String  text, byte colorType, byte fontStyle,
            int textIndex, boolean doCRLF, char last_charaster) {
		ListItem newItem = new ListItem(text, colorType, fontStyle);

		if (lines.isEmpty()) {
            lines.addElement(new TextLine());
        }
		TextLine textLine = (TextLine)lines.lastElement();
		textLine.addElement(newItem, fontSet);
		textLine.bigTextIndex = textIndex;
		if (doCRLF) {
			textLine.last_charaster = last_charaster;
			TextLine newLine = new TextLine();
			newLine.bigTextIndex = textIndex; 
			lines.addElement(newLine);
		}
	}

    public void doCRLF(int blockTextIndex) {
		if (lines.size() != 0) {
            ((TextLine)lines.lastElement()).last_charaster = '\n';
        }
		TextLine newLine = new TextLine();
		newLine.bigTextIndex = blockTextIndex; 
		lines.addElement(newLine);
	}

    private void internAdd(Font[] fontSet, ListItem imageItem, int blockTextIndex) {
		if (lines.isEmpty()) {
            lines.addElement(new TextLine());
        }
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex; 
		textLine.addElement(imageItem, fontSet);
	}

    /**
     * Add big multiline text. 
     *
	 * Text visial width can be larger then screen width.
     * Method addBigText automatically divides text to short lines
     * and adds lines to text list
     */
	public void addBigText(Font[] fontSet, String text, byte colorType,
            byte fontStyle, int textIndex, boolean withEmotions) {
        text = StringConvertor.removeCr(text);

		Font font = fontSet[fontStyle];
		
		// Width of free space in last line 
		int width = getWidth();
        if (!lines.isEmpty()) {
            width -= ((TextLine)lines.lastElement()).getWidth();
        }
		int lastWordEnd = -1;
        
        // #sijapp cond.if modules_SMILES is "true" #
        int smileCount = 100;
        // #sijapp cond.end #
        // #sijapp cond.if target is "MOTOROLA"#
        boolean isGraph = TPropFont.isValid(fontSet);
        TPropFont fnt = isGraph ? TPropFont.getFont() : null;
        // #sijapp cond.end#
        int lineStart = 0;
        int wordStart = 0;
        int wordWidth = 0;
		int textLen = text.length();
        for (int i = 0; i < textLen; i++) {
            char ch = text.charAt(i);
            if ('\n' == ch) {
                String substr = text.substring(lineStart, i);
                internAdd(fontSet, substr, colorType, fontStyle, textIndex, true, '\n');
                lineStart = i + 1;
                width = getWidth();
                wordStart = lineStart;
                wordWidth = 0;
                continue;
            }
            
            // #sijapp cond.if modules_SMILES is "true" #
            int smileIndex = withEmotions ? Emotions.getSmile(text, i) : -1;
            if (-1 != smileIndex) {
                wordStart = i;
                if (lineStart < wordStart) {
                    String substr = text.substring(lineStart, wordStart);
                    internAdd(fontSet, substr, colorType, fontStyle, textIndex, (width <= 0), '\0');
                    if (width <= 0) {
                        width = getWidth();
                    }
                }

                ListItem smileItem = Emotions.getSmileItem(smileIndex);
                width -= smileItem.getWidth();
                if (width <= 0) {
                    lines.addElement(new TextLine());
                }
                internAdd(fontSet, smileItem, textIndex);

                if (width <= 0) {
                    width = getWidth() - smileItem.getWidth();
                }

                i += smileItem.text.length() - 1;
                lineStart = i + 1;
                wordStart = lineStart;
                wordWidth = 0;
                
                smileCount--;
                if (0 == smileCount) {
                    withEmotions = false;
                }
                continue;
            }
            // #sijapp cond.end #
            
            int charWidth;
            // #sijapp cond.if target is "MOTOROLA"#
            charWidth = isGraph ? fnt.charWidth(ch) : font.charWidth(ch);
            // #sijapp cond.else#
            charWidth = font.charWidth(ch);
            // #sijapp cond.end#

            wordWidth += charWidth;
            width -= charWidth;
            if (' ' == ch) {
                wordStart = i + 1;
                wordWidth = 0;
                continue;
            }

            if (width < 0) {
                if (wordStart > lineStart) {
                    String substr = text.substring(lineStart, wordStart);
                    internAdd(fontSet, substr, colorType, fontStyle, textIndex, true, '\0');
                    lineStart = wordStart;
                    width = getWidth() - wordWidth;
                    wordWidth = 0;
                } else {
                    String substr = text.substring(lineStart, i);
                    internAdd(fontSet, substr, colorType, fontStyle, textIndex, true, '\0');
                    lineStart = i;
                    width = getWidth() - charWidth;
                    wordStart = i;
                    wordWidth = 0;
                }
                continue;
            }
		}
        String substr = text.substring(lineStart);
        if (0 < substr.length()) {
            internAdd(fontSet, substr, colorType, fontStyle, textIndex, false, ' ');
        }
	}

    public int getHeight() {
        int textHeight = 0;
		int linesCount = getSize();
		for (int line = 0; line < linesCount; line++) {
            textHeight += getLine(line).getHeight();
        }
        return textHeight;
	}

	public void addBigText(Font[] fontSet, String text, byte colorType,
            byte fontStyle, int textIndex) {
        addBigText(fontSet, text, colorType, fontStyle, textIndex, false);
    }

    public void addTextWithEmotions(Font[] fontSet, String text,
            byte colorType, byte fontStyle, int textIndex) {
        // #sijapp cond.if modules_SMILES is "true" #
        if (jimm.Options.getBoolean(jimm.Options.OPTION_USE_SMILES)
                && jimm.modules.Emotions.isSupported()) {
            addBigText(fontSet, text, colorType, fontStyle, textIndex, true);
            return;
        }
        // #sijapp cond.end #
        addBigText(fontSet, text, colorType, fontStyle, textIndex, false);
    }

    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int width, int height) {
		final int linesCount = getSize();
		int currentY = y;
		for (int lineIndex = 0; lineIndex < linesCount; lineIndex++) {
            TextLine line = getLine(lineIndex);
            
			line.paint(fontSet, x, currentY, g);
			currentY += line.getHeight();
		}
	}

    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int width, int height, int skipHeight) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.clipRect(x, y, width, height);

        final int linesCount = getSize();
        int lineIndex = 0;
		for (; (0 < skipHeight) && (lineIndex < linesCount); ++lineIndex) {
            skipHeight -= getLine(lineIndex).getHeight();
        }
		int currentY = y;
        if (0 != skipHeight) {
            lineIndex--;
            currentY -= skipHeight + getLine(lineIndex).getHeight();
        }
		for (; (0 < height) && (lineIndex < linesCount); ++lineIndex) {
            TextLine line = getLine(lineIndex);
            int lineHeight = line.getHeight();
            line.paint(fontSet, x, currentY, g);
            currentY += lineHeight;
            height -= lineHeight;
		}
        
        g.setClip(clipX, clipY, clipWidth, clipHeight);
	}

	// Returns lines of text which were added by 
	// methon addBigText in current selection
	public String getText(int textIndex, int offset, boolean wholeText) {
		int offsetCounter = 0;
		StringBuffer result = new StringBuffer();
		int currTextIndex = textIndex;

        // Fills the lines
		int size = getSize();
		for (int i = 0; i < size; i++) {
			TextLine line = getLine(i);
			if (wholeText || (line.bigTextIndex == currTextIndex)) {
				if (offset != offsetCounter) {
					offsetCounter++;
					continue;
				}
				int count = line.items.size();
				for (int k = 0; k < count; k++) {
                    String str = line.elementAt(k).text;
                    if (null != str) {
                        result.append(str);
                    }
				}
				if (line.last_charaster != '\0') {
					if (line.last_charaster == '\n') {
                        result.append("\n");
                    } else {
                        result.append(line.last_charaster);
                    }
				}
			}
		}
		String retval = result.toString().trim();
		return (retval.length() == 0) ? null : retval;
	}
}
