package io.adhara.poc.amqp.types;

import java.math.BigDecimal;

public final class Decimal64 extends Number {
	private final BigDecimal _underlying;
	private final long _bits;

	public Decimal64(BigDecimal underlying) {
		_underlying = underlying;
		_bits = calculateBits(underlying);

	}


	public Decimal64(final long bits) {
		_bits = bits;
		_underlying = calculateBigDecimal(bits);
	}

	static BigDecimal calculateBigDecimal(final long bits) {
		return BigDecimal.ZERO;
	}

	static long calculateBits(final BigDecimal underlying) {
		return 0l; // TODO
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

	public long getBits() {
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

		final Decimal64 decimal64 = (Decimal64) o;

		return _bits == decimal64._bits;
	}

	@Override
	public int hashCode() {
		return (int) (_bits ^ (_bits >>> 32));
	}
}
