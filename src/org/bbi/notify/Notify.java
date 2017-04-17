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

import com.sun.awt.AWTUtilities;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 *
 * @author wira
 */
public class Notify {
    public static String CONFIG_PATH;
    
    private SocketListener socket;
    private UDPSocketListener socketUDP;
    private FrameTimer timer;
    private final NotifyFrame frame;
    private TrayIcon trayIcon;
    private final boolean system;
    private int port;
    
    private int frameWidth, frameHeight;
    private int x, y, displayIndex, location;
    private int insetLeft, insetRight, insetTop, insetBottom;
    private final int[] displayX, displayY;
    private final int[] displayW, displayH;
    public int trayWidth, trayHeight;
    private final boolean systray;
    private boolean trayReloadOption;
    private boolean trayCustomCommands;
    private final ArrayList<String> trayCommandsLabel;
    private final ArrayList<String> trayCommands;
    private String muteCommand;
    private String unmuteCommand;
    private String setVolumeCommand;
    private String volumeUpCommand;
    private String volumeDownCommand;
    private int trayPad;
    private Color trayBG;
    private Color trayBorder;
    
    private JCheckBoxMenuItem menuToggleMute;
    
    public static final int SYSTEM = 1;
    public static final int TRAY = 2;

    public static final int PROTOCOL_UDP = 1;
    public static final int PROTOCOL_TCP = 2;
    
    public static final int LOCATION_MANUAL = 0;
    public static final int LOCATION_TOP_RIGHT = 1;
    public static final int LOCATION_BOTTOM_RIGHT = 2;
    public static final int LOCATION_TOP_LEFT = 3;
    public static final int LOCATION_BOTTOM_LEFT = 4;
    public static final int LOCATION_CENTERED = 5;
    public static final int LOCATION_BOTTOM_CENTERED = 6;
    public static final int LOCATION_TOP_CENTERED = 7;
    public static final int MAX_SCREENS = 8;
    public static final String DEFAULT_BAR_COLOR = "20bbff";
    
    public static void printUsage() {
        Log.d(0, "");
        Log.d(0, "usage: java -jar <jarfile> [options]");
        Log.d(0, "options:");
        Log.d(0, "   -c FILE         load configuration from FILE");
        Log.d(0, "   -o, --override SECTION \"KEY=VALUE\"");
        Log.d(0, "                   add or override a configuration entry");
        Log.d(0, "   -d LEVEL        program verbosity for debugging");
        Log.d(0, "                     0: silent, output errors only (default)");
        Log.d(0, "                     1: output major state changes");
        Log.d(0, "                     2: output minor state changes");
        Log.d(0, "                     3: output all debug statements");
        Log.d(0, "   -r, --reload    send reload UDP command to localhost and configured port");
        Log.d(0, "                     and quit (default port is 25311 if not configured)");
        Log.d(0, "   -u, --send-udp HOST PORT COMMAND");
        Log.d(0, "                   send an arbitrary command to a UDP host");
        Log.d(0, "   -h, --help      show this help message and quit");
        Log.d(0, "   -f, --format    print configuration file format and quit");
        Log.d(0, "   -s, --socket    print socket interface commands list and quit");        
        Log.d(0, "");
    }
    
