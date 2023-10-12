package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal128;

import java.util.Collection;
import java.util.Collections;

public class Decimal128Type extends AbstractPrimitiveType<Decimal128> {
	private final Decimal128Encoding _decimal128Encoder;

	Decimal128Type(final EncoderImpl encoder, final DecoderImpl decoder) {
		_decimal128Encoder = new Decimal128Encoding(encoder, decoder);
		encoder.register(Decimal128.class, this);
		decoder.register(this);
	}

	public Class<Decimal128> getTypeClass() {
		return Decimal128.class;
	}

	public Decimal128Encoding getEncoding(final Decimal128 val) {
		return _decimal128Encoder;
	}


	public Decimal128Encoding getCanonicalEncoding() {
		return _decimal128Encoder;
	}

	public Collection<Decimal128Encoding> getAllEncodings() {
		return Collections.singleton(_decimal128Encoder);
	}

	private class Decimal128Encoding extends FixedSizePrimitiveTypeEncoding<Decimal128> {

		public Decimal128Encoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 16;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.DECIMAL128;
		}

		public Decimal128Type getType() {
			return Decimal128Type.this;
		}

		public void writeValue(final Decimal128 val) {
			getEncoder().writeRaw(val.getMostSignificantBits());
			getEncoder().writeRaw(val.getLeastSignificantBits());
		}

		public boolean encodesSuperset(final TypeEncoding<Decimal128> encoding) {
			return (getType() == encoding.getType());
		}

		public Decimal128 readValue() {
			long msb = getDecoder().readRawLong();
			long lsb = getDecoder().readRawLong();
			return new Decimal128(msb, lsb);
		}
	}
}
