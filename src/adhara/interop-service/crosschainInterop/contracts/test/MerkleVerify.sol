/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/Merkle.sol";

contract MerkleVerify {

  function getRoot(bytes32[] memory data) public pure returns (bytes32) {
    return Merkle.getRoot(data);
  }

  function getProof(bytes32[] memory data, uint256 index) public pure returns (bytes32[] memory) {
    return Merkle.getProof(data, index);
  }

  function verifyProof(bytes32 root, bytes32[] memory proof, bytes32 value) public pure returns (bool) {
    return Merkle.verifyProof(root, proof, value);
  }

  function padWithZeros(bytes32[] memory values, bool singleLeaf) public pure returns (bytes32[] memory) {
    return Merkle.padWithZeros(values, singleLeaf);
  }
}
