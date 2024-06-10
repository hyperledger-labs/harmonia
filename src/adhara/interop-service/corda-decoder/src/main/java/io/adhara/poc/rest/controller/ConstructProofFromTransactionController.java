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
    @RequestBody LedgerTransaction ledgerTransaction)
    throws Exception {

    Map<String, Object> result = constructFunctionCallData(
      ledgerTransaction.getNetworkId(),
      ledgerTransaction.getContractAddress(),
      ledgerTransaction.getFunctionName(),
      ledgerTransaction.getEncodedInfo(),
      Boolean.parseBoolean(ledgerTransaction.getWithHiddenAuthParams()),
      ledgerTransaction.getAuthNetworkId(),
      ledgerTransaction.getAuthContractAddress());
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

    return ResponseEntity.created(location).contentType(MediaType.APPLICATION_JSON).body(result);
  }

  private static int EVENT_DATA_CONTRACT = 0;
  private static int EVENT_DATA_COMMAND = 1;
  private static int EVENT_DATA_TRADE_ID = 2;
  private static int EVENT_DATA_AMOUNT = 3;
  private static int EVENT_DATA_CURRENCY = 4;

  private static int PARTY_ISSUER_INDEX = 0; // O=PartyA, L=London, C=GB
  private static int PARTY_OWNER_INDEX = 1;  // O=PartyB, L=New York, C=US

  private static int PARTY_SENDER_INDEX = 1; // O=PartyA, L=London, C=GB
  private static int PARTY_RECEIVER_INDEX = 0; // O=PartyB, L=New York, C=US

  private static int PARTY_DCR_EARMARKED_FOR_INDEX = 2;
  private static int PARTY_DCR_LENDER_INDEX = 3;
  private static int PARTY_DCR_OWNER_INDEX = 4;

  private Map<String, Object> constructFunctionCallData(String networkId, String controlContractAddress, String functionName, String signedTransaction, boolean withHiddenAuthParams, String authNetworkId, String authContractAddress) {
    logger.info("Constructing function call data for system ["+networkId+"] to remotely call function ["+functionName+"] in contract ["+controlContractAddress+"]"+(withHiddenAuthParams ? " with auth params" : ""));
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
      if (contract.endsWith("example.contracts.DCRContract")) {
        sender = PARTY_OWNER_INDEX < parties.size() ? parties.get(PARTY_OWNER_INDEX) : null;
        sender = sender != null ? Base64.getEncoder().encodeToString(sender.getBytes()) : null;
        receiver = PARTY_ISSUER_INDEX < parties.size() ? parties.get(PARTY_ISSUER_INDEX) : null;
        receiver = receiver != null ? Base64.getEncoder().encodeToString(receiver.getBytes()) : null;
      } else if (contract.endsWith("example.contracts.XVPContract")) {
        receiver = PARTY_SENDER_INDEX < parties.size() ? parties.get(PARTY_SENDER_INDEX) : null;
        receiver = receiver != null ? Base64.getEncoder().encodeToString(receiver.getBytes()) : null;
        sender = PARTY_RECEIVER_INDEX < parties.size() ? parties.get(PARTY_RECEIVER_INDEX) : null;
        sender = sender != null ? Base64.getEncoder().encodeToString(sender.getBytes()) : null;
      } else if (contract.endsWith("states.contract.DCRContract")) {
        sender = PARTY_DCR_EARMARKED_FOR_INDEX < parties.size() ? parties.get(PARTY_DCR_EARMARKED_FOR_INDEX) : null;
        sender = sender != null ? Base64.getEncoder().encodeToString(sender.getBytes()) : null;
        receiver = PARTY_DCR_LENDER_INDEX < parties.size() ? parties.get(PARTY_DCR_LENDER_INDEX) : null;
        receiver = receiver != null ? Base64.getEncoder().encodeToString(receiver.getBytes()) : null;
      } else {
        logger.error("Error: Contract [" +contract+ "] is not recognized");
        return null;
      }
      if (sender == null) {
        logger.error("Error: Fail to extract sender from wire transaction");
        return null;
      }
      if (receiver == null) {
        logger.error("Error: Fail to extract receiver from wire transaction");
        return null;
      }
      BigInteger chainId = new BigInteger(networkId);
      String controlContract = controlContractAddress.replaceAll("\"", "");
      String authContract = authContractAddress != null ? authContractAddress.replaceAll("\"", "") : "0x";
      BigInteger authId = authNetworkId != null && !authNetworkId.isEmpty() ? new BigInteger(authNetworkId) : BigInteger.ZERO;
      String tradeId = proof.getId();
      String functionCallData = "";
      Function function = null;
      String eventSig = "0x0000000000000000000000000000000000000000000000000000000000000000";
      logger.debug("Function: " + functionName);
      if (functionName.equals("requestFollowLeg")) {
        BigDecimal amount = new BigDecimal(proof.getAmount());
        eventSig = "0xc6755b7c00000000000000000000000000000000000000000000000000000000";
        function = new Function(
          functionName,
          Arrays.<Type>asList(
            new Utf8String(tradeId),             // tradeId
            new Utf8String(sender),              // senderId
            new Utf8String(receiver),            // receiverId
            new Address(authContract),           // sourceContractAddress
            new Uint256(authId),                 // sourceNetworkId
            new Uint256(amount.unscaledValue())),// amount
          Arrays.<TypeReference<?>>asList(
            new TypeReference<Bool>() {
            })
        );
        logger.debug("TradeId: " + tradeId);
        logger.debug("Sender:" + sender);
        logger.debug("Receiver:" + receiver);
        logger.debug("Contract:" + controlContract);
        logger.debug("ChainId:" + chainId);
        logger.debug("Amount:", amount.unscaledValue());
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
        logger.debug("TradeId: " + tradeId);
        logger.debug("Sender:" + sender);
        logger.debug("Receiver:" + receiver);
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
      List<DynamicStruct> componentGroups = new ArrayList<>();
      for (int g=0; g<proof.getOutputsComponentGroup().length; g++)
        componentGroups.add(new DynamicStruct(
            new Uint8(SignatureProof.COMPONENT_GROUP_OUTPUTS),                                               // groupIndex
            new Uint8(g),                                                                                    // internalIndex
            new DynamicBytes(Numeric.hexStringToByteArray("0x" + proof.getOutputsComponentGroup()[g])) // encodedBytes
          )
        );
      for (int g=0; g<proof.getCommandsComponentGroup().length; g++)
        componentGroups.add(new DynamicStruct(
            new Uint8(SignatureProof.COMPONENT_GROUP_COMMANDS),                                               // groupIndex
            new Uint8(g),                                                                                     // internalIndex
            new DynamicBytes(Numeric.hexStringToByteArray("0x" + proof.getCommandsComponentGroup()[g])) // encodedBytes
          )
        );
      for (int g=0; g<proof.getNotaryComponentGroup().length; g++)
        componentGroups.add(new DynamicStruct(
            new Uint8(SignatureProof.COMPONENT_GROUP_NOTARY),                                                // groupIndex
            new Uint8(g),                                                                                    // internalIndex
            new DynamicBytes(Numeric.hexStringToByteArray("0x" + proof.getNotaryComponentGroup()[g]))  // encodedBytes
          )
        );
      for (int g=0; g<proof.getSignersComponentGroup().length; g++)
        componentGroups.add(new DynamicStruct(
            new Uint8(SignatureProof.COMPONENT_GROUP_SIGNERS),                                               // groupIndex
            new Uint8(g),                                                                                    // internalIndex
            new DynamicBytes(Numeric.hexStringToByteArray("0x" + proof.getSignersComponentGroup()[g])) // encodedBytes
          )
        );
      String eventData = "0x" + DefaultFunctionEncoder.encodeConstructor(
        Arrays.<Type>asList(
          new DynamicStruct(
            new DynamicBytes(Numeric.hexStringToByteArray(functionCallData)),     // callParameters
            new Utf8String("SHA-256"),                                      // hashAlgorithm
            new Bytes32(Numeric.hexStringToByteArray("0x"+proof.getSalt())),// privacySalt
            new DynamicArray<>(DynamicStruct.class, componentGroups)              // componentData
          )
        ));
      String encodedInfo = "0x" + DefaultFunctionEncoder.encodeConstructor(
        Arrays.<Type>asList(
          new Uint256(chainId),                                     // networkId
          new Address(controlContract),                             // controlContract
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
            new Uint256(0),
            new DynamicStruct(
              new Bytes32(proof.getRoot().getBytes()),
              new DynamicArray<>(Bytes32.class, Arrays.stream(proof.getProof().getProof()).map(witness -> new Bytes32(witness.getBytes())).collect(Collectors.toList())),
              new DynamicArray<>(Uint8.class, flags),
              new DynamicArray<>(Bytes32.class, Arrays.stream(proof.getProof().getLeaves()).map(leaf -> new Bytes32(leaf.getBytes())).collect(Collectors.toList()))
            ),
            new DynamicArray<>(Type.class, sigs))
          )
      );

      export.put("networkId", chainId.toString(16));
      export.put("eventSig", eventSig);
      export.put("encodedInfo", encodedInfo);
      export.put("signatureOrProof", signatureOrProof);

      result.put("proof", export);
      result.put("event", command);
      result.put("tradeId", tradeId);
      result.put("fromAccount", sender);
      result.put("toAccount", receiver);
      result.put("remoteNotional", proof.getAmount());

    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return result;
  }
}
