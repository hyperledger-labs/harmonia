package io.adhara.poc.amqp.codec;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class TimestampType extends AbstractPrimitiveType<Date> {
	private final TimestampEncoding _timestampEncoding;

	TimestampType(final EncoderImpl encoder, final DecoderImpl decoder) {
		_timestampEncoding = new TimestampEncoding(encoder, decoder);
		encoder.register(Date.class, this);
		decoder.register(this);
	}

	public Class<Date> getTypeClass() {
		return Date.class;
	}

	public TimestampEncoding getEncoding(final Date val) {
		return _timestampEncoding;
	}

	public void fastWrite(EncoderImpl encoder, long timestamp) {
		encoder.writeRaw(EncodingCodes.TIMESTAMP);
		encoder.writeRaw(timestamp);
	}

	public TimestampEncoding getCanonicalEncoding() {
		return _timestampEncoding;
	}

	public Collection<TimestampEncoding> getAllEncodings() {
		return Collections.singleton(_timestampEncoding);
	}

	public void write(long l) {
		_timestampEncoding.write(l);
	}

	private class TimestampEncoding extends FixedSizePrimitiveTypeEncoding<Date> {

		public TimestampEncoding(final EncoderImpl encoder, final DecoderImpl decoder) {
			super(encoder, decoder);
		}

		@Override
		protected int getFixedSize() {
			return 8;
		}

		@Override
		public byte getEncodingCode() {
			return EncodingCodes.TIMESTAMP;
		}

		public TimestampType getType() {
			return TimestampType.this;
		}

		public void writeValue(final Date val) {
			getEncoder().writeRaw(val.getTime());
		}

		public void write(final long l) {
			writeConstructor();
			getEncoder().writeRaw(l);

		}

		public boolean encodesSuperset(final TypeEncoding<Date> encoding) {
			return (getType() == encoding.getType());
		}

		public Date readValue() {
			return new Date(getDecoder().readRawLong());
		}
	}
}
