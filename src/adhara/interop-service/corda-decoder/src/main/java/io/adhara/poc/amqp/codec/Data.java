package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.*;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Data {

	final class Factory {

		public static Data create() {
			return new DataImpl();
		}

	}


	enum DataType {
		NULL,
		BOOL,
		UBYTE,
		BYTE,
		USHORT,
		SHORT,
		UINT,
		INT,
		CHAR,
		ULONG,
		LONG,
		TIMESTAMP,
		FLOAT,
		DOUBLE,
		DECIMAL32,
		DECIMAL64,
		DECIMAL128,
		UUID,
		BINARY,
		STRING,
		SYMBOL,
		DESCRIBED,
		ARRAY,
		LIST,
		MAP
	}

	void free();

	void clear();

	long size();

	void rewind();

	DataType next();

	DataType prev();

	boolean enter();

	boolean exit();

	DataType type();

	Binary encode();

	long encodedSize();

	long encode(ByteBuffer buf);

	long decode(ByteBuffer buf);

	void putList();

	void putMap();

	void putArray(boolean described, DataType type);

	void putDescribed();

	void putNull();

	void putBoolean(boolean b);

	void putUnsignedByte(UnsignedByte ub);

	void putByte(byte b);

	void putUnsignedShort(UnsignedShort us);

	void putShort(short s);

	void putUnsignedInteger(UnsignedInteger ui);

	void putInt(int i);

	void putChar(int c);

	void putUnsignedLong(UnsignedLong ul);

	void putLong(long l);

	void putTimestamp(Date t);

	void putFloat(float f);

	void putDouble(double d);

	void putDecimal32(Decimal32 d);

	void putDecimal64(Decimal64 d);

	void putDecimal128(Decimal128 d);

	void putUUID(UUID u);

	void putBinary(Binary bytes);

	void putBinary(byte[] bytes);

	void putString(String string);

	void putSymbol(Symbol symbol);

	void putObject(Object o);

	void putJavaMap(Map<Object, Object> map);

	void putJavaList(List<Object> list);

	void putDescribedType(DescribedType dt);

	long getList();

	long getMap();

	long getArray();

	boolean isArrayDescribed();

	DataType getArrayType();

	boolean isDescribed();

	boolean isNull();

	boolean getBoolean();

	UnsignedByte getUnsignedByte();

	byte getByte();

	UnsignedShort getUnsignedShort();

	short getShort();

	UnsignedInteger getUnsignedInteger();

	int getInt();

	int getChar();

	UnsignedLong getUnsignedLong();

	long getLong();

	Date getTimestamp();

	float getFloat();

	double getDouble();

	Decimal32 getDecimal32();

	Decimal64 getDecimal64();

	Decimal128 getDecimal128();

	UUID getUUID();

	Binary getBinary();

	String getString();

	Symbol getSymbol();

	Object getObject();

	Map<Object, Object> getJavaMap();

	List<Object> getJavaList();

	Object[] getJavaArray();

	DescribedType getDescribedType();

	String format();
}
