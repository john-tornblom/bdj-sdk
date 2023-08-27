package org.homebrew;

import java.io.OutputStream;
import java.io.PrintStream;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseStringLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseIoLib;
import org.luaj.vm2.lib.jse.JseOsLib;
import org.luaj.vm2.lib.jse.LuajavaLib;

public class LuaLoading {

    public static class MyLuajavaLib extends LuajavaLib {
	protected Class classForName(String name) throws ClassNotFoundException {
	    return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
	}
    }

    public static void runScript(String script, OutputStream os) throws Exception {
	Globals globals = new Globals();
	globals.load(new JseBaseLib());
	globals.load(new PackageLib());
	globals.load(new Bit32Lib());
	globals.load(new TableLib());
	globals.load(new JseStringLib());
	globals.load(new CoroutineLib());
	globals.load(new JseMathLib());
	globals.load(new JseIoLib());
	globals.load(new JseOsLib());
	globals.load(new MyLuajavaLib());
	LoadState.install(globals);
	LuaC.install(globals);

	if(os != null) {
	    globals.STDERR = new PrintStream(os);
	    globals.STDOUT = new PrintStream(os);
	}
        LuaValue chunk = globals.load(script);
        chunk.call(LuaValue.valueOf(script));
    }

    public static void runScript(byte[] bytes, OutputStream os) throws Exception {
	String str = new String(bytes);
	runScript(str, os);
    }

    public static void spawnServer(int port) throws Exception {
	new LoadingServer(port) {
	    public void runPayload(byte[] bytes, OutputStream os) throws Exception {
		LuaLoading.runScript(bytes, os);
	    }
	}.spawn();
    }
}
