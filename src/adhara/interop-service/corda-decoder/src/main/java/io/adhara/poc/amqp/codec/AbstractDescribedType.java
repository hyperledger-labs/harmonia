package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.UnsignedLong;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

abstract public class AbstractDescribedType<T, M> implements AMQPType<T> {
	private final DecoderImpl _decoder;
	private final EncoderImpl _encoder;
	private final Map<TypeEncoding<M>, TypeEncoding<T>> _encodings = new HashMap<TypeEncoding<M>, TypeEncoding<T>>();

	public AbstractDescribedType(EncoderImpl encoder) {
		_encoder = encoder;
		_decoder = encoder.getDecoder();
	}

	abstract protected UnsignedLong getDescriptor();

	public EncoderImpl getEncoder() {
		return _encoder;
	}

	public DecoderImpl getDecoder() {
		return _decoder;
	}

	public TypeEncoding<T> getEncoding(final T val) {
		M asUnderlying = wrap(val);
		TypeEncoding<M> underlyingEncoding = _encoder.getType(asUnderlying).getEncoding(asUnderlying);
		TypeEncoding<T> encoding = _encodings.get(underlyingEncoding);
		if (encoding == null) {
			encoding = new DynamicDescribedTypeEncoding(underlyingEncoding);
			_encodings.put(underlyingEncoding, encoding);
		}

		return encoding;
	}

	abstract protected M wrap(T val);

	public TypeEncoding<T> getCanonicalEncoding() {
		return null;
	}

	public Collection<TypeEncoding<T>> getAllEncodings() {
		Collection values = _encodings.values();
		Collection unmodifiable = Collections.unmodifiableCollection(values);
		return (Collection<TypeEncoding<T>>) unmodifiable;
	}

	public void write(final T val) {
		TypeEncoding<T> encoding = getEncoding(val);
		encoding.writeConstructor();
		encoding.writeValue(val);
	}

	private class DynamicDescribedTypeEncoding implements TypeEncoding<T> {
		private final TypeEncoding<M> _underlyingEncoding;
		private final TypeEncoding<UnsignedLong> _descriptorType;
		private final int _constructorSize;


		public DynamicDescribedTypeEncoding(final TypeEncoding<M> underlyingEncoding) {
			_underlyingEncoding = underlyingEncoding;
			_descriptorType = _encoder.getType(getDescriptor()).getEncoding(getDescriptor());
			_constructorSize = 1 + _descriptorType.getConstructorSize()
				+ _descriptorType.getValueSize(getDescriptor())
				+ _underlyingEncoding.getConstructorSize();
		}

		public AMQPType<T> getType() {
			return AbstractDescribedType.this;
		}

		public void writeConstructor() {
			_encoder.writeRaw(EncodingCodes.DESCRIBED_TYPE_INDICATOR);
			_descriptorType.writeConstructor();
			_descriptorType.writeValue(getDescriptor());
			_underlyingEncoding.writeConstructor();
		}

		public int getConstructorSize() {
			return _constructorSize;
		}

		public void writeValue(final T val) {
			_underlyingEncoding.writeValue(wrap(val));
		}

		public int getValueSize(final T val) {
			return _underlyingEncoding.getValueSize(wrap(val));
		}

		public boolean isFixedSizeVal() {
			return _underlyingEncoding.isFixedSizeVal();
		}

		public boolean encodesSuperset(final TypeEncoding<T> encoding) {
			return (getType() == encoding.getType())
				&& (_underlyingEncoding.encodesSuperset(((DynamicDescribedTypeEncoding) encoding)
				._underlyingEncoding));
		}

		@Override
		public boolean encodesJavaPrimitive() {
			return false;
		}

	}
}
