package io.adhara.poc.amqp.types;

public final class UnsignedInteger extends Number implements Comparable<UnsignedInteger> {
	private final int _underlying;
	private static final UnsignedInteger[] cachedValues = new UnsignedInteger[256];

	static {
		for (int i = 0; i < 256; i++) {
			cachedValues[i] = new UnsignedInteger(i);
		}
	}

	public static final UnsignedInteger ZERO = cachedValues[0];
	public static final UnsignedInteger ONE = cachedValues[1];
	public static final UnsignedInteger MAX_VALUE = new UnsignedInteger(0xffffffff);


	public UnsignedInteger(int underlying) {
		_underlying = underlying;
	}

	@Override
	public int intValue() {
		return _underlying;
	}

	@Override
	public long longValue() {
		return ((long) _underlying) & 0xFFFFFFFFl;
	}

	@Override
	public float floatValue() {
		return (float) longValue();
	}

	@Override
	public double doubleValue() {
		return (double) longValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UnsignedInteger that = (UnsignedInteger) o;

      return _underlying == that._underlying;
  }

	public int compareTo(UnsignedInteger o) {
		return Long.signum(longValue() - o.longValue());
	}

	@Override
	public int hashCode() {
		return _underlying;
	}

	@Override
	public String toString() {
		return String.valueOf(longValue());
	}

	public static UnsignedInteger valueOf(int underlying) {
		if ((underlying & 0xFFFFFF00) == 0) {
			return cachedValues[underlying];
		} else {
			return new UnsignedInteger(underlying);
		}
	}

	public UnsignedInteger add(final UnsignedInteger i) {
		int val = _underlying + i._underlying;
		return UnsignedInteger.valueOf(val);
	}

	public UnsignedInteger subtract(final UnsignedInteger i) {
		int val = _underlying - i._underlying;
		return UnsignedInteger.valueOf(val);
	}

	public static UnsignedInteger valueOf(final String value) {
		long longVal = Long.parseLong(value);
		return valueOf(longVal);
	}

	public static UnsignedInteger valueOf(final long longVal) {
		if (longVal < 0L || longVal >= (1L << 32)) {
			throw new NumberFormatException("Value \"" + longVal + "\" lies outside the range [" + 0L + "-" + (1L << 32) + ").");
		}
		return valueOf((int) longVal);
	}

}
