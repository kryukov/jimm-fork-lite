/*
 * Scheme.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import DrawControls.*;
import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;

/**
 * 
 * Warning! This code used hack.
 * Current scheme not cloned (the reference to the base scheme is used),
 * but current scheme content will be rewritten, when current scheme is changed.
 *
 * @author Vladimir Krukov
 */
public class Scheme {
    
    /**
     * Creates a new instance of Scheme
     */
    private Scheme() {
    }
    
    static private int[] baseTheme = {
        0xFFFFFF, 0x000000, 0xF0F0F0, 0x000000, 0x0000FF,
        0xFF0000, 0x0000FF, 0x808080, 0x000000, 0x0000FF,
        0xA0A0A0, 0x808080, 0x808080, 0x0000FF, 0xE0E0E0,
        0x006FB1, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
        0x000000, 0xFFFFFF, 0x000000, 0x0000FF, 0x000000,
        0xFF0000, 0x0000FF, 0x000000, 0x606060, 0xD0D0D0,
        0x202020, 0x202020, 0xC0F0C0, 0xA05050, 0x000000,
        0x606060, 0x202020, 0xD0D0D0, 0x202020, 0x000000,
        0x800000, 0xFF0000};
    
    private static final int PARSER_LINE    = 0;
    private static final int PARSER_NAME    = 1;
    private static final int PARSER_INDEX   = 2;
    private static final int PARSER_EQUAL   = 3;
    private static final int PARSER_EQUAL1  = 4;
    private static final int PARSER_COLOR   = 5;
    private static final int PARSER_COMMENT = 6;

    /**
     * Retrieves color value from color scheme
     */
    static public int[] getScheme() {
        return baseTheme;
    }
    /**
     * Retrieves color value from color scheme
     */
    static public int getSchemeColor(int type) {
        return baseTheme[type];
    }
    
}