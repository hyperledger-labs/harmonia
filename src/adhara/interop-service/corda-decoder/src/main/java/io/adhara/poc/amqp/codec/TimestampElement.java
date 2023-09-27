package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;
import java.util.Date;

class TimestampElement extends AtomicElement<Date> {

	private final Date _value;

	TimestampElement(Element parent, Element prev, Date d) {
		super(parent, prev);
		_value = d;
	}

	@Override
	public int size() {
		return isElementOfArray() ? 8 : 9;
	}

	@Override
	public Date getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.TIMESTAMP;
	}

	@Override
	public int encode(ByteBuffer b) {
		int size = size();
		if (size > b.remaining()) {
			return 0;
		}
		if (size == 9) {
			b.put((byte) 0x83);
		}
		b.putLong(_value.getTime());

		return size;
	}
}
