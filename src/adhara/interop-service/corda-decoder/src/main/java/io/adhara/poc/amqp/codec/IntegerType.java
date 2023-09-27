package io.adhara.poc.amqp.codec;

import java.util.Arrays;
import java.util.Collection;

public class IntegerType extends AbstractPrimitiveType<Integer> {

	public interface IntegerEncoding extends PrimitiveTypeEncoding<Integer> {
		void write(int i);

		void writeValue(int i);

		int readPrimitiveValue();
	}

	private final IntegerEncoding _integerEncoding;
	private final IntegerEncoding _smallIntegerEncoding;

	IntegerType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_integerEncoding = new AllIntegerEncoding(encoder, decoder);
		_smallIntegerEncoding = new SmallIntegerEncoding(encoder, decoder);
		encoder.register(Integer.class, this);
		decoder.register(this);
	}

	public Class<Integer> getTypeClass() {
		return Integer.class;
	}

	public IntegerEncoding getEncoding(final Integer val) {
		return getEncoding(val.intValue());
	}

	public IntegerEncoding getEncoding(final int i) {

		return (i >= -128 && i <= 127) ? _smallIntegerEncoding : _integerEncoding;
	}


	public IntegerEncoding getCanonicalEncoding() {
		return _integerEncoding;
	}

	public Collection<IntegerEncoding> getAllEncodings() {
		return Arrays.asList(_integerEncoding, _smallIntegerEncoding);
	}

	public void write(int i) {
		if (i >= -128 && i <= 127) {
			_smallIntegerEncoding.write(i);
		} else {
			_integerEncoding.write(i);
		}
	}

	private class AllIntegerEncoding extends FixedSizePrimitiveTypeEncoding<Integer> implements IntegerEncoding {

		public AllIntegerEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 4;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.INT;
		}

		public IntegerType getType() {
			return IntegerType.this;
		}

		public void writeValue(final Integer val) {
			getEncoder().writeRaw(val.intValue());
		}

		public void write(final int i) {
			writeConstructor();
			getEncoder().writeRaw(i);

		}

		public void writeValue(final int i) {
			getEncoder().writeRaw(i);
		}

		public int readPrimitiveValue() {
			return getDecoder().readRawInt();
		}

		public boolean encodesSuperset(final TypeEncoding<Integer> encoding) {
			return (getType() == encoding.getType());
		}

		public Integer readValue() {
			return readPrimitiveValue();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}

	private class SmallIntegerEncoding extends FixedSizePrimitiveTypeEncoding<Integer> implements IntegerEncoding {
		public SmallIntegerEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.SMALLINT;
		}

		@Override
		protected int getFixedSize() {
			return 1;
		}

		public void write(final int i) {
			writeConstructor();
			getEncoder().writeRaw((byte) i);
		}

		public void writeValue(final int i) {
			getEncoder().writeRaw((byte) i);
		}

		public int readPrimitiveValue() {
			return getDecoder().readRawByte();
		}

		public IntegerType getType() {
			return IntegerType.this;
		}

		public void writeValue(final Integer val) {
			getEncoder().writeRaw((byte) val.intValue());
		}

		public boolean encodesSuperset(final TypeEncoding<Integer> encoder) {
			return encoder == this;
		}

		public Integer readValue() {
			return readPrimitiveValue();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}
}
