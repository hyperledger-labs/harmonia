package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;

public final class NullType extends AbstractPrimitiveType<Void> {
	private final NullEncoding _nullEncoding;

	NullType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_nullEncoding = new NullEncoding(encoder, decoder);
		encoder.register(Void.class, this);
		decoder.register(this);
	}

	public Class<Void> getTypeClass() {
		return Void.class;
	}

	public NullEncoding getEncoding(final Void val) {
		return _nullEncoding;
	}


	public NullEncoding getCanonicalEncoding() {
		return _nullEncoding;
	}

	public Collection<NullEncoding> getAllEncodings() {
		return Collections.singleton(_nullEncoding);
	}

	public void write() {
		_nullEncoding.write();
	}

	private class NullEncoding extends FixedSizePrimitiveTypeEncoding<Void> {

		public NullEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 0;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.NULL;
		}

		public NullType getType() {
			return NullType.this;
		}

		public void writeValue(final Void val) {
		}

		public void writeValue() {
		}

		public boolean encodesSuperset(final TypeEncoding<Void> encoding) {
			return encoding == this;
		}

		public Void readValue() {
			return null;
		}

		public void write() {
			writeConstructor();
		}
	}
}
