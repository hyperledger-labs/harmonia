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

/**
 * The base class for all types of nodes in a Patricia Trie.
 */
interface Node {
    /**
     * Get the encoded version of this node.
     * @return encoded byte array of this node.
     */
    val encoded: ByteArray

    /**
     * Get the SHA-3 hash of the encoded node.
     * @return hash of the encoded node.
     */
    val hash: ByteArray
        get() = sha3(encoded)

    /**
     * Puts a key-value pair into a node of the Patricia Trie.
     * If the node does not exist, a new LeafNode is created.
     *
     * @param key The key to put.
     * @param newValue The value to put.
     * @return The node where the key-value pair was put.
     */
    fun put(key: NibbleArray, newValue: ByteArray): Node

    /**
     * Gets the value for a given key from the Patricia Trie.
     *
     * @param key The key for which to get the value.
     * @return The value associated with the key, or an empty ByteArray if the key does not exist.
     */
    fun get(key: NibbleArray): ByteArray

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param key Key as a NibbleArray
     * @param store A simple Key-Value that will collect the trie proofs
     * @return Merkle proof as KeyValueStore.
     */
    fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore) : KeyValueStore

    fun verifyMerkleProof(
        key: NibbleArray,
        expectedValue: ByteArray,
        proof: KeyValueStore
    ): Boolean

    companion object {

        private val emptyArray = ByteArray(0)
        val empty: EmptyNode get() = EmptyNode

        fun leaf(key: ByteArray, value: ByteArray): LeafNode = leaf(NibbleArray.fromBytes(key), value)
        fun leaf(key: NibbleArray, value: ByteArray): LeafNode = LeafNode(key, value)

        fun branch(sparseBranches: List<Pair<Int, Node>>): BranchNode = branch(sparseBranches, emptyArray)
        fun branch(sparseBranches: List<Pair<Int, Node>>, value: ByteArray): BranchNode {
            val branches = Array<Node>(16) { empty }
            sparseBranches.forEach { (index, branch) ->
                branches[index] = branch
            }
            return branch(branches, value)
        }

        fun branch(branches: Array<Node>, value: ByteArray): BranchNode = BranchNode(branches, value)

        fun extension(path: NibbleArray, value: Node): ExtensionNode = ExtensionNode(path, value)

        /**
         * Verifies the Merkle proof for a given key and expected value.
         *
         * @param rootHash The root hash of the trie.
         * @param key The key for which to verify the proof.
         * @param expectedValue The expected value for the key.
         * @param proof The proof to verify.
         * @return Boolean indicating whether the proof is valid.
         */
        fun verifyMerkleProof(
            rootHash: ByteArray,
            key: NibbleArray,
            expectedValue: ByteArray,
            proof: KeyValueStore
        ): Boolean {
            val node = createFromRLP(proof.get(rootHash) ?: throw IllegalArgumentException("Proof is invalid"))
            return node.verifyMerkleProof(key, expectedValue, proof)
        }

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

}
