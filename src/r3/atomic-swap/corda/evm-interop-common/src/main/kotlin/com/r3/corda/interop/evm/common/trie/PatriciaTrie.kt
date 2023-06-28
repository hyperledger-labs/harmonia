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
        root = root.put(NibbleArray.fromBytes(key), value)
    }

    /**
     * Retrieves the value associated with a given key in the Patricia Trie.
     *
     * @param key Key as ByteArray.
     * @return Value associated with the key as ByteArray.
     */
    fun get(key: ByteArray): ByteArray {
        return root.get(NibbleArray.fromBytes(key))
    }

    /**
     * Generates a Merkle proof for a given key.
     *
     * @param key Key as ByteArray.
     * @return Merkle proof as KeyValueStore.
     */
    fun generateMerkleProof(key: ByteArray) : KeyValueStore {
        return root.generateMerkleProof(NibbleArray.fromBytes(key), SimpleKeyValueStore())
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
        ): Boolean = Node.verifyMerkleProof(rootHash, NibbleArray.fromBytes(key), expectedValue, proof)
    }

}

