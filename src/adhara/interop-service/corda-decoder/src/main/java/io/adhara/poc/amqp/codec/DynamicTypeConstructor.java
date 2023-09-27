package io.adhara.poc.amqp.codec;

public class DynamicTypeConstructor implements TypeConstructor {
	private final DescribedTypeConstructor _describedTypeConstructor;
	private final TypeConstructor _underlyingEncoding;

	public DynamicTypeConstructor(final DescribedTypeConstructor dtc,
																final TypeConstructor underlyingEncoding) {
		_describedTypeConstructor = dtc;
		_underlyingEncoding = underlyingEncoding;
	}

	public Object readValue() {
		try {
			return _describedTypeConstructor.newInstance(_underlyingEncoding.readValue());
		} catch (NullPointerException npe) {
			throw new DecodeException("Unexpected null value - mandatory field not set? (" + npe.getMessage() + ")", npe);
		} catch (ClassCastException cce) {
			throw new DecodeException("Incorrect type used", cce);
		}
	}

	public boolean encodesJavaPrimitive() {
		return false;
	}

	public void skipValue() {
		_underlyingEncoding.skipValue();
	}

	public Class getTypeClass() {
		return _describedTypeConstructor.getTypeClass();
	}
}
