package org.homebrew.ftp;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;

class PassiveSocket extends ServerSocket implements FtpSocket {

	private Socket socket = null;
	private int port = -1;

	PassiveSocket() throws Throwable {
		super(0);
	}

	@Override
	public void close() {
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
			super.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	int getPort() {
		if (port != -1) {
			return port;
		}
		try {
			Method m = ServerSocket.class.getDeclaredMethod("getImpl");
			m.setAccessible(true);
			SocketImpl impl = (SocketImpl) m.invoke(this);
			m = SocketImpl.class.getDeclaredMethod("getPort");
			m.setAccessible(true);
			Integer port = (Integer) m.invoke(impl);
			this.port = port;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return port;
	}

	byte[] getAddress() {
		return FtpUtils.getHostAddress().getAddress();
	}

	@Override
	public void open() {
	}

	@Override
	public void write(byte[] msg) throws Throwable {
		socket.getOutputStream().write(msg);
	}

	@Override
	public OutputStream getOutputStream() throws Throwable {
		return socket != null ? socket.getOutputStream() : null;
	}

	@Override
	public void openConnection() throws Throwable {
		if (socket != null) {
			try {
				System.out.println("passive socket already connected");
				//passiveSocket.close();
				return;
			} catch (Throwable t) {
				// don't care
			}
		}
		socket = accept();
	}

	@Override
	public void closeConnection() {
		if (socket != null) {
			try {
				socket.close();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			socket = null;
		}
	}
}
