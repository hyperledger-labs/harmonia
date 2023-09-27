pragma solidity >=0.8;

abstract contract NonAtomicHiddenAuthParams {

  /*
   * Encode authentication parameters according to EEA guidelines.
   * @param {bytes} functionCall The function call parameters to append the authentication parameters to.
   * @param {uint256} sourceBlockchainId The source chain identifier to encode for use in authentication.
   * @param {address} sourceContract The contract address to encode for use in authentication.
   * @return {bytes} The encoded function call parameters after including the given authentication parameters.
   */
  function encodeNonAtomicAuthParams(
    bytes memory functionCall,
    uint256 sourceBlockchainId,
    address sourceContract
  ) internal pure returns (bytes memory) {
    return bytes.concat(functionCall, abi.encodePacked(sourceBlockchainId, sourceContract));
  }

  /*
   * Decode authentication parameters according to EEA guidelines.
   * @return {uint256} sourceBlockchainId The extracted source chain identifier to be used in authentication.
   * @return {address} sourceContract The extracted contract address to the authenticated.
   */
  function decodeNonAtomicAuthParams() internal pure returns (
    uint256 sourceBlockchainId,
    address sourceContract
  ) {
    bytes calldata allParams = msg.data;
    uint256 len = allParams.length;
    assembly {
      calldatacopy(0x0, sub(len, add(52, 8)), 32)
      sourceBlockchainId := mload(0)
      calldatacopy(12, sub(len, add(20, 8)), 20)
      sourceContract := mload(0)
    }
  }

}
