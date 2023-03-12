package jdk.internal.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOHelper {
	public static long transferTo(InputStream in, OutputStream out) throws IOException {
		if (out == null) {
			throw new NullPointerException("out");
		}
		long transferred = 0;
		byte[] buf = new byte[8192];
		int i;
		while ((i = in.read(buf, 0, 8192)) >= 0) {
		  out.write(buf, 0, i);
		  transferred += i;
		}
		return transferred;
	  }
}
