package io.adhara.poc.amqp.types;

public final class UnsignedByte extends Number implements Comparable<UnsignedByte> {
	private final byte _underlying;
	private static final UnsignedByte[] cachedValues = new UnsignedByte[256];

	static {
		for (int i = 0; i < 256; i++) {
			cachedValues[i] = new UnsignedByte((byte) i);
		}
	}

	public UnsignedByte(byte underlying) {
		_underlying = underlying;
	}

	@Override
	public byte byteValue() {
		return _underlying;
	}

	@Override
	public short shortValue() {
		return (short) intValue();
	}

	@Override
	public int intValue() {
		return ((int) _underlying) & 0xFF;
	}

	@Override
	public long longValue() {
		return ((long) _underlying) & 0xFFl;
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

		UnsignedByte that = (UnsignedByte) o;

      return _underlying == that._underlying;
  }

	public int compareTo(UnsignedByte o) {
		return Integer.signum(intValue() - o.intValue());
	}

	@Override
	public int hashCode() {
		return _underlying;
	}

	@Override
	public String toString() {
		return String.valueOf(intValue());
	}

	public static UnsignedByte valueOf(byte underlying) {
		final int index = ((int) underlying) & 0xFF;
		return cachedValues[index];
	}

	public static UnsignedByte valueOf(final String value)
		throws NumberFormatException {
		int intVal = Integer.parseInt(value);
		if (intVal < 0 || intVal >= (1 << 8)) {
			throw new NumberFormatException("Value \"" + value + "\" lies outside the range [" + 0 + "-" + (1 << 8) + ").");
		}
		return valueOf((byte) intVal);
	}

}
