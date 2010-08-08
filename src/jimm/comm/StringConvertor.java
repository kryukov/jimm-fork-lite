/*
 * StringConvertor.java
 *
 * Created on 6 Февраль 2007 г., 19:49
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.comm;

import java.io.*;
import java.util.Vector;
import jimm.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public final class StringConvertor {
    static public String bytesToSizeString(int v, boolean force) {
        int size = v;
        if (v < 1024 || force) {
            return size + ResourceBundle.getString("byte");
        }
        size = (v + 512) / 1024;
        v /= 1024;
        if (v < 1024 * 10) {
            return size + ResourceBundle.getString("kb");
        }
        size = (v + 512) / 1024;
        v /= 1024;
        return size + ResourceBundle.getString("mb");
    }
    
    // Converts a byte array to a hex string
    public static String byteArrayToHexString(byte[] buf) {
        StringBuffer hexString = new StringBuffer(buf.length);
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0x00FF);
            if (hex.length() < 2) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private static boolean systemUtf8 = true;
    public static String utf8beByteArrayToString(byte[] buf, int off, int len) {
//        if (systemUtf8) {
//            try {
//                return new String(buf, off, len, "UTF-8");
//            } catch (Exception e) {
//                systemUtf8 = false;
//            }
//        }
        try {
            byte[] buf2 = new byte[len + 2];
            Util.putWord(buf2, 0, len);
            System.arraycopy(buf, off, buf2, 2, len);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf2);
            DataInputStream dis = new DataInputStream(bais);
            return dis.readUTF();
        } catch (Exception e) {
            // do nothing
            e.printStackTrace();
        }
        return "";
    }
    
    // Removes all CR occurences
    public static String removeCr(String val) {
        if (val.indexOf('\r') < 0) {
            return val;
        }
        if (-1 == val.indexOf('\n')) {
            return val.replace('\r', '\n');
        }
        
        StringBuffer result = new StringBuffer();
        int size = val.length();
        for (int i = 0; i < size; i++) {
            char chr = val.charAt(i);
            if ((chr == 0) || (chr == '\r')) continue;
            result.append(chr);
        }
        return result.toString();
    }
    
    // Restores CRLF sequense from LF
    public static String restoreCrLf(String val) {
        StringBuffer result = new StringBuffer();
        int size = val.length();
        for (int i = 0; i < size; i++) {
            char chr = val.charAt(i);
            if (chr == '\r') continue;
            if (chr == '\n') {
                result.append("\r\n");
            } else {
                result.append(chr);
            }
        }
        return result.toString();
    }
    
    // Converts the specified string (val) to a byte array
    public static byte[] stringToByteArray(String val) {
        // Write string in UTF-8 format
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(val);
            byte[] raw = baos.toByteArray();
            byte[] result = new byte[raw.length - 2];
            System.arraycopy(raw, 2, result, 0, raw.length - 2);
            return result;
        } catch (Exception e) {
            // Do nothing
        }
        return null;
    }
    
    
    
    public static boolean stringEquals(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1.length() != s2.length()) {
            return false;
        }
        int size = s1.length();
        for (int i = 0; i < size; i++) {
            if (toLowerCase(s1.charAt(i)) != toLowerCase(s2.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    public static int stringCompare(String s1, String s2) {
        if (s1 == s2) {
            return 0;
        }
        int size = Math.min(s1.length(), s2.length());
        int result = 0;
        for (int i = 0; i < size; i++) {
            result = toLowerCase(s1.charAt(i)) - toLowerCase(s2.charAt(i));
            if (result != 0) {
                return result;
            }
        }
        return s1.length() - s2.length();
    }
    
    public static String toLowerCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toLowerCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }
    
    public static String toUpperCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toUpperCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }
    
    private static char toLowerCase(char c) {
        if (c >= 'A' && c <= 'Z' || c >= '\300'
                && c <= '\326' || c >= '\330'
                && c <= '\336' || c >= '\u0400'
                && c <= '\u042F') {
            if (c <= 'Z' || c >= '\u0410' && c <= '\u042F') {
                return (char)(c + 32);
            }
            if(c < '\u0410') {
                return (char)(c + 80);
            }
            return (char)(c + 32);
        }
        return Character.toLowerCase(c);
    }
    
    private static char toUpperCase(char c) {
        if (c >= 'a' && c <= 'z' || c >= '\337'
                && c <= '\366' || c >= '\370'
                && c <= '\377' || c >= '\u0430'
                && c <= '\u045F') {
            if (c <= 'z' || c >= '\u0430' && c <= '\u044F') {
                return (char)(c - 32);
            }
            if (c > '\u042F') {
                return (char)(c - 80);
            }
            return (char)(c - 32);
        }
        return Character.toUpperCase(c);
    }
    
    
    public static final boolean isEmpty(String value) {
        return (null == value) || (0 == value.length());
    }
    public static final String notNull(String value) {
        return (null == value) ? "" : value;
    }
}