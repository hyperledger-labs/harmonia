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

import com.r3.corda.interop.evm.common.trie.PatriciaTriePathPrefix.*

enum class PatriciaTriePathType(
    private val oddPrefix: PatriciaTriePathPrefix,
    private val evenPrefix: PatriciaTriePathPrefix
) {
    LEAF(LEAF_ODD, LEAF_EVEN),
    EXTENSION(EXTENSION_ODD, EXTENSION_EVEN);

    companion object {
        fun forPrefix(prefix: PatriciaTriePathPrefix): PatriciaTriePathType = when (prefix) {
            LEAF_ODD -> LEAF
            LEAF_EVEN -> LEAF
            EXTENSION_ODD -> EXTENSION
            EXTENSION_EVEN -> EXTENSION
        }
    }

    /**
     * Encode the supplied [NibbleArray] as a [ByteArray] prefixed with the appropriate prefix for this type and the
     * array length.
     *
     * @param nibbles The [NibbleArray] to encode.
     */
    fun getPrefixedBytes(nibbles: NibbleArray): ByteArray {
        val prefix = (if ((nibbles.size and 1) == 0) evenPrefix else oddPrefix).prefixNibbles

        val totalSize = prefix.size + nibbles.size
        val allNibbles = prefix.asSequence() + nibbles.asSequence()
        val result = ByteArray(totalSize shr 1)

        allNibbles.forEachIndexed { nibbleIndex, nibble ->
            val resultIndex = nibbleIndex shr 1
            result[resultIndex] = if ((nibbleIndex and 1) == 0) nibble shl 4 else result[resultIndex] or nibble
        }

        return result
    }

    private infix fun Byte.shl(shift: Int) = (toInt() shl shift).toByte()
    private infix fun Byte.or(other: Byte) = (toInt() or other.toInt()).toByte()
}

