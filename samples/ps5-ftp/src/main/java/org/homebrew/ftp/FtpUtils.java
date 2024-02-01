package org.homebrew.ftp;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class FtpUtils {

	private static InetAddress hostAddress;

	private static InetAddress getPs5IpAddress() {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			return socket.getLocalAddress();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
		return null;
	}

	public static InetAddress getHostAddress() {
		if (hostAddress == null) {
			hostAddress = getPs5IpAddress();
		}
		return hostAddress;
	}

}
