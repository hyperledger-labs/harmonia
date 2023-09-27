package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UUIDType extends AbstractPrimitiveType<UUID> {
	private final UUIDEncoding _uuidEncoding;

	UUIDType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_uuidEncoding = new UUIDEncoding(encoder, decoder);
		encoder.register(UUID.class, this);
		decoder.register(this);
	}

	public Class<UUID> getTypeClass() {
		return UUID.class;
	}

	public UUIDEncoding getEncoding(final UUID val) {
		return _uuidEncoding;
	}

	public void fastWrite(EncoderImpl encoder, UUID value) {
		encoder.writeRaw(EncodingCodes.UUID);
		encoder.writeRaw(value.getMostSignificantBits());
		encoder.writeRaw(value.getLeastSignificantBits());
	}

	public UUIDEncoding getCanonicalEncoding() {
		return _uuidEncoding;
	}

	public Collection<UUIDEncoding> getAllEncodings() {
		return Collections.singleton(_uuidEncoding);
	}

	private class UUIDEncoding extends FixedSizePrimitiveTypeEncoding<UUID> {

		public UUIDEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 16;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.UUID;
		}

		public UUIDType getType() {
			return UUIDType.this;
		}

		public void writeValue(final UUID val) {
			getEncoder().writeRaw(val.getMostSignificantBits());
			getEncoder().writeRaw(val.getLeastSignificantBits());
		}

		public boolean encodesSuperset(final TypeEncoding<UUID> encoding) {
			return (getType() == encoding.getType());
		}

		public UUID readValue() {
			long msb = getDecoder().readRawLong();
			long lsb = getDecoder().readRawLong();

			return new UUID(msb, lsb);
		}
	}
}
