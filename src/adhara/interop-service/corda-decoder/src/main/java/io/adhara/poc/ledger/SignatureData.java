package io.adhara.poc.ledger;

import io.adhara.poc.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;

@Data
@AllArgsConstructor
public class SignatureData {
	private static final Logger logger = LoggerFactory.getLogger(SignatureData.class);
	private final String by; 		// Hex-encoded public key (44 bytes) 88 characters -> compress to 32 bytes
	private final String bytes; // Hex-encoded signature (64 bytes) 128 characters -> split into R and S of 32 bytes each
	private final String data;  // Hex-encoded data (999 bytes) 1998 characters -> add as element in encoded info
	private final String meta;  // Hex-encoded meta data
	private byte[] compressedKey;

	private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;

	public static final int SECP256K1SignatureSchemeId = 2;
	public static final int SECP256R1SignatureSchemeId = 3;
	public static final int ED25519SignatureSchemeId = 4;

	public static ECPublicKey fromUncompressedPoint(final byte[] uncompressedPoint, final ECParameterSpec params)	throws Exception {
		int offset = 0;
		if (uncompressedPoint[offset++] != UNCOMPRESSED_POINT_INDICATOR) {
			throw new IllegalArgumentException("Invalid uncompressedPoint encoding, no uncompressed point indicator");
		}
		int keySizeBytes = (params.getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

		if (uncompressedPoint.length != 1 + 2 * keySizeBytes) {
			throw new IllegalArgumentException("Invalid uncompressedPoint encoding, not the correct size");
		}

		final BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
		offset += keySizeBytes;
		final BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
		final ECPoint w = new ECPoint(x, y);
		final ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(w, params);
		final KeyFactory keyFactory = KeyFactory.getInstance("EC");
		return (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
	}

	public static byte[] toUncompressedPoint(final ECPublicKey publicKey) {
		int keySizeBytes = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1)	/ Byte.SIZE;
		final byte[] uncompressedPoint = new byte[1 + 2 * keySizeBytes];
		int offset = 0;
		uncompressedPoint[offset++] = 0x04;

		final byte[] x = publicKey.getW().getAffineX().toByteArray();
		if (x.length <= keySizeBytes) {
			System.arraycopy(x, 0, uncompressedPoint, offset + keySizeBytes	- x.length, x.length);
		} else if (x.length == keySizeBytes + 1 && x[0] == 0) {
			System.arraycopy(x, 1, uncompressedPoint, offset, keySizeBytes);
		} else {
			throw new IllegalStateException("The x value is too large");
		}
		offset += keySizeBytes;

		final byte[] y = publicKey.getW().getAffineY().toByteArray();
		if (y.length <= keySizeBytes) {
			System.arraycopy(y, 0, uncompressedPoint, offset + keySizeBytes	- y.length, y.length);
		} else if (y.length == keySizeBytes + 1 && y[0] == 0) {
			System.arraycopy(y, 1, uncompressedPoint, offset, keySizeBytes);
		} else {
			throw new IllegalStateException("The y value is too large");
		}

		return uncompressedPoint;
	}

	public byte[] getPublicKey() throws InvalidKeySpecException {
		int scheme = getSignatureScheme();
		byte[] key = getCompressedPublicKey();
		switch (scheme) {
			case SECP256K1SignatureSchemeId:
			case SECP256R1SignatureSchemeId: {
				byte[] k = new byte[32];
				System.arraycopy(key, 1, k, 0, 32);
				return k;
			}
		}
		return key;
	}

	private byte[] getCompressedPublicKey() throws InvalidKeySpecException {
		if (compressedKey == null) {
			final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Utils.fromHexString(by));
			int scheme = getSignatureScheme();
			try {
				switch (scheme) {
					case SECP256K1SignatureSchemeId:
					case SECP256R1SignatureSchemeId: {
						KeyFactory kf = KeyFactory.getInstance("EC");
						PublicKey key = kf.generatePublic(keySpec);
						ECPublicKey k = (ECPublicKey) key;
						byte[] xArray = k.getW().getAffineX().toByteArray();
						compressedKey = new byte[33];
						int parity = k.getW().getAffineY().and(BigInteger.valueOf(1)).or(BigInteger.valueOf(2)).intValue();
						compressedKey[0] = (byte) parity;
						System.arraycopy(xArray, 0, compressedKey, 1, 32);
					} break;
					case ED25519SignatureSchemeId: {
						EdDSAPublicKey key = new EdDSAPublicKey(keySpec);
						compressedKey = key.getAbyte();
					} break;
					default:
						throw new InvalidKeySpecException();
				}
			} catch (Exception e) {
				throw new InvalidKeySpecException();
			}
		}
		return compressedKey;
	}

