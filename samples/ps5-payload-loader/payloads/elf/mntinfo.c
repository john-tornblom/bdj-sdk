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

#define MNT_WAIT   1  /* synchronously wait for I/O to complete */
#define MNT_NOWAIT 2  /* start all I/O, but do not wait for it */
#define MFSNAMELEN 16 /* length of type name including null */
#define MNAMELEN   88 /* size of on/from name bufs */


/**
 * Filesystem id type.
 **/
typedef struct fsid {
  int val[2];
} fsid_t;


/**
 * Filesystem statistics.
 **/
struct statfs {
  unsigned int  f_version;                /* structure version number */
  unsigned int  f_type;                   /* type of filesystem */
  unsigned long f_flags;                  /* copy of mount exported flags */
  unsigned long f_bsize;                  /* filesystem fragment size */
  unsigned long f_iosize;                 /* optimal transfer block size */
  unsigned long f_blocks;                 /* total data blocks in filesystem */
  unsigned long f_bfree;                  /* free blocks in filesystem */
           long f_bavail;                 /* free blocks avail to non-superuser */
  unsigned long f_files;                  /* total file nodes in filesystem */
           long f_ffree;                  /* free nodes avail to non-superuser */
  unsigned long f_syncwrites;             /* count of sync writes since mount */
  unsigned long f_asyncwrites;            /* count of async writes since mount */
  unsigned long f_syncreads;              /* count of sync reads since mount */
  unsigned long f_asyncreads;             /* count of async reads since mount */
  unsigned long f_spare[10];              /* unused spare */
  unsigned int  f_namemax;                /* maximum filename length */
  int           f_owner;                  /* user that mounted the filesystem */
  fsid_t        f_fsid;                   /* filesystem id */
  char          f_charspare[80];          /* spare string space */
  char          f_fstypename[MFSNAMELEN]; /* filesystem type name */
  char          f_mntfromname[MNAMELEN];  /* mounted filesystem */
  char          f_mntonname[MNAMELEN];    /* directory on which mounted */
};


/**
 * libc functions.
 **/
int (*printf)(const char *, ...);
void* (*malloc)(unsigned int);
void* (*memset)(void *, int, unsigned int);
void (*perror)(const char *);
void (*free)(void *);



/**
 * libkernel functions.
 **/
void* (*sceKernelDlsym)(int, const char*, void*) = 0;
int (*getfsstat)(struct statfs *, long, int);


/**
 * Macro to initialize a function from a shared object.
 **/
#define DLSYM(fd, sym) if(!sceKernelDlsym || \
			  sceKernelDlsym(fd, #sym, &sym)) \
    {return 1;}


/**
 * Get all mount points.
 **/
static int
getmntinfo(struct statfs **bufp, int mode) {
  struct statfs *buf;
  int nitems = 0;
  int size = 0;
  int size2 = 0;

  // get number of mount points
  if((nitems = getfsstat(0, 0, MNT_NOWAIT)) < 0) {
    perror("getfsstat");
    return -1;
  }

  // allocate sufficient space
  size = sizeof(struct statfs) * nitems;
  if(!(buf = malloc(size))) {
    perror("malloc");
    return -1;
  }

  // get the mount points
  memset(buf, 0, size);
  if((size2 = getfsstat(buf, size, mode)) < 0) {
    perror("getfsstat");
    return -1;
  }

  *bufp = buf;

  return nitems;
}


/**
 * Print all mount points to stdout.
 **/
int
main(void) {
  struct statfs *buf = 0;
  int nitems = 0;

  if((nitems = getmntinfo(&buf, MNT_WAIT)) < 0) {
    return 1;
  }

  for(int i=0; i<nitems; i++) {
    printf("%s on %s (%s, flags=0x%x)\n",
	   buf[i].f_mntfromname,
	   buf[i].f_mntonname,
	   buf[i].f_fstypename,
	   buf[i].f_flags);
  }

  free(buf);

  return 0;
}


/**
 * Entry-point for the ELF loader.
 **/
int
_start(payload_args_t *args) {
  int stdout_fd = -1;
  int stderr_fd = -1;
  int exit_code = 0;

  sceKernelDlsym = args->sceKernelDlsym;

  DLSYM(0x2, printf);
  DLSYM(0x2, malloc);
  DLSYM(0x2, memset);
  DLSYM(0x2, perror);
  DLSYM(0x2, free);

  DLSYM(0x2001, getfsstat);

  return main();
}
