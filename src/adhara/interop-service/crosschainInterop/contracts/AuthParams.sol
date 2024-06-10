pragma solidity ^0.8.13;

abstract contract AuthParams {

  /*
   * Encode authentication parameters according to EEA guidelines.
   * @param {uint256} networkId The source network identifier to encode for use in authentication.
   * @param {address} contractAddress The contract address to encode for use in authentication.
   * @param {bytes} functionCallData The function call parameters to append the authentication parameters to.
   * @return {bytes} The encoded function call parameters after including the given authentication parameters.
   */
  function encodeAuthParams(
    uint256 networkId,
    address contractAddress,
    bytes memory functionCallData
  ) internal pure returns (bytes memory) {
    return bytes.concat(functionCallData, abi.encodePacked(networkId, contractAddress));
  }

  /*
   * Decode authentication parameters according to EEA guidelines.
   * @return {uint256} networkId The extracted source network identifier to be used in authentication.
   * @return {address} contractAddress The extracted contract address to the authenticated.
   */
  function decodeAuthParams() internal pure returns (
    uint256 networkId,
    address contractAddress
  ) {
    bytes calldata allParams = msg.data;
    uint256 len = allParams.length;
    assembly ("memory-safe")  {
      calldatacopy(0x0, sub(len, add(52, 8)), 32)
      networkId := mload(0)
      calldatacopy(12, sub(len, add(20, 8)), 20)
      contractAddress := mload(0)
    }
  }

}
