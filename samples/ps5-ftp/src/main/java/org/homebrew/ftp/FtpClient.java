package org.homebrew.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.attribute.FileTime;
import jdk.internal.helper.IOHelper;

import org.homebrew.LoggingUI;
import org.homebrew.NativeMemory;
import org.homebrew.libkernel;

public class FtpClient extends Thread {

	private static final int CMD_NOOP = 0x504f4f4e;
	private static final int CMD_USER = 0x52455355;
	private static final int CMD_PASS = 0x53534150;
	private static final int CMD_QUIT = 0x54495551;
	private static final int CMD_SYST = 0x54535953;
	private static final int CMD_PASV = 0x56534150;
	private static final int CMD_PORT = 0x54524f50;
	private static final int CMD_LIST = 0x5453494c;
	private static final int CMD_PWD  = 0x00445750;
	private static final int CMD_CWD  = 0x00445743;
	private static final int CMD_TYPE = 0x45505954;
	private static final int CMD_CDUP = 0x50554443;
	private static final int CMD_RETR = 0x52544552;
	private static final int CMD_STOR = 0x524f5453;
	private static final int CMD_DELE = 0x454c4544;
	private static final int CMD_RMD  = 0x00444d52;
	private static final int CMD_MKD  = 0x00444b4d;
	private static final int CMD_RNFR = 0x52464e52;
	private static final int CMD_RNTO = 0x4f544e52;
	private static final int CMD_SIZE = 0x455a4953;
	//private static final int CMD_REST = 0x54534552;
	private static final int CMD_FEAT = 0x54414546;
	//private static final int CMD_APPE = 0x45505041;
	private static final int CMD_KILL = 0x4C4C494B;

	private static final byte[] READY_MESSAGE = "220 FTPS5 Server ready.\r\n".getBytes();
	private static final byte[] NOOP_RESPONSE = "200 No operation ;)\r\n".getBytes();
	private static final byte[] USER_RESPONSE = "331 Username OK, need password b0ss.\r\n".getBytes();
	private static final byte[] PASS_RESPONSE = "230 User logged in!\r\n".getBytes();
	private static final byte[] QUIT_RESPONSE = "221 Goodbye senpai :'(\r\n".getBytes();
	private static final byte[] SYST_RESPONSE = "215 UNIX Type: L8\r\n".getBytes();
	private static final byte[] ERR_RESPONSE  = "500 Syntax error, command unrecognized.\r\n".getBytes();
	private static final byte[] PORT_RESPONSE = "200 PORT command successful!\r\n".getBytes();
	private static final byte[] INVALID_DIRECTORY = "550 Invalid directory.\r\n".getBytes();
	private static final byte[] TRANSFER_RESPONSE = "150 Opening ASCII mode data transfer for LIST.\r\n".getBytes();
	private static final byte[] TRANSFER_COMPLETE_RESPONSE = "226 Transfer complete.\r\n".getBytes();
	private static final byte[] REQUEST_COMPLETE_RESPONSE = "250 Requested file action okay, completed.\r\n".getBytes();
	private static final byte[] OK_RESPONSE = "200 Okay\r\n".getBytes();
	private static final byte[] BAD_PARAMETERS = "504 Error: bad parameters?\r\n".getBytes();
	private static final byte[] IMAGE_OPEN = "150 Opening Image mode data transfer.\r\n".getBytes();
	private static final byte[] TRANSFER_COMPLETE = "226 Transfer completed.\r\n".getBytes();
	private static final byte[] FILE_NOT_FOUND = "550 File not found.\r\n".getBytes();
	private static final byte[] PERMISSION_DENIED = "534 Permission denied.\r\n".getBytes();
	private static final byte[] INVALID_OPERATION = "534 Operation not supported by device.\r\n".getBytes();
	private static final byte[] UNSUPPORTED_CMD = "502 Sorry, command not implemented. :(\r\n".getBytes();

	private final ServerSocket ss;
	private final Socket s;
	private FtpSocket dataSocket;
	private boolean isClosed = false;
	private byte[] buf = new byte[512];
	private String cmdArgs;
	private FtpFile cwd = new FtpFile("/");

	public FtpClient(ServerSocket ss, Socket s) {
		this.ss = ss;
		this.s = s;
		System.out.println("Client "+s.getInetAddress().getHostAddress()+" connected");
	}

