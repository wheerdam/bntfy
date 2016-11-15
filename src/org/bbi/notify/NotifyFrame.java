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

import java.awt.event.MouseEvent;
import javax.swing.JFrame;

/**
 *
 * @author wira
 */
public class NotifyFrame extends JFrame {
    private final NotifyCanvas canvas;
    private String clickCommand;
    private String volumeUp;
    private String volumeDown;
    
    public NotifyFrame() {
        Log.d(1, "NotifyFrame: new");
        canvas = new NotifyCanvas();
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
    }
    
    public void init() {
        if(!this.isDisplayable()) {
            this.setUndecorated(true);
        }        
        
        // register click command if it's defined
        if(clickCommand != null) {
            canvas.registerClicks((Object[] params) -> {
                int button = (Integer) params[3];
                if(button == MouseEvent.BUTTON1 || button == -1) {
                    int x = (Integer) params[1];
                    int y = (Integer) params[2];
                    int border = canvas.getBorderWidth();
                    int pad = canvas.getPaddingH();
                    int margin = border + pad;
                    int width = canvas.getWidth() - 2*margin;
                    if(x > margin && x < canvas.getWidth()-margin) {
                        float ratio = (x-margin) / (float)width;
                        int percent = (int)(ratio*100);
                        try {
                            Notify.run(
                                    clickCommand + " " + percent);
                        } catch(Exception e) {
                            Log.err("click callback: failed to execute command");
                            Log.err(e.toString());
                        } 
                    }
                }
            });
        }
        
        // register right click to hide the frame
        canvas.registerClicks((Object[] params) -> {
            int button = (Integer) params[3];
            if(button == MouseEvent.BUTTON3) {
                this.setVisible(false);
            }
        });
        
        // register mouse wheel up and down for volume if defined
        if(volumeUp != null && volumeDown != null) {
            canvas.registerWheelMovement((Object[] params) -> {
                int clicks = (Integer) params[1];
                int i;
                if(clicks < 0) {
                    for(i = clicks; i < 0; i++) {
                        try {
                            Notify.run(volumeUp);
                        } catch(Exception e) {
                            Log.err("click callback: failed to execute command");
                            Log.err(e.toString());
                        } 
                    }
                } else {
                    for(i = clicks; i > 0; i--) {
                        try {
                            Notify.run(volumeDown);
                        } catch(Exception e) {
                            Log.err("click callback: failed to execute command");
                            Log.err(e.toString());
                        } 
                    }
                }
            });
        }
        
        add(canvas);
    }

    public void setVolumeCommand(String command) {
        this.clickCommand = command;
    }
    
    public void setVolUpCommand(String command) {
        this.volumeUp = command;
    }
    
    public void setVolDownCommand(String command) {
        this.volumeDown = command;
    }
    
    public void updateView() {
        canvas.repaint();
    }
    
    public void displayText(String str, int alignment) {
        canvas.setMode(NotifyCanvas.MODE_TEXT);
        canvas.setText(str, alignment);
    }
    
    public void displayBar(String title, float ratio, String color, int alignment) {
        
        if(title != null) {
            canvas.setText(title + " " + (int)(ratio*100) + "%", alignment);
            canvas.setMode(NotifyCanvas.MODE_BARTITLE);
        } else {
            canvas.setMode(NotifyCanvas.MODE_BAR);
        }
        canvas.setBarRatio(ratio);
        canvas.setBarColor(color);
    }
    
    public void displayColor(String colorHex) {
        canvas.setMode(NotifyCanvas.MODE_COLOR);
        canvas.setColor(colorHex);
    }

    public NotifyCanvas getCanvas() {
        return canvas;
    }
}
