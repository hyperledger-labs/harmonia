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

/**
 * The Patricia Trie is a space-optimized version of a binary trie.
 * It's an ordered tree data structure used to store a dynamic set or associative array
 * where the keys are usually strings.
 */
class PatriciaTrie {

    /**
     * The root node of the Patricia Trie.
     */
    var root: Node = EmptyNode()

    /**
     * Puts a key-value pair in the Patricia Trie.
     *
     * @param key Key as ByteArray.
     * @param value Value as ByteArray.
     */
    fun put(key: ByteArray, value: ByteArray) {
        root = internalPut(root, key.toNibbles(), value)
    }

    /**
     * Retrieves the value associated with a given key in the Patricia Trie.
     *
     * @param key Key as ByteArray.
     * @return Value associated with the key as ByteArray.
     */
    fun get(key: ByteArray): ByteArray {
        return internalGet(key.toNibbles())
    }

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param key Key as ByteArray.
     * @return Merkle proof as KeyValueStore.
     */
    fun generateMerkleProof(key: ByteArray) : KeyValueStore {
        return generateMerkleProof(root, key.toNibbles(), SimpleKeyValueStore())
    }

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param startNode Key as ByteArray.
     * @param nibblesKey Key as a nibbles' ByteArray.
     * @param store A simple Key-Value that will collect the trie proofs
     * @return Merkle proof as KeyValueStore.
     */
    private fun generateMerkleProof(startNode: Node, nibblesKey: ByteArray, store: SimpleKeyValueStore) : KeyValueStore {
        var node = startNode
        var nodeKey = nibblesKey

        while (true) {
            when (node) {
                is EmptyNode -> throw IllegalArgumentException("Key is not part of the trie")
                is LeafNode -> {
                    if (node.path.contentEquals(nodeKey)) {
                        store.put(node.hash, node.encoded)
                        return store
                    } else {
                        throw IllegalArgumentException("Key is not part of the trie")
                    }
                }
                is BranchNode -> {
                    store.put(node.hash, node.encoded)

                    if(nodeKey.isEmpty()) {
                        require(node.value.isNotEmpty()) { "Terminal branch without value" }
                        return store
                    }

                    val nextNibble = nodeKey[0]
                    nodeKey = nodeKey.sliceArray(1 until nodeKey.size)
                    node = node.branches[nextNibble.toInt()]
                }
                is ExtensionNode -> {
                    if (nodeKey.startsWith(node.path)) {
                        store.put(node.hash, node.encoded)
                        nodeKey = nodeKey.sliceArray(node.path.size until nodeKey.size)
                        node = node.innerNode
                    } else {
                        throw IllegalArgumentException("Key is not part of the trie")
                    }
                }
                else -> throw IllegalArgumentException("Invalid node type")
            }
        }
    }

