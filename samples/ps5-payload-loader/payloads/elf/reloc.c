/**
 * Payload arguments.
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
 * Relocatable data.
 **/
static char* strings[] = {
  "ABCD",
  "EFGH",
  "IJKL",
  "MNOP"
};


/**
 * libc functions.
 **/
int (*printf)(const char *, ...);


/**
 * Entry-point for the ELF loader.
 **/
int _start(payload_args_t *args) {
  if(args->sceKernelDlsym(0x2, "printf", &printf)) {
    return -1;
  }
  
  if(strings[2][2] == 'K') {
    printf("Pass\n");
  } else {
    printf("Fail\n");
  }

  return 0;
}
