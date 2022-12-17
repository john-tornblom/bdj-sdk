package org.homebrew;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ElfLoadingServer {

    public static void spawn(int port) {
        new Thread(new Runnable() {
		public void run() {
		    try {
			ElfLoadingServer.run(port);
		    } catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		    }
		}
	    }).start();

	try {
	    Thread.sleep(3000);
	} catch (Throwable t) {
	}
    }
    
    public static void run(int port) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        ss.setReuseAddress(true);

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
	
	long elf_addr = 0;
	long ret_addr = 0;
	
	long data_rw_addr = 0;
	long text_rw_addr = 0;
	long text_rx_addr = 0;
	
	int text_size = 0;
        int data_size = 0;
	
	int main_fd = 0;
	int alias_fd = 0;
	
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

			int prot = PROT_READ | PROT_WRITE | PROT_EXEC;
			if(libkernel.jitCreateSharedMemory(0, aligned_memsz, prot,
							   ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}

			if((main_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			prot = PROT_READ | PROT_WRITE;
			if(libkernel.jitCreateAliasOfSharedMemory(main_fd, prot,
								  ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}
			
			if((alias_fd = NativeMemory.getInt(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			prot = PROT_READ | PROT_EXEC;
			long addr = 0xc00000000l;
			if((text_rx_addr = libkernel.mmap(addr, aligned_memsz,
							  prot, MAP_SHARED,
							  main_fd, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			prot = PROT_READ | PROT_WRITE;
			if(libkernel.jitMapSharedMemory(alias_fd, prot,
							ret_addr) != 0) {
			    throw new Exception(libcInternal.strerror());
			}

			if((text_rw_addr=NativeMemory.getLong(ret_addr)) == 0) {
			    throw new Exception(libcInternal.strerror());
			}

			for(int j=0; j<prog_memsz; j+=8) {
			    long v = NativeMemory.getLong(elf_addr +
							  prog_off + j);
			    NativeMemory.putLong(text_rw_addr + j, v);
			}

		    } else {
			// Regular data segment
			data_size = aligned_memsz;
			long addr = text_rx_addr + prog_vaddr;
			int prot = PROT_READ | PROT_WRITE;
			int flags = MAP_ANONYMOUS | MAP_FIXED | MAP_PRIVATE;
			if((data_rw_addr = libkernel.mmap(addr, data_size, prot,
							  flags, -1, 0)) == -1) {
			    throw new Exception(libcInternal.strerror());
			}

			// Copy in segment data
			for (int j=0; j<prog_memsz; j+=8) {
			    long v = NativeMemory.getLong(elf_addr +
							  prog_off + j);
			    NativeMemory.putLong(data_rw_addr + j, v);
			}
		    }
		}
	    }
	    
	    if(text_rx_addr != 0) {
		long args[] = new long[6];
		long func = text_rx_addr + elf_entry_point;

		args[0] = NativeLibrary.load(0x2001).findEntry("sceKernelDlsym");
		args[1] = 0;
		args[2] = 0;
		args[3] = 0;
		args[4] = 0;
		args[5] = 0;
		
		long rc = NativeInvocation.invoke(func, args);
		new PrintStream(os).println("exit code: " + rc);
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

	    if(main_fd != 0) {
		libkernel.close(main_fd);
	    }

	    if(alias_fd != 0) {
		libkernel.close(alias_fd);
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
