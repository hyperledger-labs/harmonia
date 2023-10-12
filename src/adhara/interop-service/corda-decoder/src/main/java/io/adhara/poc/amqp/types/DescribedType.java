package io.adhara.poc.amqp.types;

public interface DescribedType {
	Object getDescriptor();

	Object getDescribed();
}
