package io.adhara.poc.ledger;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class PartialMerkleTree {
  private static final Logger logger = LoggerFactory.getLogger(PartialMerkleTree.class);
  private final PartialTree root;

  public PartialMerkleTree(PartialTree root) {
    this.root = root;
  }

  public static PartialMerkleTree build(MerkleTree merkleRoot, List<SecureHash> includeHashes) throws Exception {
    checkFull(merkleRoot, 0); // Throws if it is not a full binary tree.
    List<SecureHash> usedHashes = new ArrayList<>();
    PartialPair tree = buildPartialTree(merkleRoot, includeHashes, usedHashes);
    // Too many included hashes or different ones.
    if (includeHashes.size() != usedHashes.size()) {
      throw new Exception("Some of the provided hashes are not in the tree.");
    }
    return new PartialMerkleTree(tree.getTree());
  }

  // Check if a MerkleTree is full binary tree. Returns the height of the tree if full, otherwise throws exception.
  private static int checkFull(MerkleTree tree, int level) throws Exception {
    if (tree.isLeaf()) {
      return level;
    } else if (tree.isNode()) {
      int l1 = checkFull(tree.getLeft(), level + 1);
      int l2 = checkFull(tree.getRight(), level + 1);
      if (l1 != l2) throw new Exception("Expected a full binary tree.");
      return l1;
    }
    return -1;
  }

  // The first element indicates if in a subtree there is a leaf that is included in that partial tree. The second element refers to that subtree.
  private static PartialPair buildPartialTree(MerkleTree root, List<SecureHash> includeHashes, List<SecureHash> usedHashes) {
    if (root.isLeaf()) {
      if (includeHashes.contains(root.getHash())) {
        usedHashes.add(root.getHash());
        return new PartialPair(true, new PartialTree(root.getHash(), false));
      } else {
        return new PartialPair(false, new PartialTree(root.getHash(), true));
      }
    } else if (root.isNode()) {
      PartialPair leftNode = buildPartialTree(root.getLeft(), includeHashes, usedHashes);
      PartialPair rightNode = buildPartialTree(root.getRight(), includeHashes, usedHashes);
      if (leftNode.getHasIncludedLeaf() || rightNode.getHasIncludedLeaf()) {
        // This node is on a path to some included leaves. Don't store hash.
        PartialTree newTree = new PartialTree(leftNode.getTree(), rightNode.getTree(), root.getHash().getAlgorithm());
        return new PartialPair(true, newTree);
      } else {
        // This node has no included leaves below. Cut the tree here and store a hash as a Leaf.
        PartialTree newTree = new PartialTree(root.getHash(), true);
        return new PartialPair(false, newTree);
      }
    }
    return new PartialPair();
  }

  // Recursive calculation of root of this partial tree. Modifies usedHashes to later check for inclusion with hashes provided.
  public static SecureHash rootAndUsedHashes(PartialTree node, List<SecureHash> usedHashes) {
    if (node.isIncluded()) {
      usedHashes.add(node.getHash());
      return node.getHash();
    } else if (node.isLeaf()) {
      return node.getHash();
    } else if (node.isNode()) {
      SecureHash leftHash = rootAndUsedHashes(node.getLeft(), usedHashes);
      SecureHash rightHash = rootAndUsedHashes(node.getRight(), usedHashes);
      return leftHash.concatenate(node.getHashAlgorithm(), rightHash);
    }
    return null;
  }

  // Function to verify a partial Merkle tree against an input Merkle root and a list of leaves. The tree should only contain the leaves defined in hashesToCheck.
  private boolean verify(SecureHash merkleRootHash, List<SecureHash> hashesToCheck) {
    List<SecureHash> usedHashes = new ArrayList<>();
    SecureHash verifyRoot = rootAndUsedHashes(root, usedHashes);
    return  verifyRoot == merkleRootHash // Tree roots match.
            && hashesToCheck.size() == usedHashes.size() // Obtained the same number of hashes (leaves).
            && hashesToCheck.containsAll(usedHashes); // Lists contain the same elements.
  }

  public static int height(PartialTree node)
  {
    if (node == null)
      return 0;
    else
    {
      int leftHeight = height(node.getLeft());
      int rightHeight = height(node.getRight());
      return leftHeight > rightHeight ? leftHeight + 1 : rightHeight + 1;
    }
  }

  public static void print(PartialMerkleTree root)
  {
    if (root == null)
      return;
    int depth = height(root.getRoot());
    Queue<PartialTree> q = new LinkedList<>();
    q.add(root.getRoot());
    while (true)
    {
      int nodeCount = q.size();
      if (nodeCount == 0)
        break;
      for (int i=0; i<depth; i++) {
        System.out.print("  ");
      }
      while (nodeCount > 0)
      {
        PartialTree node = q.peek();
        assert node != null;
        if (node.getHash() != null)
          System.out.print("(" + node.getHash().toString() + ")");
        q.remove();
        if (node.getLeft() != null)
          q.add(node.getLeft());
        if (node.getRight() != null)
          q.add(node.getRight());

        if (nodeCount > 1) {
          System.out.print(", ");
        }
        nodeCount--;
      }
      depth--;
      System.out.println();
    }
  }

  public MerkleProof generateMultiProof() {
    PartialTree node = root;
    List<SecureHash> leaves = new ArrayList<>();
    List<SecureHash> proof = new ArrayList<>();
    Stack<PartialTree> stack = new Stack<>();
    Queue<PartialTree> queue = new LinkedList<>();
    queue.add(node);
    while (!queue.isEmpty())
    {
      node = queue.peek();
      queue.remove();
      stack.push(node);
      if (node.getRight() != null)
        queue.add(node.getRight());
      if (node.getLeft() != null)
        queue.add(node.getLeft());
    }
    List<Byte> flags = new ArrayList<>();
    while (!stack.empty())
    {
      node = stack.peek();
      if (node.isIncluded()) {
        leaves.add(node.getHash());
      } else if (node.isLeaf()) {
        proof.add(node.getHash());
      } else if (node.isNode()) {
        byte flag = 0x00;
        boolean isLeftLeaf = leaves.contains(node.getLeft().getHash());
        boolean isRightLeaf = leaves.contains(node.getRight().getHash());
        if (isLeftLeaf && isRightLeaf) {
          // Don't use proof at this index, both are included leaves
        } else if (isLeftLeaf) {
          // Use this value
          flag |= 1;
        } else if (isRightLeaf) {
          // Use this value
          flag |= 1;
          // Swap this value
          flag |= (1 << 1);
        } else {
          boolean isLeftWitness = proof.contains(node.getLeft().getHash());
          boolean isRightWitness = proof.contains(node.getRight().getHash());
          if (isLeftWitness && isRightWitness) {
            // Don't use proof at this index. Should never happen
          } else if (isLeftWitness) {
            // Use this value
            flag |= 1;
            // Swap this value
            flag |= (1 << 1);
          } else if (isRightWitness) {
            // Use this value
            flag |= 1;
          } else {
            // Should never happen
          }
        }
        flags.add(flag);
      }
      stack.pop();
    }
    byte[] fls = new byte[flags.size()];
    for (int f=0; f<flags.size(); f++) {
      fls[f] = flags.get(f);
    }
    SecureHash[] prf = new SecureHash[proof.size()];
    proof.toArray(prf);
    SecureHash[] lvs = new SecureHash[leaves.size()];
    leaves.toArray(lvs);
    return new MerkleProof(prf, fls, lvs);
  }
}