	@Override
	public void run() {
		try {
			serve();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void serve() throws Throwable {
		try {
			sendCtrlMessage(READY_MESSAGE);
			handleFtp();
		} catch (SocketException e) {
			// abort
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			close();
		}
	}

	private void handleFtp() throws Throwable {
		while (true) {
			if (isClosed) {
				return;
			}
			if (s.isClosed()) {
				close();
				return;
			}
			String msg = readMsg();
			if (msg.equals("")) {
				close();
				return;
			}
			System.out.println("received: "+msg);
			int n = msg.indexOf(' ');
			if (n != -1) {
				String cmd = msg.substring(0, n);
				String args = msg.substring(n + 1);
				handleCommand(cmd, args);
			} else {
				handleCommand(msg, "");
			}
		}
	}

	private void handleCommand(String cmd, String args) throws Throwable {
		int value = commandValue(cmd);
		OutputStream os = s.getOutputStream();
		switch (value) {
			case CMD_NOOP:
				os.write(NOOP_RESPONSE);
				return;
			case CMD_USER:
				os.write(USER_RESPONSE);
				return;
			case CMD_PASS:
				os.write(PASS_RESPONSE);
				return;
			case CMD_QUIT:
				os.write(QUIT_RESPONSE);
				close();
				return;
			case CMD_SYST:
				os.write(SYST_RESPONSE);
				return;
			case CMD_PASV:
				handlePasv();
				return;
			case CMD_PORT:
				handlePort(args);
				return;
			case CMD_LIST:
				handleList(args);
				return;
			case CMD_PWD:
				handlePwd();
				return;
			case CMD_CWD:
				handleCwd(args);
				return;
			case CMD_TYPE:
				handleType(args);
				return;
			case CMD_CDUP:
				handleCdup();
				return;
			case CMD_RETR:
				handleRetr(args);
				return;
			case CMD_STOR:
				handleStor();
				return;
			case CMD_DELE:
				handleDele();
				return;
			case CMD_RMD:
				handleRmd();
				return;
			case CMD_MKD:
				handleMkd();
				return;
			case CMD_RNFR:
				handleRnfr();
				return;
			case CMD_RNTO:
				handleRnTo();
			case CMD_SIZE:
				handleSize(args);
				return;
			//case CMD_REST:
			case CMD_FEAT:
				handleFeat();
				return;
			case CMD_KILL:
				sendCtrlMessage(OK_RESPONSE);
				close();
				ss.close();
				return;
			//case CMD_APPE:
			default:
				System.out.println("Unsupported cmd: " + cmd);
				sendCtrlMessage(UNSUPPORTED_CMD);
				return;
		}
	}

	private void handleFeat() throws Throwable {
		sendCtrlMessage("211-extensions\r\n");
		sendCtrlMessage("KILL\r\n");
		sendCtrlMessage("211 end\r\n");
	}

	private void handleSize(String path) throws Throwable {
		try {
			FtpFile fp = new FtpFile(path);
			sendCtrlMessage("213 " + fp.length() + "\r\n");
		} catch (Throwable t) {
			sendCtrlMessage(FILE_NOT_FOUND);
		}
	}

	private void handleStor() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private void handleDele() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private void handleRmd() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private void handleMkd() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private void handleRnfr() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private void handleRnTo() throws Throwable {
		sendCtrlMessage(PERMISSION_DENIED);
	}

	private FtpFile getFile(String path) {
		if (path == null) {
			return FtpFile.ROOT;
		}
		if (path.equals("")) {
			return cwd;
		}
		if (path.startsWith("/")) {
			return new FtpFile(path);
		}
		return new FtpFile(cwd, path);
	}

	private void handleRetr(String args) throws Throwable {
		sendFile(getFile(args));
	}

	private void sendFile(FtpFile fp) throws Throwable {
		System.out.println("Sending "+fp.getAbsolutePath());
		//if (!fp.exists()) {
		//	sendCtrlMessage(FILE_NOT_FOUND);
		//	return;
		//}
		InputStream is = null;
		try {
			is = fp.getInputStream();
			openDataConnection();
			OutputStream os = getDataOutputStream();
			sendCtrlMessage(IMAGE_OPEN);
			IOHelper.transferTo(is, os);
			sendCtrlMessage(TRANSFER_COMPLETE);
			closeDataConnection();
		} catch (IOException e) {
			sendCtrlMessage(INVALID_OPERATION);
		} catch (RuntimeException e) {
			sendCtrlMessage(PERMISSION_DENIED);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	private void handleType(String args) throws Throwable {
		if (args.length() > 0) {
			int type = args.charAt(0);
			if (type == 'A' || type == 'I') {
				sendCtrlMessage(OK_RESPONSE);
				return;
			}
		}
		sendCtrlMessage(BAD_PARAMETERS);
	}

	private void handlePasv() throws Throwable {
		if (dataSocket != null) {
			dataSocket.close();
		}
		PassiveSocket sock = new PassiveSocket();
		dataSocket = sock;
		int p = sock.getLocalPort();
		byte[] addr = sock.getAddress();
		String port = Integer.toString((p >> 8) & 0xff) + ","+Integer.toString(p & 0xff);
		String ip = "(" + (addr[0] & 0xff) + "," + (addr[1] & 0xff) + "," + (addr[2] & 0xff) + ","  + (addr[3] & 0xff) + "," + port + ")";
		String reply = "227 Entering Passive Mode " + ip + "\r\n";
		s.getOutputStream().write(reply.getBytes());
	}

	private void handlePort(String args) throws Throwable {
		if (cmdArgs == null) {
			sendCtrlMessage(ERR_RESPONSE);
			return;
		}
		int lowIndex = args.lastIndexOf(',');
		int hiIndex = args.lastIndexOf(args, lowIndex - 1);
		int port = parseInt(args, lowIndex + 1, hiIndex) << 8 + parseInt(args, hiIndex + 1, -1);
		String host = args.substring(0, lowIndex).replace(',', '.');
		dataSocket = new DataSocket(host, port);
		sendCtrlMessage(PORT_RESPONSE);
	}

	private static String getRelativePath(FtpFile parent, FtpFile child) {
		String path = child.getAbsolutePath();
		String base = parent.getAbsolutePath();
		if (path.startsWith(base)) {
			int index = 0;
			if (path.length() > base.length() + 1) {
				if (path.charAt(base.length()) == '/') {
					index = base.length() + 1;
				} else {
					index = base.length();
				}
				return path.substring(index);
			} else {
				return ".";
			}
		}
		throw new IllegalArgumentException("The child path is not contained within the parent path");
	}

	private void sendList(FtpFile fp) throws Throwable {

		if (fp.isDirectory()) {
			sendDataMessage(fmtFile(fp, "."));
		} else {
			sendDataMessage(fmtFile(fp, getRelativePath(cwd, fp)));
			return;
		}

		try {
			for (FtpFile child : fp.listFiles()) {
				sendDataMessage(fmtFile(child, getRelativePath(fp, child)));
			}
		} catch (RuntimeException e) {
			sendCtrlMessage(PERMISSION_DENIED);
		}

	}

	private void handleList(String args) throws Throwable {
		FtpFile fp = getFile(args);
		if (!fp.isDirectory()) {
			System.out.println(fp.toString() + " doesn't exist");
			sendCtrlMessage(INVALID_DIRECTORY);
			return;
		}
		sendCtrlMessage(TRANSFER_RESPONSE);
		openDataConnection();
		try {
			sendList(fp);
		} finally {
			closeDataConnection();
		}
		sendCtrlMessage(TRANSFER_COMPLETE_RESPONSE);
	}

	private void handlePwd() throws Throwable {
		sendCtrlMessage("257 \"" + cwd +"\" is the current directory.\r\n");
	}

	private void handleCwd(String args) throws Throwable {
		FtpFile fp = getFile(args);
		Stat st = fp.getStat();
		if (st.isLink()) {
			fp = fp.readLink();
		}
		if (cd(fp)) {
			sendCtrlMessage(REQUEST_COMPLETE_RESPONSE);
		} else {
			if (fp.exists() && !fp.canRead()) {
				sendCtrlMessage(PERMISSION_DENIED);
			} else {
				sendCtrlMessage(INVALID_DIRECTORY);
			}
		}
	}

	private boolean cd(FtpFile fp) {
		if (fp.isDirectory() && fp.canRead()) {
			cwd = fp;
			return true;
		}
		return false;
	}

	private void handleCdup() throws Throwable {
		if (cwd.isRoot()) {
			sendCtrlMessage(INVALID_DIRECTORY);
		}
		if (cd(cwd.getParentFile())) {
			sendCtrlMessage(OK_RESPONSE);
		} else {
			sendCtrlMessage(INVALID_DIRECTORY);
		}
	}

	private static byte file_type_char(int m) {
		return  (((m)&0170000) == 0060000) ? (byte)'b' : (((m)&0170000) == 0020000) ? (byte)'c' : (((m)&0170000) == 0100000) ? (byte)'-' : (((m)&0170000) == 0040000) ? (byte)'d' : (((m)&0170000) == 0010000) ? (byte)'p' : (((m)&0170000) == 0140000) ? (byte)'s' : (((m)&0170000) == 0120000) ? (byte)'l' : (byte)' ';
	}

	private static String getMode(Stat st) {
		int file_mode = st.st_mode;
		boolean isDir = st.isDir();
		byte xTrue = isDir ? (byte)'s' : (byte)'x';
		byte xFalse = isDir ? (byte)'S' : (byte)'-';
		return new String(
			new byte[]{
				file_type_char(file_mode),
				(file_mode & 0400) != 0 ? (byte)'r' : (byte)'-',
				(file_mode & 0200) != 0 ? (byte)'w' : (byte)'-',
				(file_mode & 0100) != 0 ? xTrue : xFalse,
				(file_mode & 0040) != 0 ? (byte)'r' : (byte)'-',
				(file_mode & 0020) != 0 ? (byte)'w' : (byte)'-',
				(file_mode & 0010) != 0 ? xTrue : xFalse,
				(file_mode & 0004) != 0 ? (byte)'r' : (byte)'-',
				(file_mode & 0002) != 0 ? (byte)'w' : (byte)'-',
				(file_mode & 0001) != 0 ? xTrue : xFalse
			}
		);
	}

	private static String fmtFile(FtpFile fp, String name) throws Throwable {
		// mode 1 ps5 ps5 size month day time name
		Stat st = fp.getStat();
		String mode = getMode(st);
		String length = Long.toString(st.st_size);

		// 2022-09-25T16:44:55.358Z
		String month = getMonth(st.st_ctim);
		String day = getDay(st.st_ctim);
		//String year = getYear(st.st_ctim);
		String time = getTime(st.st_ctim);
		return mode + " 1 ps5 ps5 " + length + ' ' + month + ' ' + day + ' ' + time + ' ' + name + "\r\n";
	}

	private static String getTime(FileTime ft) {
		// 2022-09-25T16:44:55.358Z
		String time = ft.toString();
		int n = time.indexOf('T');
		return time.substring(n + 1, n + 6);
	}

	private static String getDay(FileTime ft) {
		// 2022-09-25T16:44:55.358Z
		String time = ft.toString();
		int n = time.indexOf('T');
		int m = time.lastIndexOf('-', n);
		return time.substring(m + 1, n);
	}

	@SuppressWarnings("unused")
	private static String getYear(FileTime ft) {
		// 2022-09-25T16:44:55.358Z
		String time = ft.toString();
		int n = time.indexOf('-');
		return time.substring(0, n);
	}

	private static String getMonth(FileTime ft) {
		// 2022-09-25T16:44:55.358Z
		String time = ft.toString();
		int n = time.indexOf('-');
		n = parseInt(time, n + 1, n + 3);
		switch (n) {
			case 1:
				return "Jan";
			case 2:
				return "Feb";
			case 3:
				return "Mar";
			case 4:
				return "Apr";
			case 5:
				return "May";
			case 6:
				return "Jun";
			case 7:
				return "Jul";
			case 8:
				return "Aug";
			case 9:
				return "Sep";
			case 10:
				return "Oct";
			case 11:
				return "Nov";
			case 12:
				return "Dec";
			default:
				throw new IllegalArgumentException("Invalid month: "+n);
		}
	}

	private void sendCtrlMessage(String msg) throws Throwable {
		sendCtrlMessage(msg.getBytes());
	}

	private void sendCtrlMessage(byte[] msg) throws Throwable {
		//System.out.println("sending ctrl msg: "+new String(msg));
		s.getOutputStream().write(msg);
	}

	private void openDataConnection() throws Throwable {
		dataSocket.openConnection();
	}

	private void sendDataMessage(String msg) throws Throwable {
		//System.out.println("sending data msg: "+msg);
		sendDataMessage(msg.getBytes());
	}

	private void sendDataMessage(byte[] msg) throws Throwable {
		dataSocket.write(msg);
	}

	private OutputStream getDataOutputStream() throws Throwable {
		return dataSocket.getOutputStream();
	}

	private void closeDataConnection() throws Throwable {
		dataSocket.closeConnection();
	}

	private static int parseInt(String s, int begin, int end) {
		int value = 0;
		if (end == -1) {
			end = s.length();
		}
		while (begin < end) {
			value = (value * 10) + (s.charAt(begin++) - '0');
		}
		return value;
	}

	private static int commandValue(String seq) {
		byte[] data = seq.getBytes();
		return NativeMemory.getInt(NativeMemory.addressOf(data));
	}

	private String readMsg() throws Throwable {
		String msg = "";
		while (true) {
			int n = s.getInputStream().read(buf);
			if (n == -1) {
				break;
			}
			msg += new String(buf, 0, n);
			if (n < buf.length) {
				break;
			}
		}
		if (msg.endsWith("\r\n")) {
			msg = msg.substring(0, msg.length() - 2);
		}
		return msg;
	}

	private static void close(Socket s) {
		if (s == null) {
			return;
		}
		try {
			s.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void close() {
		if (isClosed) {
			return;
		}
		try {
			isClosed = true;
			close(s);
			if (dataSocket != null) {
				dataSocket.close();
				dataSocket = null;
			}
		} catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		}
	}

	protected static String toHex(byte b) {
		String s = Integer.toHexString(b & 0xff);
		if (s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}

	public static String toHexString(byte[] bytes) {
		StringBuffer buf = new StringBuffer(bytes.length * 4);
		for (int i = 0; i < bytes.length; i += 16) {
			for (int j = 0; j < 16 && j + i < bytes.length; j++) {
				buf.append(toHex(bytes[i + j]))
					.append(' ');
			}
			buf.append('\n');
		}
		return buf.toString();
	}
}
