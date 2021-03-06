/* Copyright 2004-2010 David N. Welton, DedaSys LLC

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

package org.hecl;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import javax.microedition.lcdui.Display;


/**
 * <code>Interp</code> is the Hecl interpreter, the class responsible for
 * knowing what variables and commands are available.
 *
 * @author <a href="mailto:davidw@dedasys.com">David N. Welton </a>
 * @version 1.0
 */
public class Interp extends Thread/*implements Runnable*/ {

    Display output = null;

    public Interp(Display d) throws HeclException {
        this();
        output = d;
    }
    /**
     * Package name prefix of the module classes.
     */
    public static final String MODULE_CLASS_PACKAGE = "org.hecl";

    /**
     * A <code>Thing</code> to indicate  global reference.
     */
    static final Thing GLOBALREFTHING = new Thing("");

    /* Used to cache command lookups. */
    public long cacheversion = 0;

    /* Do we have the Java reflection stuff? */
    private static boolean javacmdpresent = false;

    /**
     * The <code>commands</code> <code>Hashtable</code> provides the
     * mapping from the strings containing command names to the code
     * implementing the commands.
     */
    protected Hashtable commands = new Hashtable();

    /**
     * The <code>auxdata</code> <code>Hashtable</code> is a place to
     * store extra information about the state of the program.
     *
     */
    protected Hashtable auxdata = new Hashtable();

    protected Stack stack = new Stack();
    protected Stack error = new Stack();

    protected Vector ci = new Vector();
    protected Hashtable classcmdcache = new Hashtable();

    /**
     * <code>procclass</code> is used by CLDC 1.0 to store the 'Proc'
     * class, since you can't just do Proc.class in that version of
     * J2ME.
     *
     */

    /**
     * <code>eol</code> is the end-of-line character or characters.
     *
     */
    public static final char eol[] = { '\n' };

    /**
     * Creates a new <code>Interp</code> instance, initializing command and
     * variable hashtables, a stack, and an error stack.
     *
     * @exception HeclException if an error occurs
     */
    public Interp() throws HeclException {

        // Set up stack frame for globals.
        stack.push(new Hashtable());
        initInterp();
	start();
    }

    /**
     * Add a new class command to an <code>Interp</code>.
     *
     * @param clazz The Java class the command should operate on.
     * @param cmd The command to add. When this paramter is <code>null</code>,
     * an existing command is removed.
     */
    public void addClassCmd(Class clazz,ClassCommand cmd) {
	// clear cache first, even when deleting a cmd
	this.classcmdcache.clear();
	
	int l = this.ci.size();
	for(int i=0; i<l; ++i) {
	    ClassCommandInfo info = (ClassCommandInfo)this.ci.elementAt(i);
	    if(info.forClass() == clazz) {
		//identical, replace
		if(cmd == null) {
		    ci.removeElementAt(i);
		} else {
		    info.setCommand(cmd);
		}
		return;
	    }
	}
	if(cmd != null)
	    this.ci.addElement(new ClassCommandInfo(clazz,cmd));
    }

    /**
     * Remove a command for a specific class from an <code>Interp</code>.
     *
     * @param clazz The class to remove the command for.
     */
    public void removeClassCmd(Class clazz) { addClassCmd(clazz,null);}
	    
    
     /**
     * Add a new class command to an <code>Interp</code>.
     *<br>
     * The current implementation does not support any subclassing and selects
     * the first class command <code>clazz</code> is assignable to.
     *
     * @param clazz The Java class to look up the class command for.
     * @return A <code>ClassCommandInfo</code> decsribing the class command,
     * or <code>null</null> if no command was found.
     */
    ClassCommandInfo findClassCmd(Class clazz) {
	ClassCommandInfo found = (ClassCommandInfo)this.classcmdcache.get(clazz);

	if(found == null) {
	    // No entry in cache, so we loop over all class commands and try
	    // to detect the most specific one.

	    int l = this.ci.size();
	    for(int i=0; i<l; ++i) {
		ClassCommandInfo info = (ClassCommandInfo)this.ci.elementAt(i);
		Class cl2 = info.forClass();
		if(cl2.isAssignableFrom(clazz)) {
		    //System.err.println("clazz="+clazz+" assignable to cl="+cl2);
		    if(found == null)
			found = info;
		    else {
			// check if this is more specialized than the one we
			// already have.
			if(found.forClass().isAssignableFrom(cl2)) {
			    //System.err.println("superclass="+found.forClass()+" for cl="+cl2);
			    found = info;
			}
			// else keep existing one
		    }
		}
	    }

	    // Add what we found to the cache, so we do not need to look it up
	    // next time.
	    if(found != null) {
		this.classcmdcache.put(clazz,found);
	    } else if (javacmdpresent) {
		/* Still nothing - let's see if we have the Java
		 * reflection stuff loaded and try using that.  FIXME -
		 * consider making this configurable? */
		return findClassCmd(clazz);

	    }

	}
	return found;
    }
    
    
    /**
     * Add a new command to an <code>Interp</code>.
     *
     * @param name the name of the command to add.
     * @param c the command to add.
     */
    public synchronized String addCommand(String name,Command c) {
	commands.put(name,c);
	return name;
    }

