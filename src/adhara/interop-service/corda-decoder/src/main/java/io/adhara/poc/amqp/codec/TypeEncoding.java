package io.adhara.poc.amqp.codec;

public interface TypeEncoding<V> {
	AMQPType<V> getType();

	void writeConstructor();

	int getConstructorSize();

	void writeValue(V val);

	int getValueSize(V val);

	boolean isFixedSizeVal();

	boolean encodesSuperset(TypeEncoding<V> encoder);

	boolean encodesJavaPrimitive();
}
