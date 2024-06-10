/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

import "contracts/interfaces/ICrosschainFunctionCall.sol";
import "contracts/libraries/Corda.sol";
import "contracts/libraries/ED25519.sol";
import "contracts/libraries/RLP.sol";
import "contracts/libraries/SolidityUtils.sol";
import "contracts/CrosschainMessaging.sol";
import "contracts/AuthParams.sol";
import "contracts/interfaces/IInteroperable.sol";

contract CrosschainFunctionCall is ICrosschainFunctionCall, AuthParams {
  /* Owner of the contract. */
  address public owner;

  /* Reference to messaging layer's contract. */
  address public messagingContractAddress;

  /* Events for debugging purposes. */
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Bool(bool, string);
  event Uint(uint256, string);
  event Address(address, string);

  /* Configurable decoding schemes registered per source network doing remote function calls */
  uint256 public CordaTradeDecodingSchemeId = 0;
  uint256 public CordaTransactionDecodingSchemeId = 1;
  uint256 public EthereumEventLogDecodingSchemeId = 2;

  /* Event decoding scheme encoded as 256-bit integer */
  struct EventDecodingScheme {
    uint256 scheme;
  }

  /* Mapping of id to event decoding scheme */
  mapping(uint256 => EventDecodingScheme) eventDecodingSchemes;

  /* Variables required for network specific contract authentication. */
  bool public appendAuthParams = false;
  mapping(uint256 => mapping(address => bool)) authParamsAppended;

  /* Mapping of remote network identification to used hashes */
  uint256 public localNetworkId;
  mapping(uint256 => mapping(bytes32 => bool)) usedHashes;

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
   * Onboard an event decoding scheme for a specific network. This method needs access controls.
   * @param {uint256} networkId The network identification to be used as mapping index into stored schemes.
   * @param {uint256} scheme The event decoding scheme identifier.
   */
  function onboardEventDecodingScheme(
    uint256 networkId,
    uint256 scheme
  ) public {
    eventDecodingSchemes[networkId] = EventDecodingScheme(scheme);
  }

  /*
   * Set the system id for the local network. This method needs access controls.
   * @param {uint256} networkId The network identifier.
   * @return {bool} Returns true if local network's system identifier was successfully updated.
   */
  function setLocalNetworkId(
    uint256 networkId
  ) public returns (bool) {
    localNetworkId = networkId;
    return true;
  }

  /*
   * Get the system id for the local network.
   * @return {uint256} Returns the local network's system identifier.
   */
  function getLocalNetworkId() public view returns (uint256) {
    return localNetworkId;
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
   * Check if a contract is authenticated for a given network.
   * @param {uint256} networkId The network identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract is part of the list of stored authenticated contracts for the given network.
   */
  function isAuthParams(
    uint256 networkId,
    address contractAddress
  ) public view returns (bool) {
    return authParamsAppended[networkId][contractAddress];
  }

  /*
   * Add a contract to the list of stored authenticated contracts, for a given network, to be used in ensuing authentication requests that ensue.
   * @param {uint256} networkId The network identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract was successfully added to the list of stored authenticated contracts for the given network.
   */
  function addAuthParams(
    uint256 networkId,
    address contractAddress
  ) public returns (bool) {
    authParamsAppended[networkId][contractAddress] = true;
    return true;
  }

  /*
   * Remove a contract from the list of stored authenticated contracts, for a given network, to be used in ensuing authentication requests.
   * @param {uint256} networkId The network identification.
   * @param {address} contractAddress The contract address.
   * @return {bool} Returns true if the given contract was successfully removed from the list of stored authenticated contracts for the given network.
   */
  function removeAuthParams(
    uint256 networkId,
    address contractAddress
  ) public returns (bool) {
    delete authParamsAppended[networkId][contractAddress];
    return true;
  }

  /*
   * Verify authentication parameters, attached to the given function call data, against authenticated contracts.
   * @param {uint256} networkId The destination network identification.
   * @param {address} contractAddress The destination contract address.
   * @param {bytes} functionCallData The function call data with attached authentication parameters.
   * @return {bool} Returns true if authentication parameters were successfully verified.
   */
  function verifyAuthParams(
    bytes memory functionCallData
  ) public view returns (bool) {
    (uint256 networkId, address contractAddress) = super.decodeAuthParams();
    return authParamsAppended[networkId][contractAddress];
  }

  /*
   * Emits a CrosschainFunctionCall event after adding authentication parameters to function call data, if enabled.
   * @param {uint256} networkId The destination network identification.
   * @param {address} contractAddress The destination contract address.
   * @param {bytes} functionCallData The function call data to emit the event with.
   */
  function outboundCall(
    uint256 networkId,
    address contractAddress,
    bytes calldata functionCallData
  ) external override {
    if (appendAuthParams) {
      bytes memory functionCallDataWithAuthParams = encodeAuthParams(localNetworkId, msg.sender, functionCallData);
      emit CrosschainFunctionCall(networkId, contractAddress, functionCallDataWithAuthParams);
    } else {
      emit CrosschainFunctionCall(networkId, contractAddress, functionCallData);
    }
  }

  /*
   * Perform function call from a remote network.
   * @param {uint256} networkId The remote source network identification.
   * @param {bytes} encodedInfo Remote source network information to verify and decode verified local network id, destination contract address and function call data from.
   * @param {bytes} encodedProof Remote source network proof need te verify encoded information.
   */
  function inboundCall(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) external override {
    CrosschainMessaging messagingContract = CrosschainMessaging(messagingContractAddress);
    // Verify message from remote source event, transaction or state change data.
    bytes memory decodedInfo = messagingContract.decodeAndVerify(networkId, encodedInfo, encodedProof);
    (uint256 destinationId, address contractAddress, bytes memory functionCallData, bytes32 sourceHash) = abi.decode(decodedInfo, (uint256, address, bytes, bytes32));
    require(localNetworkId == destinationId, string(abi.encodePacked("Local network [", SolUtils.UIntToString(localNetworkId), "] is not the destination network [", SolUtils.UIntToString(destinationId), "] for this message")));
    require(!usedHashes[networkId][sourceHash], "Remote network information was already used to perform a remote function call");
    usedHashes[networkId][sourceHash] = true;
    // Verify authentication parameters
    if (appendAuthParams) {
      require(this.verifyAuthParams(functionCallData) == true, "Verification of authentication parameters failed");
    }
    (bool success, bytes memory data) = contractAddress.call(functionCallData);
    if (!success) {
      SolUtils.revertFromReturnedData(data);
    }
    require(data.length == 0 || abi.decode(data, (bool)), "Remote function call failed");
    emit InboundCallExecuted(networkId, encodedInfo, encodedProof);
  }
}
