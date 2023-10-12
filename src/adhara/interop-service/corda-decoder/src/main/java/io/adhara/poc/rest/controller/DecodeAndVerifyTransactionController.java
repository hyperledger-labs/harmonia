package io.adhara.poc.rest.controller;

import io.adhara.poc.ledger.*;
import io.adhara.poc.rest.model.LedgerTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/decodeAndVerifyTransaction")
public class DecodeAndVerifyTransactionController {
	private static final Logger logger = LoggerFactory.getLogger(DecodeAndVerifyTransactionController.class);

	@PostMapping(path = "/", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> decodeAndVerifyEvent(
		@RequestBody LedgerTransaction ledgerTransaction)
		throws Exception {

		HashMap<String, Object> result = new HashMap<>();
		try {
			List<Extracted<String, Object>> components = new ArrayList<>();
			Decoder.parseCordaSerialization(ledgerTransaction.getEncodedInfo(), 0, "", components);
			SignatureProof proof = new SignatureProof(ledgerTransaction.getEncodedInfo(), components);
			result.put("result", proof.verify());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
		return ResponseEntity.created(location).contentType(MediaType.APPLICATION_JSON).body(result);
	}
}
