/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/X509.sol";
import "contracts/libraries/Base64.sol";
import "contracts/libraries/Merkle.sol";
import "contracts/libraries/RLP.sol";
import "contracts/libraries/SolidityUtils.sol";

/*
 * Library to handle Ethereum block header proof verification.
 */
library Ethereum {
  using RLP for RLP.RLPItem;
  using RLP for RLP.Iterator;
  using RLP for bytes;

  uint256 private constant receiptsRootIndex = 5;
  uint256 private constant headerNumberIndex = 8;
  uint256 private constant headerExtraDataIndex = 12;

  /*
   * Contains the Ethereum event log records and event index to indicate which event to use.
   * @property {uint256} index The index of the event in the event log.
   * @property {bytes32} signature The event signatures used as topic in the event log.
   * @property {bytes} logs The rlp-encoded event log records as taken from an Ethereum block.
   */
  struct EventData {
    uint256 index;
    bytes32 signature;
    bytes logs;
  }

  /*
   * Structure to hold Ethereum block header meta data.
   * @property {bytes} rlpBlockHeader The rlp-encoded block header.
   * @property {bytes} rlpBlockHeaderPreimage The rlp-encoded block header preimage.
   */
  struct BlockHeaderMeta {
    bytes rlpBlockHeader;
    bytes rlpBlockHeaderPreimage;
  }

  /*
   * Structure to hold Merkle Patricia tree proof data.
   * @property {bytes} witnesses Merkle proof witnesses.
   * @property {bytes32} root The block receipt root.
   * @property {bytes32} blockHash The block hash.
   * @property {bytes} blockHeaderMeta The rlp-encoded block header metadata.
   */
  struct ProofData {
    bytes witnesses;
    bytes32 root;
    bytes32 blockHash;
    bytes blockHeaderMeta;
  }

  /*
   * Structure to hold a signature for EEA-compliant proofs.
   * @property {uint256} by The 160-bit derived Ethereum address of the validator.
   * @property {uint256} sigR The ECDSA signature's R value.
   * @property {uint256} sigS The ECDSA signature's S value.
   * @property {uint256} sigV The ECDSA signature's V value.
   * @property {bytes} meta Signature meta data.
   */
  struct Signature {
    uint256 by;
    uint256 sigR;
    uint256 sigS;
    uint256 sigV;
    bytes meta;
  }

  /*
   * Structure to hold EEA-compliant proof consisting of proof data and signatures.
   * @property {ProofData} proofData The data contained in the proof, e.g. witnesses, flags and values.
   * @property {Signature[]} signatures The array of signatures.
   */
  struct Proof {
    uint256 typ;
    ProofData proofData;
    Signature[] signatures;
  }

  /*
   * Compare two block headers given as RLP lists.
   * @param {RLP.RLPItem[]} header1 The first header.
   * @param {RLP.RLPItem[]} header2 The second header, to compare with the first.
   * @return {bool} Returns true if the two block headers are equal.
   */
  function compareBlockHeaders(
    RLP.RLPItem[] memory header1,
    RLP.RLPItem[] memory header2
  ) internal view {
    bytes32 header1HashPart1 = keccak256(
      abi.encodePacked(
        header1[0].toBytes(), // parentHash
        header1[1].toBytes(), // sha3Uncles
        header1[2].toBytes(), // miner
        header1[4].toBytes(), // transactionsRoot
        header1[5].toBytes(), // receiptsRoot
        header1[6].toBytes(), // logsBloom
        header1[7].toBytes(), // difficulty
        header1[8].toBytes(), // number
        header1[9].toBytes(), // gasLimit
        header1[10].toBytes(),// gasUsed
        header1[11].toBytes() // time
      )
    );
    bytes32 header2HashPart1 = keccak256(
      abi.encodePacked(
        header2[0].toBytes(), // parentHash
        header2[1].toBytes(), // sha3Uncles
        header2[2].toBytes(), // miner
        header2[4].toBytes(), // transactionsRoot
        header2[5].toBytes(), // receiptsRoot
        header2[6].toBytes(), // logsBloom
        header2[7].toBytes(), // difficulty
        header2[8].toBytes(), // number
        header2[9].toBytes(), // gasLimit
        header2[10].toBytes(),// gasUsed
        header2[11].toBytes() // time
      )
    );
    require(header1HashPart1 == header2HashPart1, "Part 1 in headers does not match");
    bytes32 header1HashPart2 = keccak256(
      abi.encodePacked(
        header1[13].toBytes(), // mixHash
        header1[14].toBytes()  // nonce
      )
    );
    bytes32 header2HashPart2 = keccak256(
      abi.encodePacked(
        header2[13].toBytes(), // mixHash
        header2[14].toBytes()  // nonce
      )
    );
    require(header1HashPart2 == header2HashPart2, "Part 2 in headers does not match");
    // Extract the extraData for header1
    bytes memory rlpExtraDataHeader1 = header1[headerExtraDataIndex].toData();
    // Remove the vanity portion of the extra data to get the validator list from the extra data without round
    bytes memory rlpExtraStrippedHeader1 = new bytes(rlpExtraDataHeader1.length - 35);
    SolUtils.BytesToBytes(rlpExtraStrippedHeader1, rlpExtraDataHeader1, 35);
    // This list contains the validator addresses
    RLP.RLPItem[] memory rlpValidatorListHeader1 = rlpExtraStrippedHeader1.toRLPItem().toList();
    bytes memory header1ValidatorList;
    for (uint i = 0; i < rlpValidatorListHeader1.length; i++) {
      bytes memory validatorAddress = rlpValidatorListHeader1[i].toBytes();
      if (keccak256(abi.encodePacked(validatorAddress)) == keccak256(abi.encodePacked(hex"80"))) {
        break;
      }
      header1ValidatorList = abi.encodePacked(header1ValidatorList, validatorAddress);
    }
    bytes32 header1HashValidatorList = keccak256(header1ValidatorList);
    // Extract the extraData for header2
    bytes memory rlpExtraDataHeader2 = header2[headerExtraDataIndex].toData();
    // Remove the vanity portion of the extra data to get the validator list from the extra data without round
    bytes memory rlpExtraStrippedHeader2 = new bytes(rlpExtraDataHeader2.length - 35);
    SolUtils.BytesToBytes(rlpExtraStrippedHeader2, rlpExtraDataHeader2, 35);
    // This list contains the validator addresses
    RLP.RLPItem[] memory rlpValidatorListHeader2 = rlpExtraStrippedHeader2.toRLPItem().toList();
    bytes memory header2ValidatorList;
    for (uint i = 0; i < rlpValidatorListHeader2.length; i++) {
      bytes memory validatorAddress = rlpValidatorListHeader2[i].toBytes();
      if (keccak256(abi.encodePacked(validatorAddress)) == keccak256(abi.encodePacked(hex"80"))) {
        break;
      }
      header2ValidatorList = abi.encodePacked(header2ValidatorList, validatorAddress);
    }
    bytes32 header2HashValidatorList = keccak256(header2ValidatorList);
    require(header1HashValidatorList == header2HashValidatorList, "The validator list in headers does not match");
  }

  /*
   * Verify the BFT block header as part of the block header proving scheme.
   * @param {bytes32} blockHash The block hash to be verified against.
   * @param {bytes} BlockHeader The RLP-encoded block header to used in the verification process.
   * @param {bytes} BlockHeaderPreImage The RLP-encoded block header with no round number.
   * @return {uint256} blockNumber Returns the extracted block number.
   * @return {bytes32} receiptsRoot Returns the extracted receipts root that was signed over.
   */
  function verifyBFTBlockHeader(
    bytes32 blockHash,
    bytes memory blockHeader,
    bytes memory blockHeaderPreImage
  ) internal view returns (uint256 blockNumber, bytes32 receiptsRoot) {
    RLP.RLPItem[] memory header = blockHeader.toRLPItem().toList();
    RLP.RLPItem[] memory headerPreImage = blockHeaderPreImage.toRLPItem().toList();
    compareBlockHeaders(header, headerPreImage);
    // Extract the istanbulExtraData
    bytes memory rlpExtraData = header[headerExtraDataIndex].toData();
    bytes memory rlpIstanbulExtra = new bytes(rlpExtraData.length - 35);
    // Remove the vanity portion of the extra data to get the istanbul extra data
    SolUtils.BytesToBytes(rlpIstanbulExtra, rlpExtraData, 35);
    bytes32 calculatedBlockHash = keccak256(blockHeaderPreImage);
    require(calculatedBlockHash == blockHash, "Calculated block hash doesn't match passed in block hash");
    return (header[headerNumberIndex].toUint(), header[receiptsRootIndex].toBytes32());
  }

  /*
   * Verifies the event, according to the block header proving scheme, as a Merkle Patricia tree proof by looping through the parent nodes, each time checking that the hash of the child node is present in the parent node.
   * @param {bytes} value The event data.
   * @param {bytes} rlpNodes The RLP-encoded sibling nodes.
   * @param {bytes32} root The receipts tree root contained in the block header.
   * @return {bool} Returns true if the event was verified successfully.
   */
  function verifyEVMEvent(
    Ethereum.EventData memory eventData,
    bytes32 root,
    bytes memory witnesses
  ) internal pure returns (bool) {
    // Verify value
    RLP.RLPItem[] memory parentNodes = witnesses.toRLPItem().toList();
    RLP.RLPItem memory leafNode = parentNodes[parentNodes.length - 1];
    bytes memory leafNodeValue = leafNode.toList()[1].toData();
    require(keccak256(leafNodeValue) == keccak256(eventData.logs), "Proof value doesn't match patricia merkle tree leaf node value");
    // Verify root
    RLP.RLPItem memory rootNode = parentNodes[0];
    bytes32 rootNodeHash = keccak256(rootNode.toBytes());
    require(rootNodeHash == root, "Proof root doesn't match patricia merkle tree root node hash");
    // Verify hash inclusion
    if (parentNodes.length > 1) {
      for (uint256 i = 1; i < parentNodes.length; i++) {
        uint256 childIndex = parentNodes.length - i;
        RLP.RLPItem memory childNode = parentNodes[childIndex];
        bytes32 childNodeHash = keccak256(childNode.toBytes());
        RLP.RLPItem memory parentNode = parentNodes[childIndex - 1];
        RLP.RLPItem[] memory parentNodeList = parentNode.toList();
        if (parentNodeList.length == 17) {
          bool included = false;
          for (uint256 branchIndex = 0; branchIndex < parentNodeList.length; branchIndex++) {
            if (parentNodeList[branchIndex].isList()) {
              if (keccak256(parentNodeList[branchIndex].toBytes()) == childNodeHash) {
                included = true;
                break;
              }
            } else {
              if (parentNodeList[branchIndex].toBytes32() == childNodeHash) {
                included = true;
                break;
              }
            }
          }
          require(included == true, "Branch node couldn't be verified");
        } else if (parentNodeList.length == 2) {
          require(parentNodeList[1].toBytes32() == childNodeHash, "Extension node couldn't be verified");
        }
      }
    }
    return true;
  }

  function extractEVMEventLogData(
    Ethereum.EventData memory eventData
  ) internal view returns (bytes memory){
    // Get event log contract address, topic and data
    RLP.RLPItem[] memory eventItemList = eventData.logs.toRLPItem().toList();
    // The RLP encoded event log is at index 3
    bytes memory rlpEventLog = eventItemList[3].toBytes();
    RLP.RLPItem[] memory eventLogList = rlpEventLog.toRLPItem().toList();
    require(eventData.index < eventLogList.length, "Event index into event log is out of bounds.");
    RLP.RLPItem[] memory eventLogFields = eventLogList[eventData.index].toBytes().toRLPItem().toList();
    bytes32 topic = SolUtils.BytesToBytes32(eventLogFields[1].toBytes(), 2);
    require(topic == eventData.signature, string(abi.encodePacked("Event signature does not match the topic in event log at given index ", SolUtils.UIntToString(eventData.index), ", got ", SolUtils.Bytes32ToHexString(topic), " but expected ", SolUtils.Bytes32ToHexString(eventData.signature))));
    // The first 3 bytes need to be removed
    uint remainder = eventLogFields[2].toBytes().length % 2;
    uint trim = (remainder == 0) ? 2 : 3;
    bytes memory logData = new bytes(eventLogFields[2].toBytes().length - trim);
    SolUtils.BytesToBytes(logData, eventLogFields[2].toBytes(), trim);
    return logData;
  }
}
