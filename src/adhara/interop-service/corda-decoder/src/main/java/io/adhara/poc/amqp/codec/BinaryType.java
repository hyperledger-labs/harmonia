package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.Binary;

import java.util.Arrays;
import java.util.Collection;

public class BinaryType extends AbstractPrimitiveType<Binary> {
	private final BinaryEncoding _binaryEncoding;
	private final BinaryEncoding _shortBinaryEncoding;

	private interface BinaryEncoding extends PrimitiveTypeEncoding<Binary> {

	}

	BinaryType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_binaryEncoding = new LongBinaryEncoding(encoder, decoder);
		_shortBinaryEncoding = new ShortBinaryEncoding(encoder, decoder);
		encoder.register(Binary.class, this);
		decoder.register(this);
	}

	@Override
	public Class<Binary> getTypeClass() {
		return Binary.class;
	}

	@Override
	public BinaryEncoding getEncoding(final Binary val) {
		return val.getLength() <= 255 ? _shortBinaryEncoding : _binaryEncoding;
	}

	@Override
	public BinaryEncoding getCanonicalEncoding() {
		return _binaryEncoding;
	}

	@Override
	public Collection<BinaryEncoding> getAllEncodings() {
		return Arrays.asList(_shortBinaryEncoding, _binaryEncoding);
	}

	public void fastWrite(EncoderImpl encoder, Binary binary) {
		if (binary.getLength() <= 255) {
			// Reserve size of body + type encoding and single byte size
			encoder.getBuffer().ensureRemaining(2 + binary.getLength());
			encoder.writeRaw(EncodingCodes.VBIN8);
			encoder.writeRaw((byte) binary.getLength());
			encoder.writeRaw(binary.getArray(), binary.getArrayOffset(), binary.getLength());
		} else {
			// Reserve size of body + type encoding and four byte size
			encoder.getBuffer().ensureRemaining(5 + binary.getLength());
			encoder.writeRaw(EncodingCodes.VBIN32);
			encoder.writeRaw(binary.getLength());
			encoder.writeRaw(binary.getArray(), binary.getArrayOffset(), binary.getLength());
		}
	}

	private class LongBinaryEncoding
		extends LargeFloatingSizePrimitiveTypeEncoding<Binary>
		implements BinaryEncoding {

		public LongBinaryEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected void writeEncodedValue(final Binary val) {
			getEncoder().getBuffer().ensureRemaining(val.getLength());
			getEncoder().writeRaw(val.getArray(), val.getArrayOffset(), val.getLength());
		}

		@Override
		protected int getEncodedValueSize(final Binary val) {
			return val.getLength();
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.VBIN32;
		}

		@Override
		public BinaryType getType() {
			return BinaryType.this;
		}

		@Override
		public boolean encodesSuperset(final TypeEncoding<Binary> encoding) {
			return (getType() == encoding.getType());
		}

		@Override
		public Binary readValue() {
			final DecoderImpl decoder = getDecoder();
			int size = decoder.readRawInt();
			if (size > decoder.getByteBufferRemaining()) {
				throw new IllegalArgumentException("Binary data size " + size + " is specified to be greater than the amount of data available (" +
					decoder.getByteBufferRemaining() + ")");
			}
			byte[] data = new byte[size];
			decoder.readRaw(data, 0, size);
			return new Binary(data);
		}

		@Override
		public void skipValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();
			int size = decoder.readRawInt();
			buffer.position(buffer.position() + size);
		}
	}

	private class ShortBinaryEncoding
		extends SmallFloatingSizePrimitiveTypeEncoding<Binary>
		implements BinaryEncoding {

		public ShortBinaryEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected void writeEncodedValue(final Binary val) {
			getEncoder().getBuffer().ensureRemaining(val.getLength());
			getEncoder().writeRaw(val.getArray(), val.getArrayOffset(), val.getLength());
		}

		@Override
		protected int getEncodedValueSize(final Binary val) {
			return val.getLength();
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.VBIN8;
		}

		@Override
		public BinaryType getType() {
			return BinaryType.this;
		}

		@Override
		public boolean encodesSuperset(final TypeEncoding<Binary> encoder) {
			return encoder == this;
		}

		@Override
		public Binary readValue() {
			int size = ((int) getDecoder().readRawByte()) & 0xff;
			byte[] data = new byte[size];
			getDecoder().readRaw(data, 0, size);
			return new Binary(data);
		}

		@Override
		public void skipValue() {
			DecoderImpl decoder = getDecoder();
			ReadableBuffer buffer = decoder.getBuffer();
			int size = ((int) getDecoder().readRawByte()) & 0xff;
			buffer.position(buffer.position() + size);
		}
	}
}
