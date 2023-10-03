package io.adhara.poc.utils;

import io.adhara.poc.ledger.SecureHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {

  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  public static byte [] fromHexString(final String s) {
    if (s == null || (s.length () % 2) == 1)
      throw new IllegalArgumentException ();
    final char [] chars = s.toCharArray ();
    final int len = chars.length;
    final byte [] data = new byte [len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit (chars[i], 16) << 4) + Character.digit (chars[i + 1], 16));
    }
    return data;
  }

  private static final String HEXES = "0123456789ABCDEF";
  public static String toHexString(byte[] raw) {
    if (raw == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder( 2 * raw.length );
    for (final byte b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4))
        .append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }

  public static boolean isPow2(int num) {
    return num > 0 && ((num & (num - 1)) == 0);
  }

  public static boolean isOdd(int num) {
    return (num % 2 != 0);
  }

  public static byte[] concatBytes(byte[] left, byte[] right) {
    return ByteBuffer.allocate(left.length+right.length).put(left).put(right).array();
  }

  public static String removeLeadingZeroes(String s) {
    int index;
    for (index = 0; index < s.length(); index++) {
      if (s.charAt(index) != '0') {
        break;
      }
    }
    if (index % 2 == 1)
      index--;
    return s.substring(index);
  }

  // Base58 utils with logic is copied from bitcoinj.
  protected static final char[] BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
  protected static final char ENCODED_ZERO = BASE58_CHARS[0];
  protected static final int[] INDEXES = new int[128];
  protected static final int CHECKSUM_LEN = 4;

  static {
    Arrays.fill(INDEXES, -1);
    for (int i = 0; i < BASE58_CHARS.length; i++) {
      INDEXES[BASE58_CHARS[i]] = i;
    }
  }

  // Encodes the given bytes as a base58 string (no checksum is appended).
  public static String toBase58String(final byte[] input) {
    if (null == input || input.length == 0) {
      return "";
    }
    final char[] encoded = new char[input.length * 2];
    final byte[] copy = Arrays.copyOf(input, input.length); // Since we modify it in-place.
    // Count leading zeros.
    int zeros = 0;
    while (zeros < input.length && input[zeros] == 0) {
      ++zeros;
    }
    int inputIndex = zeros;
    int outputIndex = encoded.length;
    while (inputIndex < copy.length) {
      encoded[--outputIndex] = BASE58_CHARS[divMod(copy, inputIndex, 256, 58)];
      if (copy[inputIndex] == 0) {
        ++inputIndex; // Optimization to skip leading zeros.
      }
    }
    // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
    while (outputIndex < encoded.length && encoded[outputIndex] == ENCODED_ZERO) {
      ++outputIndex;
    }
    while (--zeros >= 0) {
      encoded[--outputIndex] = ENCODED_ZERO;
    }
    // Return encoded string (including encoded leading zeros).
    return new String(encoded, outputIndex, encoded.length - outputIndex);
  }

  // Encode byte array to base58-encoded string with checksum.
  public static String toBase58StringWithCheck(final byte[] rawData) {
    if (null == rawData || rawData.length == 0) {
      return "";
    }
    final byte[] checkSum = calculateCheckSum(rawData);
    final byte[] rawTotal = new byte[rawData.length + CHECKSUM_LEN];
    System.arraycopy(rawData, 0, rawTotal, 0, rawData.length);
    System.arraycopy(checkSum, 0, rawTotal, rawTotal.length - CHECKSUM_LEN, CHECKSUM_LEN);
    return toBase58String(rawTotal);
  }

  // Decodes the given base58 string into the original data bytes.
  public static byte[] fromBase58String(final String input) throws IOException {
    if (null == input || input.length() == 0) {
      return new byte[0];
    }
    // Convert the base58-encoded ASCII chars to a base58 byte sequence (base58 digits).
    final byte[] input58 = new byte[input.length()];
    for (int i = 0; i < input.length(); ++i) {
      char c = input.charAt(i);
      int digit = c < 128 ? INDEXES[c] : -1;
      if (digit < 0) {
        throw new UnsupportedEncodingException("Base58 decoding failed: " + digit + " at " + i);
      }
      input58[i] = (byte) digit;
    }
    // Count leading zeros.
    int zeros = 0;
    while (zeros < input58.length && input58[zeros] == 0) {
      ++zeros;
    }
    // Convert base-58 digits to base-256 digits.
    byte[] decoded = new byte[input.length()];
    int outputStart = decoded.length;
    for (int inputStart = zeros; inputStart < input58.length;) {
      decoded[--outputStart] = divMod(input58, inputStart, 58, 256);
      if (input58[inputStart] == 0) {
        ++inputStart; // optimization - skip leading zeros
      }
    }
    // Ignore extra leading zeroes that were added during the calculation.
    while (outputStart < decoded.length && decoded[outputStart] == 0) {
      ++outputStart;
    }
    // Return decoded data (including original number of leading zeros).
    return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
  }

  // Decode base58-encoded string with checksum to byte array.
  public static byte[] fromBase58StringWithCheck(final String encoded) throws IOException {
    if (null == encoded || encoded.length() == 0) {
      return new byte[0];
    }
    final byte[] rawTotal = fromBase58String(encoded);
    final byte[] rawData = Arrays.copyOfRange(rawTotal, 0, rawTotal.length - CHECKSUM_LEN);
    final byte[] checkSum =
      Arrays.copyOfRange(rawTotal, rawTotal.length - CHECKSUM_LEN, rawTotal.length);
    final byte[] calculatedCheckSum = calculateCheckSum(rawData);
    if (!Arrays.equals(checkSum, calculatedCheckSum)) {
      logger.info("Checksum mismatch - Input: {}, Computed: {}", checkSum, calculatedCheckSum);
      throw new IllegalArgumentException("Checksum is mismatch");
    }
    return rawData;
  }

  // Divides a number, represented as an array of bytes each containing a single digit in the specified base, by the given divisor. The given number is modified in-place to contain the quotient, and the return value is the remainder.
  private static byte divMod(byte[] number, int firstDigit, int base, int divisor) {
    // Long division which accounts for the base of the input digits
    int remainder = 0;
    for (int i = firstDigit; i < number.length; i++) {
      int digit = (int) number[i] & 0xff;
      int temp = remainder * base + digit;
      number[i] = (byte) (temp / divisor);
      remainder = temp % divisor;
    }
    return (byte) remainder;
  }

  // Calculate checksum.
  protected static byte[] calculateCheckSum(final byte[] rawData) {
    final SecureHash doubleHashed = SecureHash.getDoubleHashFor(rawData, SecureHash.SHA_256);
    return Arrays.copyOfRange(doubleHashed.getBytes(), 0, CHECKSUM_LEN);
  }

}


