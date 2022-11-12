package org.homebrew;

public class libkernel {
    private static NativeLibrary lib;
    
    static {
	lib = NativeLibrary.load(0x2001);
    }

    public static long usleep(int usec) {
	return lib.invoke("usleep", usec);
    }

    public static int close(int fd) {
	return (int)lib.invoke("close", fd);
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
}
