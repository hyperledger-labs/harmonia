/**
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

import net.corda.core.serialization.CordaSerializable
import org.web3j.utils.Numeric

/**
 * Implementation of KeyValueStore using a simple hash map.
 *
 * This store utilizes a hash map to store and retrieve key-value pairings.
 * Keys are wrapped in an ArrayKey data class to allow for proper comparison of byte arrays.
 */
@CordaSerializable
class SimpleKeyValueStore(val store: LinkedHashMap<Int, ByteArray>) // : KeyValueStore
{

    constructor() : this(LinkedHashMap<Int, ByteArray>())

    /**
     * Retrieves the value associated with a given key.
     *
     * @param key The key for which the value is to be retrieved.
     * @return The value associated with the key, or null if the key is not present in the store.
     */
    fun get(key: ByteArray): ByteArray? {
        return store[key.contentHashCode()]
    }

    /**
     * Inserts or updates a key-value pairing in the store.
     *
     * @param key The key to be associated with the given value.
     * @param value The value to be stored.
     */
    fun put(key: ByteArray, value: ByteArray) {
        store[key.contentHashCode()] = value
    }

    /**
     * Checks whether the store contains any key-value pairings.
     *
     * @return True if the store is empty, false otherwise.
     */
    fun isEmpty() = store.isEmpty()

    /**
     * Provides a string representation of the key-value pairings in the store.
     *
     * This is a helper function useful for debugging purposes.
     * @return A string representation of the store's contents.
     */
    override fun toString(): String {
        return store.entries.joinToString("\n") { (key, value) -> "(Key Hash: $key, Value: ${Numeric.toHexString(value)}" }
    }
}
