pragma solidity ^0.8.3;

import "contracts/interfaces/IInteroperable.sol";
import "contracts/interfaces/ICrosschainFunctionCall.sol";
import "contracts/AuthParams.sol";

contract InteropManager is IInteroperable, ICrosschainFunctionCall, AuthParams
{
  uint256 private constant MAX_REMOTE_CHAIN_SIZE = 50;
  uint256 private constant MAX_LISTING_SIZE = 50;

  uint256 localNetworkId;
  uint256[] private remoteSourceNetworkIndices;
  uint256[] private remoteDestinationNetworkIndices;
  mapping(uint256 => RemoteNetworkData) private remoteSourceNetworkData;
  mapping(uint256 => RemoteNetworkData) private remoteDestinationNetworkData;

  bytes32 private constant INTEROP_MANAGER_STATUS_NON_EXISTENT = "";
  bytes32 private constant INTEROP_MANAGER_STATUS_CREATED = "CREATED";
  bytes32 private constant INTEROP_MANAGER_STATUS_ENABLED = "ENABLED";
  bytes32 private constant INTEROP_MANAGER_STATUS_DISABLED = "DISABLED";

  /* Remote network data structure to store connectors and current status */
  struct RemoteNetworkData {
    address connectorAddress;
    bytes32 status;
  }

  /* Gets the local network id */
  function getLocalNetworkId() external view override returns (uint256) {
    return localNetworkId;
  }

  /* Sets the local network id */
  function setLocalNetworkId(uint256 networkId) external {
    localNetworkId = networkId;
  }

  /*
   * Adds a remote network as a source of remote function calls by linking a local connector to the remote network to enable interop with the remote network.
   * @param {uint256} networkId The id of the remote network.
   * @param {address} connectorAddress The address of the network connector contract in the local network.
   */
  function addRemoteSourceNetwork(
    uint256 networkId,
    address connectorAddress
  ) external override {
    if (remoteSourceNetworkData[networkId].status == INTEROP_MANAGER_STATUS_NON_EXISTENT) {
      remoteSourceNetworkIndices.push(networkId);
    }
    remoteSourceNetworkData[networkId].connectorAddress = connectorAddress;
    remoteSourceNetworkData[networkId].status = INTEROP_MANAGER_STATUS_CREATED;
  }

  /*
   * Removes a remote source network.
   * @param {uint256} networkId The id of the remote network
   */
  function removeRemoteSourceNetwork(
    uint256 networkId
  ) external override {
    delete remoteSourceNetworkData[networkId];
    uint i = 0;
    for (; i < remoteSourceNetworkIndices.length; i++) {
      if (remoteSourceNetworkIndices[i] == networkId) break;
    }
    bool found = i < remoteSourceNetworkIndices.length;
    for (; i < remoteSourceNetworkIndices.length-1; i++) {
      remoteSourceNetworkIndices[i] = remoteSourceNetworkIndices[i+1];
    }
    if (found) remoteSourceNetworkIndices.pop();
  }

  /*
   * Enables the execution of remote function calls from the remote source network.
   * @param {uint256} networkId The id of the remote network.
   */
  function enableRemoteSourceNetwork(
    uint256 networkId
  ) external override {
    require(remoteSourceNetworkData[networkId].status != INTEROP_MANAGER_STATUS_NON_EXISTENT, "The source network is unknown");
    remoteSourceNetworkData[networkId].status = INTEROP_MANAGER_STATUS_ENABLED;
  }

  /*
   * Disables the execution of remote function calls from the remote source network.
   * @param {uint256} networkId The id of the remote network.
   */
  function disableRemoteSourceNetwork(
    uint256 networkId
  ) external override {
    require(remoteSourceNetworkData[networkId].status != INTEROP_MANAGER_STATUS_NON_EXISTENT, "The source network is unknown");
    remoteSourceNetworkData[networkId].status = INTEROP_MANAGER_STATUS_DISABLED;
  }

  /*
   * Gets the remote source network data
   * @param {uint256} networkId The id of the remote network.
   */
  function getRemoteSourceNetworkData(
    uint256 networkId
  ) external view override returns (
    address connectorAddress,
    bytes32 status
  ){
    return (
      remoteSourceNetworkData[networkId].connectorAddress,
      remoteSourceNetworkData[networkId].status
    );
  }

  /*
   * Returns the list of remote source chains registered for interop.
   * @param {uint256} startIndex The starting index from where to list the items.
   * @param {uint256} limit The number of items to return in the list.
   */
  function listRemoteSourceNetworks(
    uint256 startIndex,
    uint256 limit
  ) external view override returns (
    uint256[] memory items,
    bool moreItems,
    uint256 providedStartIndex,
    uint256 providedLimit
  ){
    require(startIndex == 0 || startIndex < remoteSourceNetworkIndices.length, "Start index is out of bounds");
    providedLimit = limit;
    providedStartIndex = startIndex;
    moreItems = false;
    limit = remoteSourceNetworkIndices.length-startIndex;
    if (providedLimit == 0) {
      if (limit > MAX_LISTING_SIZE) {
        moreItems = true;
        limit = MAX_LISTING_SIZE;
      }
    } else if (limit > providedLimit) {
      moreItems = true;
      limit = providedLimit;
    }
    items = new uint256[](limit);
    for (uint i=0; i<limit; i++) {
      items[i] = remoteSourceNetworkIndices[startIndex+i];
    }
  }

  /*
   * Adds a remote network as a destination of remote function calls by linking a local connector to the remote network to enable interop with the remote network.
   * @param {uint256} networkId The id of the remote network.
   * @param {address} connectorAddress The address of the network connector contract in the local network.
   */
  function addRemoteDestinationNetwork(
    uint256 networkId,
    address connectorAddress
  ) external override {
    if (remoteDestinationNetworkData[networkId].status == INTEROP_MANAGER_STATUS_NON_EXISTENT) {
      remoteDestinationNetworkIndices.push(networkId);
    }
    remoteDestinationNetworkData[networkId].connectorAddress = connectorAddress;
    remoteDestinationNetworkData[networkId].status = INTEROP_MANAGER_STATUS_CREATED;
  }

  /*
   * Removes a remote destination network.
   * @param {uint256} networkId The id of the remote network.
   */
  function removeRemoteDestinationNetwork(
    uint256 networkId
  ) external override {
    delete remoteDestinationNetworkData[networkId];
    uint i = 0;
    for (; i < remoteDestinationNetworkIndices.length-1; i++) {
      if (remoteDestinationNetworkIndices[i] == networkId) break;
    }
    bool found = i < remoteDestinationNetworkIndices.length;
    for (; i < remoteDestinationNetworkIndices.length-1; i++) {
      remoteDestinationNetworkIndices[i] = remoteDestinationNetworkIndices[i+1];
    }
    if (found) remoteDestinationNetworkIndices.pop();
  }

  /*
   * Enables the execution of remote function calls from the remote destination network.
   * @param {uint256} networkId The id of the remote network
   */
  function enableRemoteDestinationNetwork(
    uint256 networkId
  ) external override {
    require(remoteDestinationNetworkData[networkId].status != INTEROP_MANAGER_STATUS_NON_EXISTENT, "The destination network is unknown");
    remoteDestinationNetworkData[networkId].status = INTEROP_MANAGER_STATUS_ENABLED;
  }

  /*
   * Disables the execution of remote function calls from the remote destination network.
   * @param {uint256} networkId The id of the remote network
   */
  function disableRemoteDestinationNetwork(
    uint256 networkId
  ) external override{
    require(remoteDestinationNetworkData[networkId].status != INTEROP_MANAGER_STATUS_NON_EXISTENT, "The destination network is unknown");
    remoteDestinationNetworkData[networkId].status = INTEROP_MANAGER_STATUS_DISABLED;
  }

  /*
   * Gets the remote destination network data
   * @param {uint256} networkId The id of the remote network
   */
  function getRemoteDestinationNetworkData(
    uint256 networkId
  ) external view override returns (
    address connectorAddress,
    bytes32 status
  ) {
    return (
      remoteDestinationNetworkData[networkId].connectorAddress,
      remoteDestinationNetworkData[networkId].status
    );
  }

  /*
   * Returns the list of remote destination chains registered for interop
   * @param {uint256} startIndex The starting index from where to list the items.
   * @param {uint256} limit The number of items to return in the list.
   */
  function listRemoteDestinationNetworks(
    uint256 startIndex,
    uint256 limit
  ) external view override returns (
    uint256[] memory items,
    bool moreItems,
    uint256 providedStartIndex,
    uint256 providedLimit
  ) {
    //revert("Listing remote destination chains is not yet supported");
    require(startIndex == 0 || startIndex < remoteDestinationNetworkIndices.length, "Start index is out of bounds");
    providedLimit = limit;
    providedStartIndex = startIndex;
    moreItems = false;
    limit = remoteDestinationNetworkIndices.length-startIndex;
    if (providedLimit == 0) {
      if (limit > MAX_LISTING_SIZE) {
        moreItems = true;
        limit = MAX_LISTING_SIZE;
      }
    } else if (limit > providedLimit) {
      moreItems = true;
      limit = providedLimit;
    }
    items = new uint256[](limit);
    for (uint i=0; i<limit; i++) {
      items[i] = remoteDestinationNetworkIndices[startIndex+i];
    }
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
    RemoteNetworkData memory rcd = remoteDestinationNetworkData[networkId];
    if (rcd.status == INTEROP_MANAGER_STATUS_ENABLED) {
      bytes memory functionCallDataWithAuthParams = encodeAuthParams(localNetworkId, msg.sender, functionCallData);
      emit CrosschainFunctionCall(networkId, contractAddress, functionCallDataWithAuthParams);
    } else if (rcd.status == INTEROP_MANAGER_STATUS_DISABLED || rcd.status == INTEROP_MANAGER_STATUS_CREATED) {
      emit CrosschainFunctionCall(networkId, contractAddress, functionCallData);
    } else {
      revert("Failed to append authentication parameters for unknown destination network");
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
    // Implement me if every to be align with production smart contracts
  }
}

