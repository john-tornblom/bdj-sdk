NativeMemory = luajava.bindClass('org.homebrew.NativeMemory')
NativeLibrary = luajava.bindClass('org.homebrew.NativeLibrary')

size = 0xc30
addr = NativeMemory:allocateMemory(size)

NativeMemory:setMemory(addr, size, 0);
NativeMemory:putInt(addr + 0x10, -1)
NativeMemory:putString(addr + 0x2d, 'Hello, world')

libkernel = NativeLibrary:load(0x2001)
libkernel:invoke("sceKernelSendNotificationRequest", {0, addr, size, 0})

NativeMemory:freeMemory(addr);
