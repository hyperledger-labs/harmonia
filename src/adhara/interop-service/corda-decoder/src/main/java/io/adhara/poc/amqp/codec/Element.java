package io.adhara.poc.amqp.codec;

import java.nio.ByteBuffer;

interface Element<T> {
	int size();

	T getValue();

	Data.DataType getDataType();

	int encode(ByteBuffer b);

	Element next();

	Element prev();

	Element child();

	Element parent();

	void setNext(Element elt);

	void setPrev(Element elt);

	void setParent(Element elt);

	void setChild(Element elt);

	Element replaceWith(Element elt);

	Element addChild(Element element);

	Element checkChild(Element element);

	boolean canEnter();

	void render(StringBuilder sb);

}
