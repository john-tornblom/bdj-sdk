package org.homebrew.ftp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;

import org.homebrew.PrivilegeEscalation;

public class FtpServer {
	
	private static boolean proxiesCleared = false;
	static InetAddress host = null;

	private final int port;
	private final ArrayList<FtpClient> clients = new ArrayList<>(1);
	private boolean isClosed = false;

	public FtpServer() {
		this(1337);
	}

	public FtpServer(int port) {
		this.port = port;
		clearProxies();
	}
	
	private static void clearProxies() {
		synchronized (FtpServer.class) {
			if (proxiesCleared) {
				return;
			}
			// turn off file proxy nonsense
			try {
				PrivilegeEscalation.openModuleToAllUnnamed("com.oracle.orbis.io.BDJFactory");
				Class<?> cls = Class.forName("com.oracle.orbis.io.BDJFactory");
				Field f = cls.getDeclaredField("instance");
				f.setAccessible(true);
				// now BDJFactory.needProxy always returns false
				f.set(null, null);
				proxiesCleared = true;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public void run() {
		try {
			ServerSocket ss = new ServerSocket(port);
			try {
		        ss.setReuseAddress(true);
				System.out.println("FTP server running on " + FtpUtils.getHostAddress().getHostAddress() + ":" + port);
		        while (true) {
					if (isClosed) {
						if (!ss.isClosed()) {
							ss.close();
						}
						return;
					}
		            try {
						FtpClient client = new FtpClient(ss, ss.accept());
		                clients.add(client);
						client.start();
		            } catch (SocketException e) {
						return;
					} catch (Throwable t) {
						if (!isClosed) {
							//LoggingUI.getInstance().log(t);
							close();
							if (!ss.isClosed()) {
								ss.close();
							}
							return;
						}
		            }
		        }
			} finally {
				if (!ss.isClosed()) {
					ss.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();			
		}
    }

	public void close() {
		isClosed = true;
		for (int i = 0; i < clients.size(); i++) {
			FtpClient client = clients.get(i);
			try {
				client.close();
			} catch (Throwable t) {
				//LoggingUI.getInstance().log(t);
				t.printStackTrace();
			}
		}
		clients.clear();
	}
}
