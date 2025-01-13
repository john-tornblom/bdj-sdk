package org.homebrew;

public class CString {
	private final byte[] str;
	private String jstr;

	public CString(String value) {
		jstr = value;
		byte[] data = value.getBytes();
		str = new byte[data.length + 1];
		System.arraycopy(data, 0, str, 0, data.length);
	}

	public static String fromBytes(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			if (data[i] == 0) {
				return new String(data, 0, i);
			}
		}
		return "";
	}

	public static String fromAddress(long addr) {
		return addr != 0 ? NativeMemory.getString(addr) : "";
	}

	public long getAddress() {
		return NativeMemory.addressOf(str);
	}

	@Override
	public String toString() {
		if (jstr == null) {
			jstr = new String(str, 0, str.length - 1);
		}
		return jstr;
	}
}
