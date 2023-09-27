package io.adhara.poc.rest.controller;

import io.adhara.poc.ledger.Decoder;
import io.adhara.poc.ledger.Extracted;
import io.adhara.poc.ledger.SignatureProof;
import io.adhara.poc.rest.model.LedgerTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/constructProofFromTransaction")
public class ConstructProofFromTransactionController {
	private static final Logger logger = LoggerFactory.getLogger(ConstructProofFromTransactionController.class);

	@PostMapping(path = "/", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> constructProofFromTransaction(
		@RequestHeader(name = "X-COM-PERSIST", required = false) String headerPersist,
		@RequestHeader(name = "X-COM-LOCATION", required = false, defaultValue = "ASIA") String headerLocation,
		@RequestBody LedgerTransaction ledgerTransaction)
		throws Exception {

		Map<String, Object> result = constructFunctionCallData(
			ledgerTransaction.getBlockchainId(),
			ledgerTransaction.getContractAddress(),
			ledgerTransaction.getFunctionName(),
			ledgerTransaction.getEncodedInfo(),
			Boolean.valueOf(ledgerTransaction.getWithHiddenAuthParams()),
			ledgerTransaction.getAuthBlockchainId(),
			ledgerTransaction.getAuthContractAddress());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

		return ResponseEntity.created(location).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	private static int EVENT_DATA_CONTRACT = 0;
	private static int EVENT_DATA_COMMAND = 1;
	private static int EVENT_DATA_TRADE_ID = 2;
	private static int EVENT_DATA_AMOUNT = 3;
	private static int EVENT_DATA_CURRENCY = 4;

	private static int PARTY_ISSUER_INDEX = 0;   // O=PartyA, L=London, C=GB
	private static int PARTY_OWNER_INDEX = 1;    // O=PartyB, L=New York, C=US

	private static int PARTY_SENDER_INDEX = 1;   // O=PartyA, L=London, C=GB
	private static int PARTY_RECEIVER_INDEX = 0; // O=PartyB, L=New York, C=US

	private Map<String, Object> constructFunctionCallData(String blockchainId, String controlContractAddress, String functionName, String signedTransaction, boolean withHiddenAuthParams, String authBlockchainId, String authContractAddress) {
		logger.info("Constructing function call data for system ["+blockchainId+"] to remotely call function ["+functionName+"] in contract ["+controlContractAddress+"]"+(withHiddenAuthParams ? " with auth params" : ""));
		HashMap<String, Object> result = new HashMap<>();
		try {
			HashMap<String, String> export = new HashMap<>();
			List<Extracted<String, Object>> components = new ArrayList<>();
			Decoder.parseCordaSerialization(signedTransaction, 0, "", components);
			SignatureProof proof = new SignatureProof(signedTransaction, components);
			String raw = proof.getRaw();
			if (raw == null) {
				logger.error("Error: Fail to extract wire transaction");
				return null;
			}
			String command = proof.getCommand();
			List<String> parties = proof.getParties();
			String contract = proof.getContract();
			String sender = null;
			String receiver = null;
			switch (contract) {
				case "net.corda.samples.example.contracts.DCRContract": {
					sender = PARTY_OWNER_INDEX < parties.size() ? parties.get(PARTY_OWNER_INDEX) : null;
					sender = sender != null ? Base64.getEncoder().encodeToString(sender.getBytes()) : null;
					receiver = PARTY_ISSUER_INDEX < parties.size() ? parties.get(PARTY_ISSUER_INDEX) : null;
					receiver = receiver != null ? Base64.getEncoder().encodeToString(receiver.getBytes()) : null;
				} break;
				case "net.corda.samples.example.contracts.XVPContract": {
					receiver = PARTY_SENDER_INDEX < parties.size() ? parties.get(PARTY_SENDER_INDEX) : null;
					receiver = receiver != null ? Base64.getEncoder().encodeToString(receiver.getBytes()) : null;
					sender = PARTY_RECEIVER_INDEX < parties.size() ? parties.get(PARTY_RECEIVER_INDEX) : null;
					sender = sender != null ? Base64.getEncoder().encodeToString(sender.getBytes()) : null;
				} break;
				default: {
					logger.error("Error: Contract [" +contract+ "] is not recognized");
					return null;
				}
			}
			if (sender == null) {
				logger.error("Error: Fail to extract sender from wire transaction");
				return null;
			}
			if (receiver == null) {
				logger.error("Error: Fail to extract receiver from wire transaction");
				return null;
			}
			BigInteger chainId = new BigInteger(blockchainId);
			String controlContract = controlContractAddress.replaceAll("\"", "");
			String authContract = authContractAddress != null ? authContractAddress.replaceAll("\"", "") : "";
			BigInteger authId = authBlockchainId != null && !authBlockchainId.isEmpty() ? new BigInteger(authBlockchainId) : BigInteger.ZERO;
			String tradeId = proof.getId();
			String functionCallData = "";
			Function function = null;
			String eventSig = "0x0000000000000000000000000000000000000000000000000000000000000000";
			if (functionName.equals("requestFollowLeg")) {
				BigDecimal amount = new BigDecimal(proof.getAmount());
				eventSig = "0xc6755b7c00000000000000000000000000000000000000000000000000000000";
				function = new Function(
					functionName,
					Arrays.<Type>asList(
						new Utf8String(tradeId),             // tradeId
						new Utf8String(sender),              // senderId
						new Utf8String(receiver),            // receiverId
						new Address(controlContract),        // controlContract
						new Uint256(chainId),                // sourceBlockchainId
						new Uint256(amount.unscaledValue())),// amount
					Arrays.<TypeReference<?>>asList(
						new TypeReference<Bool>() {
						})
				);
			} else if (functionName.equals("performCancellation")) {
				eventSig = "0xca2f045200000000000000000000000000000000000000000000000000000000";
				function = new Function(
					functionName,
					Arrays.<Type>asList(
						new Utf8String(tradeId),   // tradeId
						new Utf8String(sender),    // senderId
						new Utf8String(receiver)), // receiverId
					Arrays.<TypeReference<?>>asList(
						new TypeReference<Bool>() {
						})
				);
			}
			functionCallData = DefaultFunctionEncoder.encode(function);
			if (withHiddenAuthParams) {
				String authParams = DefaultFunctionEncoder.encodeConstructorPacked(
					Arrays.<Type>asList(
						new Uint256(authId),       // authBlockchainId
						new Address(authContract)) // authContract
				);
				functionCallData = functionCallData + authParams;
			}
			String eventData = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new DynamicStruct(
						new DynamicBytes(Numeric.hexStringToByteArray(functionCallData)),     // callParameters
						new Utf8String("SHA-256"),                                            // hashAlgorithm
						new Bytes32(Numeric.hexStringToByteArray("0x"+proof.getSalt())),      // privacySalt
						new DynamicStruct(
							new Uint8(SignatureProof.COMPONENT_GROUP_OUTPUTS),                  // groupIndex
							new Uint8(0),                                                       // internalIndex
							new DynamicBytes(Numeric.hexStringToByteArray("0x"+proof.getRaw())) // encodedBytes
						)
					)
				));
			String encodedInfo = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new Uint256(chainId),                                     // blockchainId
					new Address(controlContract),                             // controlContract
					new Bytes32(Numeric.hexStringToByteArray(eventSig)),      // eventSig
					new DynamicBytes(Numeric.hexStringToByteArray(eventData)) // eventData
				)
			);
			SignatureProof.Signatures signatures = proof.getSignatures();
			List<Type> sigs = new ArrayList<>();
			for (SignatureProof.Signature sig : signatures.getSignatures().getValue()) {
				sigs.add(sig.asDynamicStruct());
			}
			List<Uint8> flags = new ArrayList<>();
			byte[] flgs = proof.getProof().getFlags();
			for (int i=0; i<flgs.length; i++) {
				flags.add(new Uint8(flgs[i]));
			}
			String signatureOrProof = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new DynamicStruct(
						new DynamicStruct(
							new Bytes32(proof.getRoot().getBytes()),
							new DynamicArray<>(Bytes32.class, Arrays.stream(proof.getProof().getProof()).map(witness -> new Bytes32(witness.getBytes())).collect(Collectors.toList())),
							new DynamicArray<>(Uint8.class, flags),
							new DynamicArray<>(Bytes32.class, Arrays.stream(proof.getProof().getLeaves()).map(leaf -> new Bytes32(leaf.getBytes())).collect(Collectors.toList()))
						),
					  new DynamicArray<>(Type.class, sigs))
				  )
			);

			export.put("blockchainId", chainId.toString(16));
			export.put("eventSig", eventSig);
			export.put("encodedInfo", encodedInfo);
			export.put("signatureOrProof", signatureOrProof);

			result.put("proof", export);
			result.put("event", command);
			result.put("tradeId", tradeId);
			result.put("fromAccount", sender);
			result.put("toAccount", receiver);
			result.put("foreignNotional", proof.getAmount());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
