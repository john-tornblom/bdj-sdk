package org.homebrew;

public class NativeInvocation {
    private static long JVM_NativePath = 0;
    private static long getcontext = 0;
    private static long setcontext = 0;

    static {
	try {
	    NativeLibrary rtld = new NativeLibrary(-2);
	    JVM_NativePath = rtld.findEntry("JVM_NativePath");

	    NativeLibrary libkernel = new NativeLibrary(0x2001);
	    getcontext = libkernel.findEntry("getcontext");
	    setcontext = libkernel.findEntry("__Ux86_64_setcontext");

	    NativeLibrary libjava = new NativeLibrary(0x4a);
	    if("11.0.7-internal".equals(System.getProperty("java.version"))) {
		libjava = new NativeLibrary(0x4b);
	    }
	    
	    long apiInstance = NativeMemory.addressOf(new NativeInvocation());
	    long apiKlass = NativeMemory.getLong(apiInstance + 0x08);
	    long methods = NativeMemory.getLong(apiKlass + 0x170);
	    int numMethods = NativeMemory.getInt(methods + 0x00);
	    for (int i = 0; i < numMethods; i++) {
		long method = NativeMemory.getLong(methods + 0x08 + i * 8);
		long constMethod = NativeMemory.getLong(method + 0x08);
		long constants = NativeMemory.getLong(constMethod + 0x08);
		short nameIndex = NativeMemory.getShort(constMethod + 0x2A);
		short signatureIndex = NativeMemory.getShort(constMethod + 0x2C);
		long nameSymbol = NativeMemory.getLong(constants + 0x40 + nameIndex * 8) & ~(2 - 1);
		long signatureSymbol = NativeMemory.getLong(constants + 0x40 + signatureIndex * 8) & ~(2 - 1);
		short nameLength = NativeMemory.getShort(nameSymbol + 0x00);
		short signatureLength = NativeMemory.getShort(signatureSymbol + 0x00);
		String name = NativeMemory.getString(nameSymbol + 0x06, nameLength);

		if (name.equals("multiNewArray")) {
		    long addr = libjava.findEntry("Java_java_lang_reflect_Array_multiNewArray");
		    NativeMemory.putLong(method + 0x50, addr);
		    break;
		}
	    }
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    private static class ThreadLocalStorage extends ThreadLocal<Long> {
	private final int size;
	private long addr = 0;
	
	public ThreadLocalStorage(int size) {
		this.size = size;
	}

	@Override
	protected Long initialValue() {
	    addr = NativeMemory.allocateMemory(size);
	    return new Long(addr);
	}
    }

    private static final ThreadLocal<Long> fakeClassOopTLS = new ThreadLocalStorage(8);
    private static final ThreadLocal<Long> fakeClassTLS = new ThreadLocalStorage(0x200);
    private static final ThreadLocal<Long> fakeKlassVtableTLS = new ThreadLocalStorage(0x400);
    private static final ThreadLocal<Long> fakeKlassTLS = new ThreadLocalStorage(0x500);
    
    private static native long multiNewArray(long componentType, int[] dimensions);

    public static long invoke(long func, long[] args) {
	long fakeClassOop = fakeClassOopTLS.get();
	long fakeClass = fakeClassTLS.get();
	long fakeKlassVtable = fakeKlassVtableTLS.get();
	long fakeKlass = fakeKlassTLS.get();
	int[] dimensions = new int[]{0};

	try {
	    NativeMemory.putLong(fakeClassOop, fakeClass);
	    NativeMemory.putLong(fakeClass + 0x98, fakeKlass);
	    NativeMemory.putInt(fakeKlass + 0xc4, 0);
	    NativeMemory.putLong(fakeKlass, fakeKlassVtable);
	    NativeMemory.putLong(fakeKlassVtable + 0xd8, JVM_NativePath);
	    NativeMemory.putLong(fakeKlassVtable + 0x158, getcontext);
	    multiNewArray(fakeClassOop, dimensions);

	    NativeMemory.putLong(fakeKlassVtable + 0x158, setcontext);
	    NativeMemory.putLong(fakeKlass, fakeKlassVtable);

	    NativeMemory.putLong(fakeKlass + 0xe0, func);
	    NativeMemory.putLong(fakeKlass + 0x110, 0);
	    NativeMemory.putLong(fakeKlass + 0x118, 0);
	    for(int i=0; i<Math.min(args.length, 6); i++) {
		NativeMemory.putLong(fakeKlass + 0x48 + (i * 8), args[i]);
	    }

	    long ptr = multiNewArray(fakeClassOop, dimensions);
	    if(ptr != 0) {
		return NativeMemory.getLong(ptr);
	    } else {
		return 0;
	    }
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
	return -1;
    }
}
