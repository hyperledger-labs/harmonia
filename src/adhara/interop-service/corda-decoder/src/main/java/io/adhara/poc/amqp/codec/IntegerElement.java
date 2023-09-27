package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class IntegerElement extends AtomicElement<Integer> {

	private final int _value;

	IntegerElement(Element parent, Element prev, int i) {
		super(parent, prev);
		_value = i;
	}

	@Override
	public int size() {
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();
			if (parent.constructorType() == ArrayElement.SMALL) {
				if (-128 <= _value && _value <= 127) {
					return 1;
				} else {
					parent.setConstructorType(ArrayElement.LARGE);
					return 4;
				}
			} else {
				return 4;
			}
		} else {
			return (-128 <= _value && _value <= 127) ? 2 : 5;
		}

	}

	@Override
	public Integer getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.INT;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (size <= b.remaining()) {
			switch (size) {
				case 2:
					b.put((byte) 0x54);
				case 1:
					b.put((byte) _value);
					break;

				case 5:
					b.put((byte) 0x71);
				case 4:
					b.putInt(_value);

			}

			return size;
		}
		return 0;
	}
}
