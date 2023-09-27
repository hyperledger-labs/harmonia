package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class ByteElement extends AtomicElement<Byte> {

	private final byte _value;

	ByteElement(Element parent, Element prev, byte b) {
		super(parent, prev);
		_value = b;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 1 : 2;
	}

	@Override
	public Byte getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.BYTE;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (isElementOfArray()) {
			if (b.hasRemaining()) {
				b.put(_value);
				return 1;
			}
		} else {
			if (b.remaining() >= 2) {
				b.put((byte) 0x51);
				b.put(_value);
				return 2;
			}
		}
		return 0;
	}
}
