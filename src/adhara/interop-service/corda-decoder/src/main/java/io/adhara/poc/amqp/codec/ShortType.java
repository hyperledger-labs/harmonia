package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;

public class ShortType extends AbstractPrimitiveType<Short> {
	private final ShortEncoding _shortEncoding;

	ShortType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_shortEncoding = new ShortEncoding(encoder, decoder);
		encoder.register(Short.class, this);
		decoder.register(this);
	}

	public Class<Short> getTypeClass() {
		return Short.class;
	}

	public ShortEncoding getEncoding(final Short val) {
		return _shortEncoding;
	}

	public void write(short s) {
		_shortEncoding.write(s);
	}

	public ShortEncoding getCanonicalEncoding() {
		return _shortEncoding;
	}

	public Collection<ShortEncoding> getAllEncodings() {
		return Collections.singleton(_shortEncoding);
	}

	public class ShortEncoding extends FixedSizePrimitiveTypeEncoding<Short> {

		public ShortEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 2;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.SHORT;
		}

		public ShortType getType() {
			return ShortType.this;
		}

		public void writeValue(final Short val) {
			getEncoder().writeRaw(val);
		}

		public void writeValue(final short val) {
			getEncoder().writeRaw(val);
		}


		public void write(final short s) {
			writeConstructor();
			getEncoder().writeRaw(s);
		}

		public boolean encodesSuperset(final TypeEncoding<Short> encoding) {
			return (getType() == encoding.getType());
		}

		public Short readValue() {
			return readPrimitiveValue();
		}

		public short readPrimitiveValue() {
			return getDecoder().readRawShort();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}

	}
}