	public static byte[] getCompressedPublicKey(String by, int schemaNumber) throws InvalidKeySpecException {
		try {
			final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Utils.fromHexString(by));
			switch (schemaNumber) {
				case SECP256K1SignatureSchemeId:
				case SECP256R1SignatureSchemeId: {
					KeyFactory kf = KeyFactory.getInstance("EC");
					PublicKey key = kf.generatePublic(keySpec);
					ECPublicKey k = (ECPublicKey) key;
					byte[] xArray = k.getW().getAffineX().toByteArray();
					byte[] compressedKey = new byte[33];
					int parity = k.getW().getAffineY().and(BigInteger.valueOf(1)).or(BigInteger.valueOf(2)).intValue();
					compressedKey[0] = (byte) parity;
					System.arraycopy(xArray, 0, compressedKey, 1, 32);
					return compressedKey;
				}
				case ED25519SignatureSchemeId: {
					EdDSAPublicKey key = new EdDSAPublicKey(keySpec);
					return key.getAbyte();
				}
				default:
					throw new InvalidKeySpecException();
			}
		} catch (Exception e) {
			throw new InvalidKeySpecException();
		}
	}

	public byte[] getSignatureR() {
		int scheme = getSignatureScheme();
		switch (scheme) {
			case SECP256K1SignatureSchemeId:
			case SECP256R1SignatureSchemeId: {
				int length = 2*Integer.parseInt(bytes.substring(6, 8), 16);
				String sig = bytes.substring(8);
				String r = sig.substring(0, length);
				return Utils.fromHexString(Utils.removeLeadingZeroes(r));
			}
			case ED25519SignatureSchemeId: {
				byte[] array = Utils.fromHexString(bytes);
				byte[] r = new byte[32];
				for (int i = 0; i < 32; i++)
					r[i] = array[i];
				return r;
			}
		}
		return null;
	}

	public byte[] getSignatureS() {
		int scheme = getSignatureScheme();
		switch (scheme) {
			case SECP256K1SignatureSchemeId:
			case SECP256R1SignatureSchemeId: {
				int length = 2*Integer.parseInt(bytes.substring(6, 8), 16);
				String sig = bytes.substring(8);
				String s = sig.substring(length+4);
				return Utils.fromHexString(Utils.removeLeadingZeroes(s));
			}
			case ED25519SignatureSchemeId: {
				byte[] array = Utils.fromHexString(bytes);
				byte[] s = new byte[32];
				for (int i = 32; i < 64; i++)
					s[i-32] = array[i];
				return s;
			}
		}
		return null;
	}

	public byte[] getSignatureV() {
		byte[] v = new byte[1];
		int scheme = getSignatureScheme();
		switch (scheme) {
			case SECP256K1SignatureSchemeId:
			case SECP256R1SignatureSchemeId: {
				try {
					byte[] key = getCompressedPublicKey();
					v[0] = key[0];
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			} break;
			case ED25519SignatureSchemeId: {
				v[0] = 0x00;
			} break;
		}
		return v;
	}

	public int getSignatureScheme() {
		String schema = meta.substring(8,16);
		BigInteger bi = new BigInteger(schema, 16);
		return bi.intValue();
	}

	public byte[] getEncodedInfo() {
		return Utils.fromHexString(data);
	}
}