    public static void printConfigurationFormat() {
        Log.d(0, "configuration (INI-style format, use # for comments):");
        Log.d(0, "");
        Log.d(0, "[main]");
        Log.d(0, "port=NUMBER            port number to listen to (default 25311)");
        Log.d(0, "systemcommands={0,1}   enable config. reload and quitting from socket \n"+
                 "                         interface (default 0)");
        Log.d(0, "protocol={udp,tcp}     networking protocol to use (default UDP)");
        Log.d(0, "loopback={0,1}         only listen to loopback device (default 1)");        
        Log.d(0, "refresh_ms=TIME        set frame refresh rate TIME milliseconds (default 30)");
        Log.d(0, "autohide_ms=TIME       hide notification after TIME milliseconds (default 1000)");
        Log.d(0, "font=NAME              use the specified font (default: \"Monospaced\")");
        Log.d(0, "output_display=NUMBER  set output display, -1 for currently focused (default)");        
        Log.d(0, "set_volume=COMMAND     command to execute when the frame is clicked");
        Log.d(0, "mute_command=COMMAND   command to execute when mute menu is selected");
        Log.d(0, "unmute_command=COMMAND command to execute when unmute menu is selected");
        Log.d(0, "volume_up=COMMAND      command to execute when mouse wheel is scrolled up");
        Log.d(0, "volume_down=COMMAND    command to execute when mouse wheel is scrolled down");
        Log.d(0, "post_init=COMMAND      command to execute after initialization, useful to set\n"+
                 "                         initial display state");     
        Log.d(0, "");
        Log.d(0, "[frame] # notification frame options");
        Log.d(0, "width=PIXELS           set display frame width in pixels (default 500)");
        Log.d(0, "height=PIXELS          set display frame height in pixels (default 30)");
        Log.d(0, "window_title=NAME      set frame title (for window management purposes)");
        Log.d(0, "location={topright,bottomright,bottomcentered,bottomleft,topleft,topcentered,centered,manual}\n"+
                 "                       set notification frame location (default centered)");
        Log.d(0, "x=PIXELS\n"+
                 "y=PIXELS               set frame location if manual");
        Log.d(0, "inset_top=PIXELS\n"+
                 "inset_right=PIXELS\n"+
                 "inset_bottom=PIXELS\n"+
                 "inset_left=PIXELS      set distance of the frame from screen edges\n"+
                 "                         (defaults: 25, 15, 25, 15)");
        Log.d(0, "");
        Log.d(0, "[canvas] # canvas drawing options");
        Log.d(0, "border_width=PIXELS    border width (default 1)");
        Log.d(0, "border_color=HEXCOLOR  border color (default 20bbff)");
        Log.d(0, "padding_horizontal=PIXELS\n"+
                 "                       horizontal padding (default 2)");
        Log.d(0, "padding_vertical=PIXELS\n"+
                 "                       vertical padding (default 2)");
        Log.d(0, "usable_text_area=RATIO usable area in the proportion of the vertical space\n"+
                 "                         between the top and bottom padding of the display \n"+
                 "                         frame to display text. This value is relative to the\n"+
                 "                         font's *line height*, not the visible font glyph \n"+
                 "                         height (default 0.8, i.e. text *line height* occupies\n"+
                 "                         80% of the available vertical space)");
        Log.d(0, "background_color=HEXCOLOR\n"+
                 "                       background color (default 000000)");
        Log.d(0, "foreground_color=HEXCOLOR\n"+
                 "                       foreground / text color (default ffffff)");
        Log.d(0, "");
        Log.d(0, "[trayicon] # system tray icon options");
        Log.d(0, "enable={0,1}           enable system tray icon (default 1)");
        Log.d(0, "width=PIXELS\n"+
                 "height=PIXELS          set tray icon size (default: get from host system)");
        Log.d(0, "background_color=HEXCOLOR\n"+
                 "                       set tray icon background color in hex (default 000000)");
        Log.d(0, "border_color=HEXCOLOR  set tray icon border color in hex (default 5a5a5a)");
        Log.d(0, "padding=PIXELS         padding around tray icon (default: 0)");
        Log.d(0, "show_reload={0,1}      show the reload configuration option in tray popup menu\n"+
                 "                         (default 0)");
        Log.d(0, "custom_commands={0,1}  show custom commands in tray popup menu (default 0)");
        Log.d(0, "");        
        Log.d(0, "[traycommands]");
        Log.d(0, "LABEL=COMMAND          each command will have an entry labeled LABEL on the\n"+
                 "                         tray popup menu in the order in this section. COMMAND\n"+
                 "                         will be executed when the menu item is clicked");
        Log.d(0, "--=                    add separator");
        Log.d(0, "");
        Log.d(0, "note: COMMAND can be of multiple commands separated by semicolons");
        Log.d(0, "");
    }    
    
