package jdk.internal.access;

import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
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
import java.util.Set;
import jdk.internal.security.CodeSigner;
import jdk.internal.security.Entry;
import jdk.internal.security.ProtectionParameter;
import jdk.internal.security.Service;
import jdk.internal.security.keystore.Builder;

public interface JavaSecurityAccess {
    public class ProtectionDomainCache{
    }
    
    Object doIntersectionPrivilege(PrivilegedAction action, AccessControlContext ctx1, AccessControlContext ctx2);
    Object doIntersectionPrivilege(PrivilegedAction action, AccessControlContext ctx);
    ProtectionDomainCache getProtectionDomainCache();
    Object doPrivilegedWithCombiner(PrivilegedExceptionAction action, AccessControlContext ctx, Permission[] perms) throws PrivilegedActionException;
    Object doPrivileged(PrivilegedAction action, AccessControlContext ctx, Permission[] perms);
    Entry getEntry(KeyStore ks, String str, ProtectionParameter pp) throws NoSuchAlgorithmException, GeneralSecurityException;
    Service getService(Provider provider, String str1, String str2);
    void putService(Provider provider, Service service);
    Set getServices(Provider provider);
    Provider configure(Provider provider, String str);
    Object newInstance(Class clazz, String str, Object obj) throws Exception;
    boolean checkEngine(String str);
    String getEngineName(String str);
    CodeSource newCodeSource(URL url, CodeSigner[] cs);
    void update(MessageDigest md, ByteBuffer bb);
    Builder newKeyStoreBuilder(KeyStore ks, ProtectionParameter pp);
}