    /**
     * Remove a command from an <code>Interp</code>.
     *
     * @param name the name of the command to add.
     */
    public synchronized void removeCommand(String name) {
	commands.remove(name);
    }

    /**
     * The <code>commandExists</code> method returns true if a command
     * exists, otherwise false.
     *
     * @param name a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public synchronized boolean commandExists(String name) {
	return commands.containsKey(name);
    }

    /**
     * Attach auxiliary data to an <code>Interp</code>.
     */
    public synchronized void setAuxData(String key,Object value) {
	auxdata.put(key, value);
    }


    /**
     * Retrieve auxiliary data from an <code>Interp</code>.
     *
     * @return a <code>Object</code> value or <code>null</code> when no
     * auxiliary data under the given key is attached to the interpreter.
     */
    public synchronized Object getAuxData(String key) {
	return auxdata.get(key);
    }


    /**
     * Remove auxiliary data from an <code>Interp</code>.
     */
    public synchronized void removeAuxData(String key) {
	auxdata.remove(key);
    }

    /**
     * The <code>eval</code> method evaluates some Hecl code passed to
     * it.
     *
     * @return a <code>Thing</code> value - the result of the
     * evaluation.
     * @exception HeclException if an error occurs.
     */
    public synchronized Thing eval(Thing in) throws HeclException {
	//System.err.println("-->eval: "+in.toString());
	return CodeThing.get(this, in).run(this);
    }

    
    /**
     * This version of <code>eval</code> takes a 'level' argument that
     * tells Hecl what level to run the code at.  Level 0 means
     * global, negative numbers indicate relative levels down from the
     * current stackframe, and positive numbers mean absolute stack
     * frames counting up from 0.
     *
     * @param in a <code>Thing</code> value
     * @param level an <code>int</code> value
     * @return a <code>Thing</code> value
     * @exception HeclException if an error occurs
     */
    public Thing eval(Thing in, int level) throws HeclException {
	Thing result = null;
	Vector savedstack = new Vector();
	int stacklen = stack.size();
	int i = 0;
	int end = 0;
	HeclException save_exception = null;

	if (level >= 0) {
	    end = level;
	} else {
	    end = (stacklen - 1 + level);
	}

	/* Save the old stack frames... */
	for (i = stacklen - 1; i > end; i--) {
	    savedstack.addElement(stackDecr());
	}
	try {
	    result = eval(in);
	} catch (HeclException he) {
	    /* If this is an upeval situation, we need to catch the
	     * exception and then throw it *after* the old stack frame
	     * has been restored.  */
	    save_exception = he;
	}
	/* ... and then restore them after evaluating the code. */
	for (i = savedstack.size() - 1; i >= 0; i--) {
	    stackPush((Hashtable)savedstack.elementAt(i));
	}
	if (save_exception != null) {
	    throw save_exception;
	}

	return result;
    }

    /**
     * The <code>initCommands</code> method initializes all the built in
     * commands. These are commands available in all versions of Hecl. J2SE
     * commands are initialized in Standard.java, and J2ME commands in
     * Micro.java.
     *
     * @exception HeclException if an error occurs
     */
    private void initInterp() throws HeclException {
	/* Do not use the 'Facade' style commands as an example if you
	 * just have to add a simple command or two.  The pattern
	 * works best when you need to add several commands with
	 * related functionality. */

	//	System.err.println("-->initinterp");
	//	System.err.println("loading interp cmds...");
	/* Commands that manipulate interp data structures -
	 * variables, procs, commands, and so forth.  */
	InterpCmds.load(this);

	//	System.err.println("loading math cmds...");
	/* Math and logic commands. */
	MathCmds.load(this);

	//	System.err.println("loading list cmds...");
	/* List related commands. */
	ListCmds.load(this);

	//	System.err.println("loading control cmds...");
	/* Control commands. */
	ControlCmds.load(this);

	//	System.err.println("loading string cmds...");
	/* String commands. */
	StringCmds.load(this);

	//	System.err.println("loading hash cmds...");
	/* Hash table commands. */
	HashCmds.load(this);

        commands.put("puts", new PutsCmd());
        commands.put("sort", new SortCmd());


	addClassCmd(Proc.class, new AnonProc());

	//	System.err.println("<--initinterp");
    }

