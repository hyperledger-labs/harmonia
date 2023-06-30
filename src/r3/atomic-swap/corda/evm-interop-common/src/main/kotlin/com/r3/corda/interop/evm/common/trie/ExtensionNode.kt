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

import org.web3j.rlp.RlpDecoder
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString

/**
 * Represents an ExtensionNode in a Patricia Trie.
 *
 * An ExtensionNode consists of a path and an inner node.
 * The encoded form is an RLP (Recursive Length Prefix) encoded list of the path and the inner node.
 *
 * @property path The path of the ExtensionNode, stored as a nibbles array.
 * @property innerNode The inner Node that the ExtensionNode points to.
 */
class ExtensionNode(private val path: NibbleArray, private val innerNode: Node) : Node {

    /**
     * The RLP-encoded form of the ExtensionNode, which is an RLP-encoded list of the path and the inner node.
     */
    override val encoded: ByteArray
        get() {
            val encodedInnerNode = innerNode.encoded
            return RlpEncoder.encode(
                RlpList(
                    RlpString.create(PatriciaTriePathType.EXTENSION.applyPrefix(path).toBytes()),
                    if (encodedInnerNode.size >= 32) {
                        RlpString.create(innerNode.hash)
                    } else {
                        RlpDecoder.decode(encodedInnerNode) // TODO: review
                    }
                )
            )
        }

    private fun extendIfNonEmpty(path: NibbleArray, innerNode: Node): Node =
        if (path.isEmpty()) innerNode else ExtensionNode(path, innerNode)

    override fun put(key: NibbleArray, newValue: ByteArray): Node {
        val prefixMatch = PathPrefixMatch.match(key, path)

        return when (prefixMatch) {
            is PathPrefixMatch.Equals -> ExtensionNode(path, innerNode.put(NibbleArray.empty, newValue))
            is PathPrefixMatch.NoMatch -> BranchNode.from(
                prefixMatch.pathHead to extendIfNonEmpty(prefixMatch.pathTail, innerNode),
                prefixMatch.keyHead to LeafNode(prefixMatch.keyTail, newValue)
            )

            is PathPrefixMatch.PathPrefixesKey -> ExtensionNode(
                path,
                innerNode.put(prefixMatch.keyRemainder, newValue)
            )

            is PathPrefixMatch.KeyPrefixesPath -> extendIfNonEmpty(
                key, BranchNode.from(
                    prefixMatch.pathRemainderHead to extendIfNonEmpty(prefixMatch.pathRemainderTail, innerNode),
                    value = newValue
                )
            )

            is PathPrefixMatch.PartialMatch -> ExtensionNode(
                prefixMatch.sharedPrefix, BranchNode.from(
                    prefixMatch.pathRemainderHead to extendIfNonEmpty(prefixMatch.pathRemainderTail, innerNode),
                    prefixMatch.keyRemainderHead to LeafNode(prefixMatch.keyRemainderTail, newValue)
                )
            )
        }

        /*
        val matchingLength = path.prefixMatchingLength(key)

        // Key (1, 2, 3...) contains entire path (1, 2, 3)
        if (matchingLength == path.size) {
            // Put the value into the inner node, at the remaining key
            return ExtensionNode(path, innerNode.put(key.dropFirst(matchingLength), newValue))
        }

        // Key (1, 2, 3...) contains part of path (1, 2, 4, 5, 6)
        val matchingPath = path.takeFirst(matchingLength)       // Nibbles where key and path agree (1, 2)
        val pathIndex = path[matchingLength].toInt()            // Nibble where key and path diverge (4)
        val remainingPath = path.remainingAfter(matchingLength) // Path nibbles after divergence (5, 6)

        // Branch either to the inner node or to an extension terminating in the inner node
        val firstBranch = if (remainingPath.isEmpty()) innerNode else ExtensionNode(remainingPath, innerNode)

        val branchNode = if (matchingLength == key.size) {
            // Path (1, 2, 3...) contains entire key (1, 2, 3)

            // 3 -> firstBranch
            BranchNode.from(listOf(pathIndex to firstBranch), newValue)
        } else {
            // Path (1, 2, 3...) contains part of key (1, 2, 4, 5, 6)
            val keyIndex = key[matchingLength].toInt()            // Nibble where key and path diverge (4)
            val remainingKey = key.remainingAfter(matchingLength) // Key nibbles after divergence (5, 6)

            // 3 -> firstBranch
            // 4 -> leaf((5, 6), newValue)
            BranchNode.from(listOf(pathIndex to firstBranch, keyIndex to LeafNode(remainingKey, newValue)))
        }

        return if (matchingPath.isEmpty()) branchNode else ExtensionNode(matchingPath, branchNode)

         */
    }

    override fun get(key: NibbleArray): ByteArray {
        val matchingLength = path.prefixMatchingLength(key)
        if (matchingLength < path.size) {
            return ByteArray(0) // TODO: key not found
        }

        return innerNode.get(key.dropFirst(matchingLength))
    }

    override fun generateMerkleProof(key: NibbleArray, store: WriteableKeyValueStore): KeyValueStore =
            if (key.startsWith(path)) {
                store.put(hash, encoded)
                innerNode.generateMerkleProof(key.dropFirst(path.size), store)
            } else {
                throw IllegalArgumentException("Key is not part of the trie")
            }

    override fun verifyMerkleProof(key: NibbleArray, expectedValue: ByteArray, proof: KeyValueStore): Boolean =
        if (key.startsWith(path)) proof.verify(innerNode.hash, key.dropFirst(path.size), expectedValue)
        else throw IllegalArgumentException("Key is not part of the trie")

}