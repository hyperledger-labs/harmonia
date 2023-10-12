package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal32;

import java.nio.ByteBuffer;

class Decimal32Element extends AtomicElement<Decimal32> {

	private final Decimal32 _value;

	Decimal32Element(Element parent, Element prev, Decimal32 d) {
		super(parent, prev);
		_value = d;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 4 : 5;
	}

	@Override
	public Decimal32 getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.DECIMAL32;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 5) {
				b.put((byte) 0x74);
			}
			b.putInt(_value.getBits());
			return size;
		} else {
			return 0;
		}
	}
}
