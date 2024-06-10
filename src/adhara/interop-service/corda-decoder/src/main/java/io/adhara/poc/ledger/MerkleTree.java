package io.adhara.poc.ledger;

import io.adhara.poc.utils.Utils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class MerkleTree {

  private final SecureHash hash;
  private final MerkleTree left;
  private final MerkleTree right;
  
  private static final String digestAlgorithm = SecureHash.SHA_256;
  private static final Logger logger = LoggerFactory.getLogger(MerkleTree.class);

  public MerkleTree() {
    this.hash = null;
    this.left = null;
    this.right = null;
  }

  public MerkleTree(SecureHash hash, MerkleTree left, MerkleTree right) {
    this.hash = hash;
    this.left = left;
    this.right = right;
  }

  public MerkleTree(SecureHash hash) {
    this.hash = hash;
    this.left = null;
    this.right = null;
  }

  public static MerkleTree getMerkleTree(List<SecureHash> allLeavesHashes) {
    try {
      MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
      return (new MerkleTree()).getMerkleTree(allLeavesHashes, digest);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return null;
  }

  public boolean isNode() { return left != null && right != null; }
  public boolean isLeaf() { return left == null && right == null; }

  // Merkle tree building using hashes, with zero hash padding to full power of 2.
  private MerkleTree getMerkleTree(List<SecureHash> allLeavesHashes, MessageDigest digestService) throws Exception  {
    if (allLeavesHashes.isEmpty())
      throw new Exception("Cannot calculate Merkle root on empty hash list.");
    Set<String> algorithms = allLeavesHashes.stream().map(SecureHash::getAlgorithm).collect(Collectors.toSet());
    if (algorithms.size() != 1) {
      throw new Exception("Cannot build Merkle tree with multiple hash algorithms.");
    }
    List<MerkleTree> leaves = padWithZeros(allLeavesHashes, digestService.getAlgorithm().equals(SecureHash.SHA_256)).stream().map(MerkleTree::new).collect(Collectors.toList());
    return buildMerkleTree(leaves, digestService);
  }

  public PartialMerkleTree buildPartial(List<SecureHash> includedHashes) {
    try {
      PartialMerkleTree partial = PartialMerkleTree.build(this, includedHashes);
      return partial;
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return null;
  }

  public static List<SecureHash> getPaddedLeaves(List<SecureHash> allLeavesHashes) {
    return padWithZeros(allLeavesHashes, false);
  }

  // If number of leaves in the tree is not a power of 2, we need to pad it with zero hashes.
  private static List<SecureHash> padWithZeros(List<SecureHash> allLeavesHashes, boolean singleLeafWithoutPadding) {
    int n = allLeavesHashes.size();
    if (Utils.isPow2(n) && (n > 1 || singleLeafWithoutPadding)) return allLeavesHashes;
    List<SecureHash> paddedHashes = new ArrayList<>(allLeavesHashes);
    SecureHash zeroHash = SecureHash.getZero(paddedHashes.get(0).getAlgorithm());
    do {
      paddedHashes.add(zeroHash);
    } while (!Utils.isPow2(++n));
    return paddedHashes;
  }

  // Tail-recursive function for building a tree bottom up. Input `lastNodesList` contains the Merkle tree nodes from the previous level. Returns the tree root.
  private MerkleTree buildMerkleTree(List<MerkleTree> lastNodesList, MessageDigest digestService) throws Exception {
    if (lastNodesList.size() == 1) {
      return lastNodesList.get(0); // Root reached.
    } else {
      List<MerkleTree> newLevelHashes = new ArrayList<>();
      int n = lastNodesList.size();
      if (Utils.isOdd(n)) {
        throw new Exception("Sanity check: number of nodes should be even.");
      }
      for (int i=0; i<=n-2; i+=2) {
        MerkleTree left = lastNodesList.get(i);
        MerkleTree right = lastNodesList.get(i + 1);
        MerkleTree node = new MerkleTree(new SecureHash(digestService.digest(Utils.concatBytes(left.hash.getBytes(), right.hash.getBytes())), digestAlgorithm), left, right);
        newLevelHashes.add(node);
      }
      return buildMerkleTree(newLevelHashes, digestService);
    }
  }

  private static boolean verifyProof(SecureHash root, List<SecureHash> proof, SecureHash value) {
    // Proof length must be less than max array size.
    SecureHash rollingHash = value;
    int length = proof.size()/2;
    for (int i=0; i<length; ++i) {
      if (!proof.get(i*2).isZero())
        rollingHash = hashLeafPairs(rollingHash, proof.get(i*2+1));
      else
        rollingHash = hashLeafPairs(proof.get(i*2+1), rollingHash);
    }
    return root == rollingHash;
  }

  // GenerateProof generates the proof for a piece of data according to https://arxiv.org/pdf/2002.07648.pdf.
  private static List<SecureHash> generateProof(List<SecureHash> treeLeaves, int index) {
    int l = treeLeaves.size();
    // The size of the proof is equal to the ceiling of log2(numLeaves).
    int proofLen = (int)Math.ceil(Math.log(l)/Math.log(2));
    SecureHash[] result = new SecureHash[proofLen*2];
    int pos = 0;
    SecureHash[] leaves = new SecureHash[l];
    treeLeaves.toArray(leaves);
    while (leaves.length > 1) {
      if (index % 2 == 1) {
        result[pos*2] = SecureHash.getZero(SecureHash.SHA_256);
        result[pos*2+1] = leaves[index - 1];
      }
      else if (index + 1 == leaves.length) {
        result[pos*2] = SecureHash.getOnes(SecureHash.SHA_256);
        result[pos*2+1] = SecureHash.getZero(SecureHash.SHA_256);
      }
      else {
        result[pos*2] = SecureHash.getOnes(SecureHash.SHA_256);
        result[pos*2+1] = leaves[index + 1];
      }
      ++pos;
      index /= 2;
      leaves = hashLevel(leaves);
    }
    return Arrays.asList(result);
  }

  private static SecureHash hashLeafPairs(SecureHash left, SecureHash right) {
    return left.concatenate(SecureHash.SHA_256, right);
  }

  private static SecureHash[] hashLevel(SecureHash[] data) {
    SecureHash[] result;
    int length = data.length;
    if (length % 2 == 1) {
      result = new SecureHash[length/2 + 1];
      result[result.length - 1] = hashLeafPairs(data[length-1], SecureHash.getZero(SecureHash.SHA_256));
    } else {
      result = new SecureHash[length/2];
    }
    // Value of pos is upper bounded by data.length / 2, so safe even if array is at max size.
    int pos = 0;
    for (int i = 0; i < length-1; i+=2) {
      result[pos] = hashLeafPairs(data[i], data[i+1]);
      ++pos;
    }
    return result;
  }

  // Translate to index in tree given height and local index in layer at that height.
  public static int translateIndex(int height, int index) {
    return (int)Math.pow(2,height) - 1 + index;
  }

  // GenerateMultiProof generates the proof for multiple pieces of data.
  public static MerkleProof generateMultiProof(List<SecureHash> treeLeaves, List<SecureHash> data) {
    int l = treeLeaves.size(); // Number of leaves in tree
    int h = (int)Math.ceil(Math.log(l)/Math.log(2)); //+1; // Height of tree with l leaves
    int m = (int)Math.pow(2,h+1)-1; // Number of nodes in tree of given height
    int n = data.size();
    List<List<SecureHash>> hashes = new ArrayList<>(n);
    List<Integer> indices = new ArrayList<>(n);
    // Generate individual proofs.
    for (int i=0; i<n; i++) {
      int index = treeLeaves.indexOf(data.get(i));
      indices.add(index);
      hashes.add(generateProof(treeLeaves, index));
    }
    // Combine the hashes across all proofs and highlight all calculated indices.
    HashMap<Integer, SecureHash> usedHashes = new HashMap<>();
    boolean[] calculatedIndices = new boolean[m];
    for (int i=0; i<n; i++) { // For each included leaf
      int index = indices.get(i); // Index of the leaf
      int d = h;
      int nl = l;
      int idx = index;
      List<SecureHash> proof = hashes.get(i);
      int proofIdx = 0;
      while (nl>1) {
        int globalIndex = translateIndex(d, idx); // Index we are calculating
        if (idx % 2 == 1) {
          usedHashes.put(globalIndex-1, proof.get(proofIdx++*2+1));
        } // Use left sibling in proof
        else if (idx + 1 == nl) {
        } // Use nothing from the proof
        else {
          usedHashes.put(globalIndex+1, proof.get(proofIdx++*2+1));
        } // Use right sibling in proof
        idx /= 2;
        nl /= 2;
        d--;
        calculatedIndices[globalIndex] = true;
      }
    }
    // Remove any hashes that can be calculated.
    int numHashes = 0;
    for (int i=0; i<m; i++) {
      if (calculatedIndices[i]) {
        usedHashes.remove(i);
        numHashes++;
      }
    }
    byte[] flags = new byte[numHashes];
    List<SecureHash> proof = new ArrayList<>();
    int[] idxes = new int[numHashes];
    int leafPos = 0;
    int hashPos = 0;
    int idx = -1;
    int nh = 0;
    for (int i = 0; i < numHashes; i++) {
      if (leafPos < n) { // a is a leaf
        int j = indices.get(leafPos);
        idx = translateIndex(h, j);
        if (idx % 2 == 1) { // a is on the left
          if (usedHashes.containsKey(translateIndex(h, j+1))) {
            // toggle flag to use the value
            flags[i] |= 1;
            proof.add(usedHashes.get(translateIndex(h, j+1)));
          }
        } else { // a is on the right
          // toggle flag to swap
          flags[i] |= (1 << 1);
          if (usedHashes.containsKey(translateIndex(h, j-1))) {
            // toggle flag to use the value
            flags[i] |= 1;
            proof.add(usedHashes.get(translateIndex(h, j-1)));
          }
        }
        leafPos++;
      } else { // a is an element off the stack
        idx = idxes[hashPos];
        hashPos++;
        // get the index of sibling and see if it is a used hash
        if (idx % 2 == 1) {
          // b must be on the right so don't toggle flag
          if (usedHashes.containsKey(idx+1)) {
            flags[i] |= 1;
            proof.add(usedHashes.get(idx+1));
          }
        } else {
          // b must be on the left so toggle flag to swap
          flags[i] |= (1 << 1);
          if (usedHashes.containsKey(idx-1)) {
            flags[i] |= 1;
            proof.add(usedHashes.get(idx-1));
          }
        }
      }
      // Toggle the flag if what we need here is not a leaf,
      if ((flags[i] & 1) != 1) {  // Don't use
        if (leafPos < n) {
          leafPos++;
        } else {
          hashPos++;
        }
      }
      idxes[i] = (idx-1)/2;
      if (idx < 1)
        break;
      nh++;
    }
    SecureHash[] prf = new SecureHash[proof.size()];
    proof.toArray(prf);
    SecureHash[] lvs = new SecureHash[data.size()];
    data.toArray(lvs);
    return new MerkleProof(prf, Arrays.copyOfRange(flags, 0, nh), lvs);
  }

  // Verify verifies a multi-proof. The number of values is equal to half the number of leaves in the Merkle tree
  public static boolean verifyMultiProof(SecureHash root, SecureHash[] proof, byte[] flags, SecureHash[] leaves) {
    // Rebuilds the root hash by traversing the tree up from the leaves. The root is rebuilt by consuming and producing values on a queue. At the end of the process, the last rolling hash in the hashes array should contain the root of the merkle tree.
    int proofLength = proof.length;
    int leavesLen = leaves.length;
    int totalHashes = flags.length;
    assert leavesLen + proofLength - 1 == totalHashes;
    // The xPos values are pointers to the next value to consume in each array. All accesses are done using x[xPos++], which return the current value and increment the pointer, thus mimicking a queue's "pop".
    SecureHash[] hashes = new SecureHash[totalHashes];
    int leafPos = 0;
    int hashPos = 0;
    int proofPos = 0;
    // At each step, we compute the next hash using two values: A value from the main queue. If not all leaves have been consumed, we get the next leaf, otherwise we get the next hash.
    //                                                          Depending on the flag, either another value from the main queue (merging branches) or an element from the proof array.
    for (int i = 0; i < totalHashes; i++) {
      SecureHash a = leafPos < leavesLen ? leaves[leafPos++] : hashes[hashPos++];
      SecureHash b = (flags[i] & 1) != 1 ? (leafPos < leavesLen ? leaves[leafPos++] : hashes[hashPos++]) : proof[proofPos++]; // First bit of flag is to indicate whether it should be used.
      hashes[i] = (flags[i] & (1 << 1)) != 2 ? hashLeafPairs(a, b) : hashLeafPairs(b, a); // Second bit of flag is to indicate order in which to hash to cater for unsorted trees.
    }
    if (totalHashes > 0) {
      return root.equals(hashes[totalHashes - 1]);
    } else if (leavesLen > 0) {
      return root.equals(leaves[0]);
    } else {
      return root.equals(proof[0]);
    }
  }
}