package org.homebrew.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.homebrew.CString;
import org.homebrew.NativeMemory;
import org.homebrew.libkernel;

public class FtpFile extends File {

	private static final int F_OK = 0;
	private static final long FD_OFFSET = NativeMemory.objectFieldOffset(FileDescriptor.class, "fd");
	public static final FtpFile ROOT = new FtpFile("/");

	private final CString path;
	private Stat st = null;

	public FtpFile(String pathname) {
		super(pathname);
		path = new CString(getPath());
	}

	private FtpFile(CString pathname) {
		super(pathname.toString());
		path = pathname;
	}

	public FtpFile(String parent, String child) {
		super(parent, child);
		path = new CString(getPath());
	}

	public FtpFile(File parent, String child) {
		super(parent, child);
		path = new CString(getPath());
	}

	public FtpFile(URI uri) {
		super(uri);
		path = new CString(getPath());
	}

	private FtpFile(File file) {
		this(file.getAbsolutePath());
	}

	public Stat getStat() {
		if (st == null) {
			st = Stat.stat(path);
		}
		return st;
	}

	public boolean isRoot() {
		return this == ROOT || getAbsolutePath().equals("/");
	}

	@Override
	public FtpFile getAbsoluteFile() {
		return new FtpFile(super.getAbsoluteFile());
	}

	@Override
	public FtpFile getParentFile() {
		File fp = super.getParentFile();
		return fp != null ? new FtpFile(fp) : ROOT;
	}

	private static FtpFile[] convert(File[] files) {
		if (files == null) {
			// Java api is stupid for returning null
			return new FtpFile[0];
		}
		FtpFile[] res = new FtpFile[files.length];
		for (int i = 0; i < files.length; i++) {
			res[i] = new FtpFile(files[i]);
		}
		return res;
	}

	private String getFullPath(String path) {
		if (path.startsWith("/")) {
			return path;
		}
		String cwd = getAbsolutePath();
		if (cwd.endsWith("/")) {
			return cwd + path;
		}
		return cwd + '/' + path;
	}

	private List<String> getChildren(Predicate<String> filter) {
		Stat st = Stat.stat(path);

		if (!st.isDir()) {
			return emptyList();
		}

		int fd = -1;
		List<String> files = new ArrayList<>();
		try {
			fd = libkernel.open(path, 0);
			Iterator<Dirent> dirents = Dirent.getdents(fd, st);
			while (dirents.hasNext()) {
				Dirent dir = dirents.next();
				if (dir.d_name().equals("")) {
					continue;
				}
				String path = getFullPath(dir.d_name());
				if (filter == null || filter.test(path)) {
					files.add(path);
				}
			}
		} finally {
			libkernel.close(fd);
		}
		return files;
	}

	private static FtpFile[] convert(List<String> files) {
		FtpFile[] res = new FtpFile[files.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = new FtpFile(files.get(i));
		}
		return res;
	}

	@Override
	public String[] list() {
		List<String> files = getChildren(null);
		return files.toArray(new String[files.size()]);
	}

	@Override
	public FtpFile[] listFiles() {
		String[] files = list();
		FtpFile[] res = new FtpFile[files.length];
		for (int i = 0; i < files.length; i++) {
			res[i] = new FtpFile(files[i]);
		}
		return res;
	}

	@Override
	public FtpFile[] listFiles(FilenameFilter filter) {
		List<String> files;
		if (filter == null) {
			files = getChildren(null);
		} else {
			files = getChildren((s) -> filter.accept(this, s));
		}
		return convert(files);
	}

	@Override
	public FtpFile[] listFiles(FileFilter filter) {
		List<String> files;
		if (filter == null) {
			files = getChildren(null);
		} else {
			files = getChildren((s) -> filter.accept(new FtpFile(s)));
		}
		return convert(files);
	}

	public static FtpFile[] listRoots() {
		return convert(File.listRoots());
	}

	@Override
	public boolean exists() {
		return libkernel.access(path, F_OK) == 0 || isDirectory() || canRead();
	}

	@Override
	public boolean canExecute() {
		Stat st = getStat();
		return st.canExecute();
	}

	@Override
	public boolean canRead() {
		Stat st = getStat();
		return st.canRead();
	}

	@Override
	public boolean canWrite() {
		Stat st = getStat();
		return st.canWrite();
	}

	@Override
	public long length() {
		Stat st = getStat();
		return st.st_size;
	}

	@Override
	public boolean isDirectory() {
		Stat st = getStat();
		return st.isDir();
	}

	/**
	 * Gets a valid FileInputStream for this file
	 * @return the InputStream
	 */
	public FileInputStream getInputStream() {
		// paths are only checked when opened
		// so setting the FileDescriptor manually should work
		int fd = libkernel.open(path, 0);
		FileDescriptor desc = new FileDescriptor();
		long addr = NativeMemory.addressOf(desc) + FD_OFFSET;
		NativeMemory.setInt(addr, fd);
		return new FileInputStream(desc);
	}

	public BufferedReader getBufferedReader() {
		InputStreamReader reader = new InputStreamReader(getInputStream());
		return new BufferedReader(reader);
	}

	public FtpFile readLink() {
		byte[] linkbuf = new byte[255];
		libkernel.readlink(path.getAddress(), NativeMemory.addressOf(linkbuf), linkbuf.length);
		return new FtpFile(CString.fromBytes(linkbuf));
	}
	
	@SuppressWarnings("unchecked")
	private static <T> List<T> emptyList() {
		return (List<T>) java.util.Collections.EMPTY_LIST;
	}
}
