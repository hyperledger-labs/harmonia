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

import com.r3.corda.interop.evm.common.trie.Node.*

/**
 * The Patricia Trie is a space-optimized version of a binary trie.
 * It's an ordered tree data structure used to store a dynamic set or associative array
 * where the keys are usually strings.
 */
class PatriciaTrie {

    /**
     * The root node of the Patricia Trie.
     */
    var root: Node = Node.empty

    /**
     * Puts a key-value pair in the Patricia Trie.
     *
     * @param key Key as ByteArray.
     * @param value Value as ByteArray.
     */
    fun put(key: ByteArray, value: ByteArray) {
        root = internalPut(root, NibbleArray.fromBytes(key), value)
    }

    /**
     * Retrieves the value associated with a given key in the Patricia Trie.
     *
     * @param key Key as ByteArray.
     * @return Value associated with the key as ByteArray.
     */
    fun get(key: ByteArray): ByteArray {
        return internalGet(NibbleArray.fromBytes(key))
    }

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param key Key as ByteArray.
     * @return Merkle proof as KeyValueStore.
     */
    fun generateMerkleProof(key: ByteArray) : KeyValueStore {
        return generateMerkleProof(root, NibbleArray.fromBytes(key), SimpleKeyValueStore())
    }

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param startNode Key as ByteArray.
     * @param nibblesKey Key as a nibbles' ByteArray.
     * @param store A simple Key-Value that will collect the trie proofs
     * @return Merkle proof as KeyValueStore.
     */
    private fun generateMerkleProof(startNode: Node, nibblesKey: NibbleArray, store: WriteableKeyValueStore) : KeyValueStore {
        var node = startNode
        var nodeKey = nibblesKey

        while (true) {
            when (node) {
                is EmptyNode -> throw IllegalArgumentException("Key is not part of the trie")
                is LeafNode -> {
                    if (node.path == nodeKey) {
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

                    val nextNibble = nodeKey.head
                    nodeKey = nodeKey.tail
                    node = node.getBranch(nextNibble)
                }
                is ExtensionNode -> {
                    if (nodeKey.startsWith(node.path)) {
                        store.put(node.hash, node.encoded)
                        nodeKey = nodeKey.dropFirst(node.path.size)
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
            var nodeKey = NibbleArray.fromBytes(key)

            while (true) {
                val encodedNode = proof.get(nodeHash) ?: throw IllegalArgumentException("Proof is invalid")
                val node = Node.createFromRLP(encodedNode)

                nodeHash = when (node) {
                    is EmptyNode -> throw IllegalArgumentException("Key is not part of the trie")
                    is LeafNode -> {
                        if (node.path == nodeKey) {
                            return node.value.contentEquals(expectedValue)
                        } else {
                            throw IllegalArgumentException("Key is not part of the trie")
                        }
                    }
                    is BranchNode -> {
                        if(nodeKey.isEmpty()) {
                            return node.value.contentEquals(expectedValue)
                        }

                        val nextNibble = nodeKey.head
                        nodeKey = nodeKey.tail
                        node.getBranch(nextNibble).hash
                    }
                    is ExtensionNode -> {
                        if (nodeKey.startsWith(node.path)) {
                            nodeKey = nodeKey.dropFirst(node.path.size)
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
    private fun internalPut(node: Node, nibblesKey: NibbleArray, value: ByteArray): Node = when(node) {
        is EmptyNode -> Node.leaf(nibblesKey, value)
        is LeafNode -> putAtLeaf(node, nibblesKey, value)
        is BranchNode -> putAtBranch(node, nibblesKey, value)
        is ExtensionNode -> putAtExtension(node, nibblesKey, value)
        is HashNode -> throw IllegalArgumentException("Cannot put at HashNode")
    }

    private fun putAtExtension(node: ExtensionNode, nibblesKey: NibbleArray, value: ByteArray): Node {
        val nodePathNibbles = node.path
        val matchingLength = nodePathNibbles.prefixMatchingLength(nibblesKey)

        if (matchingLength >= nodePathNibbles.size) {
            return node.withInnerNode(internalPut(node.innerNode, nibblesKey.dropFirst(matchingLength), value))
        }

        val extNibbles = nodePathNibbles.takeFirst(matchingLength)
        val branchNibble = nodePathNibbles[matchingLength]
        val extRemainingNibbles = nodePathNibbles.dropFirst(matchingLength + 1)

        val firstBranchTarget = if (extRemainingNibbles.isEmpty()) node.innerNode
        else Node.extension(extRemainingNibbles, node.innerNode)

        val firstBranch = branchNibble.toInt() to firstBranchTarget

        val branchNode = when {
            matchingLength == nibblesKey.size -> Node.branch(listOf(firstBranch), value)
            matchingLength < nibblesKey.size -> {
                val nodeBranchNibble = nibblesKey[matchingLength]
                val nodeLeafNibbles = nibblesKey.dropFirst(matchingLength + 1)
                val remainingLeaf = Node.leaf(nodeLeafNibbles, value)
                val secondBranch = nodeBranchNibble.toInt() to remainingLeaf

                Node.branch(listOf(firstBranch, secondBranch))
            }
            else -> throw IllegalArgumentException("Something went wrong")
        }

        return if (extNibbles.isEmpty()) branchNode else Node.extension(extNibbles, branchNode)
    }

    private fun putAtBranch(node: BranchNode, nibblesKey: NibbleArray, value: ByteArray): Node =
        if (nibblesKey.isEmpty()) {
            node.withValue(value)
        } else {
            val branch = nibblesKey.head
            node.withBranch(
                branch, internalPut(
                    node.getBranch(branch),
                    nibblesKey.tail,
                    value
                )
            )
        }

    private fun putAtLeaf(node: LeafNode, nibblesKey: NibbleArray, value: ByteArray): Node {
        val nodePathNibbles = node.path
        val matchingLength = nodePathNibbles.prefixMatchingLength(nibblesKey)

        if (matchingLength == nodePathNibbles.size && matchingLength == nibblesKey.size) {
            return Node.leaf(nibblesKey, value)
        }

        var branchNode = when (matchingLength) {
            nodePathNibbles.size -> Node.branch(node.value)
            nibblesKey.size -> Node.branch(value)
            else -> Node.branch()
        }

        branchNode = if (matchingLength < nodePathNibbles.size) {
            branchNode.withBranch(
                nodePathNibbles[matchingLength],
                Node.leaf(nodePathNibbles.dropFirst(matchingLength + 1), node.value)
            )
        } else branchNode

        branchNode = if (matchingLength < nibblesKey.size) {
            branchNode.withBranch(
                nibblesKey[matchingLength],
                Node.leaf(nibblesKey.dropFirst(matchingLength + 1), value)
            )
        } else branchNode

        return if (matchingLength > 0) {
            Node.extension(nodePathNibbles.takeFirst(matchingLength), branchNode)
        } else branchNode
    }

    /**
     * Gets the value for a given key from the Patricia Trie.
     *
     * @param nibblesKey The key for which to get the value.
     * @return The value associated with the key, or an empty ByteArray if the key does not exist.
     */
    private fun internalGet(nibblesKey: NibbleArray): ByteArray {
        var node = root
        var key = nibblesKey

        while (true) {
            if (node is EmptyNode) return ByteArray(0) // TODO: key not found ?

            if (node is LeafNode) {
                val nodePathNibbles = node.path
                val matchingLength = nodePathNibbles.prefixMatchingLength(key)
                if (matchingLength != nodePathNibbles.size || matchingLength != key.size) {
                    return ByteArray(0) // key not found
                }
                return node.value
            }

            if (node is BranchNode) {
                if (key.isEmpty()) {
                    return node.value // TODO: should check if the node has a value?
                }

                node = node.getBranch(key.head)
                key = key.tail
                continue
            }

            if (node is ExtensionNode) {
                val nodePathNibbles = node.path
                val matchingLength = nodePathNibbles.prefixMatchingLength(key)
                if (matchingLength < nodePathNibbles.size) {
                    return ByteArray(0) // TODO: key not found
                }

                node = node.innerNode
                key = key.dropFirst(matchingLength)
                continue
            }

            throw IllegalArgumentException("Invalid node type")
        }
    }
}

