/*
 * Copyright 2023, R3 LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.r3.corda.interop.evm.common.trie

import org.web3j.crypto.Hash.sha3
import org.web3j.rlp.*
import org.web3j.utils.Numeric

/**
 * The base class for all types of nodes in a Patricia Trie.
 */
sealed class Node {
    /**
     * Get the encoded version of this node.
     * @return encoded byte array of this node.
     */
    abstract val encoded: ByteArray

    /**
     * Get the SHA-3 hash of the encoded node.
     * @return hash of the encoded node.
     */
    open val hash: ByteArray
        get() = sha3(encoded)

    /**
     * Puts a key-value pair into a node of the Patricia Trie.
     * If the node does not exist, a new LeafNode is created.
     *
     * @param node The node where to put the key-value pair.
     * @param key The key to put.
     * @param value The value to put.
     * @return The node where the key-value pair was put.
     */
    abstract fun put(key: NibbleArray, newValue: ByteArray): Node

    /**
     * Gets the value for a given key from the Patricia Trie.
     *
     * @param key The key for which to get the value.
     * @return The value associated with the key, or an empty ByteArray if the key does not exist.
     */
    abstract fun get(key: NibbleArray): ByteArray

    /**
     * Provide a String representation of the Node for debugging.
     * @return String representation of the Node.
     */
    override fun toString(): String {
        return "NodeType: ${javaClass.simpleName} " +
                "Hash: ${Numeric.toHexString(hash)} " +
                "Encoded: ${Numeric.toHexString(encoded)}"
    }

    companion object {

        private val emptyArray = ByteArray(0)
        val empty: EmptyNode get() = EmptyNode

        fun leaf(key: ByteArray, value: ByteArray): LeafNode = leaf(NibbleArray.fromBytes(key), value)
        fun leaf(key: NibbleArray, value: ByteArray): LeafNode = LeafNode(key, value)

        fun branch(): BranchNode = branch(emptyArray)
        fun branch(value: ByteArray) = branch(Array<Node>(16) { EmptyNode }, value)

        fun branch(sparseBranches: List<Pair<Int, Node>>): BranchNode = branch(sparseBranches, emptyArray)
        fun branch(sparseBranches: List<Pair<Int, Node>>, value: ByteArray): BranchNode {
            val branches = Array<Node>(16) { empty }
            sparseBranches.forEach { (index, branch) ->
                branches[index] = branch
            }
            return branch(branches, value)
        }

        private fun branch(branches: Array<Node>, value: ByteArray): BranchNode = BranchNode(branches, value)

        fun extension(path: NibbleArray, value: Node): ExtensionNode = ExtensionNode(path, value)

        /**
         * Create a Node from a RLP encoded byte array.
         * @param encoded RLP encoded byte array.
         * @return Node created from the RLP encoded byte array.
         */
        fun createFromRLP(encoded: ByteArray): Node =
            if (encoded.size == 32) HashNode(encoded, empty) else createFromRLP(RlpDecoder.decode(encoded) as RlpList)

        /**
         * Create a Node from a RLP list.
         * @param outerList RLP list.
         * @return Node created from the RLP list.
         */
        private fun createFromRLP(outerList: RlpList): Node {
            val rlpList = outerList.values[0] as RlpList

            return when (rlpList.values.size) {
                2 -> nonBranchNode((rlpList.values[0] as RlpString).bytes, rlpList.values[1])
                17 -> branchNode(rlpList.values.subList(0, 16), (rlpList.values[16] as RlpString).bytes)
                else -> throw IllegalArgumentException("Invalid RLP encoding")
            }
        }

        private fun branchNode(branchValues: List<RlpType>, valueBytes: ByteArray): BranchNode {
            val branches = branchValues.mapIndexed { index, value ->
                when (value) {
                    is RlpString -> if (value.bytes.isNotEmpty()) createFromRLP(value.bytes) else EmptyNode
                    is RlpList -> createFromRLP(value)
                    else -> throw IllegalArgumentException("Invalid RLP encoding")
                }
            }.toTypedArray()

            return branch(branches, valueBytes)
        }

        private fun nonBranchNode(keyBytes: ByteArray, valueOrNode: RlpType): Node {
            val allNibbles = NibbleArray.fromBytes(keyBytes)
            val prefix = PatriciaTriePathPrefix.fromNibbles(allNibbles)
            val pathType = PatriciaTriePathType.forPrefix(prefix)
            val pathNibbles = allNibbles.dropFirst(prefix.prefixNibbles.size)

            return when (valueOrNode) {
                is RlpString -> {
                    if (pathType == PatriciaTriePathType.LEAF) leaf(pathNibbles, valueOrNode.bytes)
                    else extension(pathNibbles, createFromRLP(valueOrNode.bytes))
                }

                is RlpList -> extension(pathNibbles, createFromRLP(valueOrNode))
                else -> throw IllegalArgumentException("Invalid RLP encoding")
            }
        }
    }

