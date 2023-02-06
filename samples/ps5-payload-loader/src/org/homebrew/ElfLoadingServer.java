package org.homebrew;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import jdk.internal.access.SharedSecrets;

public class ElfLoadingServer {
    private static final long arg_addr;

    private static final int OFF_PROG_HEAD_TYPE   = 0x00;
    private static final int OFF_PROG_HEAD_FLAGS  = 0x04;
    private static final int OFF_PROG_HEAD_OFF    = 0x08;
    private static final int OFF_PROG_HEAD_VADDR  = 0x10;
    private static final int OFF_PROG_HEAD_FILESZ = 0x20;
    private static final int OFF_PROG_HEAD_MEMSZ  = 0x28;

    private static final int OFF_ELF_HEAD_ENTRY = 0x18;
    private static final int OFF_ELF_HEAD_PHOFF = 0x20;
    private static final int OFF_ELF_HEAD_SHOFF = 0x28;
    private static final int OFF_ELF_HEAD_PHNUM = 0x38;
    private static final int OFF_ELF_HEAD_SHNUM = 0x3c;

    private static final int OFF_SYMB_HEAD_TYPE   = 0x04;
    private static final int OFF_SYMB_HEAD_OFFSET = 0x18;
    private static final int OFF_SYMB_HEAD_SIZE   = 0x20;

    private static final int OFF_REL_OFFSET = 0x00;
    private static final int OFF_REL_INFO   = 0x08;

    private static final int SIZE_ELF_PROG_HEAD = 0x38;
    private static final int SIZE_ELF_SYMB_HEAD = 0x40;
    private static final int SIZE_ELF_HEAD      = 0x40;

    private static final int ELF_PT_NULL = 0x00;
    private static final int ELF_PT_LOAD = 0x01;

    private static final int PROT_NONE  = 0x0;
    private static final int PROT_READ  = 0x1;
    private static final int PROT_WRITE = 0x2;
    private static final int PROT_EXEC  = 0x4;

    private static final int MAP_SHARED    = 0x1;
    private static final int MAP_PRIVATE   = 0x2;
    private static final int MAP_FIXED     = 0x10;
    private static final int MAP_ANONYMOUS = 0x1000;

    private static final int SIZEOF_RELA = 0x18;

    private static final int SHT_RELA = 4;

    private static final int R_X86_64_NONE = 0;
    private static final int R_X86_64_GLOB_DAT = 6;
    private static final int R_X86_64_JUMP_SLOT = 7;
    private static final int R_X86_64_RELATIVE = 8;

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
    
