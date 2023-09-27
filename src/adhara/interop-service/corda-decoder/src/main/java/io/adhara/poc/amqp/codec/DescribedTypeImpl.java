package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.DescribedType;

class DescribedTypeImpl implements DescribedType {
	private final Object _descriptor;
	private final Object _described;

	public DescribedTypeImpl(final Object descriptor, final Object described) {
		_descriptor = descriptor;
		_described = described;
	}

	@Override
	public Object getDescriptor() {
		return _descriptor;
	}

	@Override
	public Object getDescribed() {
		return _described;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof DescribedType)) {
			return false;
		}

		DescribedType that = (DescribedType) o;

		if (_described != null ? !_described.equals(that.getDescribed()) : that.getDescribed() != null) {
			return false;
		}
		return _descriptor != null ? _descriptor.equals(that.getDescriptor()) : that.getDescriptor() == null;
	}

	@Override
	public int hashCode() {
		int result = _descriptor != null ? _descriptor.hashCode() : 0;
		result = 31 * result + (_described != null ? _described.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "{" + _descriptor +
			": " + _described +
			'}';
	}
}