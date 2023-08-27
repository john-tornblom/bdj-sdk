package org.homebrew;

import java.io.IOException;

public class KernelPatching {

    public static int getSecurityFlags() throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	return KernelMemory.getInt(kbase + KernelOffset.SECURITY_FLAGS);
    }

    public static void setSecurityFlags(int flags) throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	KernelMemory.putInt(kbase + KernelOffset.SECURITY_FLAGS, flags);
    }

    public static byte getUtokenFlags() throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	return KernelMemory.getByte(kbase + KernelOffset.UTOKEN_FLAGS);
    }

    public static void setUtokenFlags(byte flags) throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	KernelMemory.putByte(kbase + KernelOffset.UTOKEN_FLAGS, flags);
    }

    public static long getQAFlags() throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	return KernelMemory.getLong(kbase + KernelOffset.QA_FLAGS);
    }

    public static void setQAFlags(long flags) throws IOException  {
	long kbase = KernelMemory.getBaseAddress();
	KernelMemory.putLong(kbase + KernelOffset.QA_FLAGS, flags);
    }

    public static byte getTargetId() throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	return KernelMemory.getByte(kbase + KernelOffset.TARGETID);
    }

    public static void setTargetId(byte targetId) throws IOException {
	long kbase = KernelMemory.getBaseAddress();
	KernelMemory.putByte(kbase + KernelOffset.TARGETID, targetId);
    }

    public static long getSceAuthId(int pid) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	return KernelMemory.getLong(ucred + 0x58);
    }

    public static long getSceAuthId() throws IOException {
	return getSceAuthId(libkernel.getpid());
    }

    public static void setSceAuthId(int pid, long authId) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	KernelMemory.putLong(ucred + 0x58, authId);
    }

    public static void setSceAuthId(long authId) throws IOException {
	setSceAuthId(libkernel.getpid(), authId);
    }

    public static byte[] getSceCaps(int pid) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	return KernelMemory.getBytes(ucred + 0x60, 16);
    }

    public static byte[] getSceCaps() throws IOException {
	return getSceCaps(libkernel.getpid());
    }

    public static void setSceCaps(int pid, byte[] caps) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	KernelMemory.putBytes(ucred + 0x60, caps);
    }

    public static void setSceCaps(int pid, long caps0, long caps1) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	KernelMemory.putLong(ucred + 0x60, caps0);
	KernelMemory.putLong(ucred + 0x68, caps1);
    }

    public static void setSceCaps(byte[] caps) throws IOException {
	setSceCaps(libkernel.getpid(), caps);
    }

    public static void setSceCaps(long caps0, long caps1) throws IOException {
	 setSceCaps(libkernel.getpid(), caps0, caps1);
    }

    public static long getSceAttr(int pid) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	return KernelMemory.getLong(ucred + 0x83);
    }

    public static long getSceAttr() throws IOException {
	return getSceAttr(libkernel.getpid());
    }

    public static void setSceAttr(int pid, long attr) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	KernelMemory.putLong(ucred + 0x83, attr);
    }

    public static void setSceAttr(long attr) throws IOException {
	setSceAttr(libkernel.getpid(), attr);
    }

    public static void escalatePrivileges(int pid) throws IOException {
	long ucred_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);

	KernelMemory.putInt(ucred + 0x04, 0); // cr_uid
	KernelMemory.putInt(ucred + 0x08, 0); // cr_ruid
	KernelMemory.putInt(ucred + 0x0C, 0); // cr_svuid
	KernelMemory.putInt(ucred + 0x10, 1); // cr_ngroups
	KernelMemory.putInt(ucred + 0x14, 0); // cr_rgid
    }

    public static void escalatePrivileges() throws IOException {
	escalatePrivileges(libkernel.getpid());
    }

    public static void jailbreak(int pid) throws IOException {
	long rootvnode_addr = KernelMemory.getBaseAddress() + KernelOffset.ROOTVNODE;
	long proc_fd_addr = KernelMemory.getProcAddress(pid) + KernelOffset.PROC_FD;
	long rootvnode = KernelMemory.getLong(rootvnode_addr);
	long proc_fd = KernelMemory.getLong(proc_fd_addr);

	KernelMemory.putLong(proc_fd + 0x10, rootvnode); // fd_rdir
	KernelMemory.putLong(proc_fd + 0x18, rootvnode); // fd_jdir
    }

    public static void jailbreak() throws IOException {
	jailbreak(libkernel.getpid());
    }
}
