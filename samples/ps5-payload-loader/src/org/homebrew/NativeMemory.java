package org.homebrew;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public class NativeMemory {
    private static Unsafe theUnsafe = null;
    
    static {
	try {
	    PrivilegeEscalation.openModuleToAllUnnamed("jdk.internal.misc.Unsafe");
	    theUnsafe = Unsafe.getUnsafe();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public static long allocateMemory(long size) {
	return theUnsafe.allocateMemory(size);
    }

    public static void freeMemory(long addr) {
	theUnsafe.freeMemory(addr);
    }

    public static byte getByte(long addr) {
	return theUnsafe.getByte(addr);
    }

    public static short getShort(long addr) {
	return theUnsafe.getShort(addr);
    }

    public static int getInt(long addr) {
	return theUnsafe.getInt(addr);
    }

    public static long getLong(long addr) {
	return theUnsafe.getLong(addr);
    }

    public static void putByte(long addr, byte value) {
	theUnsafe.putByte(addr, value);
    }

    public static void putInt(long addr, int value) {
	theUnsafe.putInt(addr, value);
    }

    public static void putShort(long addr, short value) {
	theUnsafe.putShort(addr, value);
    }
    
    public static void putLong(long addr, long value) {
	theUnsafe.putLong(addr, value);
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
	theUnsafe.setMemory(addr, length, value);
    }

    public static void copyMemory(long src, long dst, long len) {
        theUnsafe.copyMemory(src, dst, len);
    }

    public static long addressOf(Object obj) {
	Object[] array = new Object[]{obj};
	int baseOffset = theUnsafe.arrayBaseOffset(Object[].class);
	return theUnsafe.getLong(array, baseOffset); // << 3; // assume compressed oop with 3-bit shift.
    }
}
