package io.adhara.poc.amqp.codec;

/**
 * Marker interface that indicates the TypeConstructor can decode known Proton-J types
 * using a fast path read / write operation.  These types may result in an encode that
 * does not always write the smallest form of the given type to save time.
 *
 * @param <V> The type that this constructor handles
 */
public interface FastPathDescribedTypeConstructor<V> extends TypeConstructor<V> {

}
