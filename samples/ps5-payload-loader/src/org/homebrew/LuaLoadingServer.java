package org.homebrew;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
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

public class LuaLoadingServer {
    
    public static void spawn(int port) throws IOException {
	final ServerSocket ss = new ServerSocket(port);
	ss.setReuseAddress(true);
	
        new Thread(new Runnable() {
		public void run() {
		    try {
			LuaLoadingServer.run(ss);
		    } catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		    }
		}
	    }).start();
    }

    public static void run(ServerSocket ss) throws IOException {
        while (true) {
            try {
                serve(ss.accept());
            } catch (Throwable t) {
		LoggingUI.getInstance().log(t);
            }
        }
    }

    private static void serve(final Socket s) throws Exception {
        final PrintStream err = new PrintStream(s.getOutputStream());
        new Thread(new Runnable() {
            public void run() {
                try {
                    String script = readScript(s);
                    runScript(script, s.getOutputStream());
                } catch (Throwable t) {
                    t.printStackTrace(err);
                }

                try {
                    s.close();
                } catch (Throwable t) {
		    LoggingUI.getInstance().log(t);
                }
            }

        }).start();
    }

    private static String readScript(Socket s) throws IOException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = s.getInputStream().read();
            if (ch <= 0) {
                break;
            } else {
                sb.append((char) ch);
            }
        }
        return sb.toString();
    }

    public static class MyLuajavaLib extends LuajavaLib {
	protected Class classForName(String name) throws ClassNotFoundException {
	    return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
	}
    }
    
    private static void runScript(String script, OutputStream os) {
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

        globals.STDERR = new PrintStream(os);
        globals.STDOUT = new PrintStream(os);
        LuaValue chunk = globals.load(script);
        chunk.call(LuaValue.valueOf(script));
    }
}
