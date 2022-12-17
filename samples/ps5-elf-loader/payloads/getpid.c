/**
 * Prototype for sony dlsym function.
 **/
typedef void* (sceKernelDlsym_t)(int fd, const char* sym, void* addr);


/**
 * Entry-point for the ELF loader.
 **/
int _start(sceKernelDlsym_t *sceKernelDlsym) {
  int (*getpid)();
  
  sceKernelDlsym(0x2001, "getpid", &getpid);
  
  return getpid();
}
