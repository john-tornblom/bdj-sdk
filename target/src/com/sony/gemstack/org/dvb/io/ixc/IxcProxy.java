package com.sony.gemstack.org.dvb.io.ixc;

import com.sony.gemstack.core.CoreIxcClassLoader;
import java.rmi.RemoteException;

public abstract class IxcProxy {
    public abstract Object getRemote();
    public abstract void forgetRemote();

    protected IxcProxy(CoreIxcClassLoader loader1, CoreIxcClassLoader loader2) {
	
    }
    
    protected Object replaceObject(Object obj, CoreIxcClassLoader loader) throws RemoteException {
	return null;
    }
    
    protected Object invokeMethod(Object[] objects, String string1, String string2) throws Exception {
	return null;
    }
}

