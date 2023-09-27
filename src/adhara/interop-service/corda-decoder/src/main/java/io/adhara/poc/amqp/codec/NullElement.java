package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

class NullElement extends AtomicElement<Void> {
	NullElement(Element parent, Element prev) {
		super(parent, prev);
	}

	@Override
	public int size() {
		return isElementOfArray() ? 0 : 1;
	}

	@Override
	public Void getValue() {
		return null;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.NULL;
	}

	@Override
	public int encode(ByteBuffer b) {
		if (b.hasRemaining() && !isElementOfArray()) {
			b.put((byte) 0x40);
			return 1;
		}
		return 0;
	}
}
