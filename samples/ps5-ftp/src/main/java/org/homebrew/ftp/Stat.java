package org.homebrew.ftp;

import java.io.File;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.homebrew.CString;
import org.homebrew.NativeMemory;
import org.homebrew.PrivilegeEscalation;
import org.homebrew.libkernel;

import jdk.internal.helper.MathHelper;

public class Stat {

	public final String path;
	public final FileTime st_ctim;
	public final long st_size;
	public final int st_mode;
	public final int st_blksize;

	private Stat(String path, byte[] buf) {
		this.path = path;
		long addr = NativeMemory.addressOf(buf);
		st_mode = NativeMemory.getShort(addr + 8) & 0xffff;
		st_blksize = NativeMemory.getInt(addr + 0x58);
		st_size = NativeMemory.getLong(addr + 0x48);
		long tv_sec = NativeMemory.getLong(addr + 0x38);
		long tv_nsec = NativeMemory.getLong(addr + 0x40);
		st_ctim = toFileTime(tv_sec, tv_nsec);
	}

	public static Stat stat(String path) {
		return stat(new CString(path));
	}

	public static Stat stat(File fp) {
		CString path = new CString(fp.getAbsolutePath());
		return stat(path);
	}

	public static Stat stat(CString path) {
		byte[] buf = new byte[0x78];
		libkernel.stat(path.getAddress(), NativeMemory.addressOf(buf));
		return new Stat(path.toString(), buf);
	}

	private static FileTime toFileTime(long tv_sec, long tv_nsec) {
        if (tv_nsec == 0) {
            return FileTime.from(tv_sec, TimeUnit.SECONDS);
        } else {
            try {
                long nanos = MathHelper.addExact(tv_nsec, MathHelper.multiplyExact(tv_sec, 1_000_000_000L));
                return FileTime.from(nanos, TimeUnit.NANOSECONDS);
            } catch (ArithmeticException ignore) {
                // truncate to microseconds if nanoseconds overflow
                long micro = tv_sec*1_000_000L + tv_nsec/1_000L;
                return FileTime.from(micro, TimeUnit.MICROSECONDS);
            }
        }
    }

	public boolean isDir() {
		return (((st_mode)&0170000) == 0040000);
	}

	public boolean isLink() {
		return ((st_mode)&0170000) == 0120000;
	}

	public boolean canRead() {
		return ((st_mode)&04) != 0;
	}

	public boolean canWrite() {
		return ((st_mode)&02) != 0;
	}

	public boolean canExecute() {
		return isDir() ? false : ((st_mode)&01) != 0;
	}

	@Override
	public String toString() {
		return "stat: " + path + '\t' + st_mode + '\t' + st_size + '\t' + st_blksize + '\t' + st_ctim;
	}
}
