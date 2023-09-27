package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class FloatElement extends AtomicElement<Float> {

	private final float _value;

	FloatElement(Element parent, Element prev, float f) {
		super(parent, prev);
		_value = f;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 4 : 5;
	}

	@Override
	public Float getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.FLOAT;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 5) {
				b.put((byte) 0x72);
			}
			b.putFloat(_value);
			return size;
		} else {
			return 0;
		}
	}
}
