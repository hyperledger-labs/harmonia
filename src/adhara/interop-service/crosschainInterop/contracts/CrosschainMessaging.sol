/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

import "contracts/interfaces/ICrosschainVerifier.sol";
import "contracts/libraries/Ethereum.sol";
import "contracts/libraries/Corda.sol";
import "contracts/libraries/ED25519.sol";
import "contracts/libraries/SECP256K1.sol";
import "contracts/libraries/SECP256R1.sol";
import "contracts/libraries/SolidityUtils.sol";

/* Refer to https://entethalliance.github.io/crosschain-interoperability/draft_crosschain_techspec_messaging.html and align with section 3.2 Format of Signatures or Proofs */
contract CrosschainMessaging is ICrosschainVerifier {

  /* Configurable proving schemes registered per source network doing remote function calls. */
  uint256 public CordaTradeProvingSchemeId = 0;
  uint256 public CordaTransactionProvingSchemeId = 1;
  uint256 public EthereumBlockHeaderProvingSchemeId = 2;

  /* Event proving scheme encoded as 256-bit integer */
  struct ProvingScheme {
    uint256 scheme;
  }

  /* Mapping of network identification to proving scheme */
  mapping(uint256 => ProvingScheme) provingSchemes;

  /* Signatures scheme identification code as include in Corda signature metadata */
  uint8 public SECP256K1SignatureSchemeId = 2;
  uint8 public SECP256R1SignatureSchemeId = 3;
  uint8 public ED25519SignatureSchemeId = 4;

  /* Validator list update item for a specific block on a remote network */
  struct ValidatorUpdate {
    uint256 block;
    address[] validators;
    uint256 prev;
    uint256 next;
  }
  /* Mapping of network identification to linked list of registered validator addresses. */
  mapping(uint256 => ValidatorUpdate[]) public activeValidators;
  mapping(uint256 => uint256) private activeValidatorLastUpdates;

  /* Active Corda notaries that are authenticated to sign Corda proofs. */
  mapping(uint256 => mapping(uint256 => bool)) activeNotaries;
  /* Active Corda participants that are authenticated to sign Corda proofs. */
  mapping(uint256 => mapping(uint256 => bool)) activeParticipants;
  /* Mapping of network identification to registered Corda parameter handlers, indexed by function signatures. */
  mapping(uint256 => mapping(uint32 => Corda.ParameterHandler[])) functionParameterHandlers;
  /* Mapping of network identification to a registered function prototype, indexed by function signatures */
  mapping(uint256 => mapping(uint32 => string)) functionPrototypes;
  /* Mapping of network identification to a registered Corda commands, indexed by function signatures */
  mapping(uint256 => mapping(uint32 => string)) functionCommands;

  // Contains the decoded info for EEA interoperability proving schemes where a remote function call needs to be made.
  struct DecodedInfo {
    uint256 networkId;
    address contractAddress;
    bytes callParameters;
    bytes32 sourceHash;
  }

  /* Events for debugging purposes. */
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Uint(uint256, string);
  event Address(address, string);
  event String(string, string);
  event UInt8(uint8, string);

  constructor() {
  }

  /*
   * Set the validator addresses, for the given network, to be used in the block header proving scheme. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index.
   * @param {string} blockNumber The blockNumber where validators were updated on the remote network.
   * @param {string} validatorList The list of remote validator addresses to set.
   * @return {bool} success Returns true if the validator set could be updated.
   */
  function setValidatorList(
    uint256 networkId,
    uint256 blockNumber,
    address[] calldata validatorList
  ) public returns (bool success){
    if (activeValidators[networkId].length == 0) {
      activeValidators[networkId].push(ValidatorUpdate(0, new address[](0), 0, 0));
    }
    uint256 lastUpdateIdx = activeValidatorLastUpdates[networkId];
    ValidatorUpdate memory lastUpdate = activeValidators[networkId][lastUpdateIdx];
    while (blockNumber < lastUpdate.block) {
      lastUpdateIdx = lastUpdate.prev;
      lastUpdate = activeValidators[networkId][lastUpdateIdx];
    }
    activeValidatorLastUpdates[networkId] = insertValidatorListAfter(networkId, lastUpdateIdx, blockNumber, validatorList);
    return true;
  }

  /*
   * Remove the validator addresses, for the given network and block number. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index.
   * @param {string} blockNumber The blockNumber where validators were updated on the remote network.
   */
  function removeValidatorList(
    uint256 networkId,
    uint256 blockNumber
  ) public {
    uint256 lastUpdateIdx = activeValidatorLastUpdates[networkId];
    ValidatorUpdate memory lastUpdate = activeValidators[networkId][lastUpdateIdx];
    while (blockNumber < lastUpdate.block) {
      lastUpdateIdx = lastUpdate.prev;
      lastUpdate = activeValidators[networkId][lastUpdateIdx];
    }
    require(lastUpdate.validators.length > 0, "No validators found for this network and block number");
    require(blockNumber == lastUpdate.block, "No validators found for this network and block number, please provide exact block number where validator list was set");
    removeValidatorListAt(networkId, lastUpdateIdx);
  }

  /*
   * Get the validator addresses, for the given network, to be used in the block header proving scheme.
   * @param {uint256} networkId The network identification to be used as mapping index.
   * @param {string} blockNumber The block number as which to get remote validator sets.
   * @return {address[]} The list of remote validator addresses for the given network and block number.
   */
  function getValidatorList(
    uint256 networkId,
    uint256 blockNumber
  ) external view returns (address[] memory) {
    uint256 lastUpdateIdx = activeValidatorLastUpdates[networkId];
    ValidatorUpdate memory lastUpdate = activeValidators[networkId][lastUpdateIdx];
    while (blockNumber < lastUpdate.block) {
      lastUpdateIdx = lastUpdate.prev;
      lastUpdate = activeValidators[networkId][lastUpdateIdx];
    }
    require(lastUpdate.validators.length > 0, "No validators found for this network and block number, please set the validators for block header proof verification first");
    return lastUpdate.validators;
  }

  function insertValidatorListAfter(
    uint256 networkId,
    uint256 id,
    uint256 block,
    address[] memory validators
  ) internal returns (uint256 newId) {
    require(id == 0 || id == activeValidators[networkId][0].next || activeValidators[networkId][id].prev != 0);
    ValidatorUpdate memory node = activeValidators[networkId][id];
    activeValidators[networkId].push(ValidatorUpdate({
      block: block,
      validators: validators,
      prev: id,
      next: node.next
    }));
    newId = activeValidators[networkId].length-1;
    activeValidators[networkId][node.next].prev = newId;
    activeValidators[networkId][id].next = newId;
  }

  function removeValidatorListAt(
    uint256 networkId,
    uint256 id
  ) internal {
    require(id != 0 && (id == activeValidators[networkId][0].next || activeValidators[networkId][id].prev != 0));
    ValidatorUpdate memory node = activeValidators[networkId][id];
    activeValidators[networkId][node.next].prev = node.prev;
    activeValidators[networkId][node.prev].next = node.next;
    delete activeValidators[networkId][id];
  }

  /*
   * Onboard a proving scheme for a specific network. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index into stored schemes.
   * @param {uint256} scheme The proving scheme identifier.
   */
  function onboardProvingScheme(
    uint256 networkId,
    uint256 scheme
  ) public {
    provingSchemes[networkId] = ProvingScheme(scheme);
  }

  /*
   * Set Corda parameter handlers, for a specific network, indexed by the given function signature. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index into stored functions.
   * @param {uint32} functionSignature The function signature to be used as mapping index into stored parameter handlers for a stored function.
   */
  function setParameterHandlers(
    uint256 networkId,
    uint32 functionSignature,
    string memory functionPrototype,
    string memory functionCommand,
    Corda.ParameterHandler[] calldata paramHandlers
  ) public {
    delete functionParameterHandlers[networkId][functionSignature];
    for (uint i = 0; i < paramHandlers.length; i++) {
      functionParameterHandlers[networkId][functionSignature].push(paramHandlers[i]);
    }
    functionCommands[networkId][functionSignature] = functionCommand;
    functionPrototypes[networkId][functionSignature] = functionPrototype;
  }

  /*
   * Remove all Corda parameter handlers, for a specific network, indexed by the given function signature. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index into stored parameter handlers.
   * @param {uint32} functionSignature The proving scheme identifier.
   */
  function removeParameterHandlers(
    uint256 networkId,
    uint32 functionSignature
  ) public {
    delete functionParameterHandlers[networkId][functionSignature];
    delete functionCommands[networkId][functionSignature];
    delete functionPrototypes[networkId][functionSignature];
  }

  /*
   * Get Corda parameter handler, at given index, for a specific network, indexed by the given function signature.
   * @param {uint256} networkId The network identification to be used as mapping index into stored functions.
   * @param {uint32} functionSignature The function signature to be used as mapping index into stored parameter handlers for the stored function.
   * @param {uint256} index The index of the requested parameter handler as index in the input parameter list of the corresponding stored function.
   */
  function getParameterHandler(
    uint256 networkId,
    uint32 functionSignature,
    uint256 index
  ) public view returns (bytes memory) {
    require(index < functionParameterHandlers[networkId][functionSignature].length, "Index out of bounds");
    Corda.ParameterHandler storage handler = functionParameterHandlers[networkId][functionSignature][index];
    return abi.encode(handler);
  }

  /*
   * Get Corda command, for a specific network, indexed by the given function signature.
   * @param {uint256} networkId The network identification to be used as mapping index into stored command.
   * @param {uint32} functionSignature The function signature to be used as mapping index.
   */
  function getFunctionCommand(
    uint256 networkId,
    uint32 functionSignature
  ) public view returns (string memory) {
    return functionCommands[networkId][functionSignature];
  }

  /*
   * Get function prototype, for a specific network, indexed by the given function signature.
   * @param {uint256} networkId The network identification to be used as mapping index into stored prototypes.
   * @param {uint32} functionSignature The function signature to be used as mapping index.
   */
  function getFunctionPrototype(
    uint256 networkId,
    uint32 functionSignature
  ) public view returns (string memory) {
    return functionPrototypes[networkId][functionSignature];
  }

  /*
   * Decode and verify the event according to the registered proving scheme for the given source network.
   * @param {uint256} blocknetworkId The source network identification.
   * @param {bytes32} eventSig The event function signature.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-network control contract's address, the event function signature, and the event data.
   * @param {bytes} encodedProof The information that a validating implementation can use to determine if the event data, given as part of encodedInfo, is valid.
   */
  function decodeAndVerify(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) external view returns (bytes memory decodedInfo) {
    DecodedInfo memory decoded;
    if (provingSchemes[networkId].scheme == CordaTradeProvingSchemeId) {
      decoded = handleCordaTradeProvingScheme(networkId, encodedInfo, encodedProof);
    } else if (provingSchemes[networkId].scheme == CordaTransactionProvingSchemeId) {
      decoded = handleCordaTransactionProvingScheme(networkId, encodedInfo, encodedProof);
    } else if (provingSchemes[networkId].scheme == EthereumBlockHeaderProvingSchemeId) {
      decoded = handleEthereumBlockHeaderProvingScheme(networkId, encodedInfo, encodedProof);
    } else {
      revert("Failed to decode and verify event: Unknown proving scheme");
    }
    return abi.encode(decoded.networkId, decoded.contractAddress, decoded.callParameters, decoded.sourceHash);
  }

  /*
   * Decode and verify event according to the block header proving scheme.
   * @param {uint256} blocknetworkId The source network identification.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-network control contract's address, the event function signature, and the event data.
   * @param {bytes} encodedProof The RLP-encoded sibling nodes, the receipts tree root in the block header and the block hash.
   * @return {bool} Returns true if the block header proof was verifier successfully.
   */
  function handleEthereumBlockHeaderProvingScheme(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) public view returns (DecodedInfo memory) {
    (,, bytes memory evtData) = abi.decode(encodedInfo, (uint256, address, bytes));
    Ethereum.EventData memory eventData = abi.decode(evtData, (Ethereum.EventData));
    Ethereum.Proof memory proof = abi.decode(encodedProof, (Ethereum.Proof));
    Ethereum.BlockHeaderMeta memory blockHeaderMeta = abi.decode(proof.proofData.blockHeaderMeta, (Ethereum.BlockHeaderMeta));
    (uint256 blockNumber, bytes32 receiptsRoot) = Ethereum.verifyBFTBlockHeader(
      proof.proofData.blockHash,
      blockHeaderMeta.rlpBlockHeader,
      blockHeaderMeta.rlpBlockHeaderPreimage
    );
    require(receiptsRoot == proof.proofData.root, 'Receipts root in header is not equal to receipts root for this event');
    require(Ethereum.verifyEVMEvent(eventData, proof.proofData.root, proof.proofData.witnesses), "Patricia Merkle proof failed");
    bytes32 calculatedHash = keccak256(blockHeaderMeta.rlpBlockHeader);
    require(verifyValidatorSignatures(networkId, blockNumber, calculatedHash, proof.signatures), "Validator signatures are not valid");
    bytes memory logData = Ethereum.extractEVMEventLogData(eventData);
    (uint256 destinationId, address destinationAddress, bytes memory callParameters) = abi.decode(logData, (uint256, address, bytes));
    return DecodedInfo(destinationId, destinationAddress, callParameters, keccak256(abi.encodePacked(encodedInfo, encodedProof)));
  }

  /*
   * Verify validator signatures used in the block header proving scheme.
   * @param {bytes} rlpValidatorSignatures The RLP-encoded validator signatures to be verified.
   * @param {bytes32} signedHash The hash that was signed, to be used in verification of signatures.
   * @param {uint256} networkId The network identification to be used as mapping index into stored validator sets.
   */
  function verifyValidatorSignatures(
    uint256 networkId,
    uint256 blockNumber,
    bytes32 blockHash,
    Ethereum.Signature[] memory validatorSignatures
  ) public view returns (bool) {
    address[] memory validators = this.getValidatorList(networkId, blockNumber);
    uint256 validSeals = 0;
    address[50] memory addressReuseCheck;
    for (uint256 i = 0; i < validatorSignatures.length; i++) {
      Ethereum.Signature memory signed = validatorSignatures[i];
      address signatureAddress = SECP256K1.recover(blockHash, bytes.concat(bytes32(signed.sigR), bytes32(signed.sigS), bytes1(uint8(signed.sigV))));
      for (uint256 j = 0; j < validators.length; j++) {
        if (signatureAddress == validators[j]) {
          // Check that a validator's signature wasn't submitted multiple times
          for (uint256 k = 0; k < i; k++) {
            if (addressReuseCheck[k] == signatureAddress) {
              revert("Not allowed to submit multiple seals from the same validator");
            }
          }
          validSeals++;
          addressReuseCheck[i] = signatureAddress;
          break;
        }
      }
    }
    if (validSeals < 2 * validators.length / 3) {
      revert("Not enough valid validator seals");
    }
    return true;
  }

  /*
   * Decode a Corda trade-based proof and verify signatures from authenticated notaries.
   * @param {uint256} networkId The source network identification.
   * @param {bytes} encodedInfo Contains the Corda trade-based proof data.
   * @param {bytes} encodedProof Contains the signatures to be verified.
   * @return {bool} Returns true if sufficient valid Corda signatures were given as part of the proof.
   */
  function handleCordaTradeProvingScheme(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) public view returns (DecodedInfo memory) {
    (Corda.Proof memory sigs, DecodedInfo memory decodedInfo) = decodeAndVerifyCordaTrade(encodedInfo, encodedProof);
    if (sigs.signatures.length == 0) {
      revert("No signatures were found");
    }
    if (sigs.signatures.length > 50) {
      revert("Number of signatures exceeds the maximum allowed");
    }
    uint numNotaries = 0;
    uint256[50] memory signerReuseCheck;
    for (uint i = 0; i < sigs.signatures.length; i++) {
      Corda.Signature memory signed = sigs.signatures[i];
      for (uint256 k = 0; k < i; k++) {
        if (signerReuseCheck[k] == signed.by) {
          revert("Not allowed to submit multiple signatures by the same signer");
        }
      }
      if (activeNotaries[networkId][signed.by]) {
        uint8 scheme = uint8(extractCordaSignatureScheme(signed.meta));
        if (scheme == ED25519SignatureSchemeId) {
          require(ED25519.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes(signed.meta)) == true, "ED25519 signature does not match");
        } else if (scheme == SECP256R1SignatureSchemeId) {
          require(SECP256R1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(bytes32(signed.sigV)), bytes(signed.meta)) == true, "SECP256R1 signature does not match");
        } else if (scheme == SECP256K1SignatureSchemeId) {
          require(SECP256K1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(bytes32(signed.sigV)), bytes(signed.meta)) == true, "SECP256K1 signature does not match");
        } else {
          revert("Unsupported signature scheme");
        }
        numNotaries++;
      }
    }
    if (numNotaries < 1) {
      revert("Trade-based verification requires at least one notary signature");
    }
    return decodedInfo;
  }

  /*
   * Decode a Corda trade-based proof, verify the event and check the root that was signed against signature meta data.
   * @param {bytes} encodedInfo Contains the Corda trade-based proof data.
   * @param {bytes} encodedProof Contains the signatures to be verified.
   * @return {bool} Returns true if the Corda event was successfully verified and matched the root provided in signature meta data.
   */
  function decodeAndVerifyCordaTrade(
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) internal view returns (Corda.Proof memory, DecodedInfo memory) {
    (Corda.Proof memory sigs) = abi.decode(encodedProof, (Corda.Proof));
    (uint256 networkId, address contractAddress, bytes memory txData) = abi.decode(encodedInfo, (uint256, address, bytes));
    (Corda.EventData memory eventData) = abi.decode(txData, (Corda.EventData));
    uint32 eventSignature = uint32(bytes4(eventData.callParameters));
    require(functionParameterHandlers[networkId][eventSignature].length > 0, "No registered handlers were found for the provided network and event signature");
    require(Corda.validateTrade(Corda.ValidationData(eventData, functionPrototypes[networkId][eventSignature], "", functionParameterHandlers[networkId][eventSignature], sigs.proof, sigs.signatures)) == true, "Trade-based event verification failed");
    for (uint i = 0; i < sigs.signatures.length; i++) {
      (bytes4 platform, bytes4 schema, bytes32 root) = decodeCordaSignatureMeta(sigs.signatures[i].meta);
      bytes memory a = hex"636F7264610100000080C562000000000001D0000003D00000000300A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3DC0770200A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D";
      bytes memory b = bytes.concat(hex"C00502", hex"54", SolUtils.Bytes4ToHexBytes(platform), hex"54", SolUtils.Bytes4ToHexBytes(schema));
      bytes memory c = hex"00A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3DC02301A020";
      bytes memory d = hex"0080C562000000000002D00000031200000001D000000309000000040080C562000000000005C0E405A1226E65742E636F7264612E636F72652E63727970746F2E5369676E61626C654461746140450080C562000000000003C02602A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3D40C089020080C562000000000004C04207A1117369676E61747572654D65746164617461A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746145404041420080C562000000000004C02E07A10474784964A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736845404041420080C562000000000005C0B405A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746140450080C562000000000003C02602A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D40C054020080C562000000000004C01E07A10F706C6174666F726D56657273696F6EA103696E7445A101304041420080C562000000000004C01D07A10E736368656D654E756D6265724944A103696E7445A101304041420080C562000000000005C0BB05A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736840450080C562000000000003C02602A3226E65742E636F7264613A62373950654D424C73487875324132337944595261413D3D40C062030080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000004C01507A1066F6666736574A103696E7445A101304041420080C562000000000004C01307A10473697A65A103696E7445A101304041420080C562000000000005C08205A1276E65742E636F7264612E636F72652E63727970746F2E536563757265486173682453484132353640450080C562000000000003C02602A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3D40C022010080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000009C10100";
      bytes memory data = bytes.concat(a, b, c, root, d);
      sigs.signatures[i].meta = data;
    }
    return (sigs, DecodedInfo(networkId, contractAddress, eventData.callParameters, keccak256(abi.encode(eventData))));
  }

  /*
   * Decode a Corda transaction-based proof and verify signatures from authenticated participants and notaries.
   * @param {uint256} networkId The source network identification.
   * @param {bytes} encodedInfo Contains the Corda transaction-based proof data.
   * @param {bytes} encodedProof Contains the signatures to be verified.
   * @return {bool} Returns true if sufficient valid Corda signatures were given as part of the proof.
   */
  function handleCordaTransactionProvingScheme(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) public view returns (DecodedInfo memory) {
    (Corda.Proof memory sigs, DecodedInfo memory decodedInfo) = decodeAndVerifyCordaTransaction(networkId, encodedInfo, encodedProof);
    if (sigs.signatures.length == 0) {
      revert("No signatures were found");
    }
    if (sigs.signatures.length > 50) {
      revert("Number of signatures exceeds the maximum allowed");
    }
    uint numNotaries = 0;
    uint numParticipants = 0;
    uint256[50] memory signerReuseCheck;
    for (uint i = 0; i < sigs.signatures.length; i++) {
      Corda.Signature memory signed = sigs.signatures[i];
      for (uint256 k = 0; k < i; k++) {
        if (signerReuseCheck[k] == signed.by) {
          revert("Not allowed to submit multiple signatures by the same signer");
        }
      }
      if (activeNotaries[networkId][signed.by] || activeParticipants[networkId][signed.by]) {
        uint8 scheme = uint8(extractCordaSignatureScheme(signed.meta));
        if (scheme == ED25519SignatureSchemeId) {
          require(ED25519.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes(signed.meta)) == true, "ED25519 signature does not match");
        } else if (scheme == SECP256R1SignatureSchemeId) {
          require(SECP256R1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(uint8(signed.sigV)), bytes(signed.meta)) == true, "SECP256R1 signature does not match");
        } else if (scheme == SECP256K1SignatureSchemeId) {
          require(SECP256K1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(uint8(signed.sigV)), bytes(signed.meta)) == true, "SECP256K1 signature does not match");
        } else {
          revert("Unsupported signature scheme");
        }
        if (activeNotaries[networkId][signed.by]) {
          numNotaries++;
        }
        if (activeParticipants[networkId][signed.by]) {
          numParticipants++;
        }
      }
    }
    if (numNotaries < 1) {
      revert("Transaction-based verification requires at least one notary signature");
    }
    if (numParticipants < 2) {
      revert("Transaction-based verification requires at least two participant signatures");
    }
    return decodedInfo;
  }

  /*
   * Decode a Corda transaction-based proof, verify the event and check the root that was signed against signature meta data.
   * @param {bytes} encodedInfo Contains the Corda transaction-based proof data.
   * @param {bytes} encodedProof Contains the signatures to be verified.
   * @return {bool} Returns true if the Corda event was successfully verified and matched the root provided in signature meta data.
   */
  function decodeAndVerifyCordaTransaction(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) internal view returns (Corda.Proof memory, DecodedInfo memory) {
    (Corda.Proof memory sigs) = abi.decode(encodedProof, (Corda.Proof));
    (uint256 destinationNetworkId, address contractAddress, bytes memory txData) = abi.decode(encodedInfo, (uint256, address, bytes));
    (Corda.EventData memory eventData) = abi.decode(txData, (Corda.EventData));
    uint32 eventSignature = uint32(bytes4(eventData.callParameters));
    require(functionParameterHandlers[networkId][eventSignature].length > 0, "No registered handlers were found for the provided network and event signature");
    require(Corda.validateEvent(Corda.ValidationData(eventData, functionPrototypes[networkId][eventSignature], functionCommands[networkId][eventSignature], functionParameterHandlers[networkId][eventSignature], sigs.proof, sigs.signatures)) == true, "Transaction-based event verification failed");
    for (uint i = 0; i < sigs.signatures.length; i++) {
      (bytes4 platform, bytes4 schema, bytes32 root) = decodeCordaSignatureMeta(sigs.signatures[i].meta);
      bytes memory a = hex"636F7264610100000080C562000000000001D0000003D00000000300A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3DC0770200A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D";
      bytes memory b = bytes.concat(hex"C00502", hex"54", SolUtils.Bytes4ToHexBytes(platform), hex"54", SolUtils.Bytes4ToHexBytes(schema));
      bytes memory c = hex"00A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3DC02301A020";
      bytes memory d = hex"0080C562000000000002D00000031200000001D000000309000000040080C562000000000005C0E405A1226E65742E636F7264612E636F72652E63727970746F2E5369676E61626C654461746140450080C562000000000003C02602A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3D40C089020080C562000000000004C04207A1117369676E61747572654D65746164617461A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746145404041420080C562000000000004C02E07A10474784964A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736845404041420080C562000000000005C0B405A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746140450080C562000000000003C02602A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D40C054020080C562000000000004C01E07A10F706C6174666F726D56657273696F6EA103696E7445A101304041420080C562000000000004C01D07A10E736368656D654E756D6265724944A103696E7445A101304041420080C562000000000005C0BB05A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736840450080C562000000000003C02602A3226E65742E636F7264613A62373950654D424C73487875324132337944595261413D3D40C062030080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000004C01507A1066F6666736574A103696E7445A101304041420080C562000000000004C01307A10473697A65A103696E7445A101304041420080C562000000000005C08205A1276E65742E636F7264612E636F72652E63727970746F2E536563757265486173682453484132353640450080C562000000000003C02602A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3D40C022010080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000009C10100";
      bytes memory data = bytes.concat(a, b, c, root, d);
      sigs.signatures[i].meta = data;
    }
    return (sigs, DecodedInfo(destinationNetworkId, contractAddress, eventData.callParameters, keccak256(abi.encode(eventData))));
  }

  /*
   * Decode Corda signature meta data.
   * @param {bytes} data Meta data given as part of a Corda signature.
   * @return {bytes4} Corda platform version.
   * @return {bytes4} Signature scheme identifier.
   * @return {bytes32} Transaction tree root.
   */
  function decodeCordaSignatureMeta(
    bytes memory data
  ) internal pure returns (bytes4, bytes4, bytes32) {
    return (SolUtils.BytesToBytes4(data, 0), SolUtils.BytesToBytes4(data, 4), SolUtils.BytesToBytes32(data, 8));
  }

  /*
   * Decode Corda signature meta data and extract the signature scheme identifier.
   * @param {bytes} data Meta data given as part of a Corda signature.
   * @return {bytes1} Returns the signature scheme identifier.
   */
  function extractCordaSignatureScheme(
    bytes memory data
  ) internal pure returns (bytes1) {
    return SolUtils.BytesToBytes1(data, 110);
  }

  /*
   * Add a Corda notary's public key to the list of authenticated notaries in storage, for the given network. This method needs access controls.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if notary was successfully added.
   */
  function addNotary(
    uint256 networkId,
    uint256 publicKey
  ) public returns (bool) {
    activeNotaries[networkId][publicKey] = true;
    return true;
  }

  /*
   * Check whether a Corda notary is authenticated, for the given network.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if the notary's public key is contained in the list of authenticated notaries for the given network.
   */
  function isNotary(
    uint256 networkId,
    uint256 publicKey
  ) public view returns (bool) {
    return activeNotaries[networkId][publicKey];
  }

  /*
   * Remove a Corda notary's public key from the list of authenticated notaries in storage, for the given network. This method needs access controls.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if notary was successfully added.
   */
  function removeNotary(uint256 networkId, uint256 publicKey) public returns (bool) {
    delete activeNotaries[networkId][publicKey];
    return true;
  }

  /*
   * Add a Corda participant's public key to the list of authenticated participants in storage, for the given network. This method needs access controls.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if participant was successfully added.
   */
  function addParticipant(uint256 networkId, uint256 publicKey) public returns (bool) {
    activeParticipants[networkId][publicKey] = true;
    return true;
  }

  /*
   * Check whether a Corda participant is authenticated, for the given network.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if the participant's public key is contained in the list of authenticated participants for the given network.
   */
  function isParticipant(uint256 networkId, uint256 publicKey) public view returns (bool) {
    return activeParticipants[networkId][publicKey];
  }

  /*
   * Remove a Corda participant's public key from the list of authenticated participants in storage, for the given network. This method needs access controls.
   * @param {uint256} networkId The network identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if participant was successfully added.
   */
  function removeParticipant(uint256 networkId, uint256 publicKey) public returns (bool) {
    delete activeParticipants[networkId][publicKey];
    return true;
  }
}
