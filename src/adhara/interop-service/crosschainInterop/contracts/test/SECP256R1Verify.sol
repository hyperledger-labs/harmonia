/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

import "contracts/libraries/SECP256R1.sol";

contract SECP256R1Verify {

  event Bytes(bytes, string);
  event Bytes32(bytes32, string);
  event UInt256(uint256, string);
  event String(string, string);

  function verify(
    bytes32 k,
    bytes32 r,
    bytes32 s,
    bytes1 v,
    bytes memory m
  ) public view returns (bool) {
    return SECP256R1.verify(k, r, s, v, m);
  }

  function verifySignature(
    uint[2] memory k,
    uint[2] memory rs,
    bytes memory m
  ) public view returns (bool) {
    bytes32 messageHash = sha256(m);
    return SECP256R1.verifySignature(messageHash, rs, k);
  }
}
