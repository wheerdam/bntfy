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

import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author wira
 */
public class SocketListener extends Thread {
    private final Notify notify;
    private final int port;
    private final boolean local;

    private ServerSocket server;
    private final ArrayList<ClientHandler> clients;
    private ReentrantReadWriteLock lock;
    private boolean stop;
    
    public SocketListener(Notify notify, int port, boolean local) {
        this.notify = notify;
        this.port = port;
        this.local = local;
        clients = new ArrayList();
        lock = new ReentrantReadWriteLock(true);
        stop = false;
    }
       
    @Override
    public void run() {
        Log.d(1, this + ": run");
        
        try {
            server = local ? 
                    new ServerSocket(port, 0, InetAddress.getLoopbackAddress()) :
                    new ServerSocket(port);
        } catch(IOException ioe) {
            Log.err(this + ".run: failed to initialize server\n" +
                    ioe + "\n" + ioe.getMessage());
            disconnect();
        }

        while(!stop) {
            try {                
                Log.d(1, this + ".run: listening");
                Socket s = server.accept();
                ClientHandler client = new ClientHandler(s);
                clients.add(client);
                client.start();
            } catch(IOException ioe) {
                Log.err(this + ".run: " + ioe + "\n" +
                        ioe.getMessage());
            }
        }
        Log.d(1, this + ": exit");
    }
    
    public synchronized void disconnect() {
        lock.writeLock().lock();
        try {
            for(ClientHandler client : clients) {
                client.disconnect();
            }
        } finally {
            lock.writeLock().unlock();
        }
        
        if(server != null) {
            try {
                server.close();
            } catch(IOException ioe) {
                Log.err(this +
                        ".disconnect: " + ioe);
            }
        }
        
        stop = true;
    }
    
    @Override
    public String toString() {
        return "SocketListener[port=" + port + (local ? ",local" : "") + "]";
    }
    
    class ClientHandler extends Thread {
        private Socket s;
        private boolean stop;
        
        public ClientHandler(Socket s) {
            this.s = s;
            stop = false;
        }
        
        @Override
        public void run() {
            Log.d(1, this + ": run");
            BufferedReader r;
            try {
                r = new BufferedReader(new InputStreamReader(
                        s.getInputStream()));
            
                while(!stop) {
                    String line;
                    // blocking read
                    try {
                        while((line = r.readLine()) != null) {
                            if(line.equals("disconnect")) {
                                disconnect();
                            } else {
                                notify.notify(line);
                            }
                        }
                    } catch(IOException ioe) {
                        Log.err(this + ".run: socket read error " +
                                "\n" + ioe);
                        disconnect();
                    }
                }
            } catch(IOException ioe) {
                Log.err(this + ".run: unable to open socket " +
                        "input stream");
                disconnect();
            }
            lock.writeLock().lock();
            try {
                clients.remove(this);
            } finally {
                lock.writeLock().unlock();
            }
            Log.d(1, this + ": exit");
        }
        
        public synchronized void disconnect() {
            try {
                s.close();
            } catch(IOException ioe) {
                Log.err(this +
                        ".disconnect: " + ioe);
            }
            stop = true;
        }
        
        @Override
        public String toString() {
            return "ClientHandler[" + s.getInetAddress() + "]";
        }
    }
}
