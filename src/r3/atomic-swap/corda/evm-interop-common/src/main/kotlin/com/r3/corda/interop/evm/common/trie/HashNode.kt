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
 * A representation of a node in a Patricia Trie that is used to store a hash and an inner node.
 * This class provides methods to create a new HashNode and retrieve its encoded representation.
 */
class HashNode private constructor(
    /**
     * The hash of the node.
     */
    override val hash: ByteArray,
    /**
     * The inner node associated with this HashNode. Defaults to an EmptyNode if not provided.
     */
    private var innerNode: Node
) : Node() {
    /**
     * Returns the encoded representation of the HashNode. If the inner node is an EmptyNode,
     * it returns the hash of the node, otherwise it returns the encoded representation of the inner node.
     */
    override val encoded: ByteArray
        get() {
            if (innerNode is EmptyNode) return hash
            return innerNode.encoded
        }

    /**
     * Companion object for the HashNode class.
     */
    companion object {
        /**
         * Creates a new HashNode with the specified hash and inner node.
         * @param hash The hash of the new HashNode.
         * @param innerNode The inner node of the new HashNode. Defaults to an EmptyNode if not provided.
         * @return The newly created HashNode.
         */
        fun create(hash: ByteArray, innerNode: Node = EmptyNode()) : HashNode {
            return HashNode(hash, innerNode)
        }
    }
}