    public static void printSocketInterfaceCommands() {
        Log.d(0, "valid commands for the socket interface:");
        Log.d(0, "");
        Log.d(0, "bartitle:TITLE,PERCENT,HEXCOLOR,ALIGNMENT\n"+
                 "   show a bar filling PERCENT (0 to 100) of the frame width in HEXCOLOR\n"+
                 "   and a title text in one of three alignments: {left, centered, right}");
        Log.d(0, "");
        Log.d(0, "barpercent:PERCENT,HEXCOLOR\n"+
                 "   show a bar filling PERCENT (0 to 100) of the frame width in HEXCOLOR");
        Log.d(0, "");
        Log.d(0, "text:TEXT,ALIGNMENT\n"+
                 "   display a text notification with TEXT and the specified alignment\n"+
                 "   {left, centered, right}");
        Log.d(0, "");
        Log.d(0, "tray:TOOLTIP,PERCENT,HEXCOLOR\n"+
                 "   update the system tray icon");
        Log.d(0, "");
        Log.d(0, "color:HEXCOLOR\n"+
                 "   display a bar in HEXCOLOR");
        Log.d(0, "");
        Log.d(0, "mute:{on, off}\n"+
                 "   set the tray menu mute status on or off (note that this will NOT execute\n"+
                 "   the mute and unmute commands)");
        Log.d(0, "");
        Log.d(0, "autohideinterval:TIME\n"+
                 "   set auto hide interval in TIME milliseconds");
        Log.d(0, "");
        Log.d(0, "size:WIDTH,HEIGHT\n"+
                 "   set frame size in WIDTH and HEIGHT pixels");
        Log.d(0, "");
        Log.d(0, "execsetvol:PERCENT\nexecmute\nexecunmute\n"+
                 "   execute audio volume manipulation commands (if configured). Note that these\n"+
                 "   commands do not actually trigger a notification (the program does not\n"+
                 "   maintain audio volume state). Further commands will need to be sent to\n"+
                 "   create a notification or modify the tray icon state");
        Log.d(0, "");
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Log.d(0, "");
        Log.d(0, "org.bbi.Notify (bntfy) 1.0");
        Config config = new Config();
        CONFIG_PATH="";
        ArrayList<String[]> overrides = new ArrayList<>();
        boolean reload = false;
        
        // parse command line arguments and setup our stuff
        int len = args.length;
        for(int i = 0; i < len; i++) {
            try {
                if(args[i].equals("-c") && i+1 < args.length) {
                    CONFIG_PATH += (CONFIG_PATH.equals("") ? "" : ";") + args[i+1];
                    i++;
                } else if(args[i].equals("-d") && i+1 < args.length) {
                    Log.debugLevel = Integer.parseInt(args[i+1]);
                    i++; 
                } else if((args[i].equals("--override") ||
                          args[i].equals("-o")) && i+2 < args.length) {
                    String[] entry = new String[2];
                    entry[0] = args[i+1];
                    entry[1] = args[i+2];
                    overrides.add(entry);
                    i+=2;
                } else if(args[i].equals("--reload") ||
                        args[i].equals("-r")) {
                    reload = true;
                } else if(args[i].equals("--help") ||
                        args[i].equals("-h")) {
                    printUsage();    
                    exit(0);
                } else if(args[i].equals("--format") ||
                        args[i].equals("-f")) {
                    printConfigurationFormat();    
                    exit(0);
                } else if(args[i].equals("--socket") ||
                        args[i].equals("-s")) {
                    printSocketInterfaceCommands();    
                    exit(0);
                } else if((args[i].equals("--send-udp") ||
                        args[i].equals("-u")) &&
                        i+3 < args.length) {
                    UDPSocketListener.send(args[i+1], 
                            Integer.parseInt(args[i+2]), args[i+3]);
                    exit(0);
                } else {
                    Log.err("unknown argument: " + args[i]);
                }
            } catch(Exception e) {
                Log.err("unable to parse argument: " + args[i] + ", error=" + e);
            }
        }
        
        if(!CONFIG_PATH.equals("")) {
            config.load(CONFIG_PATH);
        }
        for(String[] entry : overrides) {
            Log.d(3, "config override: [" + entry[0] + "] " + entry[1]);
            config.parse("["+entry[0]+"]", entry[1]);
        }
        if(!reload) {
            create(config);
        } else {
            try {
                int port = (int) config.getValue(25311, "main", "port");
                UDPSocketListener.send("localhost", port, "reload");
            } catch(Exception e) {
                Log.err("Unable to send reload command: " + e);
            }
        }
    }    
    
