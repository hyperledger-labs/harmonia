package io.adhara.poc.amqp.codec;

/**
 * Codec
 */

public final class Codec {

	private Codec() {
	}

	public static Data data(long capacity) {
		return Data.Factory.create();
	}

}
