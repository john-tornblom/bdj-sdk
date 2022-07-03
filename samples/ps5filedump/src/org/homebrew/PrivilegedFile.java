package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class PrivilegedFile {
    private PrivilegeEscalationProxy proxy = null;
    private PrivilegedImpl impl = null;
    
    interface Interface extends Remote {
	public boolean canRead() throws RemoteException;
	public boolean canWrite() throws RemoteException;
	public boolean exists() throws RemoteException;
	public boolean isDirectory() throws RemoteException;
	public boolean isFile() throws RemoteException;
	public boolean isHidden() throws RemoteException;
	public long lastModified() throws RemoteException;
	public long length() throws RemoteException;
	public boolean createNewFile() throws IOException, RemoteException;
	public boolean delete() throws RemoteException;
	public void deleteOnExit() throws RemoteException;
	public String[] list() throws RemoteException;
	public boolean renameTo(File file) throws RemoteException;
    }
    
    class PrivilegedImpl extends File implements Interface {
	public PrivilegedImpl(String path) {
	    super(path);
	}
    }
    
    public PrivilegedFile(String path) {
	impl = new PrivilegedImpl(path);
	proxy = new PrivilegeEscalationProxy(impl);
    }
    
    public String getName() {
	return impl.getName();
    }
    
    public String getPath() {
	return impl.getPath();
    }
    
    public boolean canRead() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "canRead", "()Z");
	return b.booleanValue();
    }
    
    public boolean canWrite() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "canWrite", "()Z");
	return b.booleanValue();
    }
    
    public boolean exists() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "exists", "()Z");
	return b.booleanValue();
    }
    
    public boolean isDirectory() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "isDirectory", "()Z");
	return b.booleanValue();
    }
    
    public boolean isFile() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "isFile", "()Z");
	return b.booleanValue();
    }
    
    public boolean isHidden() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "isHidden", "()Z");
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

    public boolean createNewFile() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "createNewFile", "()Z");
	return b.booleanValue();
    }

    public boolean delete() {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{}, "delete", "()Z");
	return b.booleanValue();
    }

    public void deleteOnExit() {
	proxy.invokeMethod(new Object[]{}, "deleteOnExit", "()V");
    }

    public String[] list()  {
	return (String[])proxy.invokeMethod(new Object[]{}, "list", "()[Ljava/lang/String;");
    }

    public boolean renameTo(File file) {
	Boolean b = (Boolean)proxy.invokeMethod(new Object[]{file}, "renameTo", "(Ljava/io/File;)Z");
	return b.booleanValue();
    }
}

