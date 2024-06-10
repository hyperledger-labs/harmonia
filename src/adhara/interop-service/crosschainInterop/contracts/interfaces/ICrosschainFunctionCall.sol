/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

interface ICrosschainFunctionCall {

  /*
   * Attempts to trigger a function call on a remote blockchain, which will only succeed of a valid proof can be provided.
   * @param networkId The remote destination network identification where the remote function call is to be made.
   * @param contractAddress The address of the remote destination contract to which the call is to be made.
   * @param functionCallData The encoded function selector and parameter data of the function to call on the remote blockchain.
   * @dev Emits a CrosschainFunctionCall event if successful.
   * @dev Emits a OutboundCallExecuted event if successful.
   */
  function outboundCall(
    uint256 networkId,
    address contractAddress,
    bytes calldata functionCallData
  ) external;

  /*
   * Event used to facilitate remote function calls. The event is included in the EEA interop spec.
   * @property {uint256} networkId The destination network identification.
   * @property {address} contractAddress The destination contract address.
   * @property {bytes} functionCallData The function call data of the remote function that is to be called through the interoperability protocol, with augmented authentication parameters.
   */
  event CrosschainFunctionCall(
    uint256 networkId,
    address contractAddress,
    bytes functionCallData
  );

  /*
   * Event used to confirm that a remote function call was successfully initiated locally.
   * @property {uint256} networkId The destination network identification.
   * @property {address} contractAddress The destination contract address.
   * @property {bytes} functionCallData The function call data of the remote function that is to be called through the interoperability protocol.
   */
  event OutboundCallExecuted(
    uint256 networkId,
    address contractAddress,
    bytes functionCallData
  );

  /*
   * Attempts to perform a local function call initiated from a remote network.
   * @param {uint256} networkId The remote source network identification from where the call was initiated.
   * @param {bytes} encodedInfo The ABI-encoded remote source network information containing the local destination network identifier, contract address and function call data encoded in remote event, transaction or state change data that needs to be verified before executing the function call locally.
   * @param {bytes} encodedProof The ABI-encoded remote source network proof data and/or signatures that an implementation can use to verify the information given in encodedInfo.
   * @dev Emits a InboundCallExecuted event if successful.
   */
  function inboundCall(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata encodedProof
  ) external;

  /*
   * Event used to confirm that a remotely initiated function call was successfully executed locally.
   * @property {uint256} networkId The remote source network identification from where the call was initiated.
   * @property {bytes} encodedInfo The ABI-encoded remote source network information.
   * @property {bytes} encodedProof The ABI-encoded remote source network proof data and/or signatures.
   */
  event InboundCallExecuted(
    uint256 networkId,
    bytes encodedInfo,
    bytes encodedProof
  );
}