    /**
     * Companion object that provides functionality to verify a Merkle proof.
     */
    companion object {
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
            key: ByteArray,
            expectedValue: ByteArray,
            proof: KeyValueStore
        ): Boolean {
            var nodeHash = rootHash
            var nodeKey = key.toNibbles()

            while (true) {
                val encodedNode = proof.get(nodeHash) ?: throw IllegalArgumentException("Proof is invalid")
                val node = Node.createFromRLP(encodedNode)

                nodeHash = when (node) {
                    is EmptyNode -> throw IllegalArgumentException("Key is not part of the trie")
                    is LeafNode -> {
                        if (node.path.contentEquals(nodeKey)) {
                            return node.value.contentEquals(expectedValue)
                        } else {
                            throw IllegalArgumentException("Key is not part of the trie")
                        }
                    }
                    is BranchNode -> {
                        if(nodeKey.isEmpty()) {
                            return node.value.contentEquals(expectedValue)
                        }

                        val nextNibble = nodeKey[0]
                        nodeKey = nodeKey.sliceArray(1 until nodeKey.size)
                        node.branches[nextNibble.toInt()].hash
                    }
                    is ExtensionNode -> {
                        if (nodeKey.startsWith(node.path)) {
                            nodeKey = nodeKey.sliceArray(node.path.size until nodeKey.size)
                            node.innerNode.hash
                        } else {
                            throw IllegalArgumentException("Key is not part of the trie")
                        }
                    }

                    else -> throw IllegalArgumentException("Invalid node type")
                }
            }
        }
    }

    /**
     * Puts a key-value pair into a node of the Patricia Trie.
     * If the node does not exist, a new LeafNode is created.
     *
     * @param node The node where to put the key-value pair.
     * @param nibblesKey The key to put.
     * @param value The value to put.
     * @return The node where the key-value pair was put.
     */
    private fun internalPut(node: Node, nibblesKey: ByteArray, value: ByteArray): Node {
        if (node is EmptyNode) {
            return LeafNode.createFromNibbles(nibblesKey, value)
        }

        if (node is LeafNode) {
            val matchingLength = node.path.prefixMatchingLength(nibblesKey)

            if (matchingLength == node.path.size && matchingLength == nibblesKey.size) {
                return LeafNode.createFromNibbles(nibblesKey, value)
            }

            val branchNode = when (matchingLength) {
                node.path.size -> BranchNode.createWithValue(node.value)
                nibblesKey.size -> BranchNode.createWithValue(value)
                else -> BranchNode.create()
            }

            val extOrBranchNode = if (matchingLength > 0) {
                ExtensionNode.createFromNibbles(node.path.copyOfRange(0, matchingLength), branchNode)
            } else {
                branchNode
            }

            if (matchingLength < node.path.size) {
                branchNode.setBranch(
                    node.path[matchingLength],
                    LeafNode.createFromNibbles(
                        node.path.copyOfRange(matchingLength + 1, node.path.size),
                        node.value
                    )
                )
            }

            if (matchingLength < nibblesKey.size) {
                branchNode.setBranch(
                    nibblesKey[matchingLength],
                    LeafNode.createFromNibbles(
                        nibblesKey.copyOfRange(matchingLength + 1, nibblesKey.size),
                        value
                    )
                )
            }

            return extOrBranchNode
        }

        if (node is BranchNode) {
            if (nibblesKey.isNotEmpty()) {
                val branch = nibblesKey[0].toInt()
                node.branches[branch] = internalPut(
                    node.branches[branch],
                    nibblesKey.copyOfRange(1, nibblesKey.size),
                    value
                )
            } else {
                node.value = value
            }
            return node
        }

        if (node is ExtensionNode) {
            val matchingLength = node.path.prefixMatchingLength(nibblesKey)
            if (matchingLength < node.path.size) {
                val extNibbles = node.path.copyOfRange(0, matchingLength)
                val branchNibble = node.path[matchingLength]
                val extRemainingNibbles = node.path.copyOfRange(matchingLength + 1, node.path.size)

                val branchNode = BranchNode.createWithBranch(
                    branchNibble,
                    if (extRemainingNibbles.isEmpty()) {
                        node.innerNode
                    } else {
                        ExtensionNode.createFromNibbles(extRemainingNibbles, node.innerNode)
                    }
                )

                if (matchingLength < nibblesKey.size) {
                    val nodeBranchNibble = nibblesKey[matchingLength]
                    val nodeLeafNibbles = nibblesKey.copyOfRange(matchingLength + 1, nibblesKey.size)
                    val remainingLeaf = LeafNode.createFromNibbles(nodeLeafNibbles, value)
                    branchNode.setBranch(nodeBranchNibble, remainingLeaf)
                } else if (matchingLength == nibblesKey.size) {
                    branchNode.value = value
                } else throw IllegalArgumentException("Something went wrong")


                return if (extNibbles.isNotEmpty()) {
                    ExtensionNode.createFromNibbles(extNibbles, branchNode)
                } else {
                    branchNode
                }
            }

            node.innerNode = internalPut(node.innerNode, nibblesKey.copyOfRange(matchingLength, nibblesKey.size), value)
            return node
        }

        throw IllegalArgumentException("Unknown node type")
    }

    /**
     * Gets the value for a given key from the Patricia Trie.
     *
     * @param nibblesKey The key for which to get the value.
     * @return The value associated with the key, or an empty ByteArray if the key does not exist.
     */
    private fun internalGet(nibblesKey: ByteArray): ByteArray {
        var node = root
        var key = nibblesKey

        while (true) {
            if (node is EmptyNode) return ByteArray(0) // TODO: key not found ?

            if (node is LeafNode) {
                val matchingLength = node.path.prefixMatchingLength(key)
                if (matchingLength != node.path.size || matchingLength != key.size) {
                    return ByteArray(0) // key not found
                }
                return node.value
            }

            if (node is BranchNode) {
                if (key.isEmpty()) {
                    return node.value // TODO: should check if the node has a value?
                }

                node = node.branches[key[0].toInt()]
                key = key.copyOfRange(1, key.size)
                continue
            }

            if (node is ExtensionNode) {
                val matchingLength = node.path.prefixMatchingLength(key)
                if (matchingLength < node.path.size) {
                    return ByteArray(0) // TODO: key not found
                }

                node = node.innerNode
                key = key.copyOfRange(matchingLength, key.size)
                continue
            }

            throw IllegalArgumentException("Invalid node type")
        }
    }

    /**
     * Returns the length of the common prefix of two ByteArrays.
     *
     * @return The length of the common prefix.
     */
    private fun ByteArray.prefixMatchingLength(other: ByteArray): Int {
        return this.zip(other).takeWhile { (n1, n2) -> n1 == n2 }.count()
    }
}

