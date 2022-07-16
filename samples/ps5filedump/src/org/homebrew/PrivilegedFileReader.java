package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sun.awt.io.FileFileIO;

public class PrivilegedFileReader {
    private InputStream is = null;
    
    interface Interface extends Remote {
	public InputStream getInputStream() throws IOException, RemoteException;
    }
    
    class PrivilegedImpl extends FileFileIO implements Interface {
	public PrivilegedImpl(String path) {
	    super(path);
	}
    }
    
    public PrivilegedFileReader(String path) {
	final String sig = "()Ljava/io/InputStream;";
	
	try {
	    PrivilegedImpl impl = new PrivilegedImpl(path);
	    PrivilegeEscalationProxy proxy = new PrivilegeEscalationProxy(impl);
	    is = (InputStream)proxy.invokeMethod(new Object[]{}, "getInputStream", sig);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public PrivilegedFileReader(File file) {
	this(file.getPath());
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
	return is.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }
}

