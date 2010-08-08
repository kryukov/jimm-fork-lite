/*
 * Socket.java
 *
 * Created on 4 Февраль 2009 г., 15:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.jabber;

import java.io.*;
import javax.microedition.io.*;
import jimm.modules.*;

/**
 *
 * @author Vladimir Krukov
 */
public class Socket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;
    private boolean connected;
    
    /** Creates a new instance of Socket */
    public Socket() {
    }
    // #sijapp cond.if modules_ZLIB is "true" #
    public void activateStreamCompression() {
        is = new ZInputStream(is);
        os = new ZOutputStream(os, JZlib.Z_DEFAULT_COMPRESSION);
        ((ZOutputStream)os).setFlushMode(JZlib.Z_SYNC_FLUSH);
        // #sijapp cond.if modules_DEGUGLOG is "true" #
        DebugLog.println("zlib is working");
        // #sijapp cond.end #
    }
    public boolean isCommpressed() {
        return (is instanceof com.jcraft.jzlib.ZInputStream);
    }
    // #sijapp cond.end #
    public boolean isConnected() {
        return connected;
    }
    
    public void connectTo(String url) throws IOException {
        try {
            sc = (StreamConnection)Connector.open(url, Connector.READ_WRITE);
            is = sc.openInputStream();
            os = sc.openOutputStream();
            connected = true;
            
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }

    private int read(byte[] data, int offset, int length) throws IOException {
        try {
            // #sijapp cond.if modules_ZLIB is "true" #
            if (isCommpressed()) {
                return is.read(data, offset, length);
            }
            // #sijapp cond.end #
            length = Math.min(length, is.available());
            if (length == 0) {
                return 0;
            }
            int bRead = is.read(data, offset, length);
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.addInTraffic(bRead);
            // #sijapp cond.end#
            return bRead;

//            int bReadSum = 0;
//            int readSize = 0;
//            int bRead = 0;
//            length = Math.min(length, available());
//            if (length == 0) {
//                return 0;
//            }
//            do {
//                readSize = Math.min(length - bReadSum, 255);
//                bRead = is.read(data, offset + bReadSum, readSize);
//                if (-1 == bRead) {
//                    throw new IOException();
//                }
//                bReadSum += bRead;
//            jimm.modules.DebugLog.println("read " + bRead + " " + readSize);
//            } while ((bReadSum < length) && (bRead == readSize));
//            jimm.modules.DebugLog.println("sum " + bReadSum);
//            // #sijapp cond.if modules_TRAFFIC is "true" #
//            Traffic.addInTraffic(bReadSum);
//            // #sijapp cond.end#
//            return bReadSum;

        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }
    private int read(byte[] data) throws IOException {
        return read(data, 0, data.length);
    }

    public void write(byte[] data) throws IOException {
        try {
            os.write(data);
            os.flush();
        } catch (IOException e) {
            connected = false;
            throw e;
        }
        // #sijapp cond.if modules_TRAFFIC is "true" #
        // #sijapp cond.if modules_ZLIB is "true" #
        if (!isCommpressed()) {
            Traffic.addOutTraffic(data.length);
        }
        // #sijapp cond.else #
        Traffic.addOutTraffic(data.length);
        // #sijapp cond.end #
        // #sijapp cond.end#
    }
    public void close() {
        connected = false;
        try {
            is.close();
            os.close();
        } catch (Exception ex) {
        }
        try {
            sc.close();
        } catch (Exception ex) {
        }
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    private final void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    public byte readByte() throws IOException {
        if (inputBufferIndex >= inputBufferLength) {
            inputBufferIndex = 0;
            inputBufferLength = read(inputBuffer);
            while (inputBufferLength <= 0) {
                sleep(50);
                inputBufferLength = read(inputBuffer);
            }
        }
        return inputBuffer[inputBufferIndex++];
    }
    public int available() throws IOException {
        return is.available() + (inputBufferLength - inputBufferIndex);
    }
}
