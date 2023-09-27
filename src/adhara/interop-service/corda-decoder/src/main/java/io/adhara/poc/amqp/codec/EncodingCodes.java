package io.adhara.poc.amqp.codec;

public interface EncodingCodes {
	byte DESCRIBED_TYPE_INDICATOR = (byte) 0x00;

	byte NULL = (byte) 0x40;

	byte BOOLEAN = (byte) 0x56;
	byte BOOLEAN_TRUE = (byte) 0x41;
	byte BOOLEAN_FALSE = (byte) 0x42;

	byte UBYTE = (byte) 0x50;

	byte USHORT = (byte) 0x60;

	byte UINT = (byte) 0x70;
	byte SMALLUINT = (byte) 0x52;
	byte UINT0 = (byte) 0x43;

	byte ULONG = (byte) 0x80;
	byte SMALLULONG = (byte) 0x53;
	byte ULONG0 = (byte) 0x44;

	byte BYTE = (byte) 0x51;

	byte SHORT = (byte) 0x61;

	byte INT = (byte) 0x71;
	byte SMALLINT = (byte) 0x54;

	byte LONG = (byte) 0x81;
	byte SMALLLONG = (byte) 0x55;

	byte FLOAT = (byte) 0x72;

	byte DOUBLE = (byte) 0x82;

	byte DECIMAL32 = (byte) 0x74;

	byte DECIMAL64 = (byte) 0x84;

	byte DECIMAL128 = (byte) 0x94;

	byte CHAR = (byte) 0x73;

	byte TIMESTAMP = (byte) 0x83;

	byte UUID = (byte) 0x98;

	byte VBIN8 = (byte) 0xa0;
	byte VBIN32 = (byte) 0xb0;

	byte STR8 = (byte) 0xa1;
	byte STR32 = (byte) 0xb1;

	byte SYM8 = (byte) 0xa3;
	byte SYM32 = (byte) 0xb3;

	byte LIST0 = (byte) 0x45;
	byte LIST8 = (byte) 0xc0;
	byte LIST32 = (byte) 0xd0;

	byte MAP8 = (byte) 0xc1;
	byte MAP32 = (byte) 0xd1;

	byte ARRAY8 = (byte) 0xe0;
	byte ARRAY32 = (byte) 0xf0;

	static String toString(byte encoding) {
		switch (encoding) {
			case DESCRIBED_TYPE_INDICATOR:
				return "DESCRIBED_TYPE_INDICATOR:0x00";
			case NULL:
				return "NULL:0x40";
			case BOOLEAN:
				return "BOOLEAN:0x56";
			case BOOLEAN_TRUE:
				return "BOOLEAN_TRUE:0x41";
			case BOOLEAN_FALSE:
				return "BOOLEAN_FALSE:0x42";
			case UBYTE:
				return "UBYTE:0x50";
			case USHORT:
				return "USHORT:0x60";
			case UINT:
				return "UINT:0x70";
			case SMALLUINT:
				return "SMALLUINT:0x52";
			case UINT0:
				return "UINT0:0x43";
			case ULONG:
				return "ULONG:0x80";
			case SMALLULONG:
				return "SMALLULONG:0x53";
			case ULONG0:
				return "ULONG0:0x44";
			case BYTE:
				return "BYTE:0x51";
			case SHORT:
				return "SHORT:0x61";
			case INT:
				return "INT:0x71";
			case SMALLINT:
				return "SMALLINT:0x54";
			case LONG:
				return "LONG:0x81";
			case SMALLLONG:
				return "SMALLLONG:0x55";
			case FLOAT:
				return "FLOAT:0x72";
			case DOUBLE:
				return "DOUBLE:0x82";
			case DECIMAL32:
				return "DECIMAL32:0x74";
			case DECIMAL64:
				return "DECIMAL64:0x84";
			case DECIMAL128:
				return "DECIMAL128:0x94";
			case CHAR:
				return "CHAR:0x73";
			case TIMESTAMP:
				return "TIMESTAMP:0x83";
			case UUID:
				return "UUID:0x98";
			case VBIN8:
				return "VBIN8:0xa0";
			case VBIN32:
				return "VBIN32:0xb0";
			case STR8:
				return "STR8:0xa1";
			case STR32:
				return "STR32:0xb1";
			case SYM8:
				return "SYM8:0xa3";
			case SYM32:
				return "SYM32:0xb3";
			case LIST0:
				return "LIST0:0x45";
			case LIST8:
				return "LIST8:0xc0";
			case LIST32:
				return "LIST32:0xd0";
			case MAP8:
				return "MAP8:0xc1";
			case MAP32:
				return "MAP32:0xd1";
			case ARRAY8:
				return "ARRAY8:0xe0";
			case ARRAY32:
				return "ARRAY32:0xf0";
			default:
				return "Unknown-Type:" + String.format("0x%02X", encoding);
		}
	}
}
