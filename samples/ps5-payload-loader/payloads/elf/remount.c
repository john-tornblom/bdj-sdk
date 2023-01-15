/**
 * Payload arguments provided by ELF loader.
 **/
typedef struct payload_args {
  void* (*sceKernelDlsym)(int, const char*, void*);
  int *rwpipe;
  int *rwpair;
  long kpipe_addr;
  long kdata_base_addr;
  int *payloadout;
} payload_args_t;


/**
 *  FreeBSD macros.
 **/
#define STDIN_FILENO 0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2

#define	MNT_UPDATE 0x0000000000010000ULL /* not real mount, just update */


/**
 *
 **/
struct iovec {
  void         *iov_base;
  unsigned long iov_len; 
};


/**
 * libc functions.
 **/
void* (*realloc)(void *, unsigned long);
unsigned long (*strlen)(const char *);
char* (*strdup)(const char *);
void (*perror)(const char *);


/**
 * libkernel functions.
 **/
void* (*sceKernelDlsym)(int, const char*, void*) = 0;
int (*nmount)(struct iovec*, unsigned int, int);
int (*dup2)(int, int);
int (*dup)(int);


/**
 * Macro to initialize a function from a shared object.
 **/
#define DLSYM(fd, sym) if(!sceKernelDlsym || \
			  sceKernelDlsym(fd, #sym, &sym)) \
    {return 1;}



/**
 * Build an iovec structure for nmount().
 **/
static void
build_iovec(struct iovec **iov, int *iovlen, const char *name, const char *v) {
  int i;

  if(*iovlen < 0) {
    return;
  }

  i = *iovlen;
  *iov = realloc(*iov, sizeof(**iov) * (i + 2));
  if(*iov == 0) {
    *iovlen = -1;
    perror("realloc");
    return;
  }

  (*iov)[i].iov_base = strdup(name);
  (*iov)[i].iov_len = strlen(name) + 1;
  i++;

  (*iov)[i].iov_base = v ? strdup(v) : 0;
  (*iov)[i].iov_len = v ? strlen(v) + 1 : 0;
  i++;  

  *iovlen = i;
}


/**
 * Remount /system with write permissions.
 **/
int
main(void) {
  struct iovec* iov = 0;
  int iovlen = 0;

  build_iovec(&iov, &iovlen, "fstype", "exfatfs");
  build_iovec(&iov, &iovlen, "fspath", "/system");
  build_iovec(&iov, &iovlen, "from", "/dev/ssd0.system");
  build_iovec(&iov, &iovlen, "large", "yes");
  build_iovec(&iov, &iovlen, "timezone", "static");
  build_iovec(&iov, &iovlen, "async", 0);
  build_iovec(&iov, &iovlen, "ignoreacl", 0);

  if(nmount(iov, iovlen, MNT_UPDATE)) {
    perror("nmount");
    return 1;
  }

  return 0;
}


/**
 * Entry-point for the ELF loader.
 **/
int
_start(payload_args_t *args, int sock_fd) {
  int stdout_fd = -1;
  int stderr_fd = -1;
  int exit_code = 0;

  sceKernelDlsym = args->sceKernelDlsym;

  DLSYM(0x2, realloc);
  DLSYM(0x2, strlen);
  DLSYM(0x2, strdup);
  DLSYM(0x2, perror);

  DLSYM(0x2001, dup);
  DLSYM(0x2001, dup2);
  DLSYM(0x2001, nmount);

  // backup stdout and stderr
  stdout_fd = dup(STDOUT_FILENO);
  stderr_fd = dup(STDERR_FILENO);

  // redirect stdout and stderr to socket
  dup2(sock_fd, STDOUT_FILENO);
  dup2(sock_fd, STDERR_FILENO);

  exit_code = main();

  // resore stdout and stderr
  dup2(stdout_fd, STDOUT_FILENO);
  dup2(stderr_fd, STDERR_FILENO);

  return exit_code;
}