    /**
     * Represents an EmptyNode in a Patricia Trie.
     *
     * This class is a specific type of Node, with its encoded form
     * being the RLP (Recursive Length Prefix) encoding of an empty byte array.
     */
    object EmptyNode : Node() {
        /**
         * Returns the RLP-encoded form of the EmptyNode,
         * which is an empty byte array encoded in RLP.
         */
        override val encoded: ByteArray
            get() = RlpEncoder.encode(RlpString.create(ByteArray(0)))

        override fun put(key: NibbleArray, newValue: ByteArray): Node = leaf(key, newValue)

        override fun get(key: NibbleArray): ByteArray = ByteArray(0)
    }

    /**
     * Represents an ExtensionNode in a Patricia Trie.
     *
     * An ExtensionNode consists of a path and an inner node.
     * The encoded form is an RLP (Recursive Length Prefix) encoded list of the path and the inner node.
     *
     * @property path The path of the ExtensionNode, stored as a nibbles array.
     * @property innerNode The inner Node that the ExtensionNode points to.
     */
    class ExtensionNode(
        val path: NibbleArray,
        val innerNode: Node
    ) : Node() {

        /**
         * The RLP-encoded form of the ExtensionNode, which is an RLP-encoded list of the path and the inner node.
         */
        override val encoded: ByteArray
            get() {
                val encodedInnerNode = innerNode.encoded
                return RlpEncoder.encode(
                    RlpList(
                        RlpString.create(PatriciaTriePathType.EXTENSION.applyPrefix(path).toBytes()),
                        if (encodedInnerNode.size >= 32) {
                            RlpString.create(innerNode.hash)
                        } else {
                            RlpDecoder.decode(encodedInnerNode) // TODO: review
                        }
                    )
                )
            }

        override fun put(key: NibbleArray, newValue: ByteArray): Node {
            val matchingLength = path.prefixMatchingLength(key)

            // Key (1, 2, 3...) contains entire path (1, 2, 3)
            if (matchingLength == path.size) {
                // Put the value into the inner node, at the remaining key
                return extension(path, innerNode.put(key.dropFirst(matchingLength), newValue))
            }

            // Key (1, 2, 3...) contains part of path (1, 2, 4, 5, 6)
            val matchingPath = path.takeFirst(matchingLength)       // Nibbles where key and path agree (1, 2)
            val pathIndex = path[matchingLength].toInt()            // Nibble where key and path diverge (4)
            val remainingPath = path.remainingAfter(matchingLength) // Path nibbles after divergence (5, 6)

            // Branch either to the inner node or to an extension terminating in the inner node
            val firstBranch = if (remainingPath.isEmpty()) innerNode else extension(remainingPath, innerNode)

            val branchNode = if (matchingLength == key.size) {
                // Path (1, 2, 3...) contains entire key (1, 2, 3)

                // 3 -> firstBranch
                branch(listOf(pathIndex to firstBranch), newValue)
            } else {
                // Path (1, 2, 3...) contains part of key (1, 2, 4, 5, 6)
                val keyIndex = key[matchingLength].toInt()            // Nibble where key and path diverge (4)
                val remainingKey = key.remainingAfter(matchingLength) // Key nibbles after divergence (5, 6)

                // 3 -> firstBranch
                // 4 -> leaf((5, 6), newValue)
                branch(listOf(pathIndex to firstBranch, keyIndex to leaf(remainingKey, newValue)))
            }

            return if (matchingPath.isEmpty()) branchNode else extension(matchingPath, branchNode)
        }

        override fun get(key: NibbleArray): ByteArray {
            val matchingLength = path.prefixMatchingLength(key)
            if (matchingLength < path.size) {
                return ByteArray(0) // TODO: key not found
            }

            return innerNode.get(key.dropFirst(matchingLength))
        }
    }

