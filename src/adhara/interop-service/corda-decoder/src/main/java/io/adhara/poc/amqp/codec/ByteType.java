package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;

public class ByteType extends AbstractPrimitiveType<Byte> {
	private final ByteEncoding _byteEncoding;

	ByteType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_byteEncoding = new ByteEncoding(encoder, decoder);
		encoder.register(Byte.class, this);
		decoder.register(this);
	}

	public Class<Byte> getTypeClass() {
		return Byte.class;
	}

	public ByteEncoding getEncoding(final Byte val) {
		return _byteEncoding;
	}


	public ByteEncoding getCanonicalEncoding() {
		return _byteEncoding;
	}

	public Collection<ByteEncoding> getAllEncodings() {
		return Collections.singleton(_byteEncoding);
	}

	public void writeType(byte b) {
		_byteEncoding.write(b);
	}


	public class ByteEncoding extends FixedSizePrimitiveTypeEncoding<Byte> {

		public ByteEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 1;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.BYTE;
		}

		public ByteType getType() {
			return ByteType.this;
		}

		public void writeValue(final Byte val) {
			getEncoder().writeRaw(val);
		}


		public void write(final byte val) {
			writeConstructor();
			getEncoder().writeRaw(val);
		}

		public void writeValue(final byte val) {
			getEncoder().writeRaw(val);
		}

		public boolean encodesSuperset(final TypeEncoding<Byte> encoding) {
			return (getType() == encoding.getType());
		}

		public Byte readValue() {
			return readPrimitiveValue();
		}

		public byte readPrimitiveValue() {
			return getDecoder().readRawByte();
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}

	}
}
