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

/**
 * A representation of a node in a Patricia Trie that is used to store a hash and an inner node.
 * This class provides methods to create a new HashNode and retrieve its encoded representation.
 *
 * @param hash The hash of the node
 * @param innerNode The inner node associated with this HashNode. Defaults to an EmptyNode if not provided.
 */
class HashNode(override val hash: ByteArray, private val innerNode: Node) : Node {
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

    override fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore): KeyValueStore {
        throw UnsupportedOperationException("Cannot generate Merkle proof from HashNode")
    }

    override fun verifyMerkleProof(key: NibbleArray, expectedValue: ByteArray, proof: KeyValueStore): Boolean {
        throw UnsupportedOperationException("Cannot verify Merkle proof from HashNode")
    }
}