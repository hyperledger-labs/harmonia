package io.adhara.poc.amqp.types;

public class UnknownDescribedType implements DescribedType {
	private final Object _descriptor;
	private final Object _described;

	public UnknownDescribedType(final Object descriptor, final Object described) {
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
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final UnknownDescribedType that = (UnknownDescribedType) o;

		if (_described != null ? !_described.equals(that._described) : that._described != null) {
			return false;
		}
      return _descriptor != null ? _descriptor.equals(that._descriptor) : that._descriptor == null;
  }

	@Override
	public int hashCode() {
		int result = _descriptor != null ? _descriptor.hashCode() : 0;
		result = 31 * result + (_described != null ? _described.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "UnknownDescribedType{" +
			"descriptor=" + _descriptor +
			", described=" + _described +
			'}';
	}
}
