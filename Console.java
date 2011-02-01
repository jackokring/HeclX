/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import org.hecl.Interp;
import org.hecl.HeclException;
import org.hecl.Thing;

/**
 *
 * @author jacko
 */
public class Console extends TextBox implements CommandListener {

    TextBox i;
    public Command e = new Command("Eval",Command.SCREEN,0);
    public Command x = new Command("Exit",Command.EXIT,0);

    public void commandAction(Command c, Displayable d) {
        if(c==e && d==i) {
            set(me);
            lock = true;
        }
        if(c==x && d==i) set(me);
        if(c==e && d==me) set(i);
        if(c==x && d==me) exit();//exit control
    }

    MIDlet m;

    public Console(MIDlet dis) {
        super("HeclX","",1024,TextField.UNEDITABLE|TextField.ANY);
        me = this;
        m = dis;
        i = new TextBox("Input","",1024,TextField.ANY);
        i.setCommandListener(this);
        i.addCommand(e);
        i.addCommand(x);
        me.setCommandListener(this);
        me.addCommand(e);
        me.addCommand(x);
        set(me);//display
        try {
            interp = new Interp(Display.getDisplay(m));
        } catch(Exception e) {
            set(new Alert("Failed to load HeclX"));
        }
        (new Thread(r)).start();
    }

    Runnable r = new Runnable() {
        public void run() {
            while(true) {
                if(lock) {
                    lock = false;
                    String cmd = i.getString();
                    i.setString("");
                    try {
                        cmd = interp.eval(new Thing(cmd)).toString();
                    } catch(Exception e) {
                        cmd = e.toString();
                    }
                    me.setString(cmd);//result
                }
                Thread.yield();
            }
        }
    };
    Console me;
    
    boolean lock = false;
    public Interp interp;

    public void set(Displayable d) {
        Display.getDisplay(m).setCurrent(d);
    }

    public void exit() {
        set(null);
        m.notifyDestroyed();
    }

    public void pause(boolean p) {
        
    }
}