package org.homebrew;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import jdk.internal.access.SharedSecrets;

public class ElfLoading {
    private static final int OFF_EHDR_TYPE  = 0x10;
    private static final int OFF_EHDR_ENTRY = 0x18;
    private static final int OFF_EHDR_PHOFF = 0x20;
    private static final int OFF_EHDR_SHOFF = 0x28;
    private static final int OFF_EHDR_PHNUM = 0x38;
    private static final int OFF_EHDR_SHNUM = 0x3c;

    private static final int OFF_PHDR_TYPE   = 0x00;
    private static final int OFF_PHDR_FLAGS  = 0x04;
    private static final int OFF_PHDR_OFFSET = 0x08;
    private static final int OFF_PHDR_VADDR  = 0x10;
    private static final int OFF_PHDR_FILESZ = 0x20;
    private static final int OFF_PHDR_MEMSZ  = 0x28;

    private static final int OFF_SHDR_TYPE   = 0x04;
    private static final int OFF_SHDR_OFFSET = 0x18;
    private static final int OFF_SHDR_SIZE   = 0x20;

    private static final int OFF_RELA_OFFSET = 0x00;
    private static final int OFF_RELA_INFO   = 0x08;
    private static final int OFF_RELA_ADDEND = 0x10;

    private static final int SIZE_PHDR = 0x38;
    private static final int SIZE_EHDR = 0x40;
    private static final int SIZE_SHDR = 0x40;
    private static final int SIZE_RELA = 0x18;

    private static final int ET_EXEC = 2;
    private static final int ET_DYN  = 3;

    private static final int PT_LOAD = 0x01;

    private static final int SHT_RELA = 4;

    private static final int R_X86_64_RELATIVE = 8;

    private static final int PF_X = 0x1;
    private static final int PF_W = 0x2;
    private static final int PF_R = 0x4;

    private static final int PROT_NONE  = 0x0;
    private static final int PROT_READ  = 0x1;
    private static final int PROT_WRITE = 0x2;
    private static final int PROT_EXEC  = 0x4;

    private static final int MAP_SHARED    = 0x1;
    private static final int MAP_PRIVATE   = 0x2;
    private static final int MAP_FIXED     = 0x10;
    private static final int MAP_ANONYMOUS = 0x1000;

    private static final long arg_addr;

    static {
	long payload_output_addr = NativeMemory.allocateMemory(8);
	long pipe_rw_fds = NativeMemory.allocateMemory(8);
	long kern_rw_fds = NativeMemory.allocateMemory(8);

	arg_addr = NativeMemory.allocateMemory(0x30);

	NativeMemory.putInt(kern_rw_fds + 0, KernelMemory.getMasterSocket());
	NativeMemory.putInt(kern_rw_fds + 4, KernelMemory.getVictimSocket());

	NativeMemory.putInt(pipe_rw_fds + 0, KernelMemory.getPipeRead());
	NativeMemory.putInt(pipe_rw_fds + 4, KernelMemory.getPipeWrite());

	NativeMemory.putLong(arg_addr + 0x00, libkernel.addressOf("sceKernelDlsym"));
	NativeMemory.putLong(arg_addr + 0x08, pipe_rw_fds);
	NativeMemory.putLong(arg_addr + 0x10, kern_rw_fds);
	NativeMemory.putLong(arg_addr + 0x28, payload_output_addr);

	try {
	    NativeMemory.putLong(arg_addr + 0x18, KernelMemory.getPipeAddress());
	    NativeMemory.putLong(arg_addr + 0x20, KernelMemory.getBaseAddress());
	} catch(Throwable t) {
	    NativeMemory.putLong(arg_addr + 0x18, 0);
	    NativeMemory.putLong(arg_addr + 0x20, 0);
	}
    }

    public static void spawnServer(int port) throws Exception {
	new LoadingServer(port) {
	    public void runPayload(byte[] bytes, OutputStream os)
		throws Exception {
		ElfLoading.runElf(bytes, os);
	    }
	}.spawn();
    }

