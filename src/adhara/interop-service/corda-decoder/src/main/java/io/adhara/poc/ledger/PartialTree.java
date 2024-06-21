package io.adhara.poc.ledger;

import lombok.Data;

@Data
public class PartialTree {
  private SecureHash hash;
  private PartialTree left;
  private PartialTree right;
  private String hashAlgorithm;
  private Type type;

  private enum Type {
    INCLUDED,
    LEAF,
    NODE,
  }

  public PartialTree(SecureHash hash, boolean isLeaf) {
    this.hash = hash;
    this.type = isLeaf ? Type.LEAF : Type.INCLUDED;
  }

  public PartialTree(PartialTree left, PartialTree right, String hashAlgorithm) {
    this.left = left;
    this.right = right;
    this.hashAlgorithm = hashAlgorithm;
    this.type = Type.NODE;
  }

  public boolean isLeaf() { return type == Type.LEAF; }
  public boolean isIncluded() { return type == Type.INCLUDED; }
  public boolean isNode() { return type == Type.NODE; }
}