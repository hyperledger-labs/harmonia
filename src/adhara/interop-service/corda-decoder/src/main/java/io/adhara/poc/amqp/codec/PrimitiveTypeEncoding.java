package io.adhara.poc.amqp.codec;

public interface PrimitiveTypeEncoding<T> extends TypeEncoding<T>, TypeConstructor<T> {
	PrimitiveType<T> getType();

	byte getEncodingCode();

	void writeConstructor();

	int getConstructorSize();
}
