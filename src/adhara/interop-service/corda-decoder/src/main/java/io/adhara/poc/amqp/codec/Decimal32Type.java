package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Decimal32;

import java.util.Collection;
import java.util.Collections;

public class Decimal32Type extends AbstractPrimitiveType<Decimal32> {
	private final Decimal32Encoding _decimal32Encoder;

	Decimal32Type(final EncoderImpl encoder, final DecoderImpl decoder) {
		_decimal32Encoder = new Decimal32Encoding(encoder, decoder);
		encoder.register(Decimal32.class, this);
		decoder.register(this);
	}

	public Class<Decimal32> getTypeClass() {
		return Decimal32.class;
	}

	public Decimal32Encoding getEncoding(final Decimal32 val) {
		return _decimal32Encoder;
	}


	public Decimal32Encoding getCanonicalEncoding() {
		return _decimal32Encoder;
	}

	public Collection<Decimal32Encoding> getAllEncodings() {
		return Collections.singleton(_decimal32Encoder);
	}

	private class Decimal32Encoding extends FixedSizePrimitiveTypeEncoding<Decimal32> {

		public Decimal32Encoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 4;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.DECIMAL32;
		}

		public Decimal32Type getType() {
			return Decimal32Type.this;
		}

		public void writeValue(final Decimal32 val) {
			getEncoder().writeRaw(val.getBits());
		}

		public boolean encodesSuperset(final TypeEncoding<Decimal32> encoding) {
			return (getType() == encoding.getType());
		}

		public Decimal32 readValue() {
			return new Decimal32(getDecoder().readRawInt());
		}
	}
}
