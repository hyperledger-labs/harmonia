/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity >=0.8;

interface CrosschainVerifier {

  /*
   * Decode and verify the event according to the registered proving scheme for the given source chain.
   * @param {uint256} blockchainId The source chain identification.
   * @param {bytes32} eventSig The event function signature.
   * @param {bytes} encodedInfo The combined encoding of the blockchain identifier, the cross-chain control contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The information that a validating implementation can use to determine if the event data, given as part of encodedInfo, is valid.
   */
  function decodeAndVerifyEvent(
    uint256 blockchainId,
    bytes32 eventSig,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof) external view;
}
