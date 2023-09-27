package io.adhara.poc.amqp.types;

import io.adhara.poc.amqp.codec.WritableBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class Symbol implements Comparable<Symbol>, CharSequence {
	private final String _underlying;
	private final byte[] _underlyingBytes;

	private static final ConcurrentHashMap<String, Symbol> _symbols = new ConcurrentHashMap<String, Symbol>(2048);

	private Symbol(String underlying) {
		_underlying = underlying;
		_underlyingBytes = underlying.getBytes(StandardCharsets.US_ASCII);
	}

	public int length() {
		return _underlying.length();
	}

	public int compareTo(Symbol o) {
		return _underlying.compareTo(o._underlying);
	}

	public char charAt(int index) {
		return _underlying.charAt(index);
	}

	public CharSequence subSequence(int beginIndex, int endIndex) {
		return _underlying.subSequence(beginIndex, endIndex);
	}

	@Override
	public String toString() {
		return _underlying;
	}

	@Override
	public int hashCode() {
		return _underlying.hashCode();
	}

	public static Symbol valueOf(String symbolVal) {
		return getSymbol(symbolVal);
	}

	public static Symbol getSymbol(String symbolVal) {
		if (symbolVal == null) {
			return null;
		}
		Symbol symbol = _symbols.get(symbolVal);
		if (symbol == null) {
			symbolVal = symbolVal.intern();
			symbol = new Symbol(symbolVal);
			Symbol existing;
			if ((existing = _symbols.putIfAbsent(symbolVal, symbol)) != null) {
				symbol = existing;
			}
		}
		return symbol;
	}

	public void writeTo(WritableBuffer buffer) {
		buffer.put(_underlyingBytes, 0, _underlyingBytes.length);
	}

	public void writeTo(ByteBuffer buffer) {
		buffer.put(_underlyingBytes, 0, _underlyingBytes.length);
	}
}
