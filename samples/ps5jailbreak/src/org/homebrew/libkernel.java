package org.homebrew;

public class libkernel {
    static {
	NativeLibraryProxy.load("libkernel.prx", libkernel.class, false);
    }

    public static native int getpid();
}

