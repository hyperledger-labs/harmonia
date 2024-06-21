/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

interface ICrosschainVerifier {

  /*
   * Decode and verify the event, transaction or state change according to the registered proving scheme for the given source network.
   * @param {uint256} networkId The source network identification.
   * @param {bytes} encodedInfo The combined encoding of the network identifier, the destination contract's address, the event function signature, and the event data.
   * @param {bytes} signatureOrProof The information that a validating implementation can use to determine if the event data, given as part of encodedInfo, is valid.
   * @return {bytes} decodedInfo The decoded information that needs to be acted on after verification of it by means of the provided proof, if any.
   */
  function decodeAndVerify(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) external view returns (bytes memory decodedInfo);
}
