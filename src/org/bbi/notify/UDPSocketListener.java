/*
    Copyright 2016 Wira Mulia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 */
package org.bbi.notify;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

/**
 *
 * @author wira
 */
public class UDPSocketListener extends Thread {
    private final Notify notify;
    private final int port;
    private DatagramSocket socket;
    private final boolean local;
    private boolean stop;
    
    public UDPSocketListener(Notify notify, int port, boolean local) {
        this.port = port;
        this.notify = notify;
        this.local = local;
        stop = false;
    }
    
    public int getPortNumber() {
        return port;
    }
    
    @Override
    public void run() {
        Log.d(1, this + ": run");
        String data;
        try {                      
            if(!local) {
                byte[] addr = {0, 0, 0, 0};
                socket = new DatagramSocket(port,
                        InetAddress.getByAddress(addr));
            } else {
                socket = new DatagramSocket(port,
                        InetAddress.getLoopbackAddress());
            }
            Log.d(1, this + ".run: bound to " + 
                    socket.getLocalAddress() + ":" + socket.getLocalPort());
            byte[] recvBuf;
            Log.d(1, this + ".run: listening");  
            while(!stop) {
                try {
                    recvBuf = new byte[4096];
                    DatagramPacket recv = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(recv);
                    data = (new String(recv.getData(), "UTF-8")).trim();
                    notify.notify(data);
                } catch(IOException ioe) {
                    Log.err(this + ".run: unable to read data " + ioe);
                }
            }
        } catch(IOException ioe) {
            Log.err(this + ".run: unable to bind socket " + ioe);
        }
        Log.d(1, this + ": exit");
    }
            
    public static void send(String address, int port, String data) {
        try {
            byte[] buf = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName(address), port);
            DatagramSocket s = new DatagramSocket();
            s.send(packet);
            s.close();
        } catch(Exception e) {
            Log.err("UDPSocketListener.send: failed, error=" + e);
        }
    }
    
    public void disconnect() {
        socket.close();
        stop = true;
    }
    
    @Override
    public String toString() {
        return "UDPSocketListener[" + port + (local ? ",local" : "") + "]";
    }
}
