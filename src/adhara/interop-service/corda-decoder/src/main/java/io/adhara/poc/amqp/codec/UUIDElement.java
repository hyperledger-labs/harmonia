package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

class UUIDElement extends AtomicElement<UUID> {

	private final UUID _value;

	UUIDElement(Element parent, Element prev, UUID u) {
		super(parent, prev);
		_value = u;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 16 : 17;
	}

	@Override
	public UUID getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.UUID;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (b.remaining() >= size) {
			if (size == 17) {
				b.put((byte) 0x98);
			}
			b.putLong(_value.getMostSignificantBits());
			b.putLong(_value.getLeastSignificantBits());
			return size;
		} else {
			return 0;
		}
	}
}
