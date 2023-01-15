package org.homebrew;

import java.io.IOException;

public class KernelPatching {

    public static void enableDebugMenu() throws IOException {
	long kernel_base = KernelMemory.getBaseAddress();
	
	int security_flags = KernelMemory.getInt(kernel_base + KernelOffset.SECURITY_FLAGS);
	byte utoken_flags = KernelMemory.getByte(kernel_base + KernelOffset.UTOKEN_FLAGS);
	byte qa1_flags = KernelMemory.getByte(kernel_base + KernelOffset.QA_FLAGS + 1);
	byte qa2_flags = KernelMemory.getByte(kernel_base + KernelOffset.QA_FLAGS + 2);

	security_flags |= 0x14;
	utoken_flags |= 0x1;
	qa1_flags |= 0x3;
	qa2_flags |= 0x1;

	KernelMemory.putInt(kernel_base + KernelOffset.SECURITY_FLAGS, security_flags);
	KernelMemory.putByte(kernel_base + KernelOffset.UTOKEN_FLAGS, utoken_flags);
	KernelMemory.putByte(kernel_base + KernelOffset.QA_FLAGS + 1, qa1_flags);
	KernelMemory.putByte(kernel_base + KernelOffset.QA_FLAGS + 2, qa2_flags);
    }

    public static void jailbreak() throws IOException {
	long rootvnode_addr = KernelMemory.getBaseAddress() + KernelOffset.ROOTVNODE;
	long proc_fd_addr = KernelMemory.getProcAddress() + KernelOffset.PROC_FD;
	long rootvnode = KernelMemory.getLong(rootvnode_addr);
	long proc_fd = KernelMemory.getLong(proc_fd_addr);
	
	KernelMemory.putLong(proc_fd + 0x10, rootvnode); // fd_rdir
	KernelMemory.putLong(proc_fd + 0x18, rootvnode); // fd_jdir
    }

    public static void escalatePrivileges() throws IOException {
	long ucred_addr = KernelMemory.getProcAddress() + KernelOffset.PROC_UCRED;
	long ucred = KernelMemory.getLong(ucred_addr);
	
	// Escalate BSD privileges
	KernelMemory.putInt(ucred + 0x04, 0); // cr_uid
	KernelMemory.putInt(ucred + 0x08, 0); // cr_ruid
	KernelMemory.putInt(ucred + 0x0C, 0); // cr_svuid
	KernelMemory.putInt(ucred + 0x10, 1); // cr_ngroups
	KernelMemory.putInt(ucred + 0x14, 0); // cr_rgid
	
	// Escalate sony privileges
	KernelMemory.putLong(ucred + 0x58, 0x4801000000000013l); // cr_sceAuthID
	KernelMemory.putLong(ucred + 0x60, -1l);                 // cr_sceCaps[0]
	KernelMemory.putLong(ucred + 0x68, -1l);                 // cr_sceCaps[1]
	KernelMemory.putByte(ucred + 0x83, (byte)0x80);          // cr_sceAttr[0]
    }
}