    public static void runElf(String elfPath) throws Exception {
	FileOutputStream os = new FileOutputStream("/dev/null");
	try {
	    byte[] bytes = Files.readAllBytes(Paths.get(elfPath));
	    runElf(bytes, os);
	} finally {
	    os.close();
	}
    }

    private static long ROUND_PG(long val) {
	return (val + 0x3FFF) & 0xFFFFC000;
    }

    private static long TRUNC_PG(long val) {
	return val & 0xFFFFC000;
    }

    private static int PFLAGS(int p_flags) {
	int prot = 0;

	if((p_flags & PF_X) == PF_X) {
	    prot |= PROT_EXEC;
	}
	if((p_flags & PF_W) == PF_W) {
	    prot |= PROT_WRITE;
	}
	if((p_flags & PF_R) == PF_R) {
	    prot |= PROT_READ;
	}

	return prot;
    }

    private static void r_relative(long base_addr, long rela_addr)
	throws Exception {
	long r_offset = NativeMemory.getLong(rela_addr + OFF_RELA_OFFSET);
	long r_addend = NativeMemory.getLong(rela_addr + OFF_RELA_ADDEND);

	NativeMemory.putLong(base_addr + r_offset, base_addr + r_addend);
    }

    private static void pt_load(long elf_addr, long base_addr, long phdr_addr)
	throws Exception {
	long p_offset = NativeMemory.getLong(phdr_addr + OFF_PHDR_OFFSET);
	long p_vaddr = NativeMemory.getLong(phdr_addr + OFF_PHDR_VADDR);
	long p_filesz = NativeMemory.getLong(phdr_addr + OFF_PHDR_FILESZ);
	long p_memsz = NativeMemory.getLong(phdr_addr + OFF_PHDR_MEMSZ);

	if(p_memsz == 0) {
	    return;
	}

	long memsz = ROUND_PG(p_memsz);
	long addr = base_addr + p_vaddr;

	if((addr=libkernel.mmap(addr, memsz,
				PROT_READ | PROT_WRITE,
				MAP_FIXED | MAP_ANONYMOUS | MAP_PRIVATE,
				-1, 0)) == -1) {
	    throw new Exception(libcInternal.strerror());
	}

	if(p_filesz > 0) {
	    NativeMemory.copyMemory(elf_addr + p_offset, addr, p_filesz);
	}
    }

    private static void pt_reload(long base_addr, long phdr_addr)
	throws Exception {
	long p_offset = NativeMemory.getLong(phdr_addr + OFF_PHDR_OFFSET);
	long p_vaddr = NativeMemory.getLong(phdr_addr + OFF_PHDR_VADDR);
	long p_memsz = NativeMemory.getLong(phdr_addr + OFF_PHDR_MEMSZ);
	int p_flags = NativeMemory.getInt(phdr_addr + OFF_PHDR_FLAGS);

	long addr = base_addr + p_vaddr;
	long memsz = ROUND_PG(p_memsz);
	int prot = PFLAGS(p_flags);

	long ret_addr = NativeMemory.allocateMemory(8);
	long data = NativeMemory.allocateMemory(memsz);

	int alias_fd = -1;
	int shm_fd = -1;

	// Backup data
	NativeMemory.copyMemory(addr, data, memsz);

	try {
	    // Create shm with executable permissions.
	    if(libkernel.jitCreateSharedMemory(0, memsz,
					       prot | PROT_READ | PROT_WRITE,
					       ret_addr) == 0) {
		shm_fd = NativeMemory.getInt(ret_addr);
	    } else {
		throw new Exception(libcInternal.strerror());
	    }

	    // Map shm into an executable address space.
	    if(libkernel.mmap(addr, memsz, prot,
			      MAP_FIXED | MAP_PRIVATE, shm_fd, 0) == -1) {
		throw new Exception(libcInternal.strerror());
	    }

	    // Create an shm alias fd with write permissions.
	    if(libkernel.jitCreateAliasOfSharedMemory(shm_fd,
						      PROT_READ | PROT_WRITE,
						      ret_addr) == 0) {
		alias_fd = NativeMemory.getInt(ret_addr);
	    } else {
		throw new Exception(libcInternal.strerror());
	    }

	    // Map shm alias into a writable address space.
	    if((addr=libkernel.mmap(0, memsz, PROT_READ | PROT_WRITE,
				    MAP_SHARED, alias_fd, 0)) == -1) {
		throw new Exception(libcInternal.strerror());
	    }

	    // Resore data
	    NativeMemory.copyMemory(data, addr, memsz);
	    libkernel.munmap(addr, memsz);
	} finally {
	    // Cleanup resources.
	    NativeMemory.freeMemory(ret_addr);
	    NativeMemory.freeMemory(data);

	    if(alias_fd != -1) {
		libkernel.close(alias_fd);
	    }
	    if(shm_fd != -1) {
		libkernel.close(shm_fd);
	    }
	}
    }

