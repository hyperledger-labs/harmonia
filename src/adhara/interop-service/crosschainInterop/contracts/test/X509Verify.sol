
/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

import "contracts/libraries/X509.sol";

contract X509Verify {
  function decodeKey(
    string memory h
  ) public view returns (bytes memory) {
    return X509.decodeKey(h);
  }

  function decodeKeyPayable(
    string memory h
  ) public returns (bytes memory) {
    return X509.decodeKey(h);
  }
}
