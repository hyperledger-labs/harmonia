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

data class PatriciaTriePath(val pathType: PatriciaTriePathType, val path: NibbleArray) {
    companion object {
        /**
         * Given a [ByteArray] encoding a prefixed Patricia Trie path, identify the prefix and return a
         * [PatriciaTriePath] capturing the path type and the path minus the prefix.
         *
         * @param bytes The [ByteArray] to decode.
         */
        fun fromPrefixedBytes(bytes: ByteArray): PatriciaTriePath {
            val allNibbles = NibbleArray.fromBytes(bytes)

            val prefix = PatriciaTriePathPrefix.values().firstOrNull { allNibbles.startsWith(it.prefixNibbles) }
                ?: throw IllegalArgumentException("Nibbles $allNibbles do not start with a valid prefix")

            val pathType = PatriciaTriePathType.forPrefix(prefix)
            val pathNibbles = allNibbles.dropFirst(prefix.prefixNibbles.size)

            return PatriciaTriePath(pathType, pathNibbles)
        }
    }
}

/**
 * Encodes the four possible prefixes of encoded [PatriciaTriePath]s.
 */
enum class PatriciaTriePathPrefix(vararg  nibbleValues: Byte) {
    LEAF_ODD(3),
    LEAF_EVEN(2, 0),
    EXTENSION_ODD(1),
    EXTENSION_EVEN(0, 0);

    val prefixNibbles = NibbleArray.of(nibbleValues)
}

