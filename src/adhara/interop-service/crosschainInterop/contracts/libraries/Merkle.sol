/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/SolidityUtils.sol";

/*
 * Merkle tree library providing multi-valued Merkle proof verification.
 */
library Merkle {

  /*
   * Get the root of a Merkle tree with the given leaves.
   * @param {leaves} The leaves of the Merkle tree.
   * @return {bytes32} Returns the Merkle root.
   */
  function getRoot(
    bytes32[] memory leaves
  ) internal pure returns (bytes32) {
    if (leaves.length == 1)
      return sha256(abi.encodePacked(leaves[0]));
    while (leaves.length > 1) {
      leaves = hashLevel(leaves);
    }
    return leaves[0];
  }

  /*
   * Constructs the proof from the leaves of a Merkle tree and the index of the leave for which to generate the proof.
   * Only for testing purposes. Proof generation should occur off-network.
   * @param	{bytes32[]} data The leaves of the Merkle tree.
   * @param	{uint256} index The index of the element to generate the proof for.
   * @return {bytes32[]} Return the generated roof of length equal to the ceiling of log2(numLeaves).
   */
  function getProof(
    bytes32[] memory data,
    uint256 index
  ) internal pure returns (bytes32[] memory) {
    require(data.length > 1, "Error generating a proof for a single leaf");
    // The size of the proof is equal to the ceiling of log2(numLeaves).
    bytes32[] memory result = new bytes32[](log2Ceil(data.length) * 2);
    uint256 pos = 0;
    // Two overflow risks:
    // Param index is bounded by max array size = 2^256-1. Largest index in the array will be 1 less than that. Also, for dynamic arrays, size is limited to 2^64-1.
    // Param pos is bounded by log2(data.length)*2, which should be less than type(uint256).max.
    while (data.length > 1) {
    unchecked {
      if (index & 0x1 == 1) {
        result[pos * 2 + 0] = bytes32(uint(0));
        result[pos * 2 + 1] = data[index - 1];
      }
      else if (index + 1 == data.length) {
        result[pos * 2 + 0] = bytes32(uint(1));
        result[pos * 2 + 1] = bytes32(uint(0));
      }
      else {
        result[pos * 2 + 0] = bytes32(uint(1));
        result[pos * 2 + 1] = data[index + 1];
      }
      ++pos;
      index /= 2;
    }
      data = hashLevel(data);
    }
    return result;
  }

  /*
   * Verifies a proof, constructed from a Merkle tree.
   * @param	{bytes32} root The root against which to verify the proof.
   * @param {bytes32[]} proof The proof to be verified.
   * @param	{bytes32} value. The value used in the proof.
   * @return {bool} Returns true if the proof was successfully verified.
   */
  function verifyProof(
    bytes32 root,
    bytes32[] memory proof,
    bytes32 value
  ) internal pure returns (bool) {
    // Proof length must be less than max array size.
    bytes32 rollingHash = value;
    uint256 length = proof.length / 2;
  unchecked {
    for (uint i = 0; i < length; ++i) {
      if (uint(proof[i * 2 + 0]) > 0)
        rollingHash = hashLeafPairs(rollingHash, proof[i * 2 + 1]);
      else
        rollingHash = hashLeafPairs(proof[i * 2 + 1], rollingHash);
    }
  }
    return root == rollingHash;
  }

  /* Verifies that the included leaf is equal to the hash of the given value.
   * @param	{bytes32} root The root of the tree.
   * @param	{bytes32} value The leaf to verify as included.
   * @return {bool} return true if the value is an included leaf in the tree with given root.
   */
  function verifyIncludedLeaf(
    bytes32 root,
    bytes32 value
  ) internal pure returns (bool) {
    bytes32 hashed = sha256(abi.encodePacked(value));
    return root == hashed;
  }

  /*
   * Verifies a multi-proof, constructed from a partial Merkle tree.
   * @param	{bytes32} root The root of the Merkle Tree.
   * @param	{bytes32[]} proof The multi-valued Merkle proof.
   * @param	{uint8[]} flags The flags used in the multi-valued Merkle proof
   * @param	{bytes32[]} values The witnesses used in the multi-valued Merkle proof.
   */
  function verifyMultiProof(
    bytes32 root,
    bytes32[] memory proof,
    uint8[] memory flags,
    bytes32[] memory values
  ) internal pure returns (bool) {
    // Rebuilds the root hash by traversing the tree up from the leaves. The root is rebuilt by consuming and producing values on a queue. At the end of the process, the last rolling hash in the hashes array should contain the root of the merkle tree.
    uint256 proofLength = proof.length;
    uint256 leavesLen = values.length;
    uint256 totalHashes = flags.length;
    require(leavesLen + proofLength - 1 == totalHashes, "Invalid multi proof");
    // The xPos values are pointers to the next value to consume in each array. All accesses are done using x[xPos++], which return the current value and increment the pointer, thus mimicking a queue's "pop".
    bytes32[] memory hashes = new bytes32[](totalHashes);
    uint256 leafPos = 0;
    uint256 hashPos = 0;
    uint256 proofPos = 0;
    // At each step, we compute the next hash using two values: A value from the main queue. If not all leaves have been consumed, we get the next leaf, otherwise we get the next hash.
    //                                                          Depending on the flag, either another value from the main queue (merging branches) or an element from the proof array.
    for (uint256 i = 0; i < totalHashes; i++) {
      bytes32 a = leafPos < leavesLen ? values[leafPos++] : hashes[hashPos++];
      bytes32 b = (flags[i] & (1 << 0)) != 1 ? (leafPos < leavesLen ? values[leafPos++] : hashes[hashPos++]) : proof[proofPos++];
      // First bit of flag is to indicate whether it should be used.
      hashes[i] = (flags[i] & (1 << 1)) != 2 ? hashLeafPairs(a, b) : hashLeafPairs(b, a);
      // Second bit of flag is to indicate order in which to hash to cater for unsorted trees.
    }
    if (totalHashes > 0) {
    unchecked {
      return root == hashes[totalHashes - 1];
    }
    } else if (leavesLen > 0) {
      return root == values[0];
    } else {
      return root == proof[0];
    }
  }

  /*
   * Helper function to pad an array of values with zeros until it has k values, where k is a power of two.
   * @param {bytes32[]} values The values to pad with zeroes.
   * @param {bool} singleLeaf Indicate whether to pad a single leaf.
   */
  function padWithZeros(
    bytes32[] memory values,
    bool singleLeaf
  ) internal pure returns (bytes32[] memory) {
    uint256 n = values.length;
    if (isPowerOfTwo(n) && (n > 1 || singleLeaf)) return values;
    uint256 k = 1;
    while (!isPowerOfTwo(++n)) {
      k++;
    }
    n = values.length;
    bytes32[] memory padded = new bytes32[](n + k);
    uint256 i = 0;
    for (; i < n; i++) {
      padded[i] = values[i];
    }
    uint256 j = 0;
    while (j++ < k) {
      padded[i++] = 0x0000000000000000000000000000000000000000000000000000000000000000;
    }
    return padded;
  }

  /*
   * Helper function to check if a number is a power of two.
   * @param {uint} x The value to check if it is a power of two.
   * @return {bool} Returns true if the value is a power of two.
  */
  function isPowerOfTwo(
    uint256 x
  ) internal pure returns (bool) {
    return (x != 0) && ((x & (x - 1)) == 0);
  }

  /*
   * Calculate the ceiling of log_2(x) where x is assumed > 0.
   * @param {uint} x The value to use in the calculation.
   * @return {uint256} Returns the calculated value.
   */
  function log2Ceil(
    uint256 x
  ) internal pure returns (uint256) {
    uint256 ceil = 0;
    uint pOf2;
    // If x is a power of 2, then this function will return a ceiling that is 1 greater than the actual ceiling. So we need to check if x is a power of 2, and subtract one from ceil if so.
    assembly ("memory-safe")  {
    // We check by seeing if x == (~x + 1) & x. This applies a mask to find the lowest set bit of x and then checks it for equality with x. If they are equal, then x is a power of 2.
    // We do some assembly magic to treat the bool as an integer later on.
      pOf2 := eq(and(add(not(x), 1), x), x)
    }
    // If x == type(uint256).max, than ceil is capped at 256.
    // If x == 0, then pO2 == 0, so ceil won't underflow.
  unchecked {
    while (x > 0) {
      x >>= 1;
      ceil++;
    }
    ceil -= pOf2;
    // See above.
  }
    return ceil;
  }

  /*
   * Hash leaf pairs in the Merkle tree
   * @param {bytes32} left The left branch of the pair to hash.
   * @param {bytes32} right The right branch of the pair to hash.
   * @param {bytes32} hash Returns the resulting hash.
   */
  function hashLeafPairs(
    bytes32 left,
    bytes32 right
  ) internal pure returns (bytes32 hash) {
    // Checking if (left < right), and switching left and right otherwise, will result in a sorted tree and would make the proof smaller but Corda does not support this like Ethereum does.
    hash = sha256(bytes.concat(left, right));
  }

  /* Hash a level in the Merkle tree
   * @param {bytes32[]} data The data in the level to hash.
   * @return {bytes32[]} Returns the hashed level to be used as the next level.
   */
  function hashLevel(
    bytes32[] memory data
  ) internal pure returns (bytes32[] memory) {
    bytes32[] memory result;
    // Function is private to prevent unsafe data from being passed, and all internal callers check that data.length >=2.
    // Underflow is not possible as lowest possible value for data/result index is 1.
    // Overflow should be safe as length is / 2 always.
  unchecked {
    uint256 length = data.length;
    if (length & 0x1 == 1) {
      result = new bytes32[](length / 2 + 1);
      result[result.length - 1] = hashLeafPairs(data[length - 1], bytes32(0));
    } else {
      result = new bytes32[](length / 2);
    }
    // Value of pos is upper bounded by data.length / 2, so safe even if array is at max size.
    uint256 pos = 0;
    for (uint256 i = 0; i < length - 1; i += 2) {
      result[pos] = hashLeafPairs(data[i], data[i + 1]);
      ++pos;
    }
  }
    return result;
  }
}
