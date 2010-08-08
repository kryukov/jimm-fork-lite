/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: util/res/src/res/ResTask.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Vladimir Kryukov
 *******************************************************************************/

package res;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

//class BuildException extends Exception {public BuildException(Exception e) {}; public BuildException(String e) {};}
//class Task extends Object {}


class Warnings {
    private static Vector warnings = new Vector();
    public void add(String str) {
        warnings.add(str);
    }
    public void show() {
        int size = warnings.size();
        if (size == 0) return;
        System.out.println("*** WARNINGS ***");
        for (int i = 0; i < size; i++) {
            System.out.println((String)warnings.elementAt(i));
        }
        clear();
    }
    public void clear() {
        warnings.clear();
    }
}

public class ResTask extends Task {
    private Warnings warnings = new Warnings();

    public void copyFile(File in, File out) {
        try {
            FileInputStream fis = new FileInputStream(in);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];
            int i = 0;
            while((i=fis.read(buf))!=-1) {
                fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();
        } catch (Exception e) {
            
        }
    }

    // Scans the given directory (srcDir/srcDirExt) for Java source files
    private void copyDir(File srcDir, String srcDirExt, File destDir) {
        File[] files = new File(srcDir, srcDirExt).listFiles();
        if (0 == files.length) {
            return;
        }
        if (srcDirExt.length() > 0) {
            new File(destDir, srcDirExt).mkdirs();
        }

        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            if ('.' == filename.charAt(0)) {
                continue;
            }
            if (files[i].isFile()) {
                copyFile(files[i], new File(destDir, srcDirExt + File.separator + filename));

            } else if (files[i].isDirectory()) {
                copyDir(srcDir, srcDirExt + File.separator + filename, destDir);
            }
        }
    }
    public void copyFiles(String from, String to) {
        File fromDir = new File(from);
        if (!fromDir.exists()) {
            return;
        }
        if (!fromDir.isDirectory()) {
            warnings.add("Is not dir '" + from + "'");
            return;
        }
        copyDir(fromDir, "", new File(to));
    }
    
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    private String protocols;
    private String modules;
    private String srcDir;
    private String destDir;
    private String resType;
    
    public void setProtocols(String value) {
        protocols = value;
    }
    
    public void setModules(String value) {
        modules = value;
    }
    
    public void setInDir(String value) {
        srcDir = value;
    }
    
    public void setOutDir(String value) {
        destDir = value;
    }
    
    public void setResType(String value) {
        resType = value;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    private boolean empty(String str) {
        return (null == str) || (0 == str.length());
    }
    private String[] explode(String value) {
        if (empty(value)) {
            return new String[0];
        }
        Vector values = new Vector();
        StringTokenizer strTok = new StringTokenizer(value, ",");
        while (strTok.hasMoreTokens()) {
            String langName = strTok.nextToken().trim();
            values.add(langName);
        }
        String[] result = new String[values.size()];
        values.copyInto(result);
        return result;
    }
    
    
    private void executeModule(String protocol, String module) {
        String path = srcDir
                + (empty(protocol) ? "" : File.separator + protocol)
                + (empty(module) ? "" : File.separator + module)
                + (empty(resType) ? "" : File.separator + resType);
        copyFiles(path, destDir);
    }
    private void executeProtocol(String protocol, String[] moduleList) {
        if (0 == moduleList.length) {
            executeModule(protocol, "");
        }
        executeModule(protocol, "STATUSES");
        for (int moduleNum = 0; moduleNum < moduleList.length; moduleNum++) {
            executeModule(protocol, moduleList[moduleNum]);
        }
    }
    public void execute(String[] protocolList, String[] moduleList) {
        if (0 == protocolList.length) {
            executeProtocol("", moduleList);
            return;
        }
        executeProtocol("GENERAL", moduleList);
        for (int protocolNum = 0; protocolNum < protocolList.length; protocolNum++) {
            executeProtocol(protocolList[protocolNum], moduleList);
        }
        if (protocolList.length > 1) {
            executeProtocol("MULTI", moduleList);
        }
    }
    public void execute() throws BuildException {
        String[] protocolList = explode(protocols);
        String[] moduleList = explode(modules);
        execute(protocolList, moduleList);

        warnings.show();
    }
}
