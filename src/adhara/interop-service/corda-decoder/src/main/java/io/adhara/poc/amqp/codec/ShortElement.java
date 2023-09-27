package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class ShortElement extends AtomicElement<Short> {

	private final short _value;

	ShortElement(Element parent, Element prev, short s) {
		super(parent, prev);
		_value = s;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 2 : 3;
	}

	@Override
	public Short getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.SHORT;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (isElementOfArray()) {
			if (b.remaining() >= 2) {
				b.putShort(_value);
				return 2;
			}
		} else {
			if (b.remaining() >= 3) {
				b.put((byte) 0x61);
				b.putShort(_value);
				return 3;
			}
		}
		return 0;
	}
}
