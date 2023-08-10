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
    static final long arg_addr;
    
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
    
    private static void runElf(byte[] elf_bytes, OutputStream os) throws Exception {
	final int OFF_PROG_HEAD_TYPE   = 0x00;
	final int OFF_PROG_HEAD_FLAGS  = 0x04;
	final int OFF_PROG_HEAD_OFF    = 0x08;
	final int OFF_PROG_HEAD_VADDR  = 0x10;
	final int OFF_PROG_HEAD_FILESZ = 0x20;
	final int OFF_PROG_HEAD_MEMSZ  = 0x28;

	final int OFF_ELF_HEAD_ENTRY = 0x18;
	final int OFF_ELF_HEAD_PHOFF = 0x20;
	final int OFF_ELF_HEAD_PHNUM = 0x38;

	final int SIZE_ELF_PROG_HEAD = 0x38;
	final int SIZE_ELF_HEAD      = 0x40;
	
	final int ELF_PT_NULL = 0x00;
	final int ELF_PT_LOAD = 0x01;
	
	final int PROT_NONE  = 0x0;
	final int PROT_READ  = 0x1;
	final int PROT_WRITE = 0x2;
	final int PROT_EXEC  = 0x4;
	
	final int MAP_SHARED    = 0x1;
	final int MAP_PRIVATE   = 0x2;
	final int MAP_FIXED     = 0x10;
	final int MAP_ANONYMOUS = 0x1000;
	
	final long elf_size = SIZE_ELF_HEAD + (SIZE_ELF_PROG_HEAD * 0x10) + 0x200000;
	final long mapping_addr = 0x926100000l;
	final long shadow_addr = 0x920100000l;
	
	long elf_addr = 0;
	long ret_addr = 0;
	
	long data_rw_addr = 0;
	long text_rw_addr = 0;
	long text_rx_addr = 0;
	
	int text_size = 0;
        int data_size = 0;
	
	int text_rw_fd = 0;
	int text_rx_fd = 0;

	PrintStream ps = new PrintStream(os);
	
	if(elf_bytes[0] != (byte)0x7f || elf_bytes[1] != (byte)0x45 ||
	   elf_bytes[2] != (byte)0x4c || elf_bytes[3] != (byte)0x46) {
	    throw new IOException("Invalid ELF file");
	}
	
	try {
	    ret_addr = NativeMemory.allocateMemory(8);
	    elf_addr = NativeMemory.allocateMemory(elf_size);
	    for(int i=0; i<elf_bytes.length; i++) {
		NativeMemory.putByte(elf_addr + i, elf_bytes[i]);
	    }

	    int elf_prog_heads_off = NativeMemory.getInt(elf_addr + OFF_ELF_HEAD_PHOFF);
	    int elf_prog_heads_num = NativeMemory.getInt(elf_addr + OFF_ELF_HEAD_PHNUM) & 0xFFFF;
	    int elf_entry_point    = NativeMemory.getInt(elf_addr + OFF_ELF_HEAD_ENTRY);

	    for(int i=0; i<elf_prog_heads_num; i++) {
		int prog_head_off = elf_prog_heads_off + (i * SIZE_ELF_PROG_HEAD);

		int prog_type  = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_TYPE);
		int prog_flags = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_FLAGS);
		int prog_off   = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_OFF);
		int prog_vaddr = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_VADDR);
		int prog_memsz = NativeMemory.getInt(elf_addr + prog_head_off + OFF_PROG_HEAD_MEMSZ);
		
		int aligned_memsz = (prog_memsz + 0x3FFF) & 0xFFFFC000;

		if(prog_type == ELF_PT_LOAD) {
		    if((prog_flags & 1) == 1) {
			text_size = aligned_memsz;

			// Get exec fd
			if(libkernel.jitCreateSharedMemory(0, aligned_memsz,
							   PROT_READ | PROT_WRITE | PROT_EXEC,
							   ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}
			if((text_rx_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			// Get write fd
			if(libkernel.jitCreateAliasOfSharedMemory(text_rx_fd,
								  PROT_READ | PROT_WRITE,
								  ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}
			if((text_rw_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}
			
			// Map exec segment
			if((text_rx_addr = libkernel.mmap(mapping_addr + prog_vaddr,
							  aligned_memsz,
							  PROT_READ | PROT_EXEC,
							  MAP_FIXED | MAP_SHARED,
							  text_rx_fd, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Map write segment
			if((text_rw_addr = libkernel.mmap(shadow_addr,
							  aligned_memsz,
							  PROT_READ | PROT_WRITE,
							  MAP_FIXED | MAP_SHARED,
							  text_rw_fd, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in segment data
			for(int j=0; j<prog_memsz; j+=8) {
			    long v = NativeMemory.getLong(elf_addr + prog_off + j);
			    NativeMemory.putLong(text_rw_addr + j, v);
			}
		    } else {
			data_size = aligned_memsz;

			// Map write segment
			if((data_rw_addr = libkernel.mmap(mapping_addr + prog_vaddr,
							  aligned_memsz,
							  PROT_READ | PROT_WRITE,
							  MAP_ANONYMOUS | MAP_FIXED | MAP_PRIVATE,
							  -1, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in segment data
			for (int j=0; j<prog_memsz; j+=8) {
			    long v = NativeMemory.getLong(elf_addr + prog_off + j);
			    NativeMemory.putLong(data_rw_addr + j, v);
			}
		    }
		}
	    }
	    
	    if(text_rx_addr != 0) {
		long args[] = new long[6];
		long func = mapping_addr + elf_entry_point;
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

	    if(text_rx_fd != 0) {
		libkernel.close(text_rx_fd);
	    }
	    
	    if(text_rw_fd != 0) {
		libkernel.close(text_rw_fd);
	    }

	    if(text_rw_addr != 0) {
		libkernel.munmap(text_rw_addr, text_size);
	    }

	    if(text_rx_addr != 0) {
		libkernel.munmap(text_rx_addr, text_size);
	    }
	    
	    if(data_rw_addr != 0) {
		libkernel.munmap(data_rw_addr, data_size);
	    }
        }
    }
}
