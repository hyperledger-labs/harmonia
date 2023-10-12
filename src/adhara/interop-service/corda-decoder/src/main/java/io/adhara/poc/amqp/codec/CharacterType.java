package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;

public class CharacterType extends AbstractPrimitiveType<Character> {
	private final CharacterEncoding _characterEncoding;

	CharacterType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_characterEncoding = new CharacterEncoding(encoder, decoder);
		encoder.register(Character.class, this);
		decoder.register(this);
	}

	public Class<Character> getTypeClass() {
		return Character.class;
	}

	public CharacterEncoding getEncoding(final Character val) {
		return _characterEncoding;
	}


	public CharacterEncoding getCanonicalEncoding() {
		return _characterEncoding;
	}

	public Collection<CharacterEncoding> getAllEncodings() {
		return Collections.singleton(_characterEncoding);
	}

	public void write(char c) {
		_characterEncoding.write(c);
	}

	public class CharacterEncoding extends FixedSizePrimitiveTypeEncoding<Character> {

		public CharacterEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 4;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.CHAR;
		}

		public CharacterType getType() {
			return CharacterType.this;
		}

		public void writeValue(final Character val) {
			getEncoder().writeRaw((int) val.charValue() & 0xffff);
		}

		public void writeValue(final char val) {
			getEncoder().writeRaw((int) val & 0xffff);
		}

		public void write(final char c) {
			writeConstructor();
			getEncoder().writeRaw((int) c & 0xffff);

		}

		public boolean encodesSuperset(final TypeEncoding<Character> encoding) {
			return (getType() == encoding.getType());
		}

		public Character readValue() {
			return readPrimitiveValue();
		}

		public char readPrimitiveValue() {
			return (char) (getDecoder().readRawInt() & 0xffff);
		}


		@Override
		public boolean encodesJavaPrimitive() {
			return true;
		}
	}
}
