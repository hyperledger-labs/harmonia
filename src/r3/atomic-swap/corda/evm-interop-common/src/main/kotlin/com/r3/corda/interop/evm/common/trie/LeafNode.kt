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
class LeafNode(private val path: NibbleArray, private val value: ByteArray) : Node {

    /**
     * The RLP-encoded form of the LeafNode, which is an RLP-encoded list of the path and value.
     */
    override val encoded: ByteArray
        get() {
            return RlpEncoder.encode(
                RlpList(
                    RlpString.create(PatriciaTriePathType.LEAF.getPrefixedBytes(path)),
                    RlpString.create(value)
                )
            )
        }

    override fun put(key: NibbleArray, newValue: ByteArray): Node {
        val matchResult = PathPrefixMatch.match(key, path)

        return when(matchResult) {
            is PathPrefixMatch.Equals -> LeafNode(key, newValue)
            is PathPrefixMatch.NoMatch -> BranchNode.from(
                matchResult.pathHead to LeafNode(matchResult.pathTail, value),
                matchResult.keyHead to LeafNode(matchResult.keyTail, newValue)
            )
            is PathPrefixMatch.PathPrefixesKey -> ExtensionNode(path, BranchNode.from(
                matchResult.keyRemainderHead to LeafNode(matchResult.keyRemainderTail, newValue),
                value = value))
            is PathPrefixMatch.KeyPrefixesPath -> ExtensionNode(key, BranchNode.from(
                matchResult.pathRemainderHead to LeafNode(matchResult.pathRemainderTail, value),
                value = newValue))
            is PathPrefixMatch.PartialMatch -> ExtensionNode(matchResult.sharedPrefix, BranchNode.from(
                matchResult.pathRemainderHead to LeafNode(matchResult.pathRemainderTail, value),
                matchResult.keyRemainderHead to LeafNode(matchResult.keyRemainderTail, newValue)
            ))
        }
    }

    override fun get(key: NibbleArray): ByteArray = if (key == path) value else ByteArray(0)

    override fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore): KeyValueStore {
        if (path ==key) {
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