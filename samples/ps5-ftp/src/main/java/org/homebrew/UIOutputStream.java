package org.homebrew;

import java.io.OutputStream;

public class UIOutputStream extends OutputStream {
	
	private static final LoggingUI log = LoggingUI.getInstance();

	@Override
	public void write(int b) {
		write(new byte[]{(byte) b});
	}
	
	@Override
	public void write(byte b[]) {
		log.log(new String(b));
	}
	
	@Override
    public void write(byte b[], int off, int len) {
		log.log(new String(b, off, len));
	}
	
}
