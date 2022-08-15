URLClassLoader = luajava.bindClass('java.net.URLClassLoader')
url = luajava.newInstance('java.net.URL', 'file:///VP/BDMV/JAR/00000.jar');
loader = URLClassLoader:newInstance({url})

NativeMemory = loader:loadClass('org.homebrew.NativeMemory')
NativeLibrary = loader:loadClass('org.homebrew.NativeLibrary')
libkernel = NativeLibrary:load(0x2001)

size = 0xc30
addr = NativeMemory:allocateMemory(size)

NativeMemory:setMemory(addr, size, 0);
NativeMemory:putInt(addr + 0x10, -1)
NativeMemory:putString(addr + 0x2d, 'Hello, world')

libkernel:invoke("sceKernelSendNotificationRequest", {0, addr, size, 0})

NativeMemory:freeMemory(addr);
