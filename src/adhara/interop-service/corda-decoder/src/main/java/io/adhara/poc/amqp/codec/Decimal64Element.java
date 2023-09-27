package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal64;

import java.nio.ByteBuffer;

class Decimal64Element extends AtomicElement<Decimal64> {

	private final Decimal64 _value;

	Decimal64Element(Element parent, Element prev, Decimal64 d) {
		super(parent, prev);
		_value = d;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 8 : 9;
	}

	@Override
	public Decimal64 getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.DECIMAL64;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 9) {
				b.put((byte) 0x84);
			}
			b.putLong(_value.getBits());
			return size;
		} else {
			return 0;
		}
	}
}