    public static Notify create(Config config) {
        if(config == null) {
            config = new Config(); // make dummy
        }
        Notify n;
        try {
            int flags = 0;
            flags |= config.assertValue("1", "main", "systemcommands") ? Notify.SYSTEM : 0;
            flags |= config.assertValue("0", "trayicon", "enable") ? 0 : Notify.TRAY;
            
            n = new Notify(flags);
            n.setFrameSize(
                    (int) config.getValue(500, "frame", "width"),
                    (int) config.getValue(30, "frame", "height")
            );
            n.setFrameTitle(config.getValue("BBI NOTIFY", "frame", "window_title"));
            n.setCustomLocation(
                    (int) config.getValue(-1, "frame", "x"),
                    (int) config.getValue(-1, "frame", "y")
            );
            String configLocation = config.getValue("frame", "location");
            if(configLocation == null) {
                n.setFrameLocation(Notify.LOCATION_CENTERED);
            } else {
                switch(configLocation) {
                    case "manual":          n.setFrameLocation(LOCATION_MANUAL); break;
                    case "topright":        n.setFrameLocation(LOCATION_TOP_RIGHT); break;
                    case "bottomright":     n.setFrameLocation(LOCATION_BOTTOM_RIGHT); break;
                    case "topleft":         n.setFrameLocation(LOCATION_TOP_LEFT); break;
                    case "bottomleft":      n.setFrameLocation(LOCATION_BOTTOM_LEFT); break;
                    case "bottomcentered":  n.setFrameLocation(LOCATION_BOTTOM_CENTERED); break;
                    case "topcentered":     n.setFrameLocation(LOCATION_TOP_CENTERED); break;
                    default:                n.setFrameLocation(LOCATION_CENTERED); break;
                }
            }
            n.setOutputDisplay(
                    (int) config.getValue(-1, "main", "output_display"));
            n.setDisplayInsets(
                    (int) config.getValue(25, "frame", "inset_top"),
                    (int) config.getValue(15, "frame", "inset_right"),
                    (int) config.getValue(25, "frame", "inset_bottom"),
                    (int) config.getValue(15, "frame", "inset_left")
            );
            n.setCustomTrayIconSize(
                    (int) config.getValue(-1, "trayicon", "width"),
                    (int) config.getValue(-1, "trayicon", "height")
            );
            n.setTrayIconOptions(
                    config.getValue("000000", "trayicon", "background_color"),
                    config.getValue("5a5a5a", "trayicon", "border_color"),
                    (int) config.getValue(0, "trayicon", "padding"),
                    config.assertValue("1", "trayicon", "show_reload"),
                    config.assertValue("1", "trayicon", "custom_commands")
            );
            n.setMuteCommands(
                    config.getValue(null, "main", "mute_command"),
                    config.getValue(null, "main", "unmute_command")
            );
            if(config.hasSection("traycommands")) {
                for(String key : config.getKeysInOriginalOrder("traycommands")) {
                    n.addTrayCustomCommand(key, config.getValue("traycommands", key));
                }
            }
            n.setVolumeCommands(
                    config.getValue("main", "set_volume"),
                    config.getValue("main", "volume_up"),
                    config.getValue("main", "volume_down")
            );
            n.getFrame().getCanvas().setFont(
                    config.getValue("Monospaced", "main", "font"));
            n.getFrame().getCanvas().setUsableTextArea(
                    (float) config.getValue(0.8, "canvas", "usable_text_area")
            );
            n.getFrame().getCanvas().setBorder(
                    (int) config.getValue(1, "canvas", "border_width"),
                    (int) config.getValue(2, "canvas", "padding_horizontal"),
                    (int) config.getValue(2, "canvas", "padding_vertical"),
                    config.getValue("20bbff", "canvas", "border_color")
            );
            n.getFrame().getCanvas().setBackgroundColor(
                    config.getValue("000000", "canvas", "background_color"));
            n.getFrame().getCanvas().setForegroundColor(
                    config.getValue("ffffff", "canvas", "foreground_color"));
            n.getTimer().setRefreshTime(
                    (int) config.getValue(30, "main", "refresh_ms"));
            n.getTimer().setAutohideInterval(
                    (int) config.getValue(1000, "main", "autohide_ms"));
            n.start();
            n.listen(
                    (int) config.getValue(25311, "main", "port"),
                    config.assertValue("tcp", "main", "protocol") ? Notify.PROTOCOL_TCP :
                            Notify.PROTOCOL_UDP,
                    !config.assertValue("0", "main", "loopback")
            );
            run(config.getValue("", "main", "post_init"));
            return n;
        } catch(Exception e) {
            Log.err("FATAL: failed to initialize");
            Log.err("FATAL: " + e);
            Log.err("STACK TRACE:");
            e.printStackTrace();
        }
        return null;
    }    
    
    public static boolean getFlag(int value, int mask) {
        return (value & mask) == mask;
    }
        
