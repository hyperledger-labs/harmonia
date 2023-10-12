package io.adhara.poc.amqp.codec;

import java.util.Collection;

public interface AMQPType<V> {
	Class<V> getTypeClass();

	TypeEncoding<V> getEncoding(V val);

	TypeEncoding<V> getCanonicalEncoding();

	Collection<? extends TypeEncoding<V>> getAllEncodings();

	void write(V val);
}
