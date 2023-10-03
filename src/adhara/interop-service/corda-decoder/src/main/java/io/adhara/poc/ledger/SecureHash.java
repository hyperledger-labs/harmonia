package io.adhara.poc.ledger;

import io.adhara.poc.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

@Data
@AllArgsConstructor
public class SecureHash {

  public static final String SHA_256 = "SHA-256";
  public static final String SHA_512 = "SHA-512";
  private static final SecureHash NULL = new SecureHash(null, null);
  private static final Logger logger = LoggerFactory.getLogger(SecureHash.class);

  private final byte[] bytes;
  private final String algorithm;

  @NotNull
  public static SecureHash getZero(String algorithm) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      int digestLength = digest.getDigestLength();
      byte[] bytes = new byte[digestLength];
      Arrays.fill(bytes, (byte)0);
      return new SecureHash(bytes, algorithm);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
    }
    return NULL;
  }

  @NotNull
  public static SecureHash getOnes(String algorithm) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      int digestLength = digest.getDigestLength();
      byte[] bytes = new byte[digestLength];
      Arrays.fill(bytes, (byte)0xFF);
      return new SecureHash(bytes, algorithm);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
    }
    return NULL;
  }

  public boolean isZero() {
    return IntStream.range(0, bytes.length).parallel().allMatch(i -> bytes[i] == 0);
  }

  @NotNull
  public SecureHash rehash() {
    return getHashFor(bytes, algorithm);
  }

  // Append a second hash value to this hash value, and then compute the hash of the result using the specified algorithm.
  public SecureHash concatenate(String concatAlgorithm, SecureHash other) {
    if (!Objects.equals(algorithm, other.algorithm)) {
      logger.error("Cannot concatenate $algorithm with ${other.algorithm}");
      return null;
    }
    byte[] concatBytes = Utils.concatBytes(this.bytes, other.bytes);
    return getHashFor(concatBytes, concatAlgorithm);
  }

  @NotNull
  public static MessageDigest getDigestFor(String algorithm) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      digest.reset();
      return digest;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  public static SecureHash getHashFor(byte[] bytes, String algorithm) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      byte[] hashed = digest.digest(bytes);
      return new SecureHash(hashed, algorithm);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
    }
    return NULL;
  }

  @NotNull
  public static SecureHash getDoubleHashFor(byte[] bytes, String algorithm) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      byte[] hashed = digest.digest(bytes);
      hashed = digest.digest(hashed);
      return new SecureHash(hashed, algorithm);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
    }
    return NULL;
  }

  @Override
  public String toString() {
    return Utils.toHexString(bytes);
  }

  @Override
  public boolean equals(Object other) {
    return Arrays.equals(bytes, ((SecureHash)other).getBytes());
  }

}
