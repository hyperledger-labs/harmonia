package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Binary;

import java.nio.ByteBuffer;

class BinaryElement extends AtomicElement<Binary> {

	private final Binary _value;

	BinaryElement(Element parent, Element prev, Binary b) {
		super(parent, prev);
		byte[] data = new byte[b.getLength()];
		System.arraycopy(b.getArray(), b.getArrayOffset(), data, 0, b.getLength());
		_value = new Binary(data);
	}

	@Override
	public int size() {
		final int length = _value.getLength();

		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();

			if (parent.constructorType() == ArrayElement.SMALL) {
				if (length > 255) {
					parent.setConstructorType(ArrayElement.LARGE);
					return 4 + length;
				} else {
					return 1 + length;
				}
			} else {
				return 4 + length;
			}
		} else {
			if (length > 255) {
				return 5 + length;
			} else {
				return 2 + length;
			}
		}
	}

	@Override
	public Binary getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.BINARY;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() < size) {
			return 0;
		}
		if (isElementOfArray()) {
			final ArrayElement parent = (ArrayElement) parent();

			if (parent.constructorType() == ArrayElement.SMALL) {
				b.put((byte) _value.getLength());
			} else {
				b.putInt(_value.getLength());
			}
		} else if (_value.getLength() <= 255) {
			b.put((byte) 0xa0);
			b.put((byte) _value.getLength());
		} else {
			b.put((byte) 0xb0);
			b.putInt(_value.getLength());
		}
		b.put(_value.getArray(), _value.getArrayOffset(), _value.getLength());
		return size;

	}
}
