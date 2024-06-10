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
import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping(path = "/constructProofFromSignature")
public class ConstructProofFromSignatureController {
	private static final Logger logger = LoggerFactory.getLogger(ConstructProofFromSignatureController.class);

	@PostMapping(path = "/", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> constructProofFromSignature(
		@RequestBody LedgerSignature ledgerSignature)
		throws Exception {

		String by = ledgerSignature.getEncodedKey();
		String bytes = ledgerSignature.getEncodedSignature();
		Integer platformVersion = Integer.valueOf(ledgerSignature.getPlatformVersion());
		Integer schemaNumber = Integer.valueOf(ledgerSignature.getSchemaNumber());
		Boolean withHiddenParams = Boolean.valueOf(ledgerSignature.getWithHiddenAuthParams());
		byte[] compressedKey = SignatureData.getCompressedPublicKey(ledgerSignature.getEncodedKey(), schemaNumber);
		logger.debug("Constructing proof for public key ["+String.format("%064x", new BigInteger(1, compressedKey)).toUpperCase()+"]");
		SignedData signedData = new SignedData(by, bytes, ledgerSignature.getPartialMerkleRoot(), platformVersion, schemaNumber);
		String tradeId = ledgerSignature.getEncodedId();
		Map<String, Object> result = constructFunctionCallData(
			ledgerSignature.getNetworkId(),
			ledgerSignature.getContractAddress(),
			ledgerSignature.getFunctionName(),
			ledgerSignature.getSenderId(),
			ledgerSignature.getReceiverId(),
			tradeId,
			signedData,
			withHiddenParams,
			ledgerSignature.getAuthNetworkId(),
			ledgerSignature.getAuthContractAddress());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

		return ResponseEntity.created(location).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	private Map<String, Object> constructFunctionCallData(String networkId, String controlContractAddress, String functionName, String senderId, String receiverId, String tradeId, SignedData signedData, boolean withHiddenAuthParams, String authNetworkId, String authContractAddress) {
		HashMap<String, Object> result = new HashMap<>();
		try {
			HashMap<String, String> export = new HashMap<>();
			BigInteger chainId = new BigInteger(networkId);
			String controlContract = controlContractAddress.replaceAll("\"", "");
			String authContract = authContractAddress != null ? authContractAddress.replaceAll("\"", "") : "";
			BigInteger authId = authNetworkId != null && !authNetworkId.isEmpty() ? new BigInteger(authNetworkId) : BigInteger.ZERO;
			BigInteger amount = BigInteger.valueOf(0);
			String transactionHash = "0x" + tradeId;
			String functionCallData = "";
			Function function = null;
			String eventSig = "0x0000000000000000000000000000000000000000000000000000000000000000";
			if (functionName.equals("requestFollowLeg")) {
				eventSig = "0xc6755b7c00000000000000000000000000000000000000000000000000000000";
				function = new Function(
					functionName,
					Arrays.<Type>asList(
						new Utf8String(tradeId.toLowerCase()), // tradeId
						new Utf8String(senderId),              // senderId
						new Utf8String(receiverId),            // receiverId
						new Address(controlContract),          // controlContract
						new Uint256(chainId),                  // sourceNetworkId
						new Uint256(amount)),                  // amount
					Arrays.<TypeReference<?>>asList(
						new TypeReference<Bool>() {
						})
				);
			} else if (functionName.equals("performCancellation")) {
				eventSig = "0xca2f045200000000000000000000000000000000000000000000000000000000";
				function = new Function(
					functionName,
					Arrays.<Type>asList(
						new Utf8String(tradeId.toLowerCase()), // tradeId
						new Utf8String(senderId),              // senderId
						new Utf8String(receiverId)),           // receiverId
					Arrays.<TypeReference<?>>asList(
						new TypeReference<Bool>() {
						})
				);
			}
			functionCallData = DefaultFunctionEncoder.encode(function);
			if (withHiddenAuthParams) {
				String authParams = DefaultFunctionEncoder.encodeConstructorPacked(
					Arrays.<Type>asList(
						new Uint256(authId),       // authNetworkId
						new Address(authContract)) // authContract
				);
				functionCallData = functionCallData + authParams;
			}
			String eventData = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new DynamicStruct(
						new DynamicBytes(Numeric.hexStringToByteArray(functionCallData)), // callParameters
						new Utf8String("SHA-256"),                                  // hashAlgorithm
						new Bytes32(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000")), // privacySalt
						new DynamicStruct(
							new Uint8(0),                                             // groupIndex
							new Uint8(0),                                             // internalIndex
							new DynamicBytes(Numeric.hexStringToByteArray("0x00"))    // encodedBytes
						)
					)
				)
			);
			String encodedInfo = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new Uint256(chainId),                                     // sourceNetworkId
					new Address(controlContract),                             // controlContract
					new DynamicBytes(Numeric.hexStringToByteArray(eventData)) // eventData
				)
			);

			SignatureProof.Signatures signatures = SignatureProof.getSignatures(new SecureHash(Numeric.hexStringToByteArray(transactionHash), SecureHash.SHA_256), signedData);
			logSignatures(signatures);
			List<Type> sigs = new ArrayList<>();
			for (SignatureProof.Signature sig : signatures.getSignatures().getValue()) {
				sigs.add(sig.asDynamicStruct());
			}
			List<Uint8> flags = new ArrayList<>();
			List<Bytes32> proof = new ArrayList<>();
			List<Bytes32> values = new ArrayList<>();
			String signatureOrProof = "0x" + DefaultFunctionEncoder.encodeConstructor(
				Arrays.<Type>asList(
					new DynamicStruct(
						new Uint256(0),
						new DynamicStruct(
							new Bytes32(Numeric.hexStringToByteArray(transactionHash)),
							new DynamicArray<>(Bytes32.class, proof),
	            new DynamicArray<>(Uint8.class, flags),
							new DynamicArray<>(Bytes32.class, values)
						),
						new DynamicArray<>(Type.class, sigs))
					)
				);
			eventSig = String.format("0x%1$" + 64 + "s", eventData.substring(2, 10)).replace(' ', '0');

			export.put("networkId", chainId.toString(16));
			export.put("eventSig", eventSig);
			export.put("encodedInfo", encodedInfo);
			export.put("signatureOrProof", signatureOrProof);

			result.put("proof", export);
			result.put("event", "TRADE");
			result.put("tradeId", tradeId.toLowerCase());
			result.put("fromAccount", senderId);
			result.put("toAccount", receiverId);
			result.put("remoteNotional", amount);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private void logSignatures(SignatureProof.Signatures signatures) {
		int i = 0;
		for (SignatureProof.Signature sig : signatures.getSignatures().getValue()) {
			logger.debug("  [" +i+ "] by  : " + String.format("%064x", sig.getBy().getValue()).toUpperCase());
			logger.debug("  [" +i+ "] sigR: " + String.format("%064x", sig.getSigR().getValue()).toUpperCase());
			logger.debug("  [" +i+ "] sigS: " + String.format("%064x", sig.getSigS().getValue()).toUpperCase());
			logger.debug("  [" +i+ "] sigV: " + String.format("%02x", sig.getSigV().getValue()).toUpperCase());
			logger.debug("  [" +i+ "] meta: " + Utils.toHexString(sig.getMeta().getValue()).toUpperCase());
			i++;
		}
	}
}
