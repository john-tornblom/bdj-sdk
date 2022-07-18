package org.homebrew;

public class libkernel {
    static {
	NativeLibraryProxy.load("libkernel.prx", libkernel.class, false);
    }

    public static native int getpid();
    public static native long sceKernelGetFsSandboxRandomWord();

    public static String getFsSandboxRandomWord() {
	long ptr = sceKernelGetFsSandboxRandomWord();
	if(ptr != 0) {
	    return NativeMemory.getString(ptr);
	} else {
	    return "";
	}
    }
}

