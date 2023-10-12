package io.adhara.poc.amqp.codec;

public class DecodeException extends RuntimeException {
	public DecodeException() {
	}

	public DecodeException(String message) {
		super(message);
	}

	public DecodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public DecodeException(Throwable cause) {
		super(cause);
	}
}
