package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;

public class FloatType extends AbstractPrimitiveType<Float> {
	private final FloatEncoding _floatEncoding;

	FloatType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_floatEncoding = new FloatEncoding(encoder, decoder);
		encoder.register(Float.class, this);
		decoder.register(this);
	}

	public Class<Float> getTypeClass() {
		return Float.class;
	}

	public FloatEncoding getEncoding(final Float val) {
		return _floatEncoding;
	}


	public FloatEncoding getCanonicalEncoding() {
		return _floatEncoding;
	}

	public Collection<FloatEncoding> getAllEncodings() {
		return Collections.singleton(_floatEncoding);
	}

	public void write(float f) {
		_floatEncoding.write(f);
	}

	public class FloatEncoding extends FixedSizePrimitiveTypeEncoding<Float> {

		public FloatEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 4;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.FLOAT;
		}

		public FloatType getType() {
			return FloatType.this;
		}

		public void writeValue(final Float val) {
			getEncoder().writeRaw(val.floatValue());
		}

		public void writeValue(final float val) {
			getEncoder().writeRaw(val);
		}


		public void write(final float f) {
			writeConstructor();
			getEncoder().writeRaw(f);

		}

		public boolean encodesSuperset(final TypeEncoding<Float> encoding) {
			return (getType() == encoding.getType());
		}

		public Float readValue() {
			return readPrimitiveValue();
		}

		public float readPrimitiveValue() {
			return getDecoder().readRawFloat();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}
}
