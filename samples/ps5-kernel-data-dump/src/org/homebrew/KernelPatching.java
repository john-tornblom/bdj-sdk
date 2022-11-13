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

	libkernel.sendNotificationRequest("Debug menu enabled");
    }
}
