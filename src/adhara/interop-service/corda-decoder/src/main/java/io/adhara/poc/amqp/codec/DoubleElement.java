package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class DoubleElement extends AtomicElement<Double> {

	private final double _value;

	DoubleElement(Element parent, Element prev, double d) {
		super(parent, prev);
		_value = d;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 8 : 9;
	}

	@Override
	public Double getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.DOUBLE;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 9) {
				b.put((byte) 0x82);
			}
			b.putDouble(_value);
			return size;
		} else {
			return 0;
		}
	}
}
