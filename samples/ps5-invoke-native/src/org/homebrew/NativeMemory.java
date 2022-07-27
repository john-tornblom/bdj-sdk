package org.homebrew;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class NativeMemory {
    private static Object theUnsafe = null;

    private static Method allocateMemoryMethod = null;
    private static Method freeMemoryMethod = null;
    private static Method setMemoryMethod = null;

    private static Method getByteMethod = null;
    private static Method getShortMethod = null;
    private static Method getIntMethod = null;
    private static Method getLongMethod = null;

    private static Method putByteMethod = null;
    private static Method putShortMethod = null;
    private static Method putIntMethod = null;
    private static Method putLongMethod = null;

    private static Method arrayBaseOffsetMethod = null;
    static {
	try {
	    PrivilegeEscalation.openModulePackage("java.base", "jdk.internal.misc", NativeMemory.class);
	    Class UnsafeClass = Class.forName("jdk.internal.misc.Unsafe");
	    Field theUnsafeField = UnsafeClass.getDeclaredField("theUnsafe");

	    theUnsafeField.setAccessible(true);
	    theUnsafe = theUnsafeField.get(null);

	    allocateMemoryMethod = UnsafeClass.getDeclaredMethod("allocateMemory", Long.TYPE);
	    freeMemoryMethod = UnsafeClass.getDeclaredMethod("freeMemory", Long.TYPE);
	    setMemoryMethod = UnsafeClass.getDeclaredMethod("setMemory", Long.TYPE, Long.TYPE, Byte.TYPE);

	    getByteMethod = UnsafeClass.getDeclaredMethod("getByte", Long.TYPE);
	    getShortMethod = UnsafeClass.getDeclaredMethod("getShort", Long.TYPE);
	    getIntMethod = UnsafeClass.getDeclaredMethod("getInt", Long.TYPE);
	    getLongMethod = UnsafeClass.getDeclaredMethod("getLong", Object.class, Long.TYPE);

	    putByteMethod = UnsafeClass.getDeclaredMethod("putByte", Long.TYPE, Byte.TYPE);
	    putShortMethod = UnsafeClass.getDeclaredMethod("putShort", Long.TYPE, Short.TYPE);
	    putIntMethod = UnsafeClass.getDeclaredMethod("putInt", Long.TYPE, Integer.TYPE);
	    putLongMethod = UnsafeClass.getDeclaredMethod("putLong", Long.TYPE, Long.TYPE);

	    arrayBaseOffsetMethod = UnsafeClass.getDeclaredMethod("arrayBaseOffset", Class.class);
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

    public static byte getByte(long addr) {
	try {
	    Byte b = (Byte)getByteMethod.invoke(theUnsafe, new Long(addr));
	    return b.byteValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static short getShort(long addr) {
	try {
	    Short s = (Short)getShortMethod.invoke(theUnsafe, new Long(addr));
	    return s.shortValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static int getInt(long addr) {
	try {
	    Integer i = (Integer)getIntMethod.invoke(theUnsafe, new Long(addr));
	    return i.intValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static long getLong(long addr) {
	try {
	    Long l = (Long)getLongMethod.invoke(theUnsafe, null, new Long(addr));
	    return l.longValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static long getLong(Object obj, long offset) {
	try {
	    Long l = (Long)getLongMethod.invoke(theUnsafe, obj, new Long(offset));
	    return l.longValue();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return 0;
    }

    public static byte[] getBytes(long addr, int size) {
	byte[] bytes = new byte[size];
	for(int i=0; i<size; i++) {
	    bytes[i] = getByte(addr + i);
	}
	return bytes;
    }

    public static void putByte(long addr, byte value) {
	try {
	    putByteMethod.invoke(theUnsafe, new Long(addr), new Byte(value));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static void putInt(long addr, int value) {
	try {
	    putIntMethod.invoke(theUnsafe, new Long(addr), new Integer(value));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static void putLong(long addr, long value) {
	try {
	    putLongMethod.invoke(theUnsafe, new Long(addr), new Long(value));
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static void putString(long addr, String str) {
	for(int i=0; i<str.length(); i++) {
	    putByte(addr+i, (byte)str.charAt(i));
	}
    }

    public static String getString(long addr, int length) {
	byte bytes[] = new byte[length];
	for(int i=0; i<length; i++) {
	    bytes[i] = getByte(addr + i);
	}
	return new String(bytes);
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

    public static long addressOf(Object obj) {
        try {
            Object[] array = new Object[]{obj};
            Integer baseOffset = (Integer)arrayBaseOffsetMethod.invoke(theUnsafe, Object[].class);
            return getLong(array, baseOffset); // << 3; // assume compressed oop with 3-bit shift.
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }
}
