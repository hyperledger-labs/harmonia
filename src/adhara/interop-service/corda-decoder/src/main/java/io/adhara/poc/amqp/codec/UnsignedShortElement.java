package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedShort;

import java.nio.ByteBuffer;

class UnsignedShortElement extends AtomicElement<UnsignedShort> {

	private final UnsignedShort _value;

	UnsignedShortElement(Element parent, Element prev, UnsignedShort ub) {
		super(parent, prev);
		_value = ub;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 2 : 3;
	}

	@Override
	public UnsignedShort getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.USHORT;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (isElementOfArray()) {
			if (b.remaining() >= 2) {
				b.putShort(_value.shortValue());
				return 2;
			}
		} else {
			if (b.remaining() >= 3) {
				b.put((byte) 0x60);
				b.putShort(_value.shortValue());
				return 3;
			}
		}
		return 0;
	}
}
