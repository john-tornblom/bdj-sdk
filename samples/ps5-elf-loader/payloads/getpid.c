
typedef struct payload_args {
  void* (*sceKernelDlsym)(int fd, const char* sym, void* addr);
  int *rwpipe;
  int *rwpair;
  long kpipe_addr;
  long kdata_base_addr;
  int *payloadout;
} payload_args_t;


int (*getpid)();

/**
 * Entry-point for the ELF loader.
 **/
int _start(payload_args_t *args) {
  args->sceKernelDlsym(0x2001, "getpid", &getpid);
  return getpid();
}
