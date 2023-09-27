package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class LongElement extends AtomicElement<Long> {

	private final long _value;

	LongElement(Element parent, Element prev, long l) {
		super(parent, prev);
		_value = l;
	}

	@Override
	public int size() {
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();

			if (parent.constructorType() == ArrayElement.SMALL) {
				if (-128l <= _value && _value <= 127l) {
					return 1;
				} else {
					parent.setConstructorType(ArrayElement.LARGE);
				}
			}

			return 8;

		} else {
			return (-128l <= _value && _value <= 127l) ? 2 : 9;
		}

	}

	@Override
	public Long getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.LONG;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (size > b.remaining()) {
			return 0;
		}
		switch (size) {
			case 2:
				b.put((byte) 0x55);
			case 1:
				b.put((byte) _value);
				break;
			case 9:
				b.put((byte) 0x81);
			case 8:
				b.putLong(_value);

		}
		return size;
	}
}
