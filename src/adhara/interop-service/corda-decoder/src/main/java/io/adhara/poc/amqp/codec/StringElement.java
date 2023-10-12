package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class StringElement extends AtomicElement<String> {

	private static final Charset UTF_8 = StandardCharsets.UTF_8;
	private final String _value;

	StringElement(Element parent, Element prev, String s) {
		super(parent, prev);
		_value = s;
	}

	@Override
	public int size() {
		final int length = _value.getBytes(UTF_8).length;

		return size(length);
	}

	private int size(int length) {
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();

			if (parent.constructorType() == ArrayElement.SMALL) {
				if (length > 255) {
					parent.setConstructorType(ArrayElement.LARGE);
					return 4 + length;
				} else {
					return 1 + length;
				}
			} else {
				return 4 + length;
			}
		} else {
			if (length > 255) {
				return 5 + length;
			} else {
				return 2 + length;
			}
		}
	}

	@Override
	public String getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.STRING;
	}

	@Override
	public int encode(ByteBuffer b) {
		final byte[] bytes = _value.getBytes(UTF_8);
		final int length = bytes.length;

		int size = size(length);
		if (b.remaining() < size) {
			return 0;
		}
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();

			if (parent.constructorType() == ArrayElement.SMALL) {
				b.put((byte) length);
			} else {
				b.putInt(length);
			}
		} else if (length <= 255) {
			b.put((byte) 0xa1);
			b.put((byte) length);
		} else {
			b.put((byte) 0xb1);
			b.putInt(length);
		}
		b.put(bytes);
		return size;

	}
}