    public Notify(int flags) {
        frame = new NotifyFrame();     
        this.system = getFlag(flags, Notify.SYSTEM);
        this.systray = getFlag(flags, Notify.TRAY);
        if(system) {
            Log.d(1, this + ": system commands accepted over socket interface");
        }
        if(systray) {
            Log.d(1, this + ": system tray icon enabled");
        }
        
        // default locations and dimensions
        location = Notify.LOCATION_CENTERED;
        x = -1;
        y = -1;
        displayX = new int[MAX_SCREENS];
        displayY = new int[MAX_SCREENS];
        displayW = new int[MAX_SCREENS];
        displayH = new int[MAX_SCREENS];
        for(int i = 0; i < MAX_SCREENS; i++) {
            displayX[i] = -1;
            displayY[i] = -1;
            displayW[i] = -1;
            displayH[i] = -1;
        }
        frameWidth = 500;
        frameHeight = 30;
        insetLeft = 25;
        insetRight = 25;
        insetTop = 25;
        insetBottom = 25;
        displayIndex = -1;
        trayWidth = -1;
        trayHeight = -1;
        trayPad = 0;
        trayBG = Color.BLACK;
        trayBorder = new Color(0x5a, 0x5a, 0x5a);
        trayReloadOption = false;
        trayCustomCommands = false;
        trayCommands = new ArrayList();
        trayCommandsLabel = new ArrayList();
        
        // initialize timer, but don't start it yet
        timer = new FrameTimer(frame, 30, 1000);        
    }
    
    public void listen(int port, int protocol, boolean localhost) {
        this.port = port;
        switch(protocol) {
            case Notify.PROTOCOL_TCP:
                if(socket != null) {
                    socket.disconnect();
                }
                socket = new SocketListener(this, port, localhost);
                socket.start();
                break;
            case Notify.PROTOCOL_UDP:
                if(socketUDP != null) {
                    socket.disconnect();
                }
                socketUDP = new UDPSocketListener(this, port, localhost);
                socketUDP.start();
                break;
        }
    }
    
    public void setFrameSize(int w, int h) {
        this.frameWidth = w;
        this.frameHeight = h;
    } 
    
    public void setFrameTitle(String txt) {
        frame.setTitle(txt);
    }
    
    public void setCustomLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setDisplayInsets(int top, int right, int bottom, int left) {
        insetTop = top;
        insetRight = right;
        insetBottom = bottom;
        insetLeft = left;
    }
    
    public void setCustomTrayIconSize(int width, int height) {
        trayWidth = width;
        trayHeight = height;
    }
    
    public void setOutputDisplay(int display) {
        this.displayIndex = display;
    }
    
    public void setFrameLocation(int location) {
        this.location = location;
    }
    
    public void setMuteCommands(String mute, String unmute) {
        this.muteCommand = mute;
        this.unmuteCommand = unmute;
    }
    
    public void setVolumeCommands(String set, String up, String down) {
        this.setVolumeCommand = set;
        this.volumeUpCommand = up;
        this.volumeDownCommand = down;
        frame.setVolumeCommand(set);
        frame.setVolUpCommand(up);
        frame.setVolDownCommand(down);
    }
    
    public String getSetVolumeCommand() {
        return setVolumeCommand;
    }
    
    public String getVolumeUpComand() {
        return volumeUpCommand;
    }
    
    public String getVolumeDownComand() {
        return volumeDownCommand;
    }
    
    public FrameTimer getTimer() {
        return timer;
    }
    
    public void setTrayIconOptions(String background, String border, 
            int padding, boolean trayReloadOption, boolean customCommands) {
        this.trayBG = NotifyCanvas.parseHexColor(background);
        this.trayBorder = NotifyCanvas.parseHexColor(border);
        this.trayPad = padding;
        this.trayReloadOption = trayReloadOption;
        this.trayCustomCommands = customCommands;
    }
    
    public void addTrayCustomCommand(String label, String command) {
        Log.d(3, this + ".addTrayCustomCommand: " + label + "=" + command);
        trayCommandsLabel.add(label);
        trayCommands.add(command);
    }
    
