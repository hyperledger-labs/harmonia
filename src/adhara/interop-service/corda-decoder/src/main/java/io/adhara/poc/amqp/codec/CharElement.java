package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class CharElement extends AtomicElement<Integer> {

	private final int _value;

	CharElement(Element parent, Element prev, int i) {
		super(parent, prev);
		_value = i;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 4 : 5;
	}

	@Override
	public Integer getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.CHAR;
	}

	@Override
	public int encode(ByteBuffer b) {
		final int size = size();
		if (size <= b.remaining()) {
			if (size == 5) {
				b.put((byte) 0x73);
			}
			b.putInt(_value);
		}
		return 0;
	}
}
