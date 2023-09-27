package io.adhara.poc.amqp.codec;

public interface TypeConstructor<V> {
	V readValue();

	void skipValue();

	boolean encodesJavaPrimitive();

	Class<V> getTypeClass();
}
