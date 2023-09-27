package io.adhara.poc.amqp.codec;

abstract class FloatingSizePrimitiveTypeEncoding<T> extends AbstractPrimitiveTypeEncoding<T> {

	FloatingSizePrimitiveTypeEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
		super(encoder, decoder);
	}

	public final boolean isFixedSizeVal() {
		return false;
	}

	abstract int getSizeBytes();

	public void writeValue(final T val) {
		writeSize(val);
		writeEncodedValue(val);
	}

	protected abstract void writeEncodedValue(final T val);

	protected abstract void writeSize(final T val);

	public int getValueSize(final T val) {
		return getSizeBytes() + getEncodedValueSize(val);
	}

	protected abstract int getEncodedValueSize(final T val);
}
