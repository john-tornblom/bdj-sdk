package org.homebrew;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class NativeLibrary {
    private static Constructor constructor = null;
    private static Method findEntryMethod = null;
    private static Method loadMethod = null;
    private static Field handleField = null;
    private Object instance = null;

    static {
	try {
	    Class clazz = Class.forName("java.lang.ClassLoader$NativeLibrary");

	    constructor = clazz.getDeclaredConstructors()[0];
	    constructor.setAccessible(true);

	    findEntryMethod = clazz.getDeclaredMethod("findEntry", String.class);
	    findEntryMethod.setAccessible(true);

	    handleField = clazz.getDeclaredField("handle");
	    handleField.setAccessible(true);

	    loadMethod = clazz.getDeclaredMethod("load");
	    loadMethod.setAccessible(true);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public NativeLibrary(long addr) {
	try {
	    String name = new Long(addr).toString();
	    instance = constructor.newInstance(this.getClass(), name, new Boolean(true));
	    handleField.set(instance, new Long(addr));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public NativeLibrary(String path) {
	try {
	    instance = constructor.newInstance(this.getClass(), path, new Boolean(false));
	    loadMethod.invoke(instance);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public long findEntry(String name) {
	try {
	    Long l = (Long)findEntryMethod.invoke(instance, name);
	    return l.longValue();
	}  catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public long invoke(long addr, long...args) {
	long[] values = new long[6];
	for(int i=0; i<args.length; i++) {
	    values[i] = args[i];
	}
	return NativeInvocation.invoke(addr, values);
    }

    public long invoke(String name, long...args) {
	long[] values = new long[6];
	for(int i=0; i<args.length; i++) {
	    values[i] = args[i];
	}
	return NativeInvocation.invoke(findEntry(name), values);
    }

    public static NativeLibrary load(long addr) {
	return new NativeLibrary(addr);
    }

    public static NativeLibrary load(String path) {
	return new NativeLibrary(path);
    }
}
