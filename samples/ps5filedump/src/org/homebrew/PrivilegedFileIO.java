package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sun.awt.io.FileFileIO;

public class PrivilegedFileIO {
    PrivilegedImpl impl = null;
    PrivilegeEscalationProxy proxy = null;

    interface Interface extends Remote {
	public boolean canWrite() throws RemoteException;
	public boolean canRead() throws RemoteException;
	public boolean delete() throws RemoteException;
	public boolean exists() throws RemoteException;
	public InputStream getInputStream() throws IOException, RemoteException;
	public OutputStream getOutputStream() throws IOException, RemoteException;
	public boolean isDirectory() throws RemoteException;
	public boolean isFile() throws RemoteException;
	public long lastModified() throws RemoteException;
	public long length() throws RemoteException;
	public String[] list() throws RemoteException;
	public boolean mkdir() throws RemoteException;
	public boolean mkdirs() throws RemoteException;
    }

    class PrivilegedImpl extends FileFileIO implements Interface {
	public PrivilegedImpl(String path) {
	    super(path);
	}
    }

    public PrivilegedFileIO(String path) {	
	try {
	    impl = new PrivilegedImpl(path);
	    proxy = new PrivilegeEscalationProxy(impl);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public PrivilegedFileIO(File file) {
	this(file.getPath());
    }

    public String canonPath(String path) throws IOException {
	return impl.canonPath(path);
    }

    public boolean canRead() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "canRead", "()Z");
	return b.booleanValue();
    }

    public boolean canWrite() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "canWrite", "()Z");
	return b.booleanValue();
    }

    public boolean delete() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "delete", "()Z");
	return b.booleanValue();
    }

    public boolean equals(Object that) {
	return impl.equals(that);
    }

    public boolean exists() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "exists", "()Z");
	return b.booleanValue();
    }

    public String getAbsolutePath() {
	return impl.getAbsolutePath();
    }

    public String getCanonicalPath() throws IOException {
	return impl.getCanonicalPath();
    }

    public InputStream getInputStream() throws IOException {
	final String sig = "()Ljava/io/InputStream;";
	return (InputStream)proxy.invokeMethod(new Object[]{}, "getInputStream", sig);
    }

    public String getName() {
	return impl.getName();
    }

    public OutputStream getOutputStream() throws IOException {
	final String sig = "()Ljava/io/OutputStream;";
	return (OutputStream)proxy.invokeMethod(new Object[]{}, "getOutputStream", sig);
    }

    public String getParent() {
	return impl.getParent();
    }

    public String getPath() {
	return impl.getPath();
    }

    public int hashCode() {
	return impl.hashCode();
    }

    public boolean isAbsolute() {
	return impl.isAbsolute();
    }

    public boolean isDirectory() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "isDirectory", "()Z");
	return b.booleanValue();
    }

    public boolean isFile() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "isFile", "()Z");
	return b.booleanValue();
    }

    public long lastModified() {
	Long l = (Long)proxy.invokeMethod(new Object[]{}, "lastModified", "()J");
	return l.longValue();
    }

    public long length() {
	Long l = (Long)proxy.invokeMethod(new Object[]{}, "length", "()J");
	return l.longValue();
    }

    public String[] list() {
	return (String[])proxy.invokeMethod(new Object[]{}, "list", "()[Ljava/lang/String;");
    }

    public boolean mkdir() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "mkdir", "()Z");
	return b.booleanValue();
    }

    public boolean mkdirs() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "mkdirs", "()Z");
	return b.booleanValue();
    }

    public String toString() {
	return impl.toString();
    }
}

