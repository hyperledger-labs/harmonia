/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

import "contracts/../../contracts/interfaces/CrosschainFunctionCallInterface.sol";
import "contracts/../../contracts/libraries/Corda.sol";
import "contracts/../../contracts/libraries/ED25519.sol";
import "contracts/../../contracts/libraries/RLP.sol";
import "contracts/../../contracts/libraries/SolidityUtils.sol";
import "contracts/../../contracts/CrosschainMessaging.sol";
import "contracts/../../contracts/NonAtomicHiddenAuthParams.sol";

contract CrosschainFunctionCall is CrosschainFunctionCallInterface, NonAtomicHiddenAuthParams {
  using RLP for RLP.RLPItem;
  using RLP for RLP.Iterator;
  using RLP for bytes;

  /* Owner of the contract. */
  address public owner;

  /* Reference to messaging layer's contract. */
  address public messagingContractAddress;

  /*
   * Event used to facilitate remote function calls.
   * @property {uint256} destinationBlockchainId The destination chain identification.
   * @property {address} contractAddress The destination contract address.
   * @property {bytes} functionCallData The function call data of the cross-chain function that is to be called through the interoperability protocol.
   */
  event CrossBlockchainCallExecuted(
    uint256 destinationBlockchainId,
    address contractAddress,
    bytes functionCallData
  );

  /* Events for debugging purposes. */
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Bool(bool, string);
  event Uint(uint256, string);
  event Address(address, string);

  /* Configurable decoding schemes registered per source chain doing remote function calls */
  uint256 public CordaTradeDecodingSchemeId = 0;
  uint256 public CordaTransactionDecodingSchemeId = 1;
  uint256 public EthereumEventLogDecodingSchemeId = 2;

  /* Event decoding scheme encoded as 256-bit integer */
  struct EventDecodingScheme {
    uint256 scheme;
  }

  /* Mapping of id to event decoding scheme */
  mapping(uint256 => EventDecodingScheme) eventDecodingSchemes;

  /* Variables required for chain specific contract authentication. */
  uint256 public systemId;
  bool public appendAuthParams = false;
  mapping(uint256 => mapping(address => bool)) authParamsAppended;

  /* Indices into an Ethereum block header */
  uint256 public extraDataIndex = 12;
  uint256 public blockNumberIndex = 8;

  constructor() {
    owner = msg.sender;
  }

  /*
   * Setting the messaging layer's contract address.
   * @param {address} contractAddress The messaging layer's contract address.
   * @return {bool} Returns true if the messaging layer's contract address was successfully updated.
   */
  function setMessagingContractAddress(
    address contractAddress
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can set the messagingContractAddress"
    );
    messagingContractAddress = contractAddress;
    return true;
  }

  /*
   * Onboard an event decoding scheme for a specific chain. This method needs access controls.
   * @param {uint256} chainId The chain identification to be used as mapping index into stored schemes.
   * @param {uint256} scheme The event decoding scheme identifier.
   */
  function onboardEventDecodingScheme(
    uint256 chainId,
    uint256 scheme
  ) public {
    eventDecodingSchemes[chainId] = EventDecodingScheme(scheme);
  }

  /*
   * Set the system id for the local chain. This method needs access controls.
   * @param {uint256} chainId The chain identifier.
   * @return {bool} Returns true if local chain's system identifier was successfully updated.
   */
  function setSystemId(
    uint256 chainId
  ) public returns (bool) {
    systemId = chainId;
    return true;
  }

  /*
   * Get the system id for the local chain.
   * @return {uint256} Returns the local chain's system identifier.
   */
  function getSystemId() public view returns (uint256) {
    return systemId;
  }

  /*
   * Enable or disable the use of authentication parameters. This method needs access controls.
   * @param {bool} enable Boolean flag to enable or disable.
   * @return {bool} Returns true if the use of authentication parameters was successfully updated.
   */
  function setAppendAuthParams(
    bool enable
  ) public returns (bool) {
    appendAuthParams = enable;
    return true;
  }

  /*
   * Check if the use of authentication parameters is enabled.
   * @return {bool} Returns true if the use of authentication parameters is enabled.
   */
  function getAppendAuthParams() public view returns (bool) {
    return appendAuthParams;
  }

  /*
   * Check if a contract is authenticated for a given chain.
   * @param {uint256} blockchainId The chain identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract is part of the list of stored authenticated contracts for the given chain.
   */
  function isAuthParams(
    uint256 blockchainId,
    address contractAddress
  ) public view returns (bool) {
    return authParamsAppended[blockchainId][contractAddress];
  }

  /*
   * Add a contract to the list of stored authenticated contracts, for a given chain, to be used in ensuing authentication requests that ensue.
   * @param {uint256} blockchainId The chain identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract was successfully added to the list of stored authenticated contracts for the given chain.
   */
  function addAuthParams(
    uint256 blockchainId,
    address contractAddress
  ) public returns (bool) {
    authParamsAppended[blockchainId][contractAddress] = true;
    return true;
  }

  /*
   * Remove a contract from the list of stored authenticated contracts, for a given chain, to be used in ensuing authentication requests.
   * @param {uint256} blockchainId The chain identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract was successfully removed from the list of stored authenticated contracts for the given chain.
   */
  function removeAuthParams(
    uint256 blockchainId,
    address contractAddress
  ) public returns (bool) {
    delete authParamsAppended[blockchainId][contractAddress];
    return true;
  }

  /*
   * Emits a CrossBlockchainCallExecuted event after adding authentication parameters to function call data, if enabled.
   * @param {uint256} blockchainId The destination chain identification.
   * @param {address} contractAddress The destination contract address.
   * @param {bytes} functionCallData The function call data to emit the event with.
   */
  function crossBlockchainCall(
    uint256 blockchainId,
    address contractAddress,
    bytes calldata functionCallData
  ) external {
    if (appendAuthParams) {
      bytes memory functionCallDataWithAuthParams = encodeNonAtomicAuthParams(functionCallData, systemId, msg.sender);
      emit CrossBlockchainCallExecuted(blockchainId, contractAddress, functionCallDataWithAuthParams);
    } else {
      emit CrossBlockchainCallExecuted(blockchainId, contractAddress, functionCallData);
    }
  }

  /*
   * Verify authentication parameters, attached to the given function call data, against authenticated contracts.
   * @param {uint256} blockchainId The destination chain identification.
   * @param {address} contractAddress The destination contract address.
   * @param {bytes} functionCallData The function call data with attached authentication parameters.
   * @return {bool} Returns true if authentication parameters were successfully verified.
   */
  function verifyAuthParams(
    bytes memory functionCallData
  ) public returns (bool) {
    (uint256 _sourceBlockchainId, address _sourceContract) = super.decodeNonAtomicAuthParams();
    return authParamsAppended[_sourceBlockchainId][_sourceContract];
  }

  /*
   * Perform function call from a remote chain.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes32} eventSig The event function signature.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The information that a validating implementation can use to determine if the event data, given as part of encodedInfo, is valid.
   * @return {bool} Returns true if remote function call was successfully performed.
   */
  function performCallFromRemoteChain(
    uint256 blockchainId, // Source
    bytes32 eventSig,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public returns (bool) {
    CrosschainMessaging messagingContract = CrosschainMessaging(
      messagingContractAddress
    );

    // TODO: store a hash of the concatenation of the blockHash and the value (hpRLPEncodedEventLog) and check that this event log hasn't been used before
    // TODO: verify the destination blockchain id matches this chain's id before execution
    messagingContract.decodeAndVerifyEvent(
      blockchainId,
      eventSig,
      encodedInfo,
      signatureOrProof
    );
    address contractAddress;
    bytes memory functionCallData;

    if (eventDecodingSchemes[blockchainId].scheme == EthereumEventLogDecodingSchemeId) {
      (contractAddress, functionCallData) = handleEthereumEventLogDecoding(encodedInfo, signatureOrProof);
    } else if (eventDecodingSchemes[blockchainId].scheme == CordaTradeDecodingSchemeId) {
      (contractAddress, functionCallData) = handleCordaTransactionDecoding(encodedInfo, signatureOrProof);
    } else if (eventDecodingSchemes[blockchainId].scheme == CordaTransactionDecodingSchemeId) {
      (contractAddress, functionCallData) = handleCordaTransactionDecoding(encodedInfo, signatureOrProof);
    } else {
      revert("Unsupported event decoding scheme");
      return false;
    }

    // verify authentication parameters
    if (appendAuthParams) {
      require(this.verifyAuthParams(functionCallData) == true, "Verification of NonAtomicAuthParams failed");
    }

    // TODO: add check that a contract is deployed at address
    (bool success, bytes memory data) = contractAddress.call(functionCallData);
    if (!success) {
      revertFromReturnedData(data);
    }
    require(
      data.length == 0 || abi.decode(data, (bool)),
      "Cross-chain function call failed"
    );

    return true;
  }

  /*
   * Start the process of updating the validator set configuration on a remote chain.
   * If contract validator selection is used on the local chain, then the validator set manager contract address should be given as parameter, so that a call can be made to get the validator addresses.
   * If block header validator selection is used on the local chain, then a recent block header should be given as parameter, so that the validator list can be extracted from it.
   * @param {uint256} destinationBlockchainId Contains the remote chain identification.
   * @param {address} destinationContract Contains the remote chain's messaging contract's address where the validator set for interoperability needs to be updated.
   * @param {string} operationId Operation id to ensure event can be uniquely identified by event managers that lacks other idempotent operations
   * @param {address} cvsContractAddress Address of validator set management contract, use only when contract validator selection is used.
   * @param {bytes} bvsBlockHeader RLP-encoded block header with no round number, use only when block header validator selection is used.
   * @return {bool} Returns true if the process of updating the validator set configuration remotely, was successfully started.
   */
  function startValidatorUpdate(
    uint256 destinationBlockchainId,
    address destinationContract,
    string calldata operationId,
    address cvsContractAddress,
    bytes calldata bvsBlockHeader
  ) public returns (bool) {
    // Get validator list from given block header or contract address
    address[] memory proposedValidatorList;
    if (cvsContractAddress != address(0)) {
      bytes memory callData = abi.encodeWithSelector(bytes4(keccak256(bytes("getValidators()"))));
      (bool success, bytes memory data) = cvsContractAddress.call(callData);
      if (!success) {
        revertFromReturnedData(data);
      }
      require(data.length != 0, "Failed to get validators from contract");
      proposedValidatorList = abi.decode(data, (address[]));
    } else {// If bvs block header is given, get the block number from the header en get the hash
      RLP.RLPItem[] memory header = bvsBlockHeader.toRLPItem().toList();
      // Extract block number from the given block header
      uint256 blockNumber = header[blockNumberIndex].toUint();
      require(blockNumber > (block.number - 256), "Block is too outdated to be used for updating validator sets");
      bytes32 blockHash = blockhash(blockNumber);
      // Only most recent 256 blocks will return non-zero value
      // Validate by computing hash of the given block header
      bytes32 calculatedBlockHash = keccak256(bvsBlockHeader);
      require(calculatedBlockHash == blockHash, "Calculated block hash doesn't match chain block hash");
      // Extract extra data from the given block header
      bytes memory rlpExtraDataHeader = header[extraDataIndex].toData();
      bytes memory rlpExtraStrippedHeader = new bytes(rlpExtraDataHeader.length - 35);
      // Remove the vanity portion of the extra data
      SolUtils.BytesToBytes(rlpExtraStrippedHeader, rlpExtraDataHeader, 35);
      // Extract validator list from the extra data
      RLP.RLPItem[] memory validatorList = rlpExtraStrippedHeader.toRLPItem().toList();
      uint256 validatorCount = 0;
      for (uint256 i = 0; i < validatorList.length; i++) {
        if (validatorList[i].isEmpty())
          break;
        validatorCount++;
      }
      proposedValidatorList = new address[](validatorCount);
      for (uint256 i = 0; i < validatorCount; i++) {
        proposedValidatorList[i] = validatorList[i].toAddress();
      }
    }
    require(proposedValidatorList.length > 0, "Found no validators in block header");

    // Construct function call data to update validator list on foreign chain
    bytes4 SELECTOR = bytes4(keccak256(bytes("setValidatorList(uint256,string,address[])")));
    bytes memory functionCallData = abi.encodeWithSelector(
      SELECTOR,
      systemId,
      operationId,
      proposedValidatorList
    );

    CrosschainFunctionCall functionCallContract = CrosschainFunctionCall(
      address(this)
    );
    functionCallContract.crossBlockchainCall(
      destinationBlockchainId,
      destinationContract,
      functionCallData
    );

    return true;
  }

  /*
   * Extract a revert from the raw data as returned from a smart contract call. It supports panic, error & custom errors.
   * Taken from https://github.com/superfluid-finance/protocol-monorepo/blob/dev/packages/ethereum-contracts/contracts/libs/CallUtils.sol.
   * @param {bytes} returnedData The data returned from calling another contract.
   */
  function revertFromReturnedData(
    bytes memory returnedData
  ) internal pure {
    if (returnedData.length < 4) {
      // case 1: catch all
      revert("Target revert");
    } else {
      bytes4 errorSelector;
      assembly {
        errorSelector := mload(add(returnedData, 0x20))
      }
      if (errorSelector == bytes4(0x4e487b71) /* `seth sig "Panic(uint256)"` */) {
        // case 2: Panic(uint256) (Defined since 0.8.0)
        // solhint-disable-next-line max-line-length
        // ref: https://docs.soliditylang.org/en/v0.8.0/control-structures.html#panic-via-assert-and-error-via-require)
        string memory reason = "Target panicked: 0x__";
        uint errorCode;
        assembly {
          errorCode := mload(add(returnedData, 0x24))
          let reasonWord := mload(add(reason, 0x20))
        // [0..9] is converted to ['0'..'9']
        // [0xa..0xf] is not correctly converted to ['a'..'f']
        // but since panic code doesn't have those cases, we will ignore them for now!
          let e1 := add(and(errorCode, 0xf), 0x30)
          let e2 := shl(8, add(shr(4, and(errorCode, 0xf0)), 0x30))
          reasonWord := or(
          and(reasonWord, 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000),
          or(e2, e1))
          mstore(add(reason, 0x20), reasonWord)
        }
        revert(reason);
      } else {
        // case 3: Error(string) (Defined at least since 0.7.0)
        // case 4: Custom errors (Defined since 0.8.0)
        uint len = returnedData.length;
        assembly {
          revert(add(returnedData, 32), len)
        }
      }
    }
  }

  /*
   * Decode the event and Merkle Patricia proof data according to the block header proving scheme.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof Contains the signatures over the root of the Corda transaction tree.
   * @return {address} The extracted cross-chain control contract's address.
   * @return {bytes} The extracted function call parameters, that the remote function should be called with.
   */
  function handleEthereumEventLogDecoding(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public returns (
    address,
    bytes memory
  ){
    (bytes32 eventSig, bytes memory value,,,) = decodeBlockHeaderTransferEventAndProof(encodedInfo, signatureOrProof);
    // Get event log contract address, topic and data
    RLP.RLPItem[] memory eventItemList = value.toRLPItem().toList();
    // The RLP encoded event log is at index 3
    bytes memory rlpEventLog = eventItemList[3].toBytes();
    RLP.RLPItem[] memory eventLogList = rlpEventLog.toRLPItem().toList();
    RLP.RLPItem[] memory eventLogFields;
    for (uint i = 0; i < eventLogList.length; i++) {
      eventLogFields = eventLogList[i].toBytes().toRLPItem().toList();
      // The log topic is at index 1
      bytes32 topic = SolUtils.BytesToBytes32(eventLogFields[1].toBytes(), 2);
      if (topic == eventSig) {
        // The log contract address is at index 0
        //bytes memory logContractAddress = new bytes(eventLogFields[0].toBytes().length - 2);
        //SolUtils.BytesToBytes(logContractAddress, eventLogFields[0].toBytes(), 2);
        //emit Bytes(logContractAddress, "log contract address");
        //emit Bytes32(topic, "log topic");
        // The log data is at index 2
        //emit Bytes(eventLogFields[2].toBytes(), "log data");
        break;
      }
    }
    // The first 3 bytes need to be removed, assume this is to get rid of the HP encoding
    uint trim = 3;
    uint remainder = eventLogFields[2].toBytes().length % 2;
    if (remainder == 0)
      trim = 2;
    bytes memory logData = new bytes(eventLogFields[2].toBytes().length - trim);
    SolUtils.BytesToBytes(logData, eventLogFields[2].toBytes(), trim);
    (, address contractAddress, bytes memory functionCallData) = abi.decode(logData, (uint256, address, bytes));
    return (contractAddress, functionCallData);
  }

  /*
   * Decode the event and Corda proof data according to the Corda transaction-based (or trade-based) proving scheme.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the Corda event data containing a Merkle proof to verify the event's inclusion in the transaction tree.
   * @param {bytes} signatureOrProof Contains the signatures over the root of the Corda transaction tree.
   * @return {address} The extracted cross-chain control contract's address.
   * @return {bytes} The extracted function call parameters, that the remote function should be called with.
   */
  function handleCordaTransactionDecoding(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public returns (
    address,
    bytes memory
  ){
    (,address contractAddress,,bytes memory eventData) = abi.decode(encodedInfo, (uint256, address, bytes32, bytes));
    (Corda.EventData memory evtData) = abi.decode(eventData, (Corda.EventData));
    return (contractAddress, evtData.callParameters);
  }

  /*
   * Decode the event and Merkle Patricia proof data according to the block header proving scheme.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the rlp-encoded event we want to prove to be a member of the receipts tree.
   * @param {bytes} signatureOrProof The RLP-encoded sibling nodes, the receipts tree root in the block header and the block hash.
   * @return {bytes32} eventSig The extracted event function signature.
   * @return {bytes} value The extracted event data.
   * @return {bytes} rlpSiblingNodes The extracted RLP-encoded sibling nodes.
   * @return {bytes32} receiptsRoot The extracted receipts tree root.
   * @return {bytes32} blockHash The extracted the block hash.
   */
  function decodeBlockHeaderTransferEventAndProof(
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) public pure returns (
    bytes32 eventSig,
    bytes memory value,
    bytes memory rlpSiblingNodes,
    bytes32 receiptsRoot,
    bytes32 blockHash
  ){
    (,,bytes32 _eventSig, bytes memory _value) = abi.decode(encodedInfo, (uint256, address, bytes32, bytes));
    eventSig = _eventSig;
    value = _value;
    (bytes memory _rlpSiblingNodes, bytes32 _receiptsRoot, bytes32 _blockHash) = abi.decode(signatureOrProof, (bytes, bytes32, bytes32));
    rlpSiblingNodes = _rlpSiblingNodes;
    receiptsRoot = _receiptsRoot;
    blockHash = _blockHash;
  }
}
