/*
 * Copyright 2023, R3 LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

    companion object {
        /**
         * Factory function to create a LeafNode given a key and value, where the key is a byte array.
         *
         * @param key The key to use for the node path.
         * @param value The value to use for the node.
         * @return The created LeafNode.
         */
        fun fromBytes(key: ByteArray, value: ByteArray): LeafNode {
            return LeafNode(NibbleArray.fromBytes(key), value)
        }

    }
}
