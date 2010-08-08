/*
 * InputTextBox.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;

/**
 * Extended TestBox.
 * Now realized:
 * 1) long text editor;
 * 2) smiles;
 * 3) templates;
 * 4) text buffer;
 * 5) transliteration (cyrilic);
 * 6) restoring previous windows.
 *
 * @author Vladimir Kryukov
 */
public final class InputTextBox extends DisplayableEx implements CommandListener, ActionListener {
    private Command insertTemplateCommand;
    private Command pasteCommand;
    private Command quoteCommand;
    private Command clearCommand;

    private Command cancelCommand;

    private Command okCommand;
    
    private int caretPos = 0;
    private boolean cancelNotify = false;
    private static final int MAX_CHAR_PER_PAGE = 3000;
    private static final int MIN_CHAR_PER_PAGE = 1000;
    private int textLimit;
    private String caption;
    private final TextBox box;

    private int inputType;
    private String currentTextPage = null;

    private int getItemType() {
        // #sijapp cond.if target is "MIDP2" #
        if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA_S60)) {
            return Command.SCREEN;
        }
        // #sijapp cond.end #
        return Command.ITEM;
    }
    private TextBox createTextBox() {
	TextBox box = null;
        try {
            box = new TextBox(caption, "", Math.min(MAX_CHAR_PER_PAGE, textLimit), inputType);
        } catch (Exception e) {
            box = new TextBox(caption, "", Math.min(MIN_CHAR_PER_PAGE, textLimit), inputType);
        }
        setCaption(caption);
        
        int commandType = getItemType();

        if (TextField.ANY == inputType) {
            insertTemplateCommand = initCommand("templates", commandType, 3);
            pasteCommand          = initCommand("paste", commandType, 4);
            quoteCommand          = initCommand("quote", commandType, 5);
            clearCommand          = initCommand("clear", commandType, 6);
        }

        box.setCommandListener(this);
        textLimit = box.getMaxSize();
        return box;
    }
    public InputTextBox(String title, int len, int type, boolean cancelNotify) {
        this.cancelNotify = cancelNotify;
        setCaption(ResourceBundle.getString(title));
        inputType = type;
        textLimit = len;
        box = createTextBox();
    }
    public InputTextBox(String title, int len, boolean cancelNotify) {
        this(title, len, TextField.ANY, cancelNotify);
    }

    /** Creates a new instance of InputTextBox */
    public InputTextBox(String title) {
        this(title, MAX_CHAR_PER_PAGE, false);
    }

    public InputTextBox(String title, boolean cancelNotify) {
        this(title, 5000, cancelNotify);
    }

    public InputTextBox(String title, int len) {
        this(title, len, false);
    }
    
    public Command getOkCommand() {
        return okCommand;
    }
    public boolean isOkCommand(Command cmd) {
        return okCommand == cmd;
    }
    

    public void setOkCommandCaption(String title) {
        int okType = Command.OK;
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
        if (Options.getBoolean(Options.OPTION_SWAP_SEND_AND_BACK)) {
            okType = FormEx.getBackType();
        }
        // #sijapp cond.end #
        okCommand = initCommand(title, okType, 1);
        
        cancelCommand = initCommand("cancel", FormEx.getBackType(), 11);
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
        if (Options.getBoolean(Options.OPTION_SWAP_SEND_AND_BACK)) {
            cancelCommand = initCommand("cancel", getItemType(), 11);
        }
        // #sijapp cond.end #
    }
    
    private Command initCommand(String title, int type, int pos) {
        return new Command(ResourceBundle.getString(title), type, pos);
    }
    private void addCommand(Command cmd) {
        if (null != cmd) {
            box.addCommand(cmd);
        }
    }
    private void addCommands() {
        addCommand(okCommand);

        addCommand(insertTemplateCommand);
        addCommand(pasteCommand);
        addCommand(quoteCommand);
        addCommand(clearCommand);

        addCommand(cancelCommand);
        
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#

        setConstraints(inputType);

        try {
            switch (Options.getInt(Options.OPTION_INPUT_MODE)) {
                case 1:
                    box.setInitialInputMode("UCB_BASIC_LATIN");
                    break;
                case 2:
                    box.setInitialInputMode("UCB_CYRILLIC");
                    break;
            }
        } catch (Exception e) {
        }
        // #sijapp cond.end#
    }

    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    private void setConstraints(int mode) {
        if (Options.getBoolean(Options.OPTION_TF_FLAGS)) {
            mode |= TextField.INITIAL_CAPS_SENTENCE;
        }
        try {
//	    if (Jimm.isPhone(Jimm.PHONE_SE) && (0 == box.size())) {
//    		box.setConstraints(TextField.ANY | TextField.NON_PREDICTIVE);
//    	    }
            box.setConstraints(mode);
        } catch (Exception e) {
        }
    }
    // #sijapp cond.end#

    protected void showing() {
        addCommands();
    }

    public void closed() {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
        // #sijapp cond.end #
    }
    public void restore() {
        Jimm.setDisplay(this);
    }
    public final void setDisplayableToDisplay() {
        Jimm.setDisplayable(box);
    }

    private CommandListener listener;
    public void setCommandListener(CommandListener cl) {
        listener = cl;
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (c == cancelCommand) {
                back();
                if (cancelNotify) {
                    listener.commandAction(c, null);
                }
                
            } else if (c == clearCommand) {
                setString(null);
                
            } else if ((c == pasteCommand) || (c == quoteCommand)) {
                insert(JimmUI.getClipBoardText(c == quoteCommand), getCaretPosition());
                
            } else if (c == insertTemplateCommand) {
                caretPos = getCaretPosition();
                Templates.getInstance().selectTemplate(this);
                
            } else if (Templates.getInstance().isMyOkCommand(c)) {
                String s = Templates.getInstance().getSelectedTemplate();
                if (null != s) {
                    insert(s, caretPos);
                }
                
            } else {
                listener.commandAction(c, null);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Text box", e);
            // #sijapp cond.end #
            if (getOkCommand() == c) {
                back();
            }
        }
  }
    private int getCaretPosition() {
        // #sijapp cond.if target is "MOTOROLA"#
        return box.getString().length();
        // #sijapp cond.else#
        return box.getCaretPosition();
        // #sijapp cond.end#
    }

    public void action(CanvasEx canvas, int cmd) {
        // #sijapp cond.if modules_SMILES is "true" # 
        if (canvas instanceof Selector) {
            // #sijapp cond.if target is "MOTOROLA"#
            caretPos = box.getString().length();
            // #sijapp cond.end#
            String space = " ";
            insert(space + ((Selector)canvas).getSelectedCode() + space, caretPos);
        }
        // #sijapp cond.end#
    }

    public boolean isCancelCommand(Command cmd) {
        return cancelCommand == cmd;
    }
    
    public String getString() {
        return StringConvertor.removeCr(box.getString());
    }
    
    private void insert(String str, int pos) {
        try {
            box.insert(StringConvertor.restoreCrLf(str), pos);
            return;
        } catch (Exception e) {
        }
    }

    private String getCurrentPageString() {
        String text = box.getString();
        return StringConvertor.removeCr((null == text) ? "" : text);
    }
    public void saveCurrentPage() {
        currentTextPage = getCurrentPageString();
    }
    public void setTicker(String text) {
        if (Options.getBoolean(Options.OPTION_POPUP_OVER_SYSTEM)) {
            String boxText = box.getString();
            box.setTicker((null == text) ? null : new Ticker(text));
            if (!boxText.equals(box.getString()) && (0 == box.getString().length())) {
                box.setString(boxText);
            }
        }
    }
    public final void setCaption(String title) {
        caption = (null == title) ? "" : title;
        if (Options.getBoolean(Options.OPTION_UNTITLED_INPUT)) {
            title = null;
        } else {
            title = caption;
        }
        if (null != box) {
            String boxText = box.getString();
            box.setTitle(title);
            if (!boxText.equals(box.getString()) && (0 == box.getString().length())) {
                box.setString(boxText);
            }
        }
    }
    public String getCaption() {
        return caption;
    }

    private void setTextToBox(String text) {
        if (null == text) {
            box.setString("");
            return;
        }
        String normalizedText = StringConvertor.restoreCrLf(text);
        try {
            box.setString(normalizedText);
            return;
        } catch (Exception e) {
        }
        try {
            box.setString(normalizedText.substring(0, textLimit));
            return;
        } catch (Exception e) {
        }
        box.setString("");
    }
    public void setCurrentScreen() {
        setTextToBox(currentTextPage);
        setCaption(caption);
    }

    public void setString(String initText) {
        if (null == initText) {
    	    currentTextPage = "";
            setTextToBox(null);
            return;
        }
        int maxSize = box.getMaxSize();
        if (initText.length() > textLimit) {
            initText = initText.substring(0, textLimit);
        }
        currentTextPage = initText;
        setCurrentScreen();
    }
    public boolean isShown() {
        return box.isShown();
    }
}
