package io.adhara.poc.amqp.codec;

import io.adhara.poc.amqp.types.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Decoder {
	interface ListProcessor<T> {
		T process(int count, Encoder encoder);
	}


	Boolean readBoolean();

	Boolean readBoolean(Boolean defaultVal);

	boolean readBoolean(boolean defaultVal);

	Byte readByte();

	Byte readByte(Byte defaultVal);

	byte readByte(byte defaultVal);

	Short readShort();

	Short readShort(Short defaultVal);

	short readShort(short defaultVal);

	Integer readInteger();

	Integer readInteger(Integer defaultVal);

	int readInteger(int defaultVal);

	Long readLong();

	Long readLong(Long defaultVal);

	long readLong(long defaultVal);

	UnsignedByte readUnsignedByte();

	UnsignedByte readUnsignedByte(UnsignedByte defaultVal);

	UnsignedShort readUnsignedShort();

	UnsignedShort readUnsignedShort(UnsignedShort defaultVal);

	UnsignedInteger readUnsignedInteger();

	UnsignedInteger readUnsignedInteger(UnsignedInteger defaultVal);

	UnsignedLong readUnsignedLong();

	UnsignedLong readUnsignedLong(UnsignedLong defaultVal);

	Character readCharacter();

	Character readCharacter(Character defaultVal);

	char readCharacter(char defaultVal);

	Float readFloat();

	Float readFloat(Float defaultVal);

	float readFloat(float defaultVal);

	Double readDouble();

	Double readDouble(Double defaultVal);

	double readDouble(double defaultVal);

	UUID readUUID();

	UUID readUUID(UUID defaultValue);

	Decimal32 readDecimal32();

	Decimal32 readDecimal32(Decimal32 defaultValue);

	Decimal64 readDecimal64();

	Decimal64 readDecimal64(Decimal64 defaultValue);

	Decimal128 readDecimal128();

	Decimal128 readDecimal128(Decimal128 defaultValue);

	Date readTimestamp();

	Date readTimestamp(Date defaultValue);

	Binary readBinary();

	Binary readBinary(Binary defaultValue);

	Symbol readSymbol();

	Symbol readSymbol(Symbol defaultValue);

	String readString();

	String readString(String defaultValue);

	List readList();

	<T> void readList(ListProcessor<T> processor);

	Map readMap();

	<T> T[] readArray(Class<T> clazz);

	Object[] readArray();

	boolean[] readBooleanArray();

	byte[] readByteArray();

	short[] readShortArray();

	int[] readIntegerArray();

	long[] readLongArray();

	float[] readFloatArray();

	double[] readDoubleArray();

	char[] readCharacterArray();

	<T> T[] readMultiple(Class<T> clazz);

	Object[] readMultiple();

	byte[] readByteMultiple();

	short[] readShortMultiple();

	int[] readIntegerMultiple();

	long[] readLongMultiple();

	float[] readFloatMultiple();

	double[] readDoubleMultiple();

	char[] readCharacterMultiple();

	Object readObject();

	Object readObject(Object defaultValue);

	void register(final Object descriptor, final DescribedTypeConstructor dtc);

	void register(final Object descriptor, final FastPathDescribedTypeConstructor<?> dtc);

}
