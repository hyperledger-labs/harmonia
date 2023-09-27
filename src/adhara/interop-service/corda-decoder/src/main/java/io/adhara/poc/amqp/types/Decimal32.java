package io.adhara.poc.amqp.types;

import java.math.BigDecimal;

public final class Decimal32 extends Number {
	private final BigDecimal _underlying;
	private final int _bits;

	public Decimal32(BigDecimal underlying) {
		_underlying = underlying;
		_bits = calculateBits(underlying);
	}

	public Decimal32(final int bits) {
		_bits = bits;
		_underlying = calculateBigDecimal(bits);
	}

	static int calculateBits(final BigDecimal underlying) {
		return 0;  //TODO.
	}

	static BigDecimal calculateBigDecimal(int bits) {
		return BigDecimal.ZERO; // TODO
	}


	@Override
	public int intValue() {
		return _underlying.intValue();
	}

	@Override
	public long longValue() {
		return _underlying.longValue();
	}

	@Override
	public float floatValue() {
		return _underlying.floatValue();
	}

	@Override
	public double doubleValue() {
		return _underlying.doubleValue();
	}

	public int getBits() {
		return _bits;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Decimal32 decimal32 = (Decimal32) o;

      return _bits == decimal32._bits;
  }

	@Override
	public int hashCode() {
		return _bits;
	}
}
