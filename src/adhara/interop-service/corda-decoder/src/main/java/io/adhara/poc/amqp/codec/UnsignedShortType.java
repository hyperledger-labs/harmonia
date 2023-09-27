package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedShort;

import java.util.Collection;
import java.util.Collections;

public class UnsignedShortType extends AbstractPrimitiveType<UnsignedShort> {
	private final UnsignedShortEncoding _unsignedShortEncoder;

	UnsignedShortType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_unsignedShortEncoder = new UnsignedShortEncoding(encoder, decoder);
		encoder.register(UnsignedShort.class, this);
		decoder.register(this);
	}

	public Class<UnsignedShort> getTypeClass() {
		return UnsignedShort.class;
	}

	public UnsignedShortEncoding getEncoding(final UnsignedShort val) {
		return _unsignedShortEncoder;
	}

	public void fastWrite(EncoderImpl encoder, UnsignedShort value) {
		encoder.writeRaw(EncodingCodes.USHORT);
		encoder.writeRaw(value.shortValue());
	}

	public UnsignedShortEncoding getCanonicalEncoding() {
		return _unsignedShortEncoder;
	}

	public Collection<UnsignedShortEncoding> getAllEncodings() {
		return Collections.singleton(_unsignedShortEncoder);
	}

	private class UnsignedShortEncoding extends FixedSizePrimitiveTypeEncoding<UnsignedShort> {

		public UnsignedShortEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 2;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.USHORT;
		}

		public UnsignedShortType getType() {
			return UnsignedShortType.this;
		}

		public void writeValue(final UnsignedShort val) {
			getEncoder().writeRaw(val.shortValue());
		}

		public boolean encodesSuperset(final TypeEncoding<UnsignedShort> encoding) {
			return (getType() == encoding.getType());
		}

		public UnsignedShort readValue() {
			return UnsignedShort.valueOf(getDecoder().readRawShort());
		}
	}
}