    public static void spawn(int port) throws IOException {
	final ServerSocket ss = new ServerSocket(port);
	ss.setReuseAddress(true);
	
        new Thread(new Runnable() {
		public void run() {
		    try {
			ElfLoadingServer.run(ss);
		    } catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		    }
		}
	    }).start();
    }
    
    public static void run(ServerSocket ss) throws IOException {
        while (true) {
            try {
                serve(ss.accept());
            } catch (Throwable t) {
		LoggingUI.getInstance().log(t);
            }
        }
    }

    private static void serve(final Socket s) throws Exception {
        final PrintStream err = new PrintStream(s.getOutputStream());
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] bytes = readBytes(s);
                    runElf(bytes, s.getOutputStream());
                } catch (Throwable t) {
                    t.printStackTrace(err);
                }

                try {
                    s.close();
                } catch (Throwable t) {
		    LoggingUI.getInstance().log(t);
                }
            }

        }).start();
    }

    private static byte[] readBytes(Socket s) throws IOException {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	
        while (true) {
            int b = s.getInputStream().read();
            if (b < 0) {
                break;
            } else {
                buf.write(b);
            }
        }
	
        return buf.toByteArray();
    }

    private static void relocate_rela(long elf_addr, long shdr_addr, long base_addr) {
        long sh_offset = NativeMemory.getLong(shdr_addr + OFF_SYMB_HEAD_OFFSET);
        long sh_size = NativeMemory.getLong(shdr_addr + OFF_SYMB_HEAD_SIZE);
        long rel_num = sh_size / SIZEOF_RELA;

        for (int i=0; i<rel_num; i++) {
            long rela_addr = elf_addr + sh_offset + (SIZEOF_RELA * i);
            long r_offset = NativeMemory.getLong(rela_addr + OFF_REL_OFFSET);
            long r_info = NativeMemory.getLong(rela_addr + OFF_REL_INFO);

            switch((int)r_info) {
	    case R_X86_64_NONE:
		break;

	    case R_X86_64_JUMP_SLOT:
	    case R_X86_64_GLOB_DAT:
		//TODO
		break;

	    case R_X86_64_RELATIVE:
		long value_addr = base_addr + r_offset;
		long old_value = NativeMemory.getLong(value_addr);
		long new_value = old_value + base_addr;
		NativeMemory.putLong(value_addr, new_value);
		break;
            }
        }
    }

    private static void runElf(byte[] elf_bytes, OutputStream os) throws Exception {
	long elf_addr = 0;
	long ret_addr = 0;
	
	long data_addr = 0;
	long data_size = 0;

	long text_addr = 0;
	long text_size = 0;
	
	int shm_fd = -1;

	PrintStream ps = new PrintStream(os);
	
	if(elf_bytes[0] != (byte)0x7f || elf_bytes[1] != (byte)0x45 ||
	   elf_bytes[2] != (byte)0x4c || elf_bytes[3] != (byte)0x46) {
	    throw new IOException("Invalid ELF file");
	}
	
	try {
	    ret_addr = NativeMemory.allocateMemory(8);
	    elf_addr = NativeMemory.allocateMemory(elf_bytes.length);
	    for(int i=0; i<elf_bytes.length; i++) {
		NativeMemory.putByte(elf_addr + i, elf_bytes[i]);
	    }

	    long elf_entry_point     = NativeMemory.getLong(elf_addr + OFF_ELF_HEAD_ENTRY);
	    long elf_prog_heads_off  = NativeMemory.getLong(elf_addr + OFF_ELF_HEAD_PHOFF);
	    long elf_symbols_off     = NativeMemory.getLong(elf_addr + OFF_ELF_HEAD_SHOFF);
	    short elf_prog_heads_num = NativeMemory.getShort(elf_addr + OFF_ELF_HEAD_PHNUM);
	    short elf_symbols_num    = NativeMemory.getShort(elf_addr + OFF_ELF_HEAD_SHNUM);

	    for(int i=0; i<elf_prog_heads_num; i++) {
		long prog_head_off = elf_prog_heads_off + (i * SIZE_ELF_PROG_HEAD);

		int prog_type  = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_TYPE);
		int prog_flags = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_FLAGS);
		long prog_off   = NativeMemory.getLong(elf_addr + prog_head_off + OFF_PROG_HEAD_OFF);
		long prog_vaddr = NativeMemory.getLong(elf_addr + prog_head_off + OFF_PROG_HEAD_VADDR);
		long prog_memsz = NativeMemory.getLong(elf_addr + prog_head_off + OFF_PROG_HEAD_MEMSZ);
		
		long aligned_memsz = (prog_memsz + 0x3FFF) & 0xFFFFFFFFFFFFC000l;

		if(prog_type == ELF_PT_LOAD) {
		    if((prog_flags & 1) == 1) {
			int alias_fd = -1;
			long alias_addr = -1;
			text_size = aligned_memsz;

			// Create shm with executable permissions
			if(libkernel.jitCreateSharedMemory(0, aligned_memsz,
							   PROT_READ | PROT_WRITE | PROT_EXEC,
							   ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}
			if((shm_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			// Map shm into an executable address space
			if((text_addr = libkernel.mmap(0, aligned_memsz,
						       PROT_READ | PROT_EXEC, MAP_SHARED,
						       shm_fd, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Create an shm alias fd with writable permissions
			if(libkernel.jitCreateAliasOfSharedMemory(shm_fd, PROT_READ | PROT_WRITE,
								  ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}
			if((alias_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			// Map shm alias into a writable address space
			if((alias_addr = libkernel.mmap(0, aligned_memsz,
							PROT_READ | PROT_WRITE, MAP_SHARED,
							alias_fd, 0)) == -1) {
			    libkernel.close(alias_fd);
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in data
			NativeMemory.copyMemory(elf_addr + prog_off, alias_addr, prog_memsz);

			// Remove alias shm
			libkernel.munmap(alias_addr, aligned_memsz);
			libkernel.close(alias_fd);
		    } else {
			data_size = aligned_memsz;

			// Allocate memory in our address space
			if((data_addr = libkernel.mmap(0, aligned_memsz,
						       PROT_READ | PROT_WRITE,
						       MAP_ANONYMOUS | MAP_PRIVATE,
						       -1, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in data
			NativeMemory.copyMemory(elf_addr + prog_off, data_addr, prog_memsz);
		    }
		}
	    }

	    for(int i=0; i<elf_symbols_num; i++) {
		long shdr_addr = elf_addr + elf_symbols_off + (i * SIZE_ELF_SYMB_HEAD);
                int sh_type = NativeMemory.getInt(shdr_addr + OFF_SYMB_HEAD_TYPE);

                if (sh_type == SHT_RELA) {
		    // Assume .text is the first section, and that .data section
		    // follows it, hence we subtract text_size from data_addr
		    // so that the rela offsets are correct.
                    relocate_rela(elf_addr, shdr_addr, data_addr - text_size);
                }
	    }

	    if(text_addr != 0) {
		long args[] = new long[6];
		long func = text_addr + elf_entry_point;
		FileOutputStream fos = (FileOutputStream)os;
		int sock_fd = SharedSecrets.getJavaIOFileDescriptorAccess().get(fos.getFD());

		// backup stdout and stderr
		int stdout_fd = libkernel.dup(1);
		int stderr_fd = libkernel.dup(2);

		// redirect stdout and stderr to socket
		libkernel.dup2(sock_fd, 1);
		libkernel.dup2(sock_fd, 2);

		// invoke function
		args[0] = arg_addr;
		args[1] = 0;
		args[2] = 0;
		args[3] = 0;
		args[4] = 0;
		args[5] = 0;
		NativeInvocation.invoke(func, args);

		// resore stdout and stderr
		libkernel.dup2(stdout_fd, 1);
		libkernel.dup2(stderr_fd, 2);
	    } else {
		throw new IOException("Invalid ELF file");
	    }
	    
	} finally {
	    if(elf_addr != 0) {
		NativeMemory.freeMemory(elf_addr);
	    }

	    if(ret_addr != 0) {
		NativeMemory.freeMemory(ret_addr);
	    }

	    if(text_addr != 0) {
		libkernel.munmap(text_addr, text_size);
	    }

	    if(data_addr != 0) {
		libkernel.munmap(data_addr, data_size);
	    }

	    if(shm_fd >= 0) {
		libkernel.close(shm_fd);
	    }
        }
    }
}
