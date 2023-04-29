package jdk.internal.misc;

public final class Unsafe {
    private static final Unsafe theUnsafe = new Unsafe();

    private Unsafe() {
    }

    public static Unsafe getUnsafe() {
	return theUnsafe;
    }


    public long allocateMemory(long size) {
	return 0;
    }

    public void freeMemory(long addr) {
    }

    public void setMemory(long addr, long length, byte value) {
    }

    public void copyMemory(long src_addr, long dst_addr, long length) {
    }

    public byte getByte(long addr) {
	return 0;
    }

    public short getShort(long addr) {
	return 0;
    }

    public int getInt(long addr) {
	return 0;
    }

    public long getLong(long addr) {
	return 0;
    }

    public void putByte(long addr, byte value) {
    }

    public void putShort(long addr, short value) {
    }

    public void putInt(long addr, int value) {
    }

    public void putLong(long addr, long value) {
    }

    public int arrayBaseOffset(Class arrayClass) {
	return 0;
    }

    public native long getLong(Object o, long offset);
}