    public void show(boolean autohide, int...extra) {
        timer.timestamp(autohide);
        int screenIndex = displayIndex;
        int place = location;
        int offsetX, offsetY, width, height;
        
        // overrides
        if(extra.length == 1) {
            place = extra[0];
        }
        if(extra.length == 2) {
            place = extra[0];
            screenIndex = extra[1];
        }      
        if(extra.length == 3 && extra[0] == Notify.LOCATION_MANUAL &&
                !frame.isVisible()) {
            Log.d(2, this + ".show: " + 
                    "manual placement: " + extra[1] + "," + extra[2]);
            frame.setLocation(extra[1], extra[2]);
            frame.setAlwaysOnTop(true);
            if(!frame.isDisplayable()) {
                frame.setUndecorated(true);
            }
            frame.setFocusableWindowState(false);
            frame.setVisible(true);
            timer = new FrameTimer(frame, timer.getRefreshTime(), 
                                          timer.getAutohideInterval());
            timer.start();
            timer.timestamp(autohide);
            return;
        }
        
        if(!frame.isVisible()) {
            timer = new FrameTimer(frame, timer.getRefreshTime(), 
                                          timer.getAutohideInterval());
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            
            if(screenIndex >= ge.getScreenDevices().length) {
                screenIndex = -1;
            }
            
            if(screenIndex >= 0 && screenIndex < MAX_SCREENS) {
                // screen not defined yet, get dimensions
                if(displayX[screenIndex] == -1 || displayY[screenIndex] == -1) {
                    GraphicsDevice[] gd = ge.getScreenDevices();
                    if(screenIndex >= 0 && screenIndex < gd.length) {
                        Rectangle bounds = gd[screenIndex].getDefaultConfiguration().getBounds();
                        displayX[screenIndex] = bounds.x;
                        displayY[screenIndex] = bounds.y;
                        displayW[screenIndex] = bounds.width;
                        displayH[screenIndex] = bounds.height;
                    }
                }
                offsetX = displayX[screenIndex];
                offsetY = displayY[screenIndex];
                width = displayW[screenIndex];
                height = displayH[screenIndex];
                
            } else if(screenIndex >= MAX_SCREENS) {
                return; // nothing to do
            
            // find current active screen dimension (where the mouse pointer is)
            } else {
                GraphicsDevice g = MouseInfo.getPointerInfo().getDevice();
                Rectangle bounds = g.getDefaultConfiguration().getBounds();
                offsetX = bounds.x;
                offsetY = bounds.y;
                width = bounds.width;
                height = bounds.height;
            }
            
            Log.d(2, this + ".show: " + 
                    screenIndex + "= off: " + offsetX + ":" + offsetY + " - " +
                    "size: " + width + "x" + height + " - orientation: " + place);
        
            switch(place) {
                case Notify.LOCATION_TOP_RIGHT:
                    frame.setLocation(offsetX + width - insetRight - frameWidth,
                            offsetY + insetTop
                    );
                    break;
                case Notify.LOCATION_BOTTOM_RIGHT:
                    frame.setLocation(offsetX + width - insetRight - frameWidth,
                            offsetY + height - insetBottom - frameHeight
                    );
                    break;
                case Notify.LOCATION_TOP_LEFT:
                    frame.setLocation(offsetX + insetLeft,
                            offsetY + insetTop
                    );
                    break;
                case Notify.LOCATION_BOTTOM_LEFT:
                    frame.setLocation(offsetX + insetLeft,
                            offsetY + height - insetBottom - frameHeight
                    );
                    break;
                case Notify.LOCATION_BOTTOM_CENTERED:
                    frame.setLocation(offsetX + width/2 - frameWidth/2,
                            offsetY + height - insetBottom - frameHeight
                    );
                    break;
                case Notify.LOCATION_TOP_CENTERED:
                    frame.setLocation(offsetX + width/2 - frameWidth/2,
                            offsetY + insetTop
                    );
                    break;
                case Notify.LOCATION_CENTERED:
                    frame.setLocationRelativeTo(null);
                    break;
                case Notify.LOCATION_MANUAL:
                    frame.setLocation(x, y);
                    break;
            }
            
            // frame.setFocusableWindowState(false);
            frame.setVisible(true);
            timer.start();
        }
    }
    
