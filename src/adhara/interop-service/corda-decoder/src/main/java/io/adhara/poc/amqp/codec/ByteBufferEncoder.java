package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

public interface ByteBufferEncoder extends Encoder {
	void setByteBuffer(ByteBuffer buf);
}
