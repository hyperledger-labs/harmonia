/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/ED25519.sol";

contract ED25519Verify {
  function verify(
    bytes32 k,
    bytes32 r,
    bytes32 s,
    bytes memory m
  ) public pure returns (bool) {
    return ED25519.verify(k, r, s, m);
  }

  event Bool(bool, string);

  function verifyWithEvent(
    bytes32 k,
    bytes32 r,
    bytes32 s,
    bytes memory m
  ) public {
    emit Bool(ED25519.verify(k, r, s, m), "Verification result");
  }

  function decode(bytes memory d) public returns (bytes memory)  {
    return ED25519.decode(d);
  }
}
