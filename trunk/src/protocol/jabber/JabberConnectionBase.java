/*
 * JabberConnection.java
 *
 * Created on 12 Июль 2008 г., 19:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.Vector;
import javax.microedition.io.*;
import jimm.JimmException;
import jimm.comm.StringConvertor;
import jimm.modules.*;
import protocol.Protocol;

/**
 *
 * @author Vladimir Krukov
 */
public abstract class JabberConnectionBase implements Runnable {
    protected Socket socket;
    protected Protocol protocol;
    private boolean connect;
    
    /** Creates a new instance of JabberConnection */
    public JabberConnectionBase(Protocol protocol) {
        this.protocol = protocol;
    }
    public final void start() {
        connect = true;
        new Thread(this).start();
    }
    

    protected void write(byte[] data) throws JimmException {
        try {
            socket.write(data);
        } catch (Exception e) {
            throw new JimmException(120, 1);
        }
    }

    protected void connectTo(String url) throws JimmException {
        try {
            socket = new Socket();
            socket.connectTo(url);
        } catch (Exception e) {
            throw new JimmException(120, 2);
        }
    }

    final boolean isConnected() {
        return connect;
    }
    public final void disconnect() {
        connect = false;
        protocol = null;
    }

    protected final int available() throws JimmException {
        try {
            return socket.available();
        } catch (Exception e) {
            throw new JimmException(120, 3);
        }
    }

    /////////////////////////////////////////////////////    
    protected abstract void connect() throws JimmException;
    protected abstract void processPacket() throws JimmException;
    protected abstract void ping() throws JimmException;

    private final Vector packets = new Vector();
    protected void putPacketIntoQueue(Object packet) {
        synchronized (packets) {
            packets.addElement(packet);
        }
    }
    private boolean hasOutPackets() {
        synchronized (packets) {
            return !packets.isEmpty();
        }
    }
    protected abstract void writePacket(Object packet) throws JimmException;
    private void sendPacket() throws JimmException {
        Object packet = null;
        synchronized (packets) {
            packet = packets.elementAt(0);
            packets.removeElementAt(0);
        }
        writePacket(packet);
    }
    /////////////////////////////////////////////////////

    protected final void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    protected abstract void sendClosePacket();
    public final void run() {
        try {
            connect();
            while (null != protocol && socket.isConnected()) {
                if (hasOutPackets()) {
                    sendPacket();

                } else if (available() > 0) {
                    processPacket();

                } else {
                    sleep(200);
                    ping();
                }
            }
        } catch (JimmException e) {
            Protocol p = protocol;
            if (null != p) {
                p.processException(e);
            }
        } catch (Exception e) {
        }
        disconnect();
        sendClosePacket();
        socket.close();
        socket = null;
    }
}
// #sijapp cond.end #
