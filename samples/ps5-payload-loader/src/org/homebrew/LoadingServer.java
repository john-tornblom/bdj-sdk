package org.homebrew;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class LoadingServer {
    private int port;

    public LoadingServer(int port) {
	this.port = port;
    }

    public void spawn() throws IOException {
	final ServerSocket ss = new ServerSocket(port);
	ss.setReuseAddress(true);

        new Thread(new Runnable() {
		public void run() {
		    try {
			runServer(ss);
		    } catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		    }
		}
	    }).start();
    }

    protected abstract void runPayload(byte[] bytes, OutputStream os) throws Exception;

    private  void runServer(ServerSocket ss) throws IOException {
        while (true) {
            try {
                serve(ss.accept());
            } catch (Throwable t) {
		LoggingUI.getInstance().log(t);
            }
        }
    }

    private void serve(final Socket s) throws Exception {
        final PrintStream err = new PrintStream(s.getOutputStream());
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] bytes = readBytes(s);
                    runPayload(bytes, s.getOutputStream());
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

    private static byte[] readBytes(Socket s) throws IOException {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	byte[] chunk = new byte[0x4000];

        while (true) {
            int length = s.getInputStream().read(chunk, 0, chunk.length);
            if (length < 0) {
                break;
            } else {
                buf.write(chunk, 0, length);
            }
        }

        return buf.toByteArray();
    }
}
