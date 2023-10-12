package io.adhara.poc.amqp.codec;

import java.util.*;

public class ListType extends AbstractPrimitiveType<List> {
	private final ListEncoding _listEncoding;
	private final ListEncoding _shortListEncoding;
	private final ListEncoding _zeroListEncoding;
	private final EncoderImpl _encoder;

	private interface ListEncoding extends PrimitiveTypeEncoding<List> {
		void setValue(List value, int length);
	}

	ListType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_encoder = encoder;
		_listEncoding = new AllListEncoding(encoder, decoder);
		_shortListEncoding = new ShortListEncoding(encoder, decoder);
		_zeroListEncoding = new ZeroListEncoding(encoder, decoder);
		encoder.register(List.class, this);
		decoder.register(this);
	}

	@Override
	public Class<List> getTypeClass() {
		return List.class;
	}

	@Override
	public ListEncoding getEncoding(final List val) {
		int calculatedSize = calculateSize(val, _encoder);
		ListEncoding encoding = val.isEmpty()
			? _zeroListEncoding
			: (val.size() > 255 || calculatedSize >= 254)
			? _listEncoding
			: _shortListEncoding;

		encoding.setValue(val, calculatedSize);
		return encoding;
	}

	private static int calculateSize(final List val, EncoderImpl encoder) {
		int len = 0;
		final int count = val.size();

		for (int i = 0; i < count; i++) {
			Object element = val.get(i);
			AMQPType type = encoder.getType(element);
			if (type == null) {
				throw new IllegalArgumentException("No encoding defined for type: " + element.getClass());
			}
			TypeEncoding elementEncoding = type.getEncoding(element);
			len += elementEncoding.getConstructorSize() + elementEncoding.getValueSize(element);
		}
		return len;
	}

	@Override
	public ListEncoding getCanonicalEncoding() {
		return _listEncoding;
	}

	@Override
	public Collection<ListEncoding> getAllEncodings() {
		return Arrays.asList(_zeroListEncoding, _shortListEncoding, _listEncoding);
	}

	private class AllListEncoding
		extends LargeFloatingSizePrimitiveTypeEncoding<List>
		implements ListEncoding {

		private List _value;
		private int _length;

		public AllListEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected void writeEncodedValue(final List val) {
			getEncoder().getBuffer().ensureRemaining(getSizeBytes() + getEncodedValueSize(val));
			getEncoder().writeRaw(val.size());

			final int count = val.size();

			for (int i = 0; i < count; i++) {
				Object element = val.get(i);
				TypeEncoding elementEncoding = getEncoder().getType(element).getEncoding(element);
				elementEncoding.writeConstructor();
				elementEncoding.writeValue(element);
			}
		}

		@Override
		protected int getEncodedValueSize(final List val) {
			return 4 + ((val == _value) ? _length : calculateSize(val, getEncoder()));
		}


		@Override
		public byte getEncodingCode() {
			return EncodingCodes.LIST32;
		}

		@Override
		public ListType getType() {
			return ListType.this;
		}

		@Override
		public boolean encodesSuperset(final TypeEncoding<List> encoding) {
			return (getType() == encoding.getType());
		}

		@Override
		public List readValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();

			int size = decoder.readRawInt();
			// todo - limit the decoder with size
			int count = decoder.readRawInt();
			// Ensure we do not allocate an array of size greater then the available data, otherwise there is a risk for an OOM error
			if (count > decoder.getByteBufferRemaining()) {
				throw new IllegalArgumentException("List element count " + count + " is specified to be greater than the amount of data available (" +
					decoder.getByteBufferRemaining() + ")");
			}

			TypeConstructor<?> typeConstructor = null;

			List<Object> list = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				boolean arrayType = false;
				byte encodingCode = buffer.get(buffer.position());
				switch (encodingCode) {
					case EncodingCodes.ARRAY8:
					case EncodingCodes.ARRAY32:
						arrayType = true;
				}

				// Whenever we can just reuse the previously used TypeDecoder instead
				// of spending time looking up the same one again.
				if (typeConstructor == null) {
					typeConstructor = getDecoder().readConstructor();
				} else {
					if (encodingCode == EncodingCodes.DESCRIBED_TYPE_INDICATOR || !(typeConstructor instanceof PrimitiveTypeEncoding<?>)) {
						typeConstructor = getDecoder().readConstructor();
					} else {
						PrimitiveTypeEncoding<?> primitiveConstructor = (PrimitiveTypeEncoding<?>) typeConstructor;
						if (encodingCode != primitiveConstructor.getEncodingCode()) {
							typeConstructor = getDecoder().readConstructor();
						} else {
							// consume the encoding code byte for real
							encodingCode = buffer.get();
						}
					}
				}

				if (typeConstructor == null) {
					throw new DecodeException("Unknown constructor");
				}

				final Object value;

				if (arrayType) {
					value = ((ArrayType.ArrayEncoding) typeConstructor).readValueArray();
				} else {
					value = typeConstructor.readValue();
				}

				list.add(value);
			}

			return list;
		}

		@Override
		public void skipValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();
			int size = decoder.readRawInt();
			buffer.position(buffer.position() + size);
		}

		@Override
		public void setValue(final List value, final int length) {
			_value = value;
			_length = length;
		}
	}

	private class ShortListEncoding
		extends SmallFloatingSizePrimitiveTypeEncoding<List>
		implements ListEncoding {

		private List _value;
		private int _length;

		public ShortListEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected void writeEncodedValue(final List val) {
			getEncoder().getBuffer().ensureRemaining(getSizeBytes() + getEncodedValueSize(val));
			getEncoder().writeRaw((byte) val.size());

			final int count = val.size();

			for (int i = 0; i < count; i++) {
				Object element = val.get(i);
				TypeEncoding elementEncoding = getEncoder().getType(element).getEncoding(element);
				elementEncoding.writeConstructor();
				elementEncoding.writeValue(element);
			}
		}

		@Override
		protected int getEncodedValueSize(final List val) {
			return 1 + ((val == _value) ? _length : calculateSize(val, getEncoder()));
		}


		@Override
		public byte getEncodingCode() {
			return EncodingCodes.LIST8;
		}

		@Override
		public ListType getType() {
			return ListType.this;
		}

		@Override
		public boolean encodesSuperset(final TypeEncoding<List> encoder) {
			return encoder == this;
		}

		@Override
		public List readValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();

			int size = ((int) decoder.readRawByte()) & 0xff;
			// todo - limit the decoder with size
			int count = ((int) decoder.readRawByte()) & 0xff;

			TypeConstructor<?> typeConstructor = null;

			List<Object> list = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				boolean arrayType = false;
				byte encodingCode = buffer.get(buffer.position());
				switch (encodingCode) {
					case EncodingCodes.ARRAY8:
					case EncodingCodes.ARRAY32:
						arrayType = true;
				}

				// Whenever we can just reuse the previously used TypeDecoder instead
				// of spending time looking up the same one again.
				if (typeConstructor == null) {
					typeConstructor = getDecoder().readConstructor();
				} else {
					if (encodingCode == EncodingCodes.DESCRIBED_TYPE_INDICATOR || !(typeConstructor instanceof PrimitiveTypeEncoding<?>)) {
						typeConstructor = getDecoder().readConstructor();
					} else {
						PrimitiveTypeEncoding<?> primitiveConstructor = (PrimitiveTypeEncoding<?>) typeConstructor;
						if (encodingCode != primitiveConstructor.getEncodingCode()) {
							typeConstructor = getDecoder().readConstructor();
						} else {
							// consume the encoding code byte for real
							encodingCode = buffer.get();
						}
					}
				}

				if (typeConstructor == null) {
					throw new DecodeException("Unknown constructor");
				}

				final Object value;

				if (arrayType) {
					value = ((ArrayType.ArrayEncoding) typeConstructor).readValueArray();
				} else {
					value = typeConstructor.readValue();
				}

				list.add(value);
			}

			return list;
		}

		@Override
		public void skipValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();
			int size = ((int) decoder.readRawByte()) & 0xff;
			buffer.position(buffer.position() + size);
		}

		@Override
		public void setValue(final List value, final int length) {
			_value = value;
			_length = length;
		}
	}


	private class ZeroListEncoding
		extends FixedSizePrimitiveTypeEncoding<List>
		implements ListEncoding {
		public ZeroListEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.LIST0;
		}

		@Override
		protected int getFixedSize() {
			return 0;
		}

		@Override
		public ListType getType() {
			return ListType.this;
		}

		@Override
		public void setValue(List value, int length) {
		}

		@Override
		public void writeValue(final List val) {
		}

		@Override
		public boolean encodesSuperset(final TypeEncoding<List> encoder) {
			return encoder == this;
		}

		@Override
		public List readValue() {
			return Collections.EMPTY_LIST;
		}
	}
}