    /**
     * The <code>cmdRename</code> method renames a command, or throws
     * an error if the original command didn't exist.
     *
     * @param oldname a <code>String</code> value
     * @param newname a <code>String</code> value
     * @exception HeclException if an error occurs
     */
    public synchronized void cmdRename(String oldname, String newname)
	throws HeclException {
	cmdAlias(oldname, newname);
	commands.remove(oldname);
    }

    public synchronized void cmdAlias(String oldname, String newname)
	throws HeclException {
	Command tmp = (Command)commands.get(oldname);
	if (tmp == null) {
            throw new HeclException("Command " + oldname + " does not exist");
	}
	commands.put(newname, tmp);
    }


    /**
     * The <code>stackIncr</code> method creates a new stack frame. Used in
     * the Proc class.
     *
     */
    public synchronized void stackIncr() {
        stackPush(new Hashtable());
    }

    /**
     * <code>stackDecr</code> pops the stack frame, returning it so that
     * commands like upeval can save it. If it's not saved, it's gone.
     *
     */
    public synchronized Hashtable stackDecr() {
        return (Hashtable) stack.pop();
    }

    /**
     * <code>stackDecr</code> pushes a new variable hashtable
     * (probably saved via upeval) onto the stack frame.
     *
     */
    public synchronized void stackPush(Hashtable vars) {
        cacheversion++;
        stack.push(vars);
    }

    /**
     * <code>getVarhash</code> fetches the variable Hashtable at the
     * given level, where -1 means to just get the hashtable on top of
     * the stack.
     *
     * @param level an <code>int</code> value
     * @return a <code>Hashtable</code> value
     */
    private Hashtable getVarhash(int level) {
	return level < 0 ? (Hashtable)stack.peek()
	    : (Hashtable)stack.elementAt(level);
    }

    /**
     * <code>getVar</code> returns the value of a variable given its name.
     *
     * @param varname a <code>Thing</code> value
     *
     * @return a <code>Thing</code> value
     * @exception HeclException if an error occurs
     */
    public Thing getVar(Thing varname) throws HeclException {
        return getVar(varname.toString(), -1);
    }

    /**
     * <code>getVar</code> returns the value of a variable given its name.
     *
     * @param varname a <code>String</code> value
     * @return a <code>Thing</code> value
     * @exception HeclException if an error occurs
     */
    public Thing getVar(String varname) throws HeclException {
        return getVar(varname, -1);
    }

    /**
     * <code>getVar</code> returns the value of a variable given its name and
     * level.
     *
     * @param varname a <code>String</code> value
     * @param level an <code>int</code> value
     * @return a <code>Thing</code> value
     * @exception HeclException if an error occurs
     */
    public synchronized Thing getVar(String varname, int level) throws HeclException {
        Hashtable lookup = getVarhash(level);
	//System.out.println("getvar: " + varname + " " + level + " " + lookup);
        Thing res = (Thing) lookup.get(varname);
	if(res == GLOBALREFTHING) {
	    // ref to a global var
	    Hashtable globalhash = getVarhash(0);
	    res = (Thing)globalhash.get(varname);
	    if(res == GLOBALREFTHING) {
		// should not happen, but just in case...
		System.err.println("Unexpected GLOBALREFTHING in globalhash");
		res = null;
	    }
	}
        if (res == null) {
            throw new HeclException("Variable " + varname + " does not exist");
        }
	//System.err.println("<<getvar, res="+res);
        return res;
    }

    /**
     * <code>setVar</code> sets a variable in the innermost variable stack
     * frame to a value.
     *
     * @param varname a <code>Thing</code> value
     * @param value a <code>Thing</code> value
     */
    public void setVar(Thing varname, Thing value) throws HeclException {
        setVar(varname.toString(), value);
    }

    /**
     * <code>setVar</code> sets a variable in the innermost variable stack
     * frame to a value.
     *
     * @param varname a <code>String</code> value
     * @param value a <code>Thing</code> value
     */
    public void setVar(String varname, Thing value) {
        setVar(varname, value, -1);
    }

