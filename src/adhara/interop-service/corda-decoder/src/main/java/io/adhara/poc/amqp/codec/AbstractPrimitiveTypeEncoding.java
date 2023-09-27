package io.adhara.poc.amqp.codec;

abstract class AbstractPrimitiveTypeEncoding<T> implements PrimitiveTypeEncoding<T> {
	private final EncoderImpl _encoder;
	private final DecoderImpl _decoder;

	AbstractPrimitiveTypeEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
		_encoder = encoder;
		_decoder = decoder;
	}

	public final void writeConstructor() {
		_encoder.writeRaw(getEncodingCode());
	}

	public int getConstructorSize() {
		return 1;
	}

	public abstract byte getEncodingCode();

	protected EncoderImpl getEncoder() {
		return _encoder;
	}

	public Class<T> getTypeClass() {
		return getType().getTypeClass();
	}

	protected DecoderImpl getDecoder() {
		return _decoder;
	}


	public boolean encodesJavaPrimitive() {
		return false;
	}

}
