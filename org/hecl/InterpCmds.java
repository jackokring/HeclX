/* Copyright 2006 David N. Welton

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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The <code>InterpCmds</code> implements various Hecl commands that
 * deal with the state of the interpreter.
 *
 * @author <a href="mailto:davidw@dedasys.com">David N. Welton</a>
 * @version 1.0
 */
class InterpCmds extends Operator {
    public static final int SET = 1;
    public static final int UNSET = 2;
    public static final int PROC = 3;
    public static final int RENAME = 4;
    public static final int EVAL = 5;
    public static final int GLOBAL = 6;
    public static final int INTROSPECT = 7;
    public static final int RETURN = 8;
    public static final int CATCH = 9;
    public static final int EXIT = 10;
    public static final int UPCMD = 11;
    public static final int TIMECMD = 12;

    public static final int COPY = 13;
    public static final int THROW = 14;

    protected static final int GC = 19;
    protected static final int GETPROP = 20;
    protected static final int HASPROP = 21;
    protected static final int CLOCKCMD = 22;

    protected static final int FREEMEM = 23;
    protected static final int TOTALMEM = 24;

    public static final int ALIAS = 25;

    public static final int HASCLASS = 70;//Class.forName()

    public static final int CLASSINFO = 80; // hecl internal!!!

    public Thing operate(int cmd, Interp interp, Thing[] argv) throws HeclException {
	Thing result = null;
	int retval = 0;
	String subcmd  = null;
	
	switch (cmd) {
	  case SET:
	    if (argv.length == 3) {
		interp.setVar(argv[1], argv[2]);
		return argv[2];
	    }
	    return interp.getVar(argv[1]);

	  case COPY:
	    return argv[1].deepcopy();

	  case UNSET:
	    interp.unSetVar(argv[1]);
	    break;

	  case PROC:
	      if (argv.length == 4) {
		  interp.commands.put(argv[1].toString(), new Proc(argv[2], argv[3]));
	      } else {
		  return ObjectThing.create(new Proc(argv[1], argv[2]));
	      }
	    break;

	  case RENAME:
	    interp.cmdRename(argv[1].toString(), argv[2].toString());
	    break;

	  case ALIAS:
	    interp.cmdAlias(argv[1].toString(), argv[2].toString());
	    break;

	  case EVAL:
	    return interp.eval(argv[1]);

	  case GLOBAL:
	    ;

	    for (int i = 1; i < argv.length; i ++) {
		interp.setVar(argv[i].toString(),Interp.GLOBALREFTHING,-1);
	    }
	    break;
	    
	  case INTROSPECT:
	    subcmd = argv[1].toString();
	    Vector results = new Vector();

	    if (subcmd.equals("commands")) {
		for (Enumeration e = interp.commands.keys(); e.hasMoreElements();) {
		    Thing t = new Thing((String) e.nextElement());
		    results.addElement(t);
		}
		return ListThing.create(results);
	    } else if (subcmd.equals("proccode")) {
		Proc p = (Proc)interp.commands.get(argv[2].toString());
		return new Thing(p.getCode().getVal());
	    }

	    break;

	  case RETURN:
	    throw new HeclException("", HeclException.RETURN,
				    argv.length > 1 ? argv[1] : Thing.emptyThing());

	  case CATCH:
	    try {
		result = interp.eval(argv[1]);
		retval = 0;
	    } catch (HeclException e) {
		result = e.getStack();
		retval = 1;
	    }

	    if (argv.length == 3) {
		interp.setVar(argv[2].toString(),
			      result!= null ? result : Thing.emptyThing());
	    }
	    return new Thing(retval != 0 ? IntThing.ONE : IntThing.ZERO);

	  case THROW:
	    String errmsg = argv[1].toString();
	    if (argv.length == 2) {
		throw new HeclException(errmsg);
	    }
	    throw new HeclException(errmsg, argv[2].toString());

       
	  case EXIT:
	    retval = 0;
	    if (argv.length > 1) {
		retval = IntThing.get(argv[1]);
	    }
	    System.exit(retval);
	    break;

	  case UPCMD:
	    Hashtable save = null;
	    Thing code = null;
	    int level = -1;

	    if (argv.length == 2) {
		code = argv[1];
	    } else if (argv.length == 3) {
		code = argv[2];
		level = IntThing.get(argv[1]);
	    }
	    return interp.eval(code, level);
		
	  case TIMECMD:
	    int times = 1;
	    
	    if (argv.length > 2) {
		times = NumberThing.asNumber(argv[2]).intValue();
	    }
	    long then = new Date().getTime();
	    while (times > 0) {
		interp.eval(argv[1]);
		times--;
	    }
	    return LongThing.create(new Date().getTime() - then);
	    
	  case HASCLASS:
	    // beware: you may be get fooled in j2me when you use an
	    // obfuscator: custom class names may get changed. Use only for
	    // system-defined classes!
	    retval = 0;
	    try {
		retval = null != Class.forName(argv[1].toString()) ? 1 : 0;
	    }
	    catch (Exception e) {}
	    return IntThing.create(retval);

	  case CLASSINFO:
	    return new Thing("<"+argv[1].getVal().thingclass()+">");

	  case GC:
	    System.gc();
	    break;
	    
	  case GETPROP:
	    String s = System.getProperty(argv[1].toString());
	    return new Thing(s != null ? s : "");
	    
	  case HASPROP:
	    return IntThing.create(System.getProperty(argv[1].toString())!=null ? 1 : 0);

	  case CLOCKCMD:
	    subcmd = argv[1].toString();
	    {
		long l = System.currentTimeMillis();
		if(subcmd.equals("seconds"))
		    return LongThing.create(l/1000);
		if(subcmd.equals("time") || subcmd.equals("milli"))
		    return LongThing.create(l);
		if(subcmd.equals("format")) {
		    // to bad, j2me does not support DataFormat,
		    if(argv.length == 3)
			return new Thing(
			    new ListThing((new Date(LongThing.get(argv[2]))).toString()));
		    throw HeclException.createWrongNumArgsException(argv,2,"?milli?");
		}
		throw HeclException.createWrongNumArgsException(argv,1,"option ?time?");
	    }

	  case FREEMEM:
	    return LongThing.create(Runtime.getRuntime().freeMemory());
	    
	  case TOTALMEM:
	    return LongThing.create(Runtime.getRuntime().totalMemory());

	  default:
	    throw new HeclException("Unknown interp command '"
				    + argv[0].toString() + "' with code '"
				    + cmd + "'.");
	}
	return null;
    }


