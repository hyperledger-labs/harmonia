package io.adhara.poc.ledger;

import io.adhara.poc.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static io.adhara.poc.ledger.SignatureData.toUncompressedPoint;

public class KeyData {
	private static final Logger logger = LoggerFactory.getLogger(KeyData.class);

	public static String getCompressedPublicKey(int scheme, String uncompressedKey) {
		String compressedKey = "";
		final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Utils.fromHexString(uncompressedKey));
		try {
			switch (scheme) {
				case SignatureData.SECP256K1SignatureSchemeId:
				case SignatureData.SECP256R1SignatureSchemeId: {
					KeyFactory kf = KeyFactory.getInstance("EC");
					PublicKey key = kf.generatePublic(keySpec);
					ECPublicKey k = (ECPublicKey) key;
					logger.debug("Encoded:   " + Utils.toHexString(k.getEncoded()));
					logger.debug("Decoded:   " + Utils.toHexString(toUncompressedPoint(k)));
					byte[] xArray = k.getW().getAffineX().toByteArray();
					byte[] nKey = new byte[33];
					int parity = k.getW().getAffineY().and(BigInteger.valueOf(1)).or(BigInteger.valueOf(2)).intValue();
					nKey[0] = (byte) parity;
					System.arraycopy(xArray, 0, nKey, 1, 32);
					logger.debug("Compressed:" + Utils.toHexString(nKey));
					compressedKey = Utils.toHexString(nKey);
				} break;
				case SignatureData.ED25519SignatureSchemeId: {
					EdDSAPublicKey key = new EdDSAPublicKey(keySpec);
					compressedKey = Utils.toHexString(key.getAbyte());
				} break;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return compressedKey;
	}

}

