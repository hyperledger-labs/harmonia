package io.adhara.poc.amqp.codec;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

public class BigIntegerType extends AbstractPrimitiveType<BigInteger> {

	public interface BigIntegerEncoding extends PrimitiveTypeEncoding<BigInteger> {
		void write(BigInteger l);

		void writeValue(BigInteger l);

		BigInteger readPrimitiveValue();
	}

	private static final BigInteger BIG_BYTE_MIN = BigInteger.valueOf(Byte.MIN_VALUE);
	private static final BigInteger BIG_BYTE_MAX = BigInteger.valueOf(Byte.MAX_VALUE);
	private static final BigInteger BIG_LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
	private static final BigInteger BIG_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

	private final BigIntegerEncoding _BigIntegerEncoding;
	private final BigIntegerEncoding _smallBigIntegerEncoding;

	BigIntegerType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_BigIntegerEncoding = new AllBigIntegerEncoding(encoder, decoder);
		_smallBigIntegerEncoding = new SmallBigIntegerEncoding(encoder, decoder);
		encoder.register(BigInteger.class, this);
	}

	public Class<BigInteger> getTypeClass() {
		return BigInteger.class;
	}

	public BigIntegerEncoding getEncoding(final BigInteger l) {
		return (l.compareTo(BIG_BYTE_MIN) >= 0 && l.compareTo(BIG_BYTE_MAX) <= 0) ? _smallBigIntegerEncoding : _BigIntegerEncoding;
	}


	public BigIntegerEncoding getCanonicalEncoding() {
		return _BigIntegerEncoding;
	}

	public Collection<BigIntegerEncoding> getAllEncodings() {
		return Arrays.asList(_smallBigIntegerEncoding, _BigIntegerEncoding);
	}

	private long longValueExact(final BigInteger val) {
		if (val.compareTo(BIG_LONG_MIN) < 0 || val.compareTo(BIG_LONG_MAX) > 0) {
			throw new ArithmeticException("cannot encode BigInteger not representable as long");
		}
		return val.longValue();
	}

	private class AllBigIntegerEncoding extends FixedSizePrimitiveTypeEncoding<BigInteger> implements BigIntegerEncoding {

		public AllBigIntegerEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 8;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.LONG;
		}

		public BigIntegerType getType() {
			return BigIntegerType.this;
		}

		public void writeValue(final BigInteger val) {
			getEncoder().writeRaw(longValueExact(val));
		}

		public void write(final BigInteger l) {
			writeConstructor();
			getEncoder().writeRaw(longValueExact(l));

		}

		public boolean encodesSuperset(final TypeEncoding<BigInteger> encoding) {
			return (getType() == encoding.getType());
		}

		public BigInteger readValue() {
			return readPrimitiveValue();
		}

		public BigInteger readPrimitiveValue() {
			return BigInteger.valueOf(getDecoder().readLong());
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}

	private class SmallBigIntegerEncoding extends FixedSizePrimitiveTypeEncoding<BigInteger> implements BigIntegerEncoding {
		public SmallBigIntegerEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.SMALLLONG;
		}

		@Override
		protected int getFixedSize() {
			return 1;
		}

		public void write(final BigInteger l) {
			writeConstructor();
			getEncoder().writeRaw(l.byteValue());
		}

		public BigInteger readPrimitiveValue() {
			return BigInteger.valueOf(getDecoder().readRawByte());
		}

		public BigIntegerType getType() {
			return BigIntegerType.this;
		}

		public void writeValue(final BigInteger val) {
			getEncoder().writeRaw(val.byteValue());
		}

		public boolean encodesSuperset(final TypeEncoding<BigInteger> encoder) {
			return encoder == this;
		}

		public BigInteger readValue() {
			return readPrimitiveValue();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}
}
