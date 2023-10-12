package io.adhara.poc.rest.controller;

import io.adhara.poc.ledger.*;
import io.adhara.poc.rest.model.LedgerSignature;
import io.adhara.poc.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigInteger;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping(path = "/decodeAndVerifySignature")
public class DecodeAndVerifySignatureController {
	private static final Logger logger = LoggerFactory.getLogger(DecodeAndVerifySignatureController.class);

	@PostMapping(path = "/", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> decodeAndVerifySignature(
		@RequestBody LedgerSignature ledgerSignature)
		throws Exception {

		HashMap<String, Object> result = new HashMap<>();
		try {
			String by = ledgerSignature.getEncodedKey();
			String bytes = ledgerSignature.getEncodedSignature();
			Integer platformVersion = Integer.valueOf(ledgerSignature.getPlatformVersion());
			Integer schemaNumber = Integer.valueOf(ledgerSignature.getSchemaNumber());
			byte[] compressedKey = SignatureData.getCompressedPublicKey(ledgerSignature.getEncodedKey(), schemaNumber);
			logger.debug("Verifying signature for public key ["+String.format("%064x", new BigInteger(1, compressedKey)).toUpperCase()+"]");
			SignedData signedData = new SignedData(by, bytes, ledgerSignature.getPartialMerkleRoot(), platformVersion, schemaNumber);
			byte[] tradeId = Utils.fromHexString(ledgerSignature.getEncodedId());
			result.put("result", SignatureProof.verify(new SecureHash(tradeId, SecureHash.SHA_256), signedData));
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

		return ResponseEntity.created(location).contentType(MediaType.APPLICATION_JSON).body(result);
	}
}
