/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.microedition.midlet.*;

/**
 * @author jacko
 */
public class HeclX extends MIDlet {

    Console c;

    public HeclX() {
        //open console
        c = new Console(this);
    }

    public void startApp() {
        c.pause(false);
    }

    public void pauseApp() {
        c.pause(true);
    }

    public void destroyApp(boolean unconditional) {
        c.exit();
        c = null;
        notifyDestroyed();
    }
}