package org.homebrew;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaServer {

    public static void run(int port) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        ss.setReuseAddress(true);

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

    private static void runScript(String script, OutputStream os) {
        Globals globals = JsePlatform.standardGlobals();
        globals.STDERR = new PrintStream(os);
        globals.STDOUT = new PrintStream(os);
        LuaValue chunk = globals.load(script);
        chunk.call(LuaValue.valueOf(script));
    }
}
