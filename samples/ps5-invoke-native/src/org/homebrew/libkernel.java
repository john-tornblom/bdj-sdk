package org.homebrew;

public class libkernel {
    static NativeLibrary lib;

    static {
	lib = new NativeLibrary(0x2001);
    }

    public static int getpid() {
	return (int)lib.invoke("getpid");
    }

    public static int sendNotificationRequest(String msg) {
	long size = 0xc30;
	long addr = NativeMemory.allocateMemory(size);

	NativeMemory.setMemory(addr, size, (byte)0);
	NativeMemory.putInt(addr + 0x10, -1);
	NativeMemory.putString(addr + 0x2d, msg);

	long res = lib.invoke("sceKernelSendNotificationRequest", 0, addr, size, 0);

	NativeMemory.freeMemory(addr);

	return (int)res;
    }
}
