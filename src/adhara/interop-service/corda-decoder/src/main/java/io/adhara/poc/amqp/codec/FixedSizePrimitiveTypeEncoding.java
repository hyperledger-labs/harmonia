package io.adhara.poc.amqp.codec;

abstract class FixedSizePrimitiveTypeEncoding<T> extends AbstractPrimitiveTypeEncoding<T> {

	FixedSizePrimitiveTypeEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
		super(encoder, decoder);
	}

	public final boolean isFixedSizeVal() {
		return true;
	}

	public final int getValueSize(final T val) {
		return getFixedSize();
	}

	public final void skipValue() {
		getDecoder().getBuffer().position(getDecoder().getBuffer().position() + getFixedSize());
	}

	protected abstract int getFixedSize();
}
