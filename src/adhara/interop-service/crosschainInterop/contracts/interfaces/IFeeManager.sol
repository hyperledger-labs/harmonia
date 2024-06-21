pragma solidity ^0.8.13;

interface IFeeManager {

  /*
   * Adds a zero cost contract, which will exempt this address from paying fees. Will fail if there is no contract at the provided address.
   * @param contractAddress The address of the contract to exempt from paying fees
   * @return A boolean indicating successful execution of the function
   * @dev If successful, emits AddZeroCostContractExecuted(address contractAddress);
   */
  function addZeroCostContract(
    address contractAddress
  ) external virtual returns (bool);

  /*
   * Gets all the zero cost contract addresses.
   * @return zeroCostContractArray An address[] with all the zero cost contract addresses.
   */
  function getZeroCostContracts() external view returns (address[] memory zeroCostContractArray);

}
