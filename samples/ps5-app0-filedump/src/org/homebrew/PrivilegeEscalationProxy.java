package org.homebrew;

import com.sony.gemstack.org.dvb.io.ixc.IxcProxy;
import com.sony.gemstack.core.CoreIxcClassLoader;
import com.sony.gemstack.core.CoreAppContext;

public class PrivilegeEscalationProxy extends IxcProxy {
    private Object remoteObject = null;

    public PrivilegeEscalationProxy(Object obj) {
        super(CoreAppContext.getContext().getIxcClassLoader(),
	      CoreAppContext.getContext().getIxcClassLoader());
	remoteObject = obj;
    }

    public Object invokeMethod(Object[] args, String name, String signature) {
	try {
	    return super.invokeMethod(args, name, signature);
	} catch (Exception ex) {
	    LoggingUI.getInstance().log(ex);
	    return null;
	}
    }
    
    public void forgetRemote() {
	remoteObject = null;
    }
    
    public Object getRemote() {
        return remoteObject;
    }
    
    public Object replaceObject(Object obj, CoreIxcClassLoader loader) {
	remoteObject = obj;
        return remoteObject;
    }
}

