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

import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString

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
    private val path: NibbleArray,
    val value: ByteArray
) : Node {

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
        if (path == key) return Node.leaf(key, newValue)

        val matchingLength = path.prefixMatchingLength(key)

        val branches = mutableListOf<Pair<Int, Node>>()

        // If there's some path (1, 2, 3, 4) left after the match (1, 2) with key (1, 2, 5, 6)
        if (matchingLength < path.size) {
            // Put the current value in a branch: 3 -> leaf((4), value)
            branches.add(path[matchingLength].toInt() to Node.leaf(path.remainingAfter(matchingLength), value))
        }

        // If there's some key (1, 2, 5, 6) left after the match (1, 2) with path (1, 2, 3, 4)
        if (matchingLength < key.size) {
            // Put the new value in a branch: 5 -> ((6), newValue)
            branches.add(key[matchingLength].toInt() to Node.leaf(key.remainingAfter(matchingLength), newValue))
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
            path.size -> Node.branch(branches, value)
            // Implicitly, matchingLength < path.size, so branch to value is in branches
            key.size -> Node.branch(branches, newValue)
            // MatchingLength < key.size && matchingLength < path.size, so both branches are present
            else -> Node.branch(branches)
        }

        return if (matchingLength == 0) branchNode else Node.extension(path.takeFirst(matchingLength), branchNode)
    }

    override fun get(key: NibbleArray): ByteArray = if (key == path) value else ByteArray(0)

    override fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore): KeyValueStore {
        if (path == key) {
            store.put(hash, encoded)
            return store
        } else {
            throw IllegalArgumentException("Key is not part of the trie")
        }
    }

    override fun verifyMerkleProof(key: NibbleArray, expectedValue: ByteArray, proof: KeyValueStore): Boolean =
        if (path == key) {
            value.contentEquals(expectedValue)
        } else {
            throw IllegalArgumentException("Key is not part of the trie")
        }
}