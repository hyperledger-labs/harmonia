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
import org.web3j.rlp.RlpDecoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.rlp.RlpType
import org.web3j.utils.Numeric

/**
 * The base class for all types of nodes in a Patricia Trie.
 */
abstract class Node
{
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
     * Provide a String representation of the Node for debugging.
     * @return String representation of the Node.
     */
    override fun toString(): String {
        return "NodeType: ${javaClass.simpleName} Hash: ${Numeric.toHexString(hash)} Encoded: ${Numeric.toHexString(encoded)}"
    }

    companion object {

        /**
         * Create a Node from a RLP encoded byte array.
         * @param encoded RLP encoded byte array.
         * @return Node created from the RLP encoded byte array.
         */
        fun createFromRLP(encoded: ByteArray): Node {
            return if (encoded.size == 32) {
                HashNode(encoded)
            } else {
                val outerList = RlpDecoder.decode(encoded) as RlpList
                createFromRLP(outerList)
            }
        }

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
            val branches = branchValues.mapIndexedNotNull { index, value ->
                val key = index.toByte()
                when (value) {
                    is RlpString -> if (value.bytes.isNotEmpty()) key to createFromRLP(value.bytes) else null
                    is RlpList -> key to createFromRLP(value)
                    else -> throw IllegalArgumentException("Invalid RLP encoding")
                }
            }.toTypedArray()

            return BranchNode.createWithBranches(*branches, value = valueBytes)
        }

        private fun nonBranchNode(keyBytes: ByteArray, valueOrNode: RlpType): Node {
            val allNibbles = NibbleArray.fromBytes(keyBytes)
            val prefix = PatriciaTriePathPrefix.fromNibbles(allNibbles)
            val pathType = PatriciaTriePathType.forPrefix(prefix)
            val pathNibbles = allNibbles.dropFirst(prefix.prefixNibbles.size)

            return when (valueOrNode) {
                is RlpString -> {
                    if (pathType == PatriciaTriePathType.LEAF) LeafNode(pathNibbles, valueOrNode.bytes)
                    else ExtensionNode(pathNibbles, createFromRLP(valueOrNode.bytes))
                }

                is RlpList -> ExtensionNode(pathNibbles, createFromRLP(valueOrNode))
                else -> throw IllegalArgumentException("Invalid RLP encoding")
            }
        }
    }
}
