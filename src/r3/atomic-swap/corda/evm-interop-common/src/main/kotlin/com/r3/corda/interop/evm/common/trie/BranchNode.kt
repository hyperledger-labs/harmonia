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
 * Represents a BranchNode in a Patricia Trie.
 *
 * A BranchNode consists of an array of Node branches and a value.
 * The encoded form is an RLP (Recursive Length Prefix) encoded list of the branches and value.
 *
 * @property branches The array of Nodes that the BranchNode contains.
 * @property value The value of the BranchNode.
 */
class BranchNode(private val branches: Array<Node>, private val value: ByteArray) : Node {

    companion object {
        private val emptyArray = ByteArray(0)

        fun from(sparseBranches: List<Pair<Int, Node>>): BranchNode = from(sparseBranches, emptyArray)

        fun from(sparseBranches: List<Pair<Int, Node>>, value: ByteArray): BranchNode {
            val branches = Array<Node>(16) { EmptyNode }
            sparseBranches.forEach { (index, branch) ->
                    branches[index] = branch
                }
            return BranchNode(branches, value)
        }
    }

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

    private fun getBranch(branch: Byte): Node = branches[branch.toInt()]

    override fun put(key: NibbleArray, newValue: ByteArray): Node =
        if (key.isEmpty()) BranchNode(branches, newValue)
        else {
            val newBranches = Array(16) { index ->
                if (index == key.head.toInt()) getBranch(key.head).put(key.tail, newValue) else branches[index]
            }
            BranchNode(newBranches, value)
        }

    override fun get(key: NibbleArray): ByteArray = if (key.isEmpty()) value else getBranch(key.head).get(key.tail)

    override fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore): KeyValueStore {
            store.put(hash, encoded)

            if (key.isEmpty()) {
                require(value.isNotEmpty()) { "Terminal branch without value" }
                return store
            }

            return getBranch(key.head).generateMerkleProof(key.tail, store)
        }

    override fun verifyMerkleProof(key: NibbleArray, expectedValue: ByteArray, proof: KeyValueStore): Boolean =
        if (key.isEmpty()) {
            value.contentEquals(expectedValue)
        } else {
            proof.verify(getBranch(key.head).hash, key.tail, expectedValue)
        }
}