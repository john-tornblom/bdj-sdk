package org.homebrew;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class NativeMemory {
    private static Object theUnsafe = null;
    private static Method allocateMemoryMethod = null;
    private static Method freeMemoryMethod = null;
    private static Method putByteMethod = null;
    private static Method getByteMethod = null;
    private static Method setMemoryMethod = null;

    static {
	try {
	    PrivilegeEscalation.openModulePackage("java.base", "jdk.internal.misc", NativeMemory.class);
	    Class UnsafeClass = Class.forName("jdk.internal.misc.Unsafe");
	    Field theUnsafeField = UnsafeClass.getDeclaredField("theUnsafe");

	    theUnsafeField.setAccessible(true);
	    theUnsafe = theUnsafeField.get(null);

	    allocateMemoryMethod = UnsafeClass.getDeclaredMethod("allocateMemory", Long.TYPE);
	    allocateMemoryMethod.setAccessible(true);

	    freeMemoryMethod = UnsafeClass.getDeclaredMethod("freeMemory", Long.TYPE);
	    freeMemoryMethod.setAccessible(true);

	    putByteMethod = UnsafeClass.getDeclaredMethod("putByte", Long.TYPE, Byte.TYPE);
	    putByteMethod.setAccessible(true);

	    getByteMethod = UnsafeClass.getDeclaredMethod("getByte", Long.TYPE);
	    getByteMethod.setAccessible(true);

	    setMemoryMethod = UnsafeClass.getDeclaredMethod("setMemory", Long.TYPE, Long.TYPE, Byte.TYPE);
	    setMemoryMethod.setAccessible(true);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static long allocateMemory(long size) {
	try {
	    Long l = (Long)allocateMemoryMethod.invoke(theUnsafe, new Long(size));
	    return l.longValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	    return 0;
	}
    }

    public static void freeMemory(long addr) {
	try {
	    freeMemoryMethod.invoke(theUnsafe, new Long(addr));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static void putByte(long addr, byte value) {
	try {
	    putByteMethod.invoke(theUnsafe, new Long(addr), new Byte(value));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static byte getByte(long addr) {
	try {
	    Byte b = (Byte)getByteMethod.invoke(theUnsafe, new Long(addr));
	    return b.byteValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static void putString(long addr, String str) {
	for(int i=0; i<str.length(); i++) {
	    putByte(addr+i, (byte)str.charAt(i));
	}
    }

    public static String getString(long addr) {
	StringBuffer sb = new StringBuffer();

	while(true) {
	    byte b = getByte(addr);
	    if(b != 0) {
		sb.append((char)b);
	    } else {
		break;
	    }
	    addr += 1;
	}

	return sb.toString();
    }

    public static void setMemory(long addr, long length, byte value) {
	try {
	    setMemoryMethod.invoke(theUnsafe, new Long(addr), new Long(length), new Byte(value));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }
}
