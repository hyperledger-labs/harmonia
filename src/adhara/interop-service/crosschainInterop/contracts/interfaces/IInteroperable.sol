pragma solidity ^0.8.7;

interface IInteroperable
{
  /*
   * Gets the local network id
   */
  function getLocalNetworkId() external view returns (uint256);

  /*
   * Adds a remote network as a source of remote function calls
   * by linking a local connector to the remote network to enable interop with the remote network
   * @param networkId the id of the remote network
   * @param connectorAddress the address of the network connector contract in the local network
   */
  function addRemoteSourceNetwork(
    uint256 networkId,
    address connectorAddress
  ) external;

  event AddRemoteSourceNetworkExecuted(uint256 networkId, address connectorAddress);

  /*
   * Removes a remote source network
   * @param networkId the id of the remote network
   */
  function removeRemoteSourceNetwork(
    uint256 networkId
  ) external;

  event RemoveRemoteSourceNetworkExecuted(uint256 networkId);

  /*
   * Enables the execution of remote function calls from the remote source network
   * @param networkId the id of the remote network
   */
  function enableRemoteSourceNetwork(
    uint256 networkId
  ) external;

  event EnableRemoteSourceNetworkExecuted(uint256 networkId);

  /*
   * Disables the execution of remote function calls from the remote source network
   * @param networkId the id of the remote network
   */
  function disableRemoteSourceNetwork(
    uint256 networkId
  ) external;

  event DisableRemoteSourceNetworkExecuted(uint256 networkId);

  /*
   * Gets the remote source network data
   * @param networkId the id of the remote network
   */
  function getRemoteSourceNetworkData(
    uint256 networkId
  ) external view returns (
    address connectorAddress,
    bytes32 status
  );

  /*
   * Returns the list of remote source chains registered for interop
   * @param startIndex The starting index from where to list the items
   * @param limit The number of items to return in the list
   */
  function listRemoteSourceNetworks(
    uint256 startIndex,
    uint256 limit
  ) external view returns (
    uint256[] memory items,
    bool moreItems,
    uint256 providedStartIndex,
    uint256 providedLimit
  );

  /*
   * Adds a remote network as a destination of remote function calls
   * by linking a local connector to the remote network to enable interop with the remote network
   * @param networkId the id of the remote network
   * @param connectorAddress the address of the network connector contract in the local network
   */
  function addRemoteDestinationNetwork(
    uint256 networkId,
    address connectorAddress
  ) external;

  event AddRemoteDestinationNetworkExecuted(uint256 networkId, address connectorAddress);

  /*
   * Removes a remote destination network
   * @param networkId the id of the remote network
   */
  function removeRemoteDestinationNetwork(
    uint256 networkId
  ) external;

  event RemoveRemoteDestinationNetworkExecuted(uint256 networkId);

  /*
   * Enables the execution of remote function calls from the remote destination network
   * @param networkId the id of the remote network
   */
  function enableRemoteDestinationNetwork(
    uint256 networkId
  ) external;

  event EnableRemoteDestinationNetworkExecuted(uint256 networkId);

  /*
   * Disables the execution of remote function calls from the remote destination network
   * @param networkId the id of the remote network
   */
  function disableRemoteDestinationNetwork(
    uint256 networkId
  ) external;

  event DisableRemoteDestinationNetworkExecuted(uint256 networkId);

  /*
   * Gets the remote destination network data
   * @param networkId the id of the remote network
   */
  function getRemoteDestinationNetworkData(
    uint256 networkId
  ) external view returns (
    address connectorAddress,
    bytes32 status
  );

  /*
   * Returns the list of remote destination networks registered for interop
   * @param startIndex The starting index from where to list the items
   * @param limit The number of items to return in the list
   */
  function listRemoteDestinationNetworks(
    uint256 startIndex,
    uint256 limit
  ) external view returns (
    uint256[] memory items,
    bool moreItems,
    uint256 providedStartIndex,
    uint256 providedLimit
  );
}
