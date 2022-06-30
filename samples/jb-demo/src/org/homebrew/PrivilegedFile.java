package org.homebrew;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class PrivilegedFile {
    private PrivilegeEscalationProxy proxy = null;
    private PrivilegedImpl impl = null;
    
    interface Interface extends Remote {
	public String[] list() throws RemoteException;
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

    public String[] list() {
	return (String[])proxy.invokeMethod(new Object[]{}, "list", "()[Ljava/lang/String;");
    }
}

