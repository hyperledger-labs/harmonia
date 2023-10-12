package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

public interface ByteBufferDecoder extends Decoder {
	void setByteBuffer(ByteBuffer buffer);

	int getByteBufferRemaining();
}
