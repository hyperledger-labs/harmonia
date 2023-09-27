package io.adhara.poc.amqp.codec;

abstract class AtomicElement<T> extends AbstractElement<T> {

	AtomicElement(Element parent, Element prev) {
		super(parent, prev);
	}

	@Override
	public Element child() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setChild(Element elt) {
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean canEnter() {
		return false;
	}

	@Override
	public Element checkChild(Element element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element addChild(Element element) {
		throw new UnsupportedOperationException();
	}

	@Override
	String startSymbol() {
		throw new UnsupportedOperationException();
	}

	@Override
	String stopSymbol() {
		throw new UnsupportedOperationException();
	}

}
