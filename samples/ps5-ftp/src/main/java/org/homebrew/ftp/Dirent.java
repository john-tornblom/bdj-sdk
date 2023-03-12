package org.homebrew.ftp;

import java.util.Iterator;

import org.homebrew.NativeMemory;
import org.homebrew.libkernel;

public class Dirent {

	private int fileno;
	private short reclen;
	private byte type;
	private byte namelen;
	private String name;


	Dirent() {
	}

	public Dirent(byte[] buf) {
		reload(buf);
	}

	void next(byte[] buf) {
		long addr = NativeMemory.addressOf(buf);
		reload(addr + d_reclen());
	}

	void reload(long addr) {
		fileno = NativeMemory.getInt(addr);
		reclen = NativeMemory.getShort(addr + 4);
		type = NativeMemory.getByte(addr + 6);
		namelen = NativeMemory.getByte(addr + 7);
		if (namelen > 0) {
			name = NativeMemory.getString(addr + 8, namelen & 0xff);
			if (name.equals("\0")) {
				name = "";
			}
		} else {
			name = "";
		}
	}

	void reload(byte[] buf) {
		long addr = NativeMemory.addressOf(buf);
		reload(addr);
	}

	public int d_fileno() {
		return fileno;
	}

	public int d_reclen() {
		return reclen & 0xffff;
	}

	public int d_type() {
		return type & 0xff;
	}

	public int d_namlen() {
		return namelen & 0xff;
	}

	public String d_name() {
		return name;
	}

	public static Iterator<Dirent> getdents(int fd, Stat st) {
		byte[] buf = new byte[st.st_blksize];
		libkernel.getdents(fd, NativeMemory.addressOf(buf), buf.length);
		return new DirentArray(buf);
	}

	private static class DirentArray implements Iterator<Dirent> {

		private final byte[] buf;
		private Dirent it;
		private int pos = 0;

		DirentArray(byte[] buf) {
			this.buf = buf;
			it = new Dirent(buf);
		}

		@Override
		public boolean hasNext() {
			return pos < buf.length && it != null && it.d_reclen() != 0;
		}

		@Override
		public Dirent next() {
			pos += it.d_reclen();
			it.reload(NativeMemory.addressOf(buf) + pos);
			return it;
		}

	}
}
