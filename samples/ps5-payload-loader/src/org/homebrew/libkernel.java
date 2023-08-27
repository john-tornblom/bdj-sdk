package org.homebrew;

public class libkernel {
    private static NativeLibrary lib;
    
    static {
	lib = NativeLibrary.load(0x2001);
    }

    public static long addressOf(String symbol) {
	return lib.findEntry(symbol);
    }
    
    public static int __error() {
	return (int)lib.invoke("__error");
    }

    public static long usleep(int usec) {
	return lib.invoke("usleep", usec);
    }

    public static int getpid() {
	return (int)lib.invoke("getpid");
    }

    public static int getuid() {
	return (int)lib.invoke("getuid");
    }
    
    public static int close(int fd) {
	return (int)lib.invoke("close", fd);
    }

    public static long read(int fd, long buf, long count) {
	return lib.invoke("read", fd, buf, count);
    }

    public static long write(int fd, long buf, long count) {
	return lib.invoke("write", fd, buf, count);
    }

    public static int pipe(long fds) {
	return (int)lib.invoke("pipe", fds);
    }

    public static int dup(int fd) {
	return (int)lib.invoke("dup", fd);
    }

    public static int dup2(int oldfd, int newfd) {
	return (int)lib.invoke("dup2", oldfd, newfd);
    }
    
    public static int kqueue() {
	return (int)lib.invoke("kqueue");
    }

    public static int kevent(int kq, long changelist, int nchanges,
			     long eventlist, int nevents, long timeout) {
	return (int)lib.invoke("kevent", kq, changelist, nchanges, eventlist,
			       nevents, timeout);
    }
    
    public static int socket(int domain, int type, int protocol) {
	return (int)lib.invoke("socket", domain, type, protocol);
    }

    public static int getsockopt(int sockfd, int level, int optname, long optval,
				 long optlen) {
	 return (int)lib.invoke("getsockopt", sockfd, level, optname, optval, optlen);
    }

    public static int setsockopt(int sockfd, int level, int optname, long optval,
				 int optlen) {
	return (int)lib.invoke("setsockopt", sockfd, level, optname, optval, optlen);
    }

    public static int cpuset_setaffinity(int level, int which, long id, int setsize, long mask) {
	return (int)lib.invoke("cpuset_setaffinity", level, which, id, setsize, mask);
    }

    public static int rtprio_thread(int function, int lwpid, long prio) {
	return (int)lib.invoke("rtprio_thread", function, lwpid, prio);
    }

    public static int getSystemSwVersion() {
	String name = "kern.sdk_version";
	long p_name = NativeMemory.allocateMemory(name.length() + 1);
	long p_size = NativeMemory.allocateMemory(4);
	long p_value = NativeMemory.allocateMemory(4);

	try {
	    NativeMemory.setMemory(p_name, name.length() + 1, (byte)0);
	    NativeMemory.putString(p_name, name);
	    NativeMemory.putInt(p_size, 4);
	    NativeMemory.putInt(p_value, 0);

	    lib.invoke("sysctlbyname", p_name, p_value, p_size, 0, 0);
	    return NativeMemory.getInt(p_value);
	} finally {
	    NativeMemory.freeMemory(p_name);
	    NativeMemory.freeMemory(p_size);
	    NativeMemory.freeMemory(p_value);
	}
    }

    public static int sendNotificationRequest(String msg) {
	long size = 0xc30;
	long addr = NativeMemory.allocateMemory(size);

	try {
	    NativeMemory.setMemory(addr, size, (byte)0);
	    NativeMemory.putInt(addr + 0x10, -1);
	    NativeMemory.putString(addr + 0x2d, msg);
	    return (int)lib.invoke("sceKernelSendNotificationRequest", 0, addr, size, 0);
	} finally {
	    NativeMemory.freeMemory(addr);
	}
    }

    public static int ptrace(int request, int pid, long addr, int data) {
	return (int)lib.invoke("ptrace", request, pid, addr, data);
    }

    public static long mmap(long addr, long length, int prot, int flags,
			   int fd, long offset) {
	return lib.invoke("mmap", addr, length, prot, flags, fd, offset);
    }

    public static int mprotect(long addr, long length, int prot) {
	return (int)lib.invoke("mprotect", addr, length, prot);
    }

    public static int munmap(long addr, long length) {
	return (int)lib.invoke("munmap", addr, length);
    }

    public static int waitpid(int pid, long wstatus, int options) {
	return (int)lib.invoke("waitpid", pid, wstatus, options);
    }
    
    public static int jitCreateSharedMemory(long name, long size, int prot, long dest) {
	return (int)lib.invoke("sceKernelJitCreateSharedMemory", name, size, prot, dest);
    }
    
    public static int jitCreateAliasOfSharedMemory(int fd, int prot, long dest) {
	return (int)lib.invoke("sceKernelJitCreateAliasOfSharedMemory", fd, prot, dest);
    }

    public static long jitMapSharedMemory(int fd, int prot, long dest) {
	return lib.invoke("sceKernelJitMapSharedMemory", fd, prot, dest);
    }
}
