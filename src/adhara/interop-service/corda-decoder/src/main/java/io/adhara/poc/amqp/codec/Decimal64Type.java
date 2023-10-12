package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal64;

import java.util.Collection;
import java.util.Collections;

public class Decimal64Type extends AbstractPrimitiveType<Decimal64> {
	private final Decimal64Encoding _decimal64Encoder;

	Decimal64Type(final EncoderImpl encoder, final DecoderImpl decoder) {
		_decimal64Encoder = new Decimal64Encoding(encoder, decoder);
		encoder.register(Decimal64.class, this);
		decoder.register(this);
	}

	public Class<Decimal64> getTypeClass() {
		return Decimal64.class;
	}

	public Decimal64Encoding getEncoding(final Decimal64 val) {
		return _decimal64Encoder;
	}


	public Decimal64Encoding getCanonicalEncoding() {
		return _decimal64Encoder;
	}

	public Collection<Decimal64Encoding> getAllEncodings() {
		return Collections.singleton(_decimal64Encoder);
	}

	private class Decimal64Encoding extends FixedSizePrimitiveTypeEncoding<Decimal64> {

		public Decimal64Encoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 8;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.DECIMAL64;
		}

		public Decimal64Type getType() {
			return Decimal64Type.this;
		}

		public void writeValue(final Decimal64 val) {
			getEncoder().writeRaw(val.getBits());
		}

		public boolean encodesSuperset(final TypeEncoding<Decimal64> encoding) {
			return (getType() == encoding.getType());
		}

		public Decimal64 readValue() {
			return new Decimal64(getDecoder().readRawLong());
		}
	}
}
