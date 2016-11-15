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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author wira
 */
public class NotifyCanvas extends JPanel {
    private int mode;
    private BufferedImage text;
    private float barRatio = 0;
    private int W, H;
    private int backgroundAlpha = 255;
    
    private Color colorBG = new Color(0, 0, 0);
    private Color colorBORDER = new Color(0x20, 0xbb, 0xff);
    private Color colorFG = new Color(0xff, 0xff, 0xff);
    private Color colorBAR = new Color(0x20, 0xbb, 0xff);
    private Color colorDISPLAY = new Color(0xff, 0x00, 0x00);
    
    private int borderWidth = 1;
    private int paddingH = 2;
    private int paddingV = 2;
    private float textUsableArea = 0.8f;
    
    private String font = "Monospaced";
    private ArrayList<Callback> clickHooks;
    private ArrayList<Callback> wheelHooks;
    
    public static final int MODE_TEXT       = 0;
    public static final int MODE_BARTITLE   = 1;
    public static final int MODE_BAR        = 2;
    public static final int MODE_COLOR      = 3;
    
    public static final int TEXT_LEFT       = 0;
    public static final int TEXT_CENTERED   = 1;
    public static final int TEXT_RIGHT      = 2;
    
    public static final int EVENT_CLICK     = 0;
    public static final int EVENT_DRAG      = 1;
    public static final int EVENT_WHEEL     = 2;
    
