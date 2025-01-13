package org.homebrew.ftp;

import java.io.OutputStream;

interface FtpSocket {

	void open() throws Throwable;
	default void openConnection() throws Throwable {
		open();
	}
	void close();
	default void closeConnection() {
		close();
	}
	void write(byte[] msg) throws Throwable;
	default void write(String msg) throws Throwable {
		write(msg.getBytes());
	}
	OutputStream getOutputStream() throws Throwable;
}
