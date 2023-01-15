/**
 *
 **/
typedef struct payload_args {
  void* (*sceKernelDlsym)(int fd, const char* sym, void* addr);
  int *rwpipe;
  int *rwpair;
  long kpipe_addr;
  long kdata_base_addr;
  int *payloadout;
} payload_args_t;


/**
 * libc functions.
 **/
int (*printf)(const char *, ...);


/**
 * libkernel functions.
 **/
int (*getpid)();


/**
 * Entry-point for the ELF loader.
 **/
int _start(payload_args_t *args) {
  int pid;
  
  args->sceKernelDlsym(0x2001, "getpid", &getpid);
  args->sceKernelDlsym(0x2, "printf", &printf);
  
  pid = getpid();
  printf("%d\n", pid);
  
  return pid;
}