    public static void runElf(byte[] elf_bytes, OutputStream os) throws Exception {
	long elf_addr = 0;

	long base_addr = -1;
	long base_size = 0;

	long min_vaddr = -1;
	long max_vaddr = -1;

	PrintStream ps = new PrintStream(os);

	if(elf_bytes[0] != (byte)0x7f || elf_bytes[1] != (byte)0x45 ||
	   elf_bytes[2] != (byte)0x4c || elf_bytes[3] != (byte)0x46) {
	    throw new IOException("Invalid ELF file");
	}

	try {
	    elf_addr = NativeMemory.allocateMemory(elf_bytes.length);
	    for(int i=0; i<elf_bytes.length; i++) {
		NativeMemory.putByte(elf_addr + i, elf_bytes[i]);
	    }

	    short e_type = NativeMemory.getShort(elf_addr + OFF_EHDR_TYPE);
	    long e_entry  = NativeMemory.getLong(elf_addr + OFF_EHDR_ENTRY);
	    long e_phoff  = NativeMemory.getLong(elf_addr + OFF_EHDR_PHOFF);
	    long e_shoff  = NativeMemory.getLong(elf_addr + OFF_EHDR_SHOFF);
	    short e_phnum = NativeMemory.getShort(elf_addr + OFF_EHDR_PHNUM);
	    short e_shnum = NativeMemory.getShort(elf_addr + OFF_EHDR_SHNUM);

	    // Compute size of virtual memory region.
	    for(int i=0; i<e_phnum; i++) {
		long phdr_addr = elf_addr + e_phoff + (i * SIZE_PHDR);
		long p_vaddr = NativeMemory.getLong(phdr_addr + OFF_PHDR_VADDR);
		long p_memsz = NativeMemory.getLong(phdr_addr + OFF_PHDR_MEMSZ);

		if(p_vaddr < min_vaddr || min_vaddr == -1) {
		    min_vaddr = p_vaddr;
		}

		if(max_vaddr < p_vaddr + p_memsz) {
		    max_vaddr = p_vaddr + p_memsz;
		}
	    }

	    min_vaddr = TRUNC_PG(min_vaddr);
	    max_vaddr = ROUND_PG(max_vaddr);
	    base_size = max_vaddr - min_vaddr;

	    int flags = MAP_PRIVATE | MAP_ANONYMOUS;
	    if(e_type == ET_DYN) {
		base_addr = 0;
	    } else if(e_type == ET_EXEC) {
		base_addr = min_vaddr;
		flags |= MAP_FIXED;
	    } else {
		throw new IOException("Unsupported ELF file");
	    }

	    // Reserve an address space of sufficient size.
	    if((base_addr=libkernel.mmap(base_addr, base_size, PROT_NONE,
					 flags, -1, 0)) == -1) {
		throw new Exception(libcInternal.strerror());
	    }

	    // Parse program headers.
	    for(int i=0; i<e_phnum; i++) {
		long phdr_addr = elf_addr + e_phoff + (i * SIZE_PHDR);
		int p_type  = NativeMemory.getInt(phdr_addr + OFF_PHDR_TYPE);

		if(p_type == PT_LOAD) {
		    pt_load(elf_addr, base_addr, phdr_addr);
		}
	    }

	    // Apply relocations.
	    for(int i=0; i<e_shnum; i++) {
		long shdr_addr = elf_addr + e_shoff + (i * SIZE_SHDR);
                int sh_type = NativeMemory.getInt(shdr_addr + OFF_SHDR_TYPE);

                if (sh_type != SHT_RELA) {
		    continue;
		}

		long sh_offset = NativeMemory.getLong(shdr_addr + OFF_SHDR_OFFSET);
		long sh_size = NativeMemory.getLong(shdr_addr + OFF_SHDR_SIZE);

		for (int j=0; j<sh_size/SIZE_RELA; j++) {
		    long rela_addr = elf_addr + sh_offset + (SIZE_RELA * j);
		    long r_info = NativeMemory.getLong(rela_addr + OFF_RELA_INFO);
		    if(r_info == R_X86_64_RELATIVE) {
			r_relative(base_addr, rela_addr);
		    }
		}
	    }

	    // Set protection bits on mapped segments.
	    for(int i=0; i<e_phnum; i++) {
		long phdr_addr = elf_addr + e_phoff + (i * SIZE_PHDR);
		long p_memsz = NativeMemory.getLong(phdr_addr + OFF_PHDR_MEMSZ);
		long p_vaddr = NativeMemory.getLong(phdr_addr + OFF_PHDR_VADDR);
		int p_type = NativeMemory.getInt(phdr_addr + OFF_PHDR_TYPE);
		int p_flags = NativeMemory.getInt(phdr_addr + OFF_PHDR_FLAGS);

		if(p_type != PT_LOAD || p_memsz == 0) {
		    continue;
		}

		if((p_flags & PF_X) == PF_X) {
		    pt_reload(base_addr, phdr_addr);
		    continue;
		}

		long addr = base_addr + p_vaddr;
		long memsz = ROUND_PG(p_memsz);
		int prot = PFLAGS(p_flags);
		if(libkernel.mprotect(addr, memsz, prot) != 0) {
		    throw new Exception(libcInternal.strerror());
		}
	    }

	    if(base_addr != -1) {
		long args[] = new long[6];
		int sock_fd = -1;
		int stdout_fd = -1;
		int stderr_fd = -1;

		if(os != null) {
		    FileOutputStream fos = (FileOutputStream)os;
		    sock_fd = SharedSecrets.getJavaIOFileDescriptorAccess().get(fos.getFD());

		    // Backup stdout and stderr.
		    stdout_fd = libkernel.dup(1);
		    stderr_fd = libkernel.dup(2);

		    // Redirect stdout and stderr to socket.
		    libkernel.dup2(sock_fd, 1);
		    libkernel.dup2(sock_fd, 2);
		}

		// Invoke entry point.
		args[0] = arg_addr;
		args[1] = 0;
		args[2] = 0;
		args[3] = 0;
		args[4] = 0;
		args[5] = 0;
		NativeInvocation.invoke(base_addr + e_entry, args);

		if(os != null) {
		    // Resore stdout and stderr.
		    libkernel.dup2(stdout_fd, 1);
		    libkernel.dup2(stderr_fd, 2);
		}
	    } else {
		throw new IOException("Invalid ELF file");
	    }
	} finally {
	    if(elf_addr != 0) {
		NativeMemory.freeMemory(elf_addr);
	    }
	    if(base_addr != -1) {
		libkernel.munmap(base_addr, base_size);
	    }
        }
    }
}
