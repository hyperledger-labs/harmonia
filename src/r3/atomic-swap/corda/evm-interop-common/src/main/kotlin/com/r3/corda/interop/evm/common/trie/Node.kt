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

    /**
     * Convert a ByteArray from prefixed nibbles to bytes.
     * Prefixed nibbles always have an even count.
     * @return ByteArray of bytes.
     */
    protected fun ByteArray.fromPrefixedNibblesToBytes(): ByteArray {
        require(this.size % 2 == 0) { "Nibble array size must be even" }

        val result = ByteArray(this.size / 2)
        for (i in this.indices step 2) {
            // Since these are nibbles, we do not AND with 0xF0 and 0x0F.
            // Before calling this function, make sure to validate the input data (sanity check).
            // Each byte in the input should be a valid nibble, i.e., in the range 0..15.
            result[i / 2] = ((this[i].toInt() shl 4) or (this[i + 1].toInt())).toByte()
        }
        return result
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
                2 -> {
                    val path = PatriciaTriePath.fromBytes((rlpList.values[0] as RlpString).bytes)

                    val valueOrNode = rlpList.values[1]

                    when (valueOrNode) {
                        is RlpString -> {
                            when(path.type) {
                                PatriciaTriePathType.LEAF -> LeafNode(path.pathNibbles, valueOrNode.bytes)
                                else -> ExtensionNode(path.pathNibbles, createFromRLP(valueOrNode.bytes))
                            }
                        }
                        is RlpList -> ExtensionNode(path.pathNibbles, createFromRLP(valueOrNode))
                        else -> throw IllegalArgumentException("Invalid RLP encoding")
                    }
                }
                17 -> {
                    val branches = rlpList.values.subList(0, 16).mapIndexedNotNull { index, value ->
                        val key = index.toByte()
                        when (value) {
                            is RlpString -> if (value.bytes.isNotEmpty()) key to createFromRLP(value.bytes) else null
                            is RlpList -> key to createFromRLP(value)
                            else -> throw IllegalArgumentException("Invalid RLP encoding")
                        }
                    }.toTypedArray()

                    val value = (rlpList.values[16] as RlpString).bytes
                    BranchNode.createWithBranches(*branches, value = value)
                }
                else -> throw IllegalArgumentException("Invalid RLP encoding")
            }
        }
    }
}
