package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class BooleanElement extends AtomicElement<Boolean> {
	private final boolean _value;

	public BooleanElement(Element parent, Element current, boolean b) {
		super(parent, current);
		_value = b;
	}

	@Override
	public int size() {
		// in non-array parent then there is a single byte encoding, in an array there is a 1-byte encoding but no
		// constructor
		return 1;
	}

	@Override
	public Boolean getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.BOOL;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (b.hasRemaining()) {
			if (isElementOfArray()) {
				b.put(_value ? (byte) 1 : (byte) 0);
			} else {
				b.put(_value ? (byte) 0x41 : (byte) 0x42);
			}
			return 1;
		}
		return 0;
	}

}
