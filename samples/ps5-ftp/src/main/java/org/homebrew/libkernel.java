package org.homebrew;

import java.lang.reflect.Method;

public class libkernel {

	@SuppressWarnings("removal")
	private static final Integer ZERO = new Integer(0);
	
	public static final NativeLibrary lib;
	private static Method errorGetter;

	static {
		lib = NativeLibrary.load(0x2001);
		try {
			Class<?> cls = Class.forName("sun.nio.fs.UnixNativeDispatcher");
			errorGetter = cls.getDeclaredMethod("strerror", Integer.TYPE);
			errorGetter.setAccessible(true);
		} catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		}
	}

	private static int check(int v) {
		if (v != -1) {
			return v;
		}
		throw new RuntimeException(strerror());
	}

	public static int open(String path, int flags) {
		return open(path, flags, 0);
	}

	public static int open(CString path, int flags) {
		return open(path, flags, 0);
	}

	public static int open(String path, int flags, int flags2) {
		CString buf = new CString(path);
		return check((int)lib.invoke("open", buf.getAddress(), flags, flags2));
	}

	public static int open(CString path, int flags, int flags2) {
		return check((int)lib.invoke("open", path.getAddress(), flags, flags2));
	}
	
	public static int close(int fd) {
		if (fd == -1) {
			return 0;
		}
		return (int) lib.invoke("close", fd);
	}

	public static int readlink(long path, long buf, int bufsiz) {
		return check((int) lib.invoke("readlink", path, buf, bufsiz));
	}

	public static int access(String path, int mode) {
		return access(new CString(path), mode);
	}

	public static int access(CString path, int mode) {
		return (int) lib.invoke("access", path.getAddress(), mode);
	}
	
	public static String strerror() {
		try {
			return new String((byte[]) errorGetter.invoke(null, ZERO));
		} catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		}
		return "";
	}

	public static int stat(long path, long buf) {
		return check((int) lib.invoke("stat", path, buf));
	}

	public static int getdents(int fd, long buf, int count) {
		return check((int) lib.invoke("getdents", fd, buf, count));
	}

}
