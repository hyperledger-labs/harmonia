/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/Ethereum.sol";

contract EthereumVerify {

  function verifyBFTBlockHeader(
    bytes32 blockHash,
    bytes memory blockHeader,
    bytes memory blockHeaderPreImage
  ) public view returns (uint256 blockNumber, bytes32 receiptsRoot) {
    return Ethereum.verifyBFTBlockHeader(blockHash, blockHeader, blockHeaderPreImage);
  }

  function verifyEVMEvent(
    Ethereum.EventData memory eventData,
    bytes32 root,
    bytes memory witnesses
  ) public pure returns (bool) {
    return Ethereum.verifyEVMEvent(eventData, root, witnesses);
  }

  function compareBlockHeaders(
    RLP.RLPItem[] memory header1,
    RLP.RLPItem[] memory header2
  ) internal view {
    Ethereum.compareBlockHeaders(header1, header2);
  }
}
