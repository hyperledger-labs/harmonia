/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

import "contracts/../../contracts/interfaces/CrosschainVerifier.sol";
import "contracts/../../contracts/libraries/Corda.sol";
import "contracts/../../contracts/libraries/ED25519.sol";
import "contracts/../../contracts/libraries/RLP.sol";
import "contracts/../../contracts/libraries/SECP256K1.sol";
import "contracts/../../contracts/libraries/SECP256R1.sol";
import "contracts/../../contracts/libraries/SolidityUtils.sol";

/* Refer to https://entethalliance.github.io/crosschain-interoperability/draft_crosschain_techspec_messaging.html and align with section 3.2 Format of Signatures or Proofs */
contract CrosschainMessaging is CrosschainVerifier {
  using RLP for RLP.RLPItem;
  using RLP for RLP.Iterator;
  using RLP for bytes;

  using Object for Object.Obj;

  /* Indices into an Ethereum block header */
  uint256 public parentHashIndex = 0;
  uint256 public receiptsRootIndex = 5;
  uint256 public headerExtraDataIndex = 12;
  uint256 public extraDataVanityIndex = 0;
  uint256 public extraDataValidatorsIndex = 1;
  uint256 public extraDataVoteIndex = 2;
  uint256 public extraDataRoundIndex = 3;
  uint256 public extraDataSealsIndex = 4;

  /* Configurable proving schemes registered per source chain doing remote function calls. */
  uint256 public CordaTradeProvingSchemeId = 0;
  uint256 public CordaTransactionProvingSchemeId = 1;
  uint256 public EthereumBlockHeaderProvingSchemeId = 2;

  /* Event proving scheme encoded as 256-bit integer */
  struct ProvingScheme {
    uint256 scheme;
  }

  /* Mapping of chain identification to proving scheme */
  mapping(uint256 => ProvingScheme) provingSchemes;

  /* Signatures scheme identification code as include in Corda signature metadata */
  uint8 public SECP256K1SignatureSchemeId = 2;
  uint8 public SECP256R1SignatureSchemeId = 3;
  uint8 public ED25519SignatureSchemeId = 4;

  /* Mapping of block hash to block header receipts roots. */
  mapping(bytes32 => bytes32) receiptsRoots;
  /* Mapping of chain identification to registered validator addresses. */
  mapping(uint256 => address[]) public chainHeadValidators;

  /* Active Corda notaries that are authenticated to sign Corda proofs. */
  mapping(uint256 => mapping(uint256 => bool)) activeNotaries;
  /* Active Corda participants that are authenticated to sign Corda proofs. */
  mapping(uint256 => mapping(uint256 => bool)) activeParticipants;
  /* Mapping of chain identification to registered Corda parameter handlers, indexed by function signatures. */
  mapping(uint256 => mapping(uint32 => Corda.ParameterHandler[])) parameterHandlers;

  /* Events for debugging purposes. */
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Uint(uint256, string);
  event Address(address, string);
  event String(string, string);
  event UInt8(uint8, string);

  constructor() {}

  /*
   * Getter for the receipts root for a given block hash.
   * @param {bytes32} blockHash The block hash as mapping index.
   * @return {bytes32} The receipts root as indexed by the given block hash.
   */
  function getReceiptsRoot(
    bytes32 blockHash
  ) public view returns (bytes32) {
    return receiptsRoots[blockHash];
  }

  /*
   * Set the initial receipts root and update validator addresses, for the given chain, as included in the given block header, to be used in the block header proving scheme. This method needs access controls.
   * @param {bytes32} blockHash The block hash to be used as mapping index.
   * @param {uint256} chainId The chain identification to be used in the validator set mapping.
   * @param {bytes32} receiptsRoot The receipts root to store for this block hash.
   * @param {bytes} blockHeader The RLP-encoded block header.
   * @return {bool} sufficient Returns true if sufficient information, to perform the updates, were given.
   */
  function addInitialParentHash(
    bytes32 blockHash,
    uint256 chainId,
    bytes32 receiptsRoot,
    bytes memory blockHeader
  ) public returns (bool sufficient) {
    // TODO: Rename to updateOrInitialiseValidatorList ?
    // TODO: add check that blockHeader hashes to the block hash provided
    receiptsRoots[blockHash] = receiptsRoot;

    RLP.RLPItem[] memory header = blockHeader.toRLPItem().toList();
    // Extract the istanbulExtraData for header
    bytes memory rlpExtraDataHeader = header[headerExtraDataIndex].toData();
    bytes memory rlpIstanbulExtraHeader = new bytes(
      rlpExtraDataHeader.length - 35
    );
    // Remove the vanity portion of the extra data to get the istanbul extra data
    SolUtils.BytesToBytes(rlpIstanbulExtraHeader, rlpExtraDataHeader, 35);
    // This list contains the validator addresses
    RLP.RLPItem[] memory istanbulExtraList = rlpIstanbulExtraHeader
    .toRLPItem()
    .toList();

    bool isEmpty = false;
    for (uint256 i = 0; isEmpty != true && i < istanbulExtraList.length; i++) {
      isEmpty = istanbulExtraList[i].isEmpty();
      if (isEmpty == false) {
        emit Address(
          istanbulExtraList[i].toAddress(),
          "Initial parent block validator"
        );
        chainHeadValidators[chainId].push(istanbulExtraList[i].toAddress());
      }
    }
    return true;
  }

  /*
   * Set the validator addresses remotely, for the given chain, to be used in the block header proving scheme. This method needs access controls.
   * This method will shrink or grow the current validator list and replace all entries with the provided addresses.
   * @param {uint256} chainId The chain identification to be used as mapping index.
   * @param {string} operationId The operation id used when validator are updated remotely through the interoperability protocol.
   * @param {string} validatorList The list of validator addresses to set.
   * @return {bool} success Returns true if the validator set could be updated.
   */
  function setValidatorList(
    uint256 chainId,
    string calldata operationId,
    address[] calldata validatorList
  ) public returns (bool success){
    uint256 i = 0;
    // Shrink chainHeadValidators, if needed
    while (chainHeadValidators[chainId].length > validatorList.length) {
      chainHeadValidators[chainId].pop();
    }
    // First populate existing slots
    for (; i < chainHeadValidators[chainId].length; i++) {
      chainHeadValidators[chainId][i] = validatorList[i];
    }
    // Now increase chainHeadValidators array
    for (; i < validatorList.length; i++) {
      chainHeadValidators[chainId].push(validatorList[i]);
    }
    return true;
  }

  /*
   * Compare two block headers given as RLP lists.
   * @param {RLP.RLPItem[]} header1 The first header.
   * @param {RLP.RLPItem[]} header2 The second header, to compare with the first.
   * @return {bool} Returns true if the two block headers are equal.
   */
  function compareHeaders(
    RLP.RLPItem[] memory header1,
    RLP.RLPItem[] memory header2
  ) private view returns (bool) {
    bytes32 header1HashPart1 = keccak256(
      abi.encodePacked(
        header1[0].toBytes(), // parentHash
        header1[1].toBytes(), // sha3Uncles
        header1[2].toBytes(), // miner
        header1[4].toBytes(), // transactionsRoot
        header1[5].toBytes(), // receiptsRoot
        header1[6].toBytes(), // logsBloom
        header1[7].toBytes(), // difficulty
        header1[8].toBytes(), // number
        header1[9].toBytes(), // gasLimit
        header1[10].toBytes(), // gasUsed
        header1[11].toBytes() // time
      )
    );

    bytes32 header2HashPart1 = keccak256(
      abi.encodePacked(
        header2[0].toBytes(), // parentHash
        header2[1].toBytes(), // sha3Uncles
        header2[2].toBytes(), // miner
        header2[4].toBytes(), // transactionsRoot
        header2[5].toBytes(), // receiptsRoot
        header2[6].toBytes(), // logsBloom
        header2[7].toBytes(), // difficulty
        header2[8].toBytes(), // number
        header2[9].toBytes(), // gasLimit
        header2[10].toBytes(), // gasUsed
        header2[11].toBytes()  // time
      )
    );
    require(
      header1HashPart1 == header2HashPart1,
      "Part 1 in headers does not match"
    );

    bytes32 header1HashPart2 = keccak256(
      abi.encodePacked(
        header1[13].toBytes(), // mixHash
        header1[14].toBytes()  // nonce
      )
    );

    bytes32 header2HashPart2 = keccak256(
      abi.encodePacked(
        header2[13].toBytes(), // mixHash
        header2[14].toBytes()  // nonce
      )
    );
    require(
      header1HashPart2 == header2HashPart2,
      "Part 2 in headers does not match"
    );

    // Extract the extraData for header1
    bytes memory rlpExtraDataHeader1 = header1[headerExtraDataIndex].toData();
    // Remove the vanity portion of the extra data to get the validator list from the extra data without round
    bytes memory rlpExtraStrippedHeader1 = new bytes(rlpExtraDataHeader1.length - 35);
    SolUtils.BytesToBytes(rlpExtraStrippedHeader1, rlpExtraDataHeader1, 35);
    // This list contains the validator addresses
    RLP.RLPItem[] memory rlpValidatorListHeader1 = rlpExtraStrippedHeader1.toRLPItem().toList();
    bytes memory header1ValidatorList;
    for (uint i = 0; i < rlpValidatorListHeader1.length; i++) {
      bytes memory validatorAddress = rlpValidatorListHeader1[i].toBytes();
      if (keccak256(abi.encodePacked(validatorAddress)) == keccak256(abi.encodePacked(hex"80"))) {
        break;
      }
      header1ValidatorList = abi.encodePacked(header1ValidatorList, validatorAddress);
    }
    bytes32 header1HashValidatorList = keccak256(header1ValidatorList);

    // Extract the extraData for header2
    bytes memory rlpExtraDataHeader2 = header2[headerExtraDataIndex].toData();
    // Remove the vanity portion of the extra data to get the validator list from the extra data without round
    bytes memory rlpExtraStrippedHeader2 = new bytes(rlpExtraDataHeader2.length - 35);
    SolUtils.BytesToBytes(rlpExtraStrippedHeader2, rlpExtraDataHeader2, 35);
    // This list contains the validator addresses
    RLP.RLPItem[] memory rlpValidatorListHeader2 = rlpExtraStrippedHeader2.toRLPItem().toList();
    bytes memory header2ValidatorList;
    for (uint i = 0; i < rlpValidatorListHeader2.length; i++) {
      bytes memory validatorAddress = rlpValidatorListHeader2[i].toBytes();
      bytes memory zero = hex"80";
      if (keccak256(abi.encodePacked(validatorAddress)) == keccak256(abi.encodePacked(hex"80"))) {
        break;
      }
      header2ValidatorList = abi.encodePacked(header2ValidatorList, validatorAddress);
    }
    bytes32 header2HashValidatorList = keccak256(header2ValidatorList);

    require(
      header1HashValidatorList == header2HashValidatorList,
      "The validator list in headers does not match"
    );

    return true;
  }

  /*
   * Verify the BFT block header as part of the block header proving scheme.
   * @param {bytes32} blockHash The block hash to be verified against.
   * @param {bytes} rlpEncodedBlockHeader The RLP-encoded block header to used in the verification process.
   * @param {bytes} rlpEncodedBlockHeaderNoRoundNumber The RLP-encoded block header with no round number.
   * @param {bytes} rlpValidatorSignatures The RLP-encoded validator signatures.
   * @param {uint256} chainId The chain identification.
   * @return {bool} success Returns true if the BFT block header was verified successfully.
   * @return {RLP.RLPItem[]} istanbulExtraList The extracted istanbul extra data as a list.
   * @return {RLP.RLPItem[]} header The extracted header data as a list.
   */
  function verifyBFTBlockHeader(
    bytes32 blockHash,
    bytes memory rlpEncodedBlockHeader,
    bytes memory rlpEncodedBlockHeaderNoRoundNumber,
    bytes memory rlpValidatorSignatures,
    uint256 chainId
  ) public view returns (bool success, RLP.RLPItem[] memory istanbulExtraList, RLP.RLPItem[] memory header) {

    header = rlpEncodedBlockHeader.toRLPItem().toList();

    RLP.RLPItem[] memory headerNoRoundNumber = rlpEncodedBlockHeaderNoRoundNumber.toRLPItem().toList();

    compareHeaders(header, headerNoRoundNumber);

    // Verify the validator signatures
    bytes32 signedHash = keccak256(rlpEncodedBlockHeader);
    require(
      verifyValidatorSignatures(rlpValidatorSignatures, signedHash, chainId),
      "Validator signatures are not valid"
    );

    // Extract the istanbulExtraData
    bytes memory rlpExtraData = header[headerExtraDataIndex].toData();
    bytes memory rlpIstanbulExtra = new bytes(rlpExtraData.length - 35);
    // Remove the vanity portion of the extra data to get the istanbul extra data
    SolUtils.BytesToBytes(rlpIstanbulExtra, rlpExtraData, 35);
    // This list contains the validator addresses
    istanbulExtraList = rlpIstanbulExtra.toRLPItem().toList();

    bytes32 calculatedBlockHash = keccak256(rlpEncodedBlockHeaderNoRoundNumber);
    require(
      calculatedBlockHash == blockHash,
      "Calculated block hash doesn't match passed in block hash"
    );

    success = true;
  }

  /*
   * Update the validator set, for a given chain, from the provided RLP-encoded list.
   * This method will shrink or grow the current validator list and replace all entries with the provided addresses.
   * @param {RLP.RLPItem[]} istanbulExtraList The extracted istanbul extra data as a list.
   * @param {uint256} chainId The chain identification.
   * @return {bool} Returns true if the validator set was updated successfully.
   */
  function updateChainHeadValidatorList(
    RLP.RLPItem[] memory istanbulExtraList,
    uint256 chainId
  ) private returns (bool) {
    // Update the chain head validator list
    uint256 i = 0;
    uint256 chainHeadValidatorsLength = chainHeadValidators[chainId].length;
    for (i = 0; i < istanbulExtraList.length; i++) {
      bytes memory addressCheck = istanbulExtraList[i].toBytes();
      if (addressCheck.length == 21) {
        if (i < chainHeadValidatorsLength) {
          chainHeadValidators[chainId][i] = istanbulExtraList[i].toAddress();
        } else {
          chainHeadValidators[chainId].push(istanbulExtraList[i].toAddress());
        }
      } else {
        while (chainHeadValidators[chainId].length > i) {
          chainHeadValidators[chainId].pop();
        }
        // emit Uint(chainHeadValidators[chainId].length, "chainHeadValidators.length");
        break;
      }
    }

    return true;
  }

  /*
   * Verify validator signatures used in the block header proving scheme.
   * @param {bytes} rlpValidatorSignatures The RLP-encoded validator signatures to be verified.
   * @param {bytes32} signedHash The hash that was signed, to be used in verification of signatures.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored validator sets.
   */
  function verifyValidatorSignatures(
    bytes memory rlpValidatorSignatures,
    bytes32 signedHash,
    uint256 chainId
  ) public view returns (bool) {

    require(chainHeadValidators[chainId].length > 0, "No validators set for this chainId, please set the chain head validators");

    uint256 validSeals = 0;
    address[50] memory addressReuseCheck;
    // TODO: this number should be more than the possible number of validators
    RLP.RLPItem[] memory validatorSignatures = rlpValidatorSignatures
    .toRLPItem()
    .toList();
    for (uint256 i = 0; i < validatorSignatures.length; i++) {
      address signatureAddress = SECP256K1.recover(
        signedHash,
        validatorSignatures[i].toData()
      );
      //emit Address(signatureAddress, "Validator address");
      for (uint256 j = 0; j < chainHeadValidators[chainId].length; j++) {
        if (signatureAddress == chainHeadValidators[chainId][j]) {
          //emit Uint(j, "Recovered signature was at position");
          // Check that a validator's signature wasn't submitted multiple times
          for (uint256 k = 0; k < i; k++) {
            if (addressReuseCheck[k] == signatureAddress) {
              revert(
              "Not allowed to submit multiple seals from the same validator"
              );
            }
          }
          validSeals++;
          addressReuseCheck[i] = signatureAddress;
          break;
        }
      }
    }

    if (validSeals < chainHeadValidators[chainId].length / 2) {
      revert("Not enough valid validator seals");
    }

    return true;
  }

  /*
   * Onboard a proving scheme for a specific chain. This method needs access controls.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored schemes.
   * @param {uint256} scheme The proving scheme identifier.
   */
  function onboardProvingScheme(
    uint256 chainId,
    uint256 scheme
  ) public {
    provingSchemes[chainId] = ProvingScheme(scheme);
  }

  /*
   * Set Corda parameter handlers, for a specific chain, indexed by the given function signature. This method needs access controls.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored functions.
   * @param {uint32} functionSignature The function signature to be used as mapping index into stored parameter handlers for a stored function.
   */
  function setParameterHandlers(
    uint256 chainId,
    uint32 functionSignature,
    Corda.ParameterHandler[] calldata paramHandlers
  ) public {
    delete parameterHandlers[chainId][functionSignature];
    for (uint i = 0; i < paramHandlers.length; i++) {
      parameterHandlers[chainId][functionSignature].push(paramHandlers[i]);
    }
  }

  /*
   * Remove all Corda parameter handlers, for a specific chain, indexed by the given function signature. This method needs access controls.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored parameter handlers.
   * @param {uint32} functionSignature The proving scheme identifier.
   */
  function removeParameterHandlers(
    uint256 chainId,
    uint32 functionSignature
  ) public {
    delete parameterHandlers[chainId][functionSignature];
  }

  /*
   * Get Corda parameter handler, at given index, for a specific chain, indexed by the given function signature. This method needs access controls.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored functions.
   * @param {uint32} functionSignature The function signature to be used as mapping index into stored parameter handlers for the stored function.
   * @param {uint256} index The index of the requested parameter handler as index in the input parameter list of the corresponding stored function.
   */
  function getParameterHandler(
    uint256 chainId,
    uint32 functionSignature,
    uint256 index
  ) public view returns (bytes memory) {
    require(index < parameterHandlers[chainId][functionSignature].length, "Index out of bounds");
    Corda.ParameterHandler storage handler = parameterHandlers[chainId][functionSignature][index];
    return abi.encode(handler);
  }

  /*
   * Decode and verify the event according to the registered proving scheme for the given source chain.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes32} eventSig The event function signature.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The information that a validating implementation can use to determine if the event data, given as part of encodedInfo, is valid.
   */
  function decodeAndVerifyEvent(
    uint256 blockchainId,
    bytes32 eventSig,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) external view {
    if (provingSchemes[blockchainId].scheme == CordaTradeProvingSchemeId) {
      require(handleCordaTradeProvingScheme(blockchainId, encodedInfo, signatureOrProof) == true, 'Corda trade-based proof failed to decode or verify');
    } else if (provingSchemes[blockchainId].scheme == CordaTransactionProvingSchemeId) {
      require(handleCordaTransactionProvingScheme(blockchainId, encodedInfo, signatureOrProof) == true, 'Corda transaction-based proof failed to decode or verify');
    } else if (provingSchemes[blockchainId].scheme == EthereumBlockHeaderProvingSchemeId) {
      require(handleEthereumBlockHeaderProvingScheme(blockchainId, encodedInfo, signatureOrProof) == true, 'Ethereum block-header-based proof failed to decode or verify');
    } else {
      revert("Failed to decode and verify event: Unknown proving scheme");
    }
  }

  /*
   * Data structure for block header proving scheme.
   * @property {bytes} value The event data.
   * @property {bytes} rlpSiblingNodes The RLP-encoded sibling nodes.
   * @property {bytes32} receiptsRoot The RLP-encoded receipts root.
   * @property {bytes32} blockHash The block hash.
   * @property {bytes} rlpBlockHeader The RLP-encoded block header.
   * @property {bytes} rlpBlockHeaderExcludingRound The RLP encoded block header excluding round.
   * @property {bytes} rlpValidatorSignatures The RLP-encoded validator signatures.
   */
  struct BFTBlockHeaderAndEvent {
    bytes value;
    bytes rlpSiblingNodes;
    bytes32 receiptsRoot;
    bytes32 blockHash;
    bytes rlpBlockHeader;
    bytes rlpBlockHeaderExcludingRound;
    bytes rlpValidatorSignatures;
  }

  /*
   * Decode and verify event according to the block header proving scheme.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The RLP-encoded sibling nodes, the receipts tree root in the block header and the block hash.
   * @return {bool} Returns true if the block header proof was verifier successfully.
   */
  function handleEthereumBlockHeaderProvingScheme(
    uint256 blockchainId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public view returns (bool) {

    BFTBlockHeaderAndEvent memory bftBlockHeaderAndEvent = decodeBlockHeaderTransferEventAndProof(encodedInfo, signatureOrProof);

    //TODO: use blockchainId to index in to receiptsRoot to ensure correct source chain is used
    (bool success,, RLP.RLPItem[] memory header) = verifyBFTBlockHeader(
      bftBlockHeaderAndEvent.blockHash,
      bftBlockHeaderAndEvent.rlpBlockHeader,
      bftBlockHeaderAndEvent.rlpBlockHeaderExcludingRound,
      bftBlockHeaderAndEvent.rlpValidatorSignatures,
      blockchainId
    );
    require(success == true, "Block header verification failed");

    require(header[receiptsRootIndex].toBytes32() == bftBlockHeaderAndEvent.receiptsRoot, 'ReceiptsRoot in header is not equal to receiptsRoot for event');

    require(
      verifyEVMEvent(bftBlockHeaderAndEvent.value, bftBlockHeaderAndEvent.rlpSiblingNodes, bftBlockHeaderAndEvent.receiptsRoot) == true,
      "Patricia Merkle proof failed"
    );

    return true;
  }

  /*
   * Decode according to the block header proving scheme.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The RLP-encoded sibling nodes, the receipts tree root in the block header and the block hash.
   * @return {BFTBlockHeaderAndEvent} Returns extracted block header proof data.
   */
  function decodeBlockHeaderTransferEventAndProof(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public pure returns (BFTBlockHeaderAndEvent memory) {
    (, , , bytes memory value) = abi.decode(encodedInfo, (uint256, address, bytes32, bytes));
    (bytes memory rlpSiblingNodes, bytes32 receiptsRoot, bytes32 blockHash, bytes memory rlpBlockHeader, bytes memory rlpBlockHeaderExcludingRound, bytes memory rlpValidatorSignatures) = abi.decode(signatureOrProof, (bytes, bytes32, bytes32, bytes, bytes, bytes));

    return BFTBlockHeaderAndEvent(value, rlpSiblingNodes, receiptsRoot, blockHash, rlpBlockHeader, rlpBlockHeaderExcludingRound, rlpValidatorSignatures);
  }

  /*
   * Verifies the event, according to the block header proving scheme, as a Merkle Patricia tree proof by looping through the parent nodes, each time checking that the hash of the child node is present in the parent node.
   * @param {bytes} value The event data.
   * @param {bytes} rlpNodes The RLP-encoded sibling nodes.
   * @param {bytes32} root The receipts tree root contained in the block header.
   * @return {bool} Returns true if the event was verified successfully.
   */
  function verifyEVMEvent(
    bytes memory value,
    bytes memory rlpNodes,
    bytes32 root
  ) public pure returns (bool) {
    // verify value
    RLP.RLPItem[] memory parentNodes = rlpNodes.toRLPItem().toList();
    RLP.RLPItem memory leafNode = parentNodes[parentNodes.length - 1];
    bytes memory leafNodeValue = leafNode.toList()[1].toData();
    require(
      keccak256(leafNodeValue) == keccak256(value), "FAILURE: proof value doesn't match patricia merkle tree leaf node value"
    );

    // verify root
    RLP.RLPItem memory rootNode = parentNodes[0];
    bytes32 rootNodeHash = keccak256(rootNode.toBytes());
    require(
      rootNodeHash == root, "FAILURE: proof root doesn't match patricia merkle tree root node hash"
    );

    // verify hash inclusion
    if (parentNodes.length > 1) {
      for (uint256 i = 1; i < parentNodes.length; i++) {
        uint256 childIndex = parentNodes.length - i;
        RLP.RLPItem memory childNode = parentNodes[childIndex];
        bytes32 childNodeHash = keccak256(childNode.toBytes());

        RLP.RLPItem memory parentNode = parentNodes[childIndex - 1];
        RLP.RLPItem[] memory parentNodeList = parentNode.toList();
        if (parentNodeList.length == 17) {
          bool included = false;
          for (uint256 branchIndex = 0; branchIndex < parentNodeList.length; branchIndex++) {
            if (parentNodeList[branchIndex].isList()) {
              if (keccak256(parentNodeList[branchIndex].toBytes()) == childNodeHash) {
                included = true;
                break;
              }
            } else {
              if (parentNodeList[branchIndex].toBytes32() == childNodeHash) {
                included = true;
                break;
              }
            }
          }
          require(
            included == true, "FAILURE: branch node couldn't be verified"
          );
        } else if (parentNodeList.length == 2) {
          require(
            parentNodeList[1].toBytes32() == childNodeHash, "FAILURE: extension node couldn't be verified"
          );
        }
      }
    }
    return true;
  }

  /*
   * Decode and verify event according to Corda trade-based proving scheme.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the Corda event data containing the transaction tree root as trade id.
   * @param {bytes} signatureOrProof Contains the participant and notary signatures over the root of the Corda transaction tree.
   * @return {bool} Returns true if the trade-based proof was successfully verified.
   */
  function handleCordaTradeProvingScheme(
    uint256 blockchainId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public view returns (bool) {
    return decodeAndVerifyCordaTradeSignatures(blockchainId, encodedInfo, signatureOrProof);
  }

  /*
   * Decode and verify event according to Corda transaction-based proving scheme.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the Corda event data containing a Merkle proof to verify the component's inclusion in the transaction tree.
   * @param {bytes} signatureOrProof Contains the participant and notary signatures over the root of the Corda transaction tree.
   * @return {bool} Returns true if the Corda transaction-based proof was successfully verified.
   */
  function handleCordaTransactionProvingScheme(
    uint256 blockchainId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public view returns (bool) {
    return decodeAndVerifyCordaTransactionSignatures(blockchainId, encodedInfo, signatureOrProof);
  }

  /*
   * Error structure to handle unsupported signature schemes.
   * @property {uint8} scheme The scheme identification code.
   * @property {string} message The error message.
   */
  error UnsupportedSignatureScheme(
    uint8 scheme,
    string message
  );

  /*
   * Decode a Corda trade-based proof and verify signatures from authenticated notaries.
   * @param {uint256} chainId The source chain identification.
   * @param {bytes} encodedInfo Contains the Corda trade-based proof data.
   * @param {bytes} signatureOrProof Contains the signatures to be verified.
   * @return {bool} Returns true if sufficient valid Corda signatures were given as part of the proof.
   */
  function decodeAndVerifyCordaTradeSignatures(
    uint256 chainId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof) public view returns (bool) {
    Corda.Signatures memory sigs = decodeAndVerifyCordaTrade(encodedInfo, signatureOrProof);
    if (sigs.signatures.length == 0) {
      return false;
    }
    uint numNotaries = 0;
    for (uint i = 0; i < sigs.signatures.length; i++) {
      Corda.Signature memory signed = sigs.signatures[i];
      if (activeNotaries[chainId][signed.by]) {
        uint8 scheme = uint8(extractCordaSignatureScheme(signed.meta));
        if (scheme == ED25519SignatureSchemeId) {
          require(ED25519.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes(signed.meta)) == true, "ED25519 signature does not match");
        } else if (scheme == SECP256R1SignatureSchemeId) {
          require(SECP256R1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(bytes32(signed.sigV)), bytes(signed.meta)) == true, "SECP256R1 signature does not match");
        } else if (scheme == SECP256K1SignatureSchemeId) {
          require(SECP256K1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(bytes32(signed.sigV)), bytes(signed.meta)) == true, "SECP256K1 signature does not match");
        } else {
          revert("Unsupported signature scheme");
          return false;
        }
        numNotaries++;
      }
    }
    if (numNotaries < 1) {
      revert("Trade-based verification requires at least one notary signature");
    }
    return true;
  }

  /*
   * Decode a Corda trade-based proof, verify the event and check the root that was signed against signature meta data.
   * @param {bytes} encodedInfo Contains the Corda trade-based proof data.
   * @param {bytes} signatureOrProof Contains the signatures to be verified.
   * @return {bool} Returns true if the Corda event was successfully verified and matched the root provided in signature meta data.
   */
  function decodeAndVerifyCordaTrade(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof) internal view returns (Corda.Signatures memory) {
    (Corda.Signatures memory sigs) = abi.decode(signatureOrProof, (Corda.Signatures));
    (uint256 chainId,,, bytes memory eventData) = abi.decode(encodedInfo, (uint256, address, bytes32, bytes));
    (Corda.EventData memory evtData) = abi.decode(eventData, (Corda.EventData));
    uint32 eventSignature = uint32(bytes4(evtData.callParameters));
    require(parameterHandlers[chainId][eventSignature].length > 0, "No registered handlers were found for the provided chain and event signature");
    require(Corda.validateTrade(evtData, parameterHandlers[chainId][eventSignature], sigs.proof, sigs.signatures) == true, "Trade-based event verification failed");
    for (uint i = 0; i < sigs.signatures.length; i++) {
      (bytes4 platform, bytes4 schema, bytes32 root) = decodeCordaSignatureMeta(sigs.signatures[i].meta);
      bytes memory a = hex"636F7264610100000080C562000000000001D0000003D00000000300A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3DC0770200A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D";
      bytes memory b = bytes.concat(hex"C00502", hex"54", bytes4ToHex(platform), hex"54", bytes4ToHex(schema));
      bytes memory c = hex"00A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3DC02301A020";
      bytes memory d = hex"0080C562000000000002D00000031200000001D000000309000000040080C562000000000005C0E405A1226E65742E636F7264612E636F72652E63727970746F2E5369676E61626C654461746140450080C562000000000003C02602A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3D40C089020080C562000000000004C04207A1117369676E61747572654D65746164617461A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746145404041420080C562000000000004C02E07A10474784964A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736845404041420080C562000000000005C0B405A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746140450080C562000000000003C02602A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D40C054020080C562000000000004C01E07A10F706C6174666F726D56657273696F6EA103696E7445A101304041420080C562000000000004C01D07A10E736368656D654E756D6265724944A103696E7445A101304041420080C562000000000005C0BB05A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736840450080C562000000000003C02602A3226E65742E636F7264613A62373950654D424C73487875324132337944595261413D3D40C062030080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000004C01507A1066F6666736574A103696E7445A101304041420080C562000000000004C01307A10473697A65A103696E7445A101304041420080C562000000000005C08205A1276E65742E636F7264612E636F72652E63727970746F2E536563757265486173682453484132353640450080C562000000000003C02602A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3D40C022010080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000009C10100";
      bytes memory data = bytes.concat(a, b, c, root, d);
      sigs.signatures[i].meta = data;
    }
    return sigs;
  }

  /*
   * Decode a Corda transaction-based proof and verify signatures from authenticated participants and notaries.
   * @param {uint256} chainId The source chain identification.
   * @param {bytes} encodedInfo Contains the Corda transaction-based proof data.
   * @param {bytes} signatureOrProof Contains the signatures to be verified.
   * @return {bool} Returns true if sufficient valid Corda signatures were given as part of the proof.
   */
  function decodeAndVerifyCordaTransactionSignatures(
    uint256 chainId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof) public view returns (bool) {
    Corda.Signatures memory sigs = decodeAndVerifyCordaTransaction(encodedInfo, signatureOrProof);
    if (sigs.signatures.length == 0) {
      return false;
    }
    uint numNotaries = 0;
    uint numParticipants = 0;
    for (uint i = 0; i < sigs.signatures.length; i++) {
      Corda.Signature memory signed = sigs.signatures[i];
      if (activeNotaries[chainId][signed.by] || activeParticipants[chainId][signed.by]) {
        uint8 scheme = uint8(extractCordaSignatureScheme(signed.meta));
        if (scheme == ED25519SignatureSchemeId) {
          require(ED25519.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes(signed.meta)) == true, "ED25519 signature does not match");
        } else if (scheme == SECP256R1SignatureSchemeId) {
          require(SECP256R1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(uint8(signed.sigV)), bytes(signed.meta)) == true, "SECP256R1 signature does not match");
        } else if (scheme == SECP256K1SignatureSchemeId) {
          require(SECP256K1.verify(bytes32(signed.by), bytes32(signed.sigR), bytes32(signed.sigS), bytes1(uint8(signed.sigV)), bytes(signed.meta)) == true, "SECP256K1 signature does not match");
        } else {
          revert("Unsupported signature scheme");
          return false;
        }
        if (activeNotaries[chainId][signed.by]) {
          numNotaries++;
        }
        if (activeParticipants[chainId][signed.by]) {
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
    return true;
  }

  /*
   * Decode a Corda transaction-based proof, verify the event and check the root that was signed against signature meta data.
   * @param {bytes} encodedInfo Contains the Corda transaction-based proof data.
   * @param {bytes} signatureOrProof Contains the signatures to be verified.
   * @return {bool} Returns true if the Corda event was successfully verified and matched the root provided in signature meta data.
   */
  function decodeAndVerifyCordaTransaction(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof) internal view returns (Corda.Signatures memory) {
    (Corda.Signatures memory sigs) = abi.decode(signatureOrProof, (Corda.Signatures));
    (uint256 chainId,,, bytes memory eventData) = abi.decode(encodedInfo, (uint256, address, bytes32, bytes));
    (Corda.EventData memory evtData) = abi.decode(eventData, (Corda.EventData));
    uint32 eventSignature = uint32(bytes4(evtData.callParameters));
    require(parameterHandlers[chainId][eventSignature].length > 0, "No registered handlers were found for the provided chain and event signature");
    require(Corda.validateEvent(evtData, parameterHandlers[chainId][eventSignature], sigs.proof, sigs.signatures) == true, "Transaction-based event verification failed");
    for (uint i = 0; i < sigs.signatures.length; i++) {
      (bytes4 platform, bytes4 schema, bytes32 root) = decodeCordaSignatureMeta(sigs.signatures[i].meta);
      bytes memory a = hex"636F7264610100000080C562000000000001D0000003D00000000300A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3DC0770200A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D";
      bytes memory b = bytes.concat(hex"C00502", hex"54", bytes4ToHex(platform), hex"54", bytes4ToHex(schema));
      bytes memory c = hex"00A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3DC02301A020";
      bytes memory d = hex"0080C562000000000002D00000031200000001D000000309000000040080C562000000000005C0E405A1226E65742E636F7264612E636F72652E63727970746F2E5369676E61626C654461746140450080C562000000000003C02602A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3D40C089020080C562000000000004C04207A1117369676E61747572654D65746164617461A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746145404041420080C562000000000004C02E07A10474784964A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736845404041420080C562000000000005C0B405A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746140450080C562000000000003C02602A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D40C054020080C562000000000004C01E07A10F706C6174666F726D56657273696F6EA103696E7445A101304041420080C562000000000004C01D07A10E736368656D654E756D6265724944A103696E7445A101304041420080C562000000000005C0BB05A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736840450080C562000000000003C02602A3226E65742E636F7264613A62373950654D424C73487875324132337944595261413D3D40C062030080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000004C01507A1066F6666736574A103696E7445A101304041420080C562000000000004C01307A10473697A65A103696E7445A101304041420080C562000000000005C08205A1276E65742E636F7264612E636F72652E63727970746F2E536563757265486173682453484132353640450080C562000000000003C02602A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3D40C022010080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000009C10100";
      bytes memory data = bytes.concat(a, b, c, root, d);
      sigs.signatures[i].meta = data;
    }
    return sigs;
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
  ) internal view returns (bytes4, bytes4, bytes32) {
    return (SolUtils.BytesToBytes4(data, 0), SolUtils.BytesToBytes4(data, 4), SolUtils.BytesToBytes32(data, 8));
  }

  /*
   * Decode Corda signature meta data and extract the signature scheme identifier.
   * @param {bytes} data Meta data given as part of a Corda signature.
   * @return {bytes1} Returns the signature scheme identifier.
   */
  function extractCordaSignatureScheme(
    bytes memory data
  ) internal view returns (bytes1) {
    return SolUtils.BytesToBytes1(data, 110);
  }

  /*
   * Helper function to convert bytes into hex-encoded bytes.
   * @param {bytes4} i Bytes to be converted.
   * @return {bytes} Returns hex-encoded bytes.
   */
  function bytes4ToHex(
    bytes4 i
  ) public pure returns (bytes memory) {
    return toSlice(abi.encodePacked(i), 3, 4);
  }

  /*
   * Helper function to extract a slice from bytes.
   * @param {bytes} strBytes Bytes to be sliced.
   * @return {bytes} Returns a slice of bytes from the start index to the end index.
   */
  function toSlice(
    bytes memory strBytes,
    uint startIndex,
    uint endIndex) public pure returns (bytes memory) {
    bytes memory result = new bytes(endIndex - startIndex);
    for (uint i = startIndex; i < endIndex; i++) {
      result[i - startIndex] = strBytes[i];
    }
    return result;
  }

  /*
   * Add a Corda notary's public key to the list of authenticated notaries in storage, for the given chain. This method needs access controls.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if notary was successfully added.
   */
  function addNotary(
    uint256 chainId,
    uint256 publicKey
  ) public returns (bool) {
    activeNotaries[chainId][publicKey] = true;
    return true;
  }

  /*
   * Check whether a Corda notary is authenticated, for the given chain.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if the notary's public key is contained in the list of authenticated notaries for the given chain.
   */
  function isNotary(
    uint256 chainId,
    uint256 publicKey
  ) public view returns (bool) {
    return activeNotaries[chainId][publicKey];
  }

  /*
   * Remove a Corda notary's public key from the list of authenticated notaries in storage, for the given chain. This method needs access controls.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The notary's compressed public key.
   * @return {bool} Returns true if notary was successfully added.
   */
  function removeNotary(uint256 chainId, uint256 publicKey) public returns (bool) {
    delete activeNotaries[chainId][publicKey];
    return true;
  }

  /*
   * Add a Corda participant's public key to the list of authenticated participants in storage, for the given chain. This method needs access controls.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if participant was successfully added.
   */
  function addParticipant(uint256 chainId, uint256 publicKey) public returns (bool) {
    activeParticipants[chainId][publicKey] = true;
    return true;
  }

  /*
   * Check whether a Corda participant is authenticated, for the given chain.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if the participant's public key is contained in the list of authenticated participants for the given chain.
   */
  function isParticipant(uint256 chainId, uint256 publicKey) public view returns (bool) {
    return activeParticipants[chainId][publicKey];
  }

  /*
   * Remove a Corda participant's public key from the list of authenticated participants in storage, for the given chain. This method needs access controls.
   * @param {uint256} chainId The chain identification.
   * @param {uint256} publicKey The participant's compressed public key.
   * @return {bool} Returns true if participant was successfully added.
   */
  function removeParticipant(uint256 chainId, uint256 publicKey) public returns (bool) {
    delete activeParticipants[chainId][publicKey];
    return true;
  }
}
