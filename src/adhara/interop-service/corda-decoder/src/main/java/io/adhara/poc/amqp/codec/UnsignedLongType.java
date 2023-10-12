package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedLong;

import java.util.Arrays;
import java.util.Collection;

public class UnsignedLongType extends AbstractPrimitiveType<UnsignedLong> {
	public interface UnsignedLongEncoding extends PrimitiveTypeEncoding<UnsignedLong> {

	}

	private final UnsignedLongEncoding _unsignedLongEncoding;
	private final UnsignedLongEncoding _smallUnsignedLongEncoding;
	private final UnsignedLongEncoding _zeroUnsignedLongEncoding;


	UnsignedLongType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_unsignedLongEncoding = new AllUnsignedLongEncoding(encoder, decoder);
		_smallUnsignedLongEncoding = new SmallUnsignedLongEncoding(encoder, decoder);
		_zeroUnsignedLongEncoding = new ZeroUnsignedLongEncoding(encoder, decoder);
		encoder.register(UnsignedLong.class, this);
		decoder.register(this);
	}

	public Class<UnsignedLong> getTypeClass() {
		return UnsignedLong.class;
	}

	public UnsignedLongEncoding getEncoding(final UnsignedLong val) {
		long l = val.longValue();
		return l == 0L
			? _zeroUnsignedLongEncoding
			: (l >= 0 && l <= 255L) ? _smallUnsignedLongEncoding : _unsignedLongEncoding;
	}

	public void fastWrite(EncoderImpl encoder, UnsignedLong value) {
		long longValue = value.longValue();
		if (longValue == 0) {
			encoder.writeRaw(EncodingCodes.ULONG0);
		} else if (longValue > 0 && longValue <= 255) {
			encoder.writeRaw(EncodingCodes.SMALLULONG);
			encoder.writeRaw((byte) longValue);
		} else {
			encoder.writeRaw(EncodingCodes.ULONG);
			encoder.writeRaw(longValue);
		}
	}

	public UnsignedLongEncoding getCanonicalEncoding() {
		return _unsignedLongEncoding;
	}

	public Collection<UnsignedLongEncoding> getAllEncodings() {
		return Arrays.asList(_zeroUnsignedLongEncoding, _smallUnsignedLongEncoding, _unsignedLongEncoding);
	}


	private class AllUnsignedLongEncoding
		extends FixedSizePrimitiveTypeEncoding<UnsignedLong>
		implements UnsignedLongEncoding {

		public AllUnsignedLongEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 8;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.ULONG;
		}

		public UnsignedLongType getType() {
			return UnsignedLongType.this;
		}

		public void writeValue(final UnsignedLong val) {
			getEncoder().writeRaw(val.longValue());
		}


		public boolean encodesSuperset(final TypeEncoding<UnsignedLong> encoding) {
			return (getType() == encoding.getType());
		}

		public UnsignedLong readValue() {
			return UnsignedLong.valueOf(getDecoder().readRawLong());
		}
	}

	private class SmallUnsignedLongEncoding
		extends FixedSizePrimitiveTypeEncoding<UnsignedLong>
		implements UnsignedLongEncoding {
		public SmallUnsignedLongEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.SMALLULONG;
		}

		@Override
		protected int getFixedSize() {
			return 1;
		}


		public UnsignedLongType getType() {
			return UnsignedLongType.this;
		}

		public void writeValue(final UnsignedLong val) {
			getEncoder().writeRaw((byte) val.longValue());
		}

		public boolean encodesSuperset(final TypeEncoding<UnsignedLong> encoder) {
			return encoder == this || encoder instanceof ZeroUnsignedLongEncoding;
		}

		public UnsignedLong readValue() {
			return UnsignedLong.valueOf(((long) getDecoder().readRawByte()) & 0xffl);
		}
	}


	private class ZeroUnsignedLongEncoding
		extends FixedSizePrimitiveTypeEncoding<UnsignedLong>
		implements UnsignedLongEncoding {
		public ZeroUnsignedLongEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.ULONG0;
		}

		@Override
		protected int getFixedSize() {
			return 0;
		}


		public UnsignedLongType getType() {
			return UnsignedLongType.this;
		}

		public void writeValue(final UnsignedLong val) {
		}

		public boolean encodesSuperset(final TypeEncoding<UnsignedLong> encoder) {
			return encoder == this;
		}

		public UnsignedLong readValue() {
			return UnsignedLong.ZERO;
		}
	}
}