    public void notify(String str) {
        Log.d(3, this + ".notify: data=" + str);        
        if(system && str.equals("quit")) {
            stop();
            Notify.exit(0);
        } else if(system && str.equals("reload")) {
            reload();
        }
        frame.setSize(frameWidth, frameHeight);        
        try {
            String[] tokens = str.trim().split(":", 2);
            switch (tokens[0]) {
                case "bartitle":
                    {
                        tokens = tokens[1].split(",");
                        int alignment = 0;
                        float ratio = Float.parseFloat(tokens[1].trim())/100;
                        switch(tokens[3].trim().toLowerCase()) {
                            case "left":
                                alignment = NotifyCanvas.TEXT_LEFT;
                                break;
                            case "right":
                                alignment = NotifyCanvas.TEXT_RIGHT;
                                break;
                            case "centered":
                                alignment = NotifyCanvas.TEXT_CENTERED;
                                break;
                        }       
                        frame.displayBar(tokens[0].trim(),
                                ratio, tokens[2],
                                alignment);
                        show(true);
                        break;
                    }
                case "barpercent":
                    {
                        tokens = tokens[1].split(",");
                        int alignment = 0;
                        float ratio = Float.parseFloat(tokens[0].trim())/100;
                        switch(tokens[1].trim().toLowerCase()) {
                            case "left":
                                alignment = NotifyCanvas.TEXT_LEFT;
                                break;
                            case "right":
                                alignment = NotifyCanvas.TEXT_RIGHT;
                                break;
                            case "centered":
                                alignment = NotifyCanvas.TEXT_CENTERED;
                                break;
                        }       
                        frame.displayBar(null, ratio, tokens[1], alignment);
                        show(true);
                        break;
                    }
                case "text":
                    {
                        tokens = tokens[1].split(",");
                        int alignment = 0;
                        if(tokens.length > 1) {
                            switch(tokens[1].trim().toLowerCase()) {
                                case "left":
                                    alignment = NotifyCanvas.TEXT_LEFT;
                                    break;
                                case "right":
                                    alignment = NotifyCanvas.TEXT_RIGHT;
                                    break;
                                case "centered":
                                    alignment = NotifyCanvas.TEXT_CENTERED;
                                    break;
                            }
                        } else {
                            alignment = NotifyCanvas.TEXT_CENTERED;
                        }
                        frame.displayText(tokens[0].trim(),
                                alignment);
                        show(true);
                        break;
                    }
                case "tray":
                    {
                        if(systray) {
                            tokens = tokens[1].split(",");
                            float ratio = Float.parseFloat(tokens[1].trim())/100;
                            trayIcon.setToolTip(tokens[0].equals("") ? null : 
                                    tokens[0].trim());
                            trayIcon.setImage(createTrayIcon(ratio, tokens[2], 
                                    trayWidth, trayHeight));
                        }
                        break;
                    }
                case "color":
                    {
                        frame.displayColor(tokens[1].trim());
                        show(true);
                        break;
                    }
                case "mute":
                    {   
                        if(menuToggleMute == null)
                            return;
                        if(tokens[1].equals("on")) {
                            menuToggleMute.setSelected(true);
                        } else if(tokens[1].equals("off")) {
                            menuToggleMute.setSelected(false);
                        }
                        break;
                    }
                case "execmute":
                    {
                        run(muteCommand);
                        break;
                    }
                case "execunmute":
                    {
                        run(unmuteCommand);
                        break;
                    }
                case "execsetvol":
                    {
                        run(setVolumeCommand + " " + tokens[1]);
                        break;
                    }
                case "execvolup":
                    {
                        run(volumeUpCommand);
                        break;
                    }
                case "execvoldown":
                    {
                        run(volumeDownCommand);
                        break;
                    }
                case "autohideinterval":
                    {
                        timer.setAutohideInterval(Long.parseLong(tokens[1]));
                        break;
                    }
                case "size":
                    {
                        tokens = tokens[1].split(",");
                        frameWidth = Integer.parseInt(tokens[0].trim());
                        frameHeight = Integer.parseInt(tokens[1].trim());
                        break;
                    }
                case "opacity":
                    {
                        float ratio = Float.parseFloat(tokens[1])/100;
                        AWTUtilities.setWindowOpacity(frame, ratio);
                    }
                default:
                    break;
            }
        } catch(Exception e) {
            Log.err(this + ": unable to parse command " + str);
            Log.err(this + ": " + e);
            e.printStackTrace();
        }
    }
    
    public NotifyFrame getFrame() {
        return frame;
    }
    
    public void start() {
        frame.init();
        
        if(systray) {
            initTrayIcon();
        }
        
        frame.pack();
        frame.setSize(frameWidth, frameHeight); 
    }
    
    public void stop() {
        if(socket != null) {
            socket.disconnect();
        }
        if(socketUDP != null) {
            socketUDP.disconnect();
        }
        timer.stopThread();
        SystemTray.getSystemTray().remove(trayIcon);
    }
    
    public void reload() {
        frame.dispose();
        stop();
        Notify.create(new Config(CONFIG_PATH)); //
    }
     
