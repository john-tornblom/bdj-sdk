package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sun.net.www.protocol.file.FileURLConnection;

public class PrivilegedFileReader {
    private PrivilegeEscalationProxy proxy = null;
    private PrivilegedImpl impl = null;
    
    interface Interface extends Remote {
	public void connect() throws IOException, RemoteException;
    }
    
    class PrivilegedImpl extends FileURLConnection implements Interface {
	public PrivilegedImpl(URL url, File file) {
	    super(url, file);
	}
    }
    
    public PrivilegedFileReader(String path) {
	try {
	    impl = new PrivilegedImpl(new URL("file://" + path), new File(path));
	    proxy = new PrivilegeEscalationProxy(impl);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public PrivilegedFileReader(File file) {
	this(file.getPath());
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
	proxy.invokeMethod(new Object[]{}, "connect", "()V");
	return impl.getInputStream().read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }
}

