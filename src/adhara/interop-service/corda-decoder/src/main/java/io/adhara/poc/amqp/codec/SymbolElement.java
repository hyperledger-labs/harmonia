package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Symbol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class SymbolElement extends AtomicElement<Symbol> {

	private static final Charset ASCII = StandardCharsets.US_ASCII;
	private final Symbol _value;

	SymbolElement(Element parent, Element prev, Symbol s) {
		super(parent, prev);
		_value = s;
	}

	@Override
	public int size() {
		final int length = _value.length();

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
	public Symbol getValue() {
		return _value;
	}

	@Override
	public Data.DataType getDataType() {
		return Data.DataType.SYMBOL;
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
				b.put((byte) _value.length());
			} else {
				b.putInt(_value.length());
			}
		} else if (_value.length() <= 255) {
			b.put((byte) 0xa3);
			b.put((byte) _value.length());
		} else {
			b.put((byte) 0xb3);
			b.put((byte) _value.length());
		}
		b.put(_value.toString().getBytes(ASCII));
		return size;

	}
}
