package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedByte;

import java.util.Collection;
import java.util.Collections;

public class UnsignedByteType extends AbstractPrimitiveType<UnsignedByte> {
	private final UnsignedByteEncoding _unsignedByteEncoding;

	UnsignedByteType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_unsignedByteEncoding = new UnsignedByteEncoding(encoder, decoder);
		encoder.register(UnsignedByte.class, this);
		decoder.register(this);
	}

	public Class<UnsignedByte> getTypeClass() {
		return UnsignedByte.class;
	}

	public UnsignedByteEncoding getEncoding(final UnsignedByte val) {
		return _unsignedByteEncoding;
	}

	public void fastWrite(EncoderImpl encoder, UnsignedByte value) {
		encoder.writeRaw(EncodingCodes.UBYTE);
		encoder.writeRaw(value.byteValue());
	}

	public UnsignedByteEncoding getCanonicalEncoding() {
		return _unsignedByteEncoding;
	}

	public Collection<UnsignedByteEncoding> getAllEncodings() {
		return Collections.singleton(_unsignedByteEncoding);
	}

	public class UnsignedByteEncoding extends FixedSizePrimitiveTypeEncoding<UnsignedByte> {

		public UnsignedByteEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 1;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.UBYTE;
		}

		public UnsignedByteType getType() {
			return UnsignedByteType.this;
		}

		public void writeValue(final UnsignedByte val) {
			getEncoder().writeRaw(val.byteValue());
		}

		public boolean encodesSuperset(final TypeEncoding<UnsignedByte> encoding) {
			return (getType() == encoding.getType());
		}

		public UnsignedByte readValue() {
			return UnsignedByte.valueOf(getDecoder().readRawByte());
		}
	}
}
