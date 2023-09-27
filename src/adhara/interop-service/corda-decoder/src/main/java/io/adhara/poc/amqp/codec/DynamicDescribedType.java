package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.DescribedType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynamicDescribedType implements AMQPType<DescribedType> {

	private final EncoderImpl _encoder;
	private final Map<TypeEncoding, TypeEncoding> _encodings = new HashMap<TypeEncoding, TypeEncoding>();
	private final Object _descriptor;

	public DynamicDescribedType(EncoderImpl encoder, final Object descriptor) {
		_encoder = encoder;
		_descriptor = descriptor;
	}


	public Class<DescribedType> getTypeClass() {
		return DescribedType.class;
	}

	public TypeEncoding<DescribedType> getEncoding(final DescribedType val) {
		TypeEncoding underlyingEncoding = _encoder.getType(val.getDescribed()).getEncoding(val.getDescribed());
		TypeEncoding encoding = _encodings.get(underlyingEncoding);
		if (encoding == null) {
			encoding = new DynamicDescribedTypeEncoding(underlyingEncoding);
			_encodings.put(underlyingEncoding, encoding);
		}

		return encoding;
	}

	public TypeEncoding<DescribedType> getCanonicalEncoding() {
		return null;
	}

	public Collection<TypeEncoding<DescribedType>> getAllEncodings() {
		Collection values = _encodings.values();
		Collection unmodifiable = Collections.unmodifiableCollection(values);
		return (Collection<TypeEncoding<DescribedType>>) unmodifiable;
	}

	public void write(final DescribedType val) {
		TypeEncoding<DescribedType> encoding = getEncoding(val);
		encoding.writeConstructor();
		encoding.writeValue(val);
	}

	private class DynamicDescribedTypeEncoding implements TypeEncoding {
		private final TypeEncoding _underlyingEncoding;
		private final TypeEncoding _descriptorType;
		private final int _constructorSize;


		public DynamicDescribedTypeEncoding(final TypeEncoding underlyingEncoding) {
			_underlyingEncoding = underlyingEncoding;
			_descriptorType = _encoder.getType(_descriptor).getEncoding(_descriptor);
			_constructorSize = 1 + _descriptorType.getConstructorSize()
				+ _descriptorType.getValueSize(_descriptor)
				+ _underlyingEncoding.getConstructorSize();
		}

		public AMQPType getType() {
			return DynamicDescribedType.this;
		}

		public void writeConstructor() {
			_encoder.writeRaw(EncodingCodes.DESCRIBED_TYPE_INDICATOR);
			_descriptorType.writeConstructor();
			_descriptorType.writeValue(_descriptor);
			_underlyingEncoding.writeConstructor();
		}

		public int getConstructorSize() {
			return _constructorSize;
		}

		public void writeValue(final Object val) {
			_underlyingEncoding.writeValue(((DescribedType) val).getDescribed());
		}

		public int getValueSize(final Object val) {
			return _underlyingEncoding.getValueSize(((DescribedType) val).getDescribed());
		}

		public boolean isFixedSizeVal() {
			return _underlyingEncoding.isFixedSizeVal();
		}

		public boolean encodesSuperset(final TypeEncoding encoding) {
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