    public static void load(Interp ip) throws HeclException {
	Operator.load(ip,cmdtable);
    }


    public static void unload(Interp ip) throws HeclException {
	Operator.unload(ip,cmdtable);
    }


    private InterpCmds(int cmdcode,int minargs,int maxargs) {
	super(cmdcode,minargs,maxargs);
    }

    private static Hashtable cmdtable = new Hashtable();

    static {
	cmdtable.put("set", new InterpCmds(SET, 1 ,2));
        cmdtable.put("unset", new InterpCmds(UNSET, 1, 1));
        cmdtable.put("proc", new InterpCmds(PROC, 2, 3));
        cmdtable.put("rename", new InterpCmds(RENAME, 2, 2));
        cmdtable.put("alias", new InterpCmds(ALIAS, 2, 2));
        cmdtable.put("eval", new InterpCmds(EVAL, 1, 1));
        cmdtable.put("global", new InterpCmds(GLOBAL, 0, -1));
        cmdtable.put("intro", new InterpCmds(INTROSPECT, 1, -1));
        cmdtable.put("return", new InterpCmds(RETURN, 0, 1));
        cmdtable.put("catch", new InterpCmds(CATCH, 1, 2));
        cmdtable.put("throw", new InterpCmds(THROW, 1, 2));
        cmdtable.put("exit", new InterpCmds(EXIT, 0, 1));
        cmdtable.put("upeval", new InterpCmds(UPCMD, 1, 2));
        cmdtable.put("time", new InterpCmds(TIMECMD, 1, 2));

        cmdtable.put("copy", new InterpCmds(COPY, 1, 1));

	cmdtable.put("system.gc", new InterpCmds(GC,0,0));
	cmdtable.put("system.getproperty", new InterpCmds(GETPROP,1,1));
	cmdtable.put("system.hasproperty", new InterpCmds(HASPROP,1,1));
	cmdtable.put("clock", new InterpCmds(CLOCKCMD,1,2));

	cmdtable.put("runtime.freememory", new InterpCmds(FREEMEM,0,0));
	cmdtable.put("runtime.totalmemory", new InterpCmds(TOTALMEM,0,0));

        cmdtable.put("hasclass", new InterpCmds(HASCLASS, 1, 1));

        cmdtable.put("classof", new InterpCmds(CLASSINFO, 1, 1));

    }
}
