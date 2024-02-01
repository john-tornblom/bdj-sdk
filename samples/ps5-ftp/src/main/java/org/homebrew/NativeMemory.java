package org.homebrew;

import jdk.internal.misc.Unsafe;

public class NativeMemory {
	static final Unsafe theUnsafe;
	private static final int ARRAY_BASE_OFFSET = 0x18;

	static {
		PrivilegeEscalation.openModuleToAllUnnamed("jdk.internal.misc.Unsafe");
		theUnsafe = Unsafe.getUnsafe();
	}

	private static long check(long ptr) {
		if (ptr == 0) {
			throw new NullPointerException();
		}
		return ptr;
	}

	public static long allocateMemory(long size) {
		return theUnsafe.allocateMemory(size);
	}

	public static long calloc(long size) {
		long mem = theUnsafe.allocateMemory(size);
		theUnsafe.setMemory(mem, size, (byte) 0);
		return mem;
	}

	public static void freeMemory(long addr) {
		if (addr != 0) {
			theUnsafe.freeMemory(addr);
		}
	}

	public static void free(long... addrs) {
		for (long addr : addrs) {
			freeMemory(addr);
		}
	}

	public static byte getByte(long addr) {
		return theUnsafe.getByte(check(addr));
	}

	public static short getShort(long addr) {
		return theUnsafe.getShort(check(addr));
	}

	public static int getInt(long addr) {
		return theUnsafe.getInt(check(addr));
	}

	public static int getInt(long addr, int index) {
		return theUnsafe.getInt(check(addr) + (index * Integer.BYTES));
	}

	public static long getLong(long addr) {
		return theUnsafe.getLong(check(addr));
	}

	public static void putByte(long addr, byte value) {
		theUnsafe.putByte(check(addr), value);
	}

	public static void putInt(long addr, int value) {
		theUnsafe.putInt(check(addr), value);
	}

	public static void putShort(long addr, short value) {
		theUnsafe.putShort(check(addr), value);
	}

	public static void putLong(long addr, long value) {
		theUnsafe.putLong(check(addr), value);
	}

	public static void setByte(long addr, byte value) {
		putByte(addr, value);
	}

	public static void setShort(long addr, short value) {
		putShort(addr, value);
	}

	public static void setInt(long addr, int value) {
		putInt(addr, value);
	}

	public static void setLong(long addr, long value) {
		putLong(addr, value);
	}

	public static void putString(long addr, String str) {
		check(addr);
		for(int i=0; i<str.length(); i++) {
			putByte(addr+i, (byte)str.charAt(i));
		}
	}

	public static void setString(long addr, String str) {
		putString(addr, str);
	}

	public static String getString(long addr, int length) {
		check(addr);
		byte bytes[] = new byte[length];
		for(int i=0; i<length; i++) {
			bytes[i] = getByte(addr + i);
		}
		return new String(bytes);
	}

	public static String getString(long addr) {
		check(addr);
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
		theUnsafe.setMemory(check(addr), length, value);
	}

	public static void memset(long addr, int value, long length) {
		theUnsafe.setMemory(check(addr), length, (byte) value);
	}

	public static long addressOf(Object obj) {
		Object[] array = new Object[]{obj};
		return theUnsafe.getLong(array, ARRAY_BASE_OFFSET); // << 3; // assume compressed oop with 3-bit shift.
	}

	public static long addressOf(byte[] obj) {
		return addressOf((Object) obj) + ARRAY_BASE_OFFSET;
	}
	
	public static long addressOf(char[] obj) {
		return addressOf((Object) obj) + ARRAY_BASE_OFFSET;
	}
	
	public static long addressOf(short[] obj) {
		return addressOf((Object) obj) + ARRAY_BASE_OFFSET;
	}

	public static long addressOf(int[] obj) {
		return addressOf((Object) obj) + ARRAY_BASE_OFFSET;
	}

	public static long addressOf(long[] obj) {
		return addressOf((Object) obj) + ARRAY_BASE_OFFSET;
	}
	
	public static long objectFieldOffset(Class<?> cls, String str) {
		return theUnsafe.objectFieldOffset(cls, str);
	}
}
