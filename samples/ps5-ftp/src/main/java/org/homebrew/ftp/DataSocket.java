package org.homebrew.ftp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

class DataSocket extends Socket implements FtpSocket {

	private final String host;
	private final int port;

	DataSocket(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	@Override
	public void open() throws IOException {
		connect(new InetSocketAddress(host, port));
	}

	@Override
	public void write(byte[] msg) throws Throwable {
		getOutputStream().write(msg);
	}

	@Override
	public void close() {
		try {
			super.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


}
