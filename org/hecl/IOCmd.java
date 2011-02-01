/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hecl;
import javax.microedition.lcdui.*;

/**
 *
 * @author jacko
 */
public abstract class IOCmd implements Command, CommandListener {
    
    Displayable d, old;
    Display m;
    final javax.microedition.lcdui.Command ex =
            new javax.microedition.lcdui.Command("Exit",javax.microedition.lcdui.Command.EXIT,0);

    public Thing cmdCode(Interp interp, Thing[] argv) throws HeclException {
        if(d == null) {
            d = make();
            d.setCommandListener(this);
            d.addCommand(ex);
            m = interp.output;
        }
        old = m.getCurrent();
        m.setCurrent(d);
        return doCode(interp,argv,d);
    }

    abstract Thing doCode(Interp interp, Thing[] argv, Displayable d) throws HeclException;

    abstract Displayable make();

    abstract void process(javax.microedition.lcdui.Command c);

    public void commandAction(javax.microedition.lcdui.Command c, Displayable di) {
        if(c == ex) {
            m.setCurrent(old);
            d = null;
        } else process(c);
    }
}
