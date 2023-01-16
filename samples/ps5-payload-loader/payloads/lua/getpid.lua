NativeLibrary = luajava.bindClass('org.homebrew.NativeLibrary')
libkernel = NativeLibrary:load(0x2001)

pid = libkernel:invoke('getpid', {})
print('getpid(): ' .. pid)
