URLClassLoader = luajava.bindClass('java.net.URLClassLoader')
url = luajava.newInstance('java.net.URL', 'file:///VP/BDMV/JAR/00000.jar');
loader = URLClassLoader:newInstance({url})

NativeMemory = loader:loadClass('org.homebrew.NativeMemory')
NativeLibrary = loader:loadClass('org.homebrew.NativeLibrary')
libkernel = NativeLibrary:load(0x2001)

pid = libkernel:invoke('getpid', {})
print('getpid(): ' .. pid)
