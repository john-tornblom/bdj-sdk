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
    private static final int OFF_PHDR_TYPE   = 0x00;
    private static final int OFF_PHDR_FLAGS  = 0x04;
    private static final int OFF_PHDR_OFF    = 0x08;
    private static final int OFF_PHDR_VADDR  = 0x10;
    private static final int OFF_PHDR_MEMSZ  = 0x28;

    private static final int OFF_EHDR_ENTRY = 0x18;
    private static final int OFF_EHDR_PHOFF = 0x20;
    private static final int OFF_EHDR_PHNUM = 0x38;

    private static final int SIZE_PHDR = 0x38;
    private static final int SIZE_EHDR = 0x40;

    private static final int PT_LOAD = 0x01;

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
	byte[] chunk = new byte[0x4000];
	
        while (true) {
            int length = s.getInputStream().read(chunk, 0, chunk.length);
            if (length < 0) {
                break;
            } else {
                buf.write(chunk, 0, length);
            }
        }
	
        return buf.toByteArray();
    }
    
    private static void runElf(byte[] elf_bytes, OutputStream os) throws Exception {
	long elf_addr = 0;
	long ret_addr = 0;
	
	long text_addr = 0;
	long data_addr = 0;

	int text_size = 0;
        int data_size = 0;
	
	int shm_fd = 0;

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

	    long e_entry  = NativeMemory.getLong(elf_addr + OFF_EHDR_ENTRY);
	    long e_phoff  = NativeMemory.getLong(elf_addr + OFF_EHDR_PHOFF);
	    short e_phnum = NativeMemory.getShort(elf_addr + OFF_EHDR_PHNUM);

	    for(int i=0; i<e_phnum; i++) {
		long phdr_addr = elf_addr + e_phoff + (i * SIZE_PHDR);

		int p_type  = NativeMemory.getInt(phdr_addr + OFF_PHDR_TYPE);
		int p_flags = NativeMemory.getInt(phdr_addr + OFF_PHDR_FLAGS);
		long p_off   = NativeMemory.getLong(phdr_addr + OFF_PHDR_OFF);
		long p_vaddr = NativeMemory.getLong(phdr_addr + OFF_PHDR_VADDR);
		long p_memsz = NativeMemory.getLong(phdr_addr + OFF_PHDR_MEMSZ);
		int aligned_memsz = (int)(p_memsz + 0x3FFF) & 0xFFFFC000;

		if(p_type == PT_LOAD) {
		    if((p_flags & 1) == 1) {
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
			if((text_addr = libkernel.mmap(p_vaddr, aligned_memsz,
						       PROT_READ | PROT_EXEC,
						       MAP_FIXED | MAP_SHARED,
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
			NativeMemory.copyMemory(elf_addr + p_off, alias_addr, p_memsz);

			// Remove alias shm
			libkernel.munmap(alias_addr, aligned_memsz);
			libkernel.close(alias_fd);
		    } else {
			data_size = aligned_memsz;

			// Map write segment
			if((data_addr = libkernel.mmap(p_vaddr,
						       aligned_memsz,
						       PROT_READ | PROT_WRITE,
						       MAP_ANONYMOUS | MAP_FIXED | MAP_PRIVATE,
						       -1, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in segment data
			NativeMemory.copyMemory(elf_addr + p_off, data_addr, p_memsz);
		    }
		}
	    }
	    
	    if(text_addr != 0) {
		long args[] = new long[6];
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
		NativeInvocation.invoke(e_entry, args);

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
	    if(shm_fd != 0) {
		libkernel.close(shm_fd);
	    }
        }
    }
}
