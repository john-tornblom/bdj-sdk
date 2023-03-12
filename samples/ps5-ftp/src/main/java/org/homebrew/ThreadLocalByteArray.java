package org.homebrew;

public class ThreadLocalByteArray extends ThreadLocal<byte[]> {

	private final int size;

	public ThreadLocalByteArray(int size) {
		this.size = size;
	}

	@Override
	protected byte[] initialValue() {
		return new byte[size];
	}

	public long getAddress() {
		return NativeMemory.addressOf(get());
	}
}
