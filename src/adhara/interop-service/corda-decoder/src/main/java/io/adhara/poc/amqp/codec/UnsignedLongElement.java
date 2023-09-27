package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedLong;

import java.nio.ByteBuffer;

class UnsignedLongElement extends AtomicElement<UnsignedLong> {

	private final UnsignedLong _value;

	UnsignedLongElement(Element parent, Element prev, UnsignedLong ul) {
		super(parent, prev);
		_value = ul;
	}

	@Override
	public int size() {
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();
			if (parent.constructorType() == ArrayElement.TINY) {
				if (_value.longValue() == 0l) {
					return 0;
				} else {
					parent.setConstructorType(ArrayElement.SMALL);
				}
			}

			if (parent.constructorType() == ArrayElement.SMALL) {
				if (0l <= _value.longValue() && _value.longValue() <= 255l) {
					return 1;
				} else {
					parent.setConstructorType(ArrayElement.LARGE);
				}
			}

			return 8;

		} else {
			return 0l == _value.longValue() ? 1 : (1l <= _value.longValue() && _value.longValue() <= 255l) ? 2 : 9;
		}

	}

	@Override
	public UnsignedLong getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.ULONG;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (size > b.remaining()) {
			return 0;
		}
		switch (size) {
			case 1:
				if (isElementOfArray()) {
					b.put((byte) _value.longValue());
				} else {
					b.put((byte) 0x44);
				}
				break;
			case 2:
				b.put((byte) 0x53);
				b.put((byte) _value.longValue());
				break;
			case 9:
				b.put((byte) 0x80);
			case 8:
				b.putLong(_value.longValue());

		}

		return size;
	}
}
