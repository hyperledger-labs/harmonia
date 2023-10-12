package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedByte;

import java.nio.ByteBuffer;

class UnsignedByteElement extends AtomicElement<UnsignedByte> {

	private final UnsignedByte _value;

	UnsignedByteElement(Element parent, Element prev, UnsignedByte ub) {
		super(parent, prev);
		_value = ub;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 1 : 2;
	}

	@Override
	public UnsignedByte getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.UBYTE;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (isElementOfArray()) {
			if (b.hasRemaining()) {
				b.put(_value.byteValue());
				return 1;
			}
		} else {
			if (b.remaining() >= 2) {
				b.put((byte) 0x50);
				b.put(_value.byteValue());
				return 2;
			}
		}
		return 0;
	}
}
