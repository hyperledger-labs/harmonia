package io.adhara.poc.amqp.codec;

abstract class AbstractPrimitiveType<T> implements PrimitiveType<T> {
	public final void write(T val) {
		final TypeEncoding<T> encoding = getEncoding(val);
		encoding.writeConstructor();
		encoding.writeValue(val);
	}

}
