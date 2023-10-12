package io.adhara.poc.amqp.codec;

public interface DescribedTypeConstructor<V> {
	V newInstance(Object described);

	Class getTypeClass();
}