    /**
     * Represents a BranchNode in a Patricia Trie.
     *
     * A BranchNode consists of an array of Node branches and a value.
     * The encoded form is an RLP (Recursive Length Prefix) encoded list of the branches and value.
     *
     * @property branches The array of Nodes that the BranchNode contains.
     * @property value The value of the BranchNode.
     */
    class BranchNode(
        private val branches: Array<Node>,
        val value: ByteArray
    ) : Node() {

        /**
         * The RLP-encoded form of the BranchNode, which is an RLP-encoded list of the branches and value.
         */
        override val encoded: ByteArray
            get() {
                return RlpEncoder.encode(RlpList(branches.map { node ->
                    val encodedNode = node.encoded
                    when {
                        node is EmptyNode -> RlpString.create(ByteArray(0))
                        // NOTE: in the next line, if node can be a HashNode then we need to call
                        // node.hash otherwise we can optimize and call Hash.sha3(encodedNode)
                        encodedNode.size >= 32 -> RlpString.create(node.hash)
                        else -> RlpString.create(encodedNode)
                    }
                }.plus(RlpString.create(value))))
            }

        fun getBranch(branch: Byte): Node = branches[branch.toInt()]

        override fun put(key: NibbleArray, newValue: ByteArray): Node =
            if (key.isEmpty()) branch(branches, newValue)
            else {
                val newBranches = Array(16) { index ->
                    if (index == key.head.toInt()) getBranch(key.head).put(key.tail, newValue) else branches[index]
                }
                branch(newBranches, value)
            }

        override fun get(key: NibbleArray): ByteArray = if (key.isEmpty()) value else getBranch(key.head).get(key.tail)

    }

    /**
     * A representation of a node in a Patricia Trie that is used to store a hash and an inner node.
     * This class provides methods to create a new HashNode and retrieve its encoded representation.
     *
     * @param hash The hash of the node
     * @param innerNode The inner node associated with this HashNode. Defaults to an EmptyNode if not provided.
     */
    class HashNode(override val hash: ByteArray, private val innerNode: Node) : Node() {
        /**
         * Returns the encoded representation of the HashNode. If the inner node is an EmptyNode,
         * it returns the hash of the node, otherwise it returns the encoded representation of the inner node.
         */
        override val encoded: ByteArray get() = if (innerNode is EmptyNode) hash else innerNode.encoded

        override fun put(key: NibbleArray, newValue: ByteArray): Node {
            throw UnsupportedOperationException("Cannot put into HashNode")
        }

        override fun get(key: NibbleArray): ByteArray {
            throw UnsupportedOperationException("Cannot get from HashNode")
        }
    }

    /**
     * Represents a LeafNode in a Patricia Trie.
     *
     * A LeafNode consists of a path (represented as a nibble array) and a value (represented as a byte array).
     * The encoded form is an RLP (Recursive Length Prefix) encoded list of the path and value.
     *
     * @property path The path of the node, represented as a nibble array.
     * @property value The value of the node, represented as a byte array.
     */
    class LeafNode(
        val path: NibbleArray,
        val value: ByteArray
    ) : Node() {

        /**
         * The RLP-encoded form of the LeafNode, which is an RLP-encoded list of the path and value.
         */
        override val encoded: ByteArray
            get() {
                return RlpEncoder.encode(
                    RlpList(
                        RlpString.create(PatriciaTriePathType.LEAF.applyPrefix(path).toBytes()),
                        RlpString.create(value)
                    )
                )
            }

        override fun put(key: NibbleArray, newValue: ByteArray): Node {
            // Overwrite value if key and path match exactly
            if (path == key) return leaf(key, newValue)

            val matchingLength = path.prefixMatchingLength(key)

            val branches = mutableListOf<Pair<Int, Node>>()

            // If there's some path (1, 2, 3, 4) left after the match (1, 2) with key (1, 2, 5, 6)
            if (matchingLength < path.size) {
                // Put the current value in a branch: 3 -> leaf((4), value)
                branches.add(path[matchingLength].toInt() to leaf(path.remainingAfter(matchingLength), value))
            }

            // If there's some key (1, 2, 5, 6) left after the match (1, 2) with path (1, 2, 3, 4)
            if (matchingLength < key.size) {
                // Put the new value in a branch: 5 -> ((6), newValue)
                branches.add(key[matchingLength].toInt() to leaf(key.remainingAfter(matchingLength), newValue))
            }

            /*
            Note implicit logic here:

            * matchingLength is never more than path.size, and never more than key.size
            * path.size and key.size are never equal, or we would have hit the exact match condition above.
            * If matchingLength is path.size, it cannot be equal to key.size, so it must be less than key.size.
            * If matchingLength is key.size, it cannot be equal to path.size, so it must be less than path.size

             */
            val branchNode = when (matchingLength) {
                // Implicitly, matchingLength < key.size, so branch to newValue is in branches
                path.size -> branch(branches, value)
                // Implicitly, matchingLength < path.size, so branch to value is in branches
                key.size -> branch(branches, newValue)
                // MatchingLength < key.size && matchingLength < path.size, so both branches are present
                else -> branch(branches)
            }

            return if (matchingLength == 0) branchNode else extension(path.takeFirst(matchingLength), branchNode)
        }

        override fun get(key: NibbleArray): ByteArray = if (key == path) value else ByteArray(0)
    }

}
