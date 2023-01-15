package org.homebrew;

public class libcInternal {
    private static NativeLibrary lib;

    static {
	lib = NativeLibrary.load(2);
    }

    public static String strerror(int errno) {
	long addr = lib.invoke("strerror", errno);
	return NativeMemory.getString(addr);
    }
    
    public static String strerror() {
	int errno = libkernel.__error();
	if(errno == 0) {
	    return "";
	}

	return strerror(errno);
    }
}
