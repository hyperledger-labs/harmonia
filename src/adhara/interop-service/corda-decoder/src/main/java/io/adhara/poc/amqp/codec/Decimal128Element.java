package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal128;

import java.nio.ByteBuffer;

class Decimal128Element extends AtomicElement<Decimal128> {

	private final Decimal128 _value;

	Decimal128Element(Element parent, Element prev, Decimal128 d) {
		super(parent, prev);
		_value = d;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 16 : 17;
	}

	@Override
	public Decimal128 getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.DECIMAL128;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 17) {
				b.put((byte) 0x94);
			}
			b.putLong(_value.getMostSignificantBits());
			b.putLong(_value.getLeastSignificantBits());
			return size;
		} else {
			return 0;
		}
	}
}