    private void initTrayIcon() {
        if(!SystemTray.isSupported()) {
            Log.err("System tray icon is not supported by this " +
                    "platform.");
            return;
        }
        
        try {
            final SystemTray tray = SystemTray.getSystemTray();
            if(trayWidth == -1 || trayHeight == -1) {
                trayWidth = tray.getTrayIconSize().width;
                trayHeight = tray.getTrayIconSize().height;
            }
            trayIcon = new TrayIcon(createTrayIcon(0, "20bbff", 
                    trayWidth, trayHeight));
            final JPopupMenu popup = new JPopupMenu();
            if(trayCustomCommands && trayCommands.size() > 0) { 
                int i = 0;
                for(String key : trayCommandsLabel) {
                    if(key.equals("--")) {
                        popup.add(new JSeparator());
                        i++;
                        continue;
                    }
                    String exec = trayCommands.get(i);
                    JMenuItem menu = new JMenuItem(key);
                    menu.addActionListener((ActionEvent e) -> {
                        run(exec);
                    });
                    popup.add(menu);
                    i++;
                }
                popup.add(new JSeparator());
            }

            if(trayReloadOption) {
                JMenuItem menuReloadConfig = new JMenuItem("Reload Config");
                menuReloadConfig.addActionListener((ActionEvent e) -> {
                    reload();
                });
                popup.add(menuReloadConfig);
            }
            String mute = muteCommand;
            String unmute = unmuteCommand;
            if(mute != null && unmute != null) {
                menuToggleMute = new JCheckBoxMenuItem("Toggle Mute");
                menuToggleMute.addActionListener((ActionEvent e) -> {
                        run(menuToggleMute.isSelected() ? mute : unmute);
                });

                popup.add(menuToggleMute);
                popup.add(new JSeparator());
            }               
            
            JMenuItem menuExit = new JMenuItem("Exit");
            menuExit.addActionListener((ActionEvent e) -> {
                int ret = JOptionPane.showConfirmDialog(null,  
                        "Close the awesome notification system?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if(ret == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            });
            popup.add(menuExit);
            JMenuItem menuCancel = new JMenuItem("Cancel");            
            menuCancel.addActionListener((ActionEvent e) -> {
                popup.setVisible(false);
            });
            popup.add(menuCancel);
            
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(e.getButton() == MouseEvent.BUTTON1) {
                        if(!frame.isVisible()) {
                            show(false, Notify.LOCATION_MANUAL,
                                    e.getX()-frameWidth-20,
                                    e.getY()-frameHeight-5
                            );
                        } else {
                            timer.stopThread();
                            frame.setVisible(false);
                        }
                    } else if(e.getButton() == MouseEvent.BUTTON2) {
                        String mute = muteCommand;
                        String unmute = unmuteCommand;
                        if(mute != null && unmute != null) {
                            run(!menuToggleMute.isSelected() ? mute : unmute);
                            menuToggleMute.setSelected(!menuToggleMute.isSelected());
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if(e.isPopupTrigger()) {
                        popup.setLocation(e.getX(), e.getY());
                        popup.setInvoker(popup);
                        popup.setVisible(true);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(e.isPopupTrigger()) {
                        popup.setLocation(e.getX(), e.getY());
                        popup.setInvoker(popup);
                        popup.setVisible(true);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });
            
            tray.add(trayIcon);
        } catch(AWTException e) {
            Log.err("Unable to initialize system tray icon.");
        }        
    }    
    
    public BufferedImage createTrayIcon(float ratio, String color,
            int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();        
        g.setColor(trayBG);
        g.fillRect(0, 0, w, h);
        g.setColor(trayBorder);
        g.drawRect(0+trayPad, 0+trayPad, w-1-2*trayPad, h-1-2*trayPad);
        g.setColor(NotifyCanvas.parseHexColor(color));
        g.fillRect(1+trayPad,
                1+trayPad+(h-2-2*trayPad)-(int)(ratio*(h-2-2*trayPad)), 
                w-2-2*trayPad, (int)(ratio*(h-2-2*trayPad)));
        
        return img;
    }
    
    public static void exit(int code) {
        System.exit(code);
    }
    
    public static void run(String command) {
        String[] commands = command.split(";");
        for(String exec : commands) {
            Log.d(3, "Notify.run: \"" + exec.trim() + "\"");
            try {
                Runtime.getRuntime().exec(exec.trim());
            } catch(Exception ee) {
                Log.err(ee.toString());
            }
        }
    }
    
    @Override
    public String toString() {
        return "Notify[" + port + "]";
    }
}
