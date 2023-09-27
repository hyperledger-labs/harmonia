package io.adhara.poc.amqp.codec;

abstract class LargeFloatingSizePrimitiveTypeEncoding<T> extends FloatingSizePrimitiveTypeEncoding<T> {

	LargeFloatingSizePrimitiveTypeEncoding(final EncoderImpl encoder, DecoderImpl decoder) {
		super(encoder, decoder);
	}

	@Override
	public int getSizeBytes() {
		return 4;
	}

	@Override
	protected void writeSize(final T val) {
		getEncoder().writeRaw(getEncodedValueSize(val));
	}
}
