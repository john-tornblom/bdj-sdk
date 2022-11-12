package org.homebrew;

import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class KernelDataDumping {

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
        new Thread(new Runnable() {
            public void run() {
                try {
                    dump(s);
                } catch (Throwable t) {
		    LoggingUI.getInstance().log(t);
                }

                try {
                    s.close();
                } catch (Throwable t) {
		    LoggingUI.getInstance().log(t);
                }
            }

        }).start();
    }

    private static void dump(Socket s) throws IOException {
	BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
	long length = Long.decode(br.readLine());

	long base_addr = KernelMemory.getBaseAddress();
	for(long offset=0; offset<length; offset+=8) {
	    long val = KernelMemory.getLong(base_addr + offset);

	    for(int i=Long.BYTES-1; i>=0; i--) {
		s.getOutputStream().write((byte)(val & 0xFF));
		val >>= Byte.SIZE;
	    }
	}
    }
}
