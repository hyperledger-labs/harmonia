package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DroppingWritableBuffer implements WritableBuffer {
	private int _pos = 0;

	@Override
	public boolean hasRemaining() {
		return true;
	}

	@Override
	public void put(byte b) {
		_pos += 1;
	}

	@Override
	public void putFloat(float f) {
		_pos += 4;
	}

	@Override
	public void putDouble(double d) {
		_pos += 8;
	}

	@Override
	public void put(byte[] src, int offset, int length) {
		_pos += length;
	}

	@Override
	public void putShort(short s) {
		_pos += 2;
	}

	@Override
	public void putInt(int i) {
		_pos += 4;
	}

	@Override
	public void putLong(long l) {
		_pos += 8;
	}

	@Override
	public int remaining() {
		return Integer.MAX_VALUE - _pos;
	}

	@Override
	public int position() {
		return _pos;
	}

	@Override
	public void position(int position) {
		_pos = position;
	}

	@Override
	public void put(ByteBuffer payload) {
		_pos += payload.remaining();
		payload.position(payload.limit());
	}

	@Override
	public int limit() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void put(ReadableBuffer payload) {
		_pos += payload.remaining();
		payload.position(payload.limit());
	}

	@Override
	public void put(String value) {
		_pos += value.getBytes(StandardCharsets.UTF_8).length;
	}
}
