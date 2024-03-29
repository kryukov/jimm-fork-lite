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
 File: src/jimm/util/ResourceBundle.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package jimm.util;

import java.util.Hashtable;
import java.io.InputStream;
import java.io.DataInputStream;
import jimm.comm.StringConvertor;
import jimm.comm.Util;

public class ResourceBundle {
    // List of available language packs
    public static String[] langAvailable;
    public static String[] langAvailableName;
    // Resource hashtable
    static private Hashtable resources = null;
	
    public static final int FLAG_ELLIPSIS = 1 << 0;  

	
    public static void loadLanguageList() {
        String[][] config = {{"RU"}, {"RU Russian"}};
        langAvailable = config[0];
        langAvailableName = config[1];
    }
    
    // Current language
    private static String currentLanguage;
    
    // Get user interface language/localization for current session
    public static String getCurrUiLanguage() {
        return currentLanguage;
    }
    public static String getLanguageCode() {
        String country = getCurrUiLanguage();
        int separatorIndex = country.indexOf('_');
        if (-1 != separatorIndex) {
            country = country.substring(0, separatorIndex);
        }
        return country.toLowerCase();
    }
    public static String getSystemLanguage() {
        return langAvailable[0];
    }
    
    // Set user interface language/localization for current session
    public static void setCurrUiLanguage(String currUiLanguage) {
        String language = ResourceBundle.langAvailable[0];
        for (int i = 0; i < ResourceBundle.langAvailable.length; i++) {
            if (langAvailable[i].equals(currUiLanguage)) {
                language = langAvailable[i];
                break;
            }
        }
        currentLanguage = language;
        loadLang();
    }
    
    private static void loadLang() {
        InputStream istream = null;
        try {
            resources = new Hashtable();
            istream = resources.getClass().getResourceAsStream("/" + currentLanguage + ".lng");
            DataInputStream dos = new DataInputStream(istream);
            int size = dos.readShort();
            for (int j = 0; j < size; j++) {
                resources.put(dos.readUTF(), dos.readUTF());
            }
            dos.close();
        } catch (Exception e) {
        }
        try {
            istream.close();
        } catch (Exception e) {
        }
    }
    
    public static void setString(String key, String value) {
        resources.put(key, value);
    }
    
    // Get string from active language pack
    public static String getString(String key) {
        if (null == key) return null;
        String value = (String) resources.get(key);
        return (null == value) ? key : value;
    }
    
    public static String getString(String key, int flags) {
        String result = getString(key);
        
        if ((flags & FLAG_ELLIPSIS) != 0) {
            result += "...";
        }
        
        return result;
    }
}

