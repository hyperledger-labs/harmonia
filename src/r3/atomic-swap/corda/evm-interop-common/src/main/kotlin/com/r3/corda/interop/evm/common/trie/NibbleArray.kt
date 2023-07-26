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

/**
 * Represents an array of nibbles (values between 0-15 inclusive)
 */
class NibbleArray(private val values: ByteArray) {

    companion object {
        val empty: NibbleArray = NibbleArray(ByteArray(0))

        fun fromBytes(bytes: ByteArray): NibbleArray {
            val result = ByteArray(bytes.size shl 1)

            bytes.forEachIndexed { byteIndex, byte ->
                val nibbleIndex = byteIndex shl 1
                result[nibbleIndex] = (byte shr 4) and 0x0F
                result[nibbleIndex + 1] = byte and 0x0F
            }

            return NibbleArray(result)
        }
    }

    fun toBytes(): ByteArray {
        require(isEvenSized) {
            "Cannot convert odd-sized nibble array to bytes"
        }

        val result = ByteArray(values.size shr 1)

        values.forEachIndexed { nibbleIndex, nibble ->
            val resultIndex = nibbleIndex shr 1
            result[resultIndex] = if (nibbleIndex.isEven) nibble shl 4
            else result[resultIndex] or nibble
        }

        return result
    }

    fun prepend(prefix: NibbleArray): NibbleArray {
        val prepended = ByteArray(prefix.size + values.size)

        prefix.values.copyInto(prepended, 0)
        values.copyInto(prepended, prefix.size)

        return NibbleArray(prepended)
    }

    fun dropFirst(numberOfNibbles: Int): NibbleArray =
        NibbleArray(values.copyOfRange(numberOfNibbles, values.size))

    fun remainingAfter(index: Int): NibbleArray = dropFirst(index + 1)

    fun takeFirst(numberOfNibbles: Int): NibbleArray =
        NibbleArray(values.copyOfRange(0, numberOfNibbles))

    val size: Int get() = values.size

    val isEvenSized: Boolean get() = size.isEven

    val head: Byte get() = values.firstOrNull() ?:
        throw IllegalStateException("head called on empty nibble array")

    val tail: NibbleArray get() = dropFirst(1)

    fun isEmpty(): Boolean = values.isEmpty()

    fun startsWith(other: NibbleArray): Boolean {
        if (other.size > size) {
            return false
        }
        for (i in other.values.indices) {
            if (values[i] != other[i]) {
                return false
            }
        }
        return true
    }

    fun prefixMatchingLength(other: NibbleArray): Int {
        var ptr = 0
        while (ptr < size && ptr < other.size) {
            if (values[ptr] != other.values[ptr]) break
            ptr++
        }
        return ptr
    }

    operator fun get(index: Int): Byte = values[index]

    override fun equals(other: Any?) =
        this === other || (other is NibbleArray && values.contentEquals(other.values))

    override fun hashCode(): Int = values.contentHashCode()

}

/**
 * Represents the result of comparing the prefixes of a key and a path.
 */
sealed class PathPrefixMatch {

    companion object {
        /**
         * Compare a key and a path, and return a [PathPrefixMatch] representing the agreement between their prefixes.
         */
        fun match(key: NibbleArray, path: NibbleArray): PathPrefixMatch {
            if (key == path) return Equals

            val matchesUpTo = path.prefixMatchingLength(key)
            return when (matchesUpTo) {
                0 -> NoMatch(
                    path[0].toInt(),
                    path.remainingAfter(0),
                    key[0].toInt(),
                    key.remainingAfter(0)
                )

                key.size -> KeyPrefixesPath(path.dropFirst(matchesUpTo))
                path.size -> PathPrefixesKey(key.dropFirst(matchesUpTo))
                else -> PartialMatch(
                    path.takeFirst(matchesUpTo),
                    path.dropFirst(matchesUpTo),
                    key.dropFirst(matchesUpTo)
                )
            }
        }
    }

    /**
     * The path and the key are entirely equal.
     */
    object Equals: PathPrefixMatch()

    /**
     * The path and the key are entirely unequal.
     */
    data class NoMatch(
        val pathHead: Int,
        val pathTail: NibbleArray,
        val keyHead: Int,
        val keyTail: NibbleArray
    ) : PathPrefixMatch()

    /**
     * The entire key (e.g. 1, 2, 3) is a prefix to the path (e.g. 1, 2, 3, 4)
     */
    data class KeyPrefixesPath(val pathRemainder: NibbleArray): PathPrefixMatch() {
        val pathRemainderHead: Int get() = pathRemainder.head.toInt()
        val pathRemainderTail: NibbleArray get() = pathRemainder.tail
    }

    /**
     * The entire path (e.g. 1, 2, 3) is a prefix to the entire key (e.g. 1, 2, 3, 4)
     */
    data class PathPrefixesKey(val keyRemainder: NibbleArray): PathPrefixMatch() {
        val keyRemainderHead: Int get() = keyRemainder.head.toInt()
        val keyRemainderTail: NibbleArray get() = keyRemainder.tail
    }

    /**
     * The key and the path have a shared prefix (e.g. 1, 2) not equal to the entire key (e.g. 1, 2, 3)
     * or the entire path (e.g. 1, 2, 4)
     */
    data class PartialMatch(
        val sharedPrefix: NibbleArray,
        val pathRemainder: NibbleArray,
        val keyRemainder: NibbleArray): PathPrefixMatch() {
        val pathRemainderHead: Int get() = pathRemainder.head.toInt()
        val pathRemainderTail: NibbleArray get() = pathRemainder.tail
        val keyRemainderHead: Int get() = keyRemainder.head.toInt()
        val keyRemainderTail: NibbleArray get() = keyRemainder.tail
    }
}

// Logic operations for Bytes
private infix fun Byte.shl(shift: Int) = (toInt() shl shift).toByte()
private infix fun Byte.shr(shift: Int) = (toInt() shr shift).toByte()
private infix fun Byte.and(other: Byte) = (toInt() and other.toInt()).toByte()
private infix fun Byte.or(other: Byte) = (toInt() or other.toInt()).toByte()

// Test if an Int is even
private val Int.isEven: Boolean get() = (this and 1) == 0

// Not available in this version of Kotlin
private fun ByteArray.copyInto(other: ByteArray, startIndex: Int) {
    var ptr = startIndex
    forEach {
        other[ptr++] = it
    }
}