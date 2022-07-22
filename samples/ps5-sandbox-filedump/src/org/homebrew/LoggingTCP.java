package org.homebrew;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class LoggingTCP {
    private static LoggingTCP instance = null;
    private static Socket sock = null;
    public static final String host = "192.168.1.135";
    public static final int port = 18194;

    private LoggingTCP() {
	try {
	    sock = new Socket(host, port);
	} catch(Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static LoggingTCP getInstance() {
	if(instance == null) {
	    instance = new LoggingTCP();
	}
	return instance;
    }

    public void log(byte[] bytes, int len) {
	if(sock == null) {
	    return;
	}

	try {
	    sock.getOutputStream().write(bytes, 0, len);
	} catch(Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public void log(String msg) {
	if(sock == null) {
	    return;
	}

	try {
	    byte[] bytes = (msg + "\n").getBytes();
	    sock.getOutputStream().write(bytes);
	} catch(Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public void log(Throwable t) {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);

	t.printStackTrace(pw);
	log(sw.toString());
    }
}