    /**
     * <code>setVar</code> sets a variable to a value in the variable stack
     * frame specified by <code>level</code>.
     *
     * @param varname a <code>String</code> value
     * @param value a <code>Thing</code> value
     * @param level an <code>int</code> value
     */
    public synchronized void setVar(String varname, Thing value, int level) {
        Hashtable lookup = getVarhash(level);

	// Bump the cache number so that SubstThing.get refetches the
	// variable.
        cacheversion++;
	//if(value == GLOBALREFTHING) System.err.println("flag '"+varname+"' as global on level="+level);
	//System.err.println("set local("+level+") var="+varname + ", val="+value.toString());



	if (value.isLiteral()) {
	    try {
		Thing copy = value.deepcopy();
		value = copy;
	    } catch (HeclException he) {
		/* This isn't going to happen - we're dealing with a
		 * literal from the parser. */
		System.err.println("Interp.java: This can never happen!");
	    }
	}

	// first take care of GLOBALREFTHING used to flag ref to global var
	if(value == GLOBALREFTHING) {
	    // do not clutter global table with GLOBALREFTHING
	    Hashtable globalhash = getVarhash(0);
	    if(lookup != globalhash) {
		//System.err.println(" not on global level");
		lookup.put(varname, value);
	    } else {
		//System.err.println(" ignored, already in global scope");
	    }
	    return;
	}
	
	if(lookup.containsKey(varname)) {
	    Thing oldval = (Thing)lookup.get(varname);
	    if(oldval == GLOBALREFTHING) {
		// level must be at != 0
		//System.err.println(" forwarded to global value");
		lookup = getVarhash(0);
	    }
	}
	lookup.put(varname, value);
    }


    /**
     * <code>unSetVar</code> unsets a variable in the current stack
     * frame.
     *
     * @param varname a <code>Thing</code> value
     */
    public void unSetVar(Thing varname) throws HeclException {
	unSetVar(varname.toString(),-1);
    }

    public synchronized void unSetVar(String varname) throws HeclException {
	unSetVar(varname,-1);
    }
    
    public synchronized void unSetVar(String varname,int level) throws HeclException {
        Hashtable lookup = getVarhash(level);
	// Bump the cache number so that SubstThing.get refetches the
	// variable.
	Thing value = (Thing)lookup.get(varname);
	if (value != null) {
	    cacheversion++;
	    lookup.remove(varname);
	    if (value.global) {
		Hashtable globalhash = getVarhash(0);
		value = (Thing)globalhash.get(varname);
		if (value != null) {
		    globalhash.remove(varname);
		}
	    }
	} else {
            throw new HeclException("Variable " + varname + " does not exist");
	}
    }
    
	    
    /**
     * <code>existsVar</code> returns <code>true</code> if the given
     * variable exists in the current variable stack frame, <code>false</code>
     * if it does not.
     *
     * @param varname a <code>Thing</code> value
     * @return a <code>boolean</code> value
     */
    public boolean existsVar(Thing varname) throws HeclException {
        return existsVar(varname.toString());
    }

    /**
     * <code>existsVar</code> returns <code>true</code> if the given
     * variable exists in the current variable stack frame, <code>false</code>
     * if it does not.
     *
     * @param varname a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean existsVar(String varname) {
        return existsVar(varname, -1);
    }

    /**
     * <code>existsVar</code> returns <code>true</code> if the given
     * variable exists in the variable stack frame given by <code>level</code>,
     * <code>false</code> if it does not.
     *
     * @param varname a <code>String</code> value
     * @param level an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public synchronized boolean existsVar(String varname, int level) {
        Hashtable lookup = getVarhash(level);
        return lookup.containsKey(varname);
    }


    /**
     * <code>addError</code> adds a Thing as an error message.
     *
     * @param err a <code>Thing</code> value
     */
    public void addError(Thing err) {
        error.push(err);
    }

    /**
     * <code>clearError</code> clears the error stack.
     *
     */
    public void clearError() {
        error = new Stack();
    }


    /**
     * <code>checkArgCount</code> checks to see whether the command
     * actually has the required number of arguments. The first element of the
     * parameter array <code>argv</code> is not counted as argument!
     *
     * @param argv A <code>Thing[]</code> parameter array.
     * @param minargs The minimal number of arguments or -1 if no check is required.
     * @param maxargs The maximal number of arguments or -1 if no check is required.
     * @exception HeclException if an error occurs
     */
    public static void checkArgCount(Thing[] argv,int minargs,int maxargs) 
	throws HeclException {
	int n = argv.length-1;		    // Ignore command name
	if(minargs >= 0 && n < minargs) {
	    throw new HeclException("Too few arguments, at least "
				    + minargs + " arguments required.");
	}
	if(maxargs >= 0 && n > maxargs) {
	    throw new HeclException("Bad argument count, max. "
				    + maxargs
				    +" arguments allowed.");
	}
    }
}
