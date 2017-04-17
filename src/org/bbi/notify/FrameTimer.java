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

/**
 *
 * @author wira
 */
public class FrameTimer extends Thread {
    private final NotifyFrame frame;
    private long delayTime;
    private long autohideInterval;
    private long frameDisplayTime;
    
    private boolean stop;
    private boolean autohide;
    
    public FrameTimer(NotifyFrame frame, long delayTime, long autohideInterval) {
        this.frame = frame;
        this.delayTime = delayTime;
        this.autohideInterval = autohideInterval;
        frameDisplayTime = System.currentTimeMillis();
        stop = false;
        autohide = true;
    }
    
    public void timestamp(boolean autohide) {
        Log.d(1, "FrameTimer: timestamp");
        frameDisplayTime = System.currentTimeMillis();
        this.autohide = autohide;
        
    }
    
    public void setAutohideInterval(long timeMs) {
        this.autohideInterval = timeMs;
    }
    
    public void setRefreshTime(long timeMs) {
        this.delayTime = timeMs;
    }
    
    public long getAutohideInterval() {
        return this.autohideInterval;
    }
    
    public long getRefreshTime() {
        return this.delayTime;
    }

    @Override
    public void run() {
        Log.d(1, "FrameTimer: run");
        long startTime, timeUsed;
        while(!stop) {
            startTime = System.currentTimeMillis();
            if(autohide && frame.isVisible() && autohideInterval > 0 &&
                    startTime - frameDisplayTime > autohideInterval) {
                frame.setVisible(false);
                stop = true;
            } else if(frame.isVisible()) {
                frame.updateView();
            } else if(!frame.isVisible()) {
                stop = true;
            }
            
            timeUsed = System.currentTimeMillis() - startTime;
            if(timeUsed < delayTime) {
                try {
                    Thread.sleep(delayTime - timeUsed);
                } catch(InterruptedException ie) {
                    Log.err("SocketListener.run: " + ie + "\n" +
                            ie.getMessage());
                }
            }
        }
        Log.d(1, "FrameTimer: exit");
    }
    
    public void stopThread() {
        stop = true;
    }    
}
