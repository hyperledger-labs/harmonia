package io.adhara.poc.amqp.codec;

abstract class SmallFloatingSizePrimitiveTypeEncoding<T> extends FloatingSizePrimitiveTypeEncoding<T> {

	SmallFloatingSizePrimitiveTypeEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
		super(encoder, decoder);
	}

	@Override
	public int getSizeBytes() {
		return 1;
	}

	@Override
	protected void writeSize(final T val) {
		getEncoder().writeRaw((byte) getEncodedValueSize(val));
	}
}
