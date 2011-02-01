
package org.hecl;
import javax.microedition.lcdui.*;

/**
 * <code>PutsCmd</code> implements the "puts" command.
 *
 * Outputs puts to a common form control.
 */

class PutsCmd extends IOCmd {
    Thing doCode(Interp interp, Thing[] argv, Displayable d) throws HeclException {
        for(int i=1;i<argv.length;i++)
            ((Form)d).append(argv[i].toString());
	return null;
    }

    Displayable make() {
        return new Form("Output");
    }

    void process(javax.microedition.lcdui.Command c) {
        //no extra commands
    }
}
