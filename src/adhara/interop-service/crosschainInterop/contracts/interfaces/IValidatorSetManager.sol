pragma solidity ^0.8.13;

interface IValidatorSetManager {
  /*
   * Set the validators.
   * @param {address[]} validators The new set of validators
   * @dev Emits a SetValidatorsExecuted event if successful.
   */
  function setValidators(address[] calldata validators) external;

  event SetValidatorsExecuted(uint256 blockNumber, address[] validators);

  /*
   * Set the validators and sync remote chains through the interop manager.
   * @param {address[]} validators The new set of validators
   * @dev Emits a SetValidatorsExecuted event if successful.
   */
  function setValidatorsAndSyncRemotes(address[] calldata validators) external;

  event SetValidatorsAndSyncRemoteExecuted(uint256 networkId, uint256 blockNumber, address[] validators);

  /*
   * Method used by blockchain client to get the validator set. This method interface is dictated by Hyperledger Besu, and should ONLY be modified if a change is required in order to maintain compatibility with Hyperledger Besu.
   * @return {address[]} An address array with the addresses of the desired active validators.
   */
  function getValidators() external view returns (address[] memory);

  /*
   * Get the current validators and sync remote chains through the interop manager.
   * @dev Emits a SetValidatorsExecuted event if successful.
   */
  function getValidatorsAndSyncRemotes() external;

  event GetValidatorsAndSyncRemoteExecuted(uint256 networkId, uint256 blockNumber, address[] validators);

  /*
   * Set the address of the interop manager on the network.
   * @param {address} interopManager The address of the interop manager contract
   * @return {bool} A boolean indicating successful execution of the function.
   * @dev Emits a SetInteropManagerExecuted event if successful.
   */
  function setInteropManager(address interopManager) external returns (bool);

  event SetInteropManagerExecuted(address interopManager);

  /*
   * Get the address of the interop manager
   * @return {address} interopManager the address of the interop manager.
   */
  function getInteropManager() external view returns (address interopManager);
}
