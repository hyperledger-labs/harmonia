/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity >=0.8;

interface CrosschainFunctionCallInterface {
  /*
   * Emits a CrossBlockchainCallExecuted event.
   * @param {uint256} blockchainId The destination chain identification.
   * @param {address} contractAddress The destination contract address.
   * @param {bytes} functionCallData The function call data to emit the event with.
   */
  function crossBlockchainCall(uint256 blockchainId, address contractAddress, bytes calldata functionCallData) external;
}