    public void registerClicks(Callback c) {
        if(clickHooks == null) {
            clickHooks = new ArrayList();
            
            this.addMouseListener(new MouseListener() {
                @Override
                public void mouseEntered(MouseEvent e) {}
                
                @Override
                public void mouseExited(MouseEvent e) {}
                
                @Override
                public void mouseReleased(MouseEvent e) {}
                
                @Override
                public void mousePressed(MouseEvent e) {}
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    for(Callback c : clickHooks) {
                        c.callback(EVENT_CLICK, e.getX(), e.getY(), e.getButton());
                    }
                }
            });
            
            
            this.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    for(Callback c : clickHooks) {
                        c.callback(EVENT_DRAG, e.getX(), e.getY(), -1);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {}     
            });
        }
        
        clickHooks.add(c);
    }    
    
    public void registerWheelMovement(Callback c) {
        if(wheelHooks == null ){
            wheelHooks = new ArrayList();
            
            this.addMouseWheelListener((MouseWheelEvent e) -> {
                for(Callback wheelHook : wheelHooks) {
                    wheelHook.callback(EVENT_WHEEL, e.getWheelRotation());
                }
            });
        }
        
        wheelHooks.add(c);
    }
    
    public void setOpacity(int alpha) {
        this.backgroundAlpha = alpha;
        this.setOpaque(false);
    }
    
    public void setMode(int mode) {
        this.mode = mode;
    }
    
    public void setFont(String font) {
        this.font = font;
    }
    
    public void setText(String str, int alignment) {
        Graphics2D g = (Graphics2D) this.getGraphics();
        W = this.getWidth();
        H = this.getHeight();        
        String lines[] = str.split("\\r?\\n");
        int fontsize = 0;
        do {
            fontsize++;
            g.setFont(new Font(font, Font.BOLD, fontsize));
        } while(g.getFontMetrics().getHeight()*lines.length <
                textUsableArea * (H-2*borderWidth-2*paddingV));
        int imageW = (W-2*borderWidth-2*paddingH);
        int imageH = (H-2*borderWidth-2*paddingV);
        text = new BufferedImage(imageW, imageH, BufferedImage.TYPE_4BYTE_ABGR);
        g = (Graphics2D) text.getGraphics();
        g.setFont(new Font(font, Font.BOLD, fontsize));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(colorFG);
        for(int i = 0; i < lines.length; i++) {
            int x;
            switch(alignment) {
                case TEXT_LEFT:
                    x = borderWidth + paddingH;
                    break;
                    
                case TEXT_RIGHT:
                    x = imageW - borderWidth - paddingH - 
                            g.getFontMetrics().stringWidth(lines[i]);
                    break;
                
                default:
                    x = imageW/2 - g.getFontMetrics().stringWidth(lines[i])/2;                    
            }
            float textExtraPadding = (1-textUsableArea)/2;
            int y = (int)(textExtraPadding*imageH + (i+1)*g.getFontMetrics().getHeight())
                    - g.getFontMetrics().getDescent();
            g.drawString(lines[i], x, y);
        }
    }
    
    public void setBarRatio(float f) {
        this.barRatio = f < 0 ? 0 : f > 1 ? 1 : f;
    }
    
    public float getBarRatio() {
        return barRatio;
    }
    
    public void setColor(String hex) {
        try {
            colorDISPLAY = parseHexColor(hex);
        } catch(NumberFormatException nfe) {
            Log.err(this + ": failed to set display color");
        }
    }
    
    public void setBarColor(String hex) {
        try {
            colorBAR = parseHexColor(hex);
        } catch(NumberFormatException nfe) {
            Log.err(this + ": failed to set bar color");
        }
    }
    
    public void setBorder(int width, int paddingH, int paddingV, String hex) {
        this.borderWidth = width;
        this.paddingH = paddingH;
        this.paddingV = paddingV;
        try {
            colorBORDER = parseHexColor(hex);
        } catch(NumberFormatException nfe) {
            Log.err(this + ": failed to set bar color");
        }
    }
    
    public void setBackgroundColor(String hex) {
        try {
            colorBG = parseHexColor(hex);
        } catch(NumberFormatException nfe) {
            Log.err(this + ": failed to set bar color");
        }
    }
    
    public void setForegroundColor(String hex) {
        try {
            colorFG = parseHexColor(hex);
        } catch(NumberFormatException nfe) {
            Log.err(this + ": failed to set bar color");
        }
    }
    
    public void setUsableTextArea(float f) {
        this.textUsableArea = f;
    }
    
    public int getBorderWidth() {
        return borderWidth;
    }  
    
    public int getPaddingH() {
        return paddingH;
    }
    
    public int getPaddingV() {
        return paddingV;
    }
    
    public static Color parseHexColor(String hex) {
        try {
            if(hex.startsWith("#")) {
                hex = hex.substring(1, hex.length());
            }
            Color c = null;
            if(hex.length() == 6) {
                c = new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16)            
                );
            } else if(hex.length() == 8) {
                c = new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16),
                        Integer.parseInt(hex.substring(6, 8), 16)
                );
            }

            return c;
        } catch(Exception e) {
            // just return black if we failed to parse
            return Color.BLACK;
        }
    }
    
    @Override
    public void paint(Graphics _g) {
        Graphics2D g = (Graphics2D) _g;
        W = this.getWidth();
        H = this.getHeight();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(colorBG);
        g.fillRect(0, 0, W, H);
        g.setColor(colorBORDER);
        for(int i = 0; i < borderWidth; i++) {
            g.drawRect(i, i, W-2*i-1, H-2*i-1);
        }         
        
        int width, height;
        
        switch(mode) {
            case MODE_BAR:
                drawBar(g);
                break;
            
            case MODE_BARTITLE:
                drawBar(g);
                
            case MODE_TEXT:
                if(text != null) {
                    g.drawImage(text, borderWidth+paddingH,
                            borderWidth+paddingV, this);
                }
                break;
                
            case MODE_COLOR:
                height = H - 2*borderWidth - 2*paddingV;
                width = W - 2*borderWidth - 2*paddingH;
                g.setColor(colorDISPLAY);
                g.fillRect(borderWidth+paddingH, borderWidth+paddingV,
                        width, height);
                break;
        }
    }
    
    private void drawBar(Graphics2D g) {
        int totalBarWidth = W - 2*borderWidth - 2*paddingH;
        int height = H - 2*borderWidth - 2*paddingV;
        int width = (int) (barRatio * totalBarWidth);
        g.setColor(colorBAR);
        g.fillRect(borderWidth+paddingH, borderWidth+paddingV,
                width, height);
    }
    
    @Override
    public String toString() {
        return "NotifyCanvas[mode=" + mode + "]";
    }
}
