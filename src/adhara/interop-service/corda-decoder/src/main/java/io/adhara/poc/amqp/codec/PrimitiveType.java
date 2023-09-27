package io.adhara.poc.amqp.codec;

import java.util.Collection;

public interface PrimitiveType<V> extends AMQPType<V> {

	PrimitiveTypeEncoding<V> getEncoding(V val);

	PrimitiveTypeEncoding<V> getCanonicalEncoding();

	Collection<? extends PrimitiveTypeEncoding<V>> getAllEncodings();


}
