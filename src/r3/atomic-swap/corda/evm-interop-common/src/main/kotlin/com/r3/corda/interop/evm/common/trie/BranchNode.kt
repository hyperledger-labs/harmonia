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
 * Represents a BranchNode in a Patricia Trie.
 *
 * A BranchNode consists of an array of Node branches and a value.
 * The encoded form is an RLP (Recursive Length Prefix) encoded list of the branches and value.
 *
 * @property branches The array of Nodes that the BranchNode contains.
 * @property value The value of the BranchNode.
 */
class BranchNode private constructor(
    val branches: Array<Node>,
    var value: ByteArray
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

    /**
     * Set a branch at the nibbleKey index to the provided node.
     *
     * @param nibbleKey The index at which to set the branch.
     * @param node The node to set at the index.
     */
    fun setBranch(nibbleKey: Byte, node: Node) {
        branches[nibbleKey.toInt()] = node
    }

    companion object {

        private val emptyNode = EmptyNode()

        /**
         * Factory function to create a BranchNode with multiple branches and the optionally
         * provided value.
         *
         * @param branches The branches to set in the BranchNode.
         * @param value The value to use for the BranchNode. Defaults to an empty byte array.
         * @return The created BranchNode.
         */
        fun createWithBranches(vararg branches: Pair<Byte, Node>, value: ByteArray = ByteArray(0)): BranchNode {
            val branch = BranchNode(Array(16) { emptyNode }, value)
            branches.forEach { (nibbleKey, node) ->
                branch.setBranch(nibbleKey, node)
            }
            return branch
        }
    }
}