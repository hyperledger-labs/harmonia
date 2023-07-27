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
class NibbleArray private constructor(private val values: ByteArray, private val range: IntRange) {

    companion object {

        /**
         * An empty [NibbleArray].
         */
        val empty: NibbleArray = NibbleArray(ByteArray(0), IntRange.EMPTY)

        /**
         * Construct a [NibbleArray] containing the provided nibble values.
         *
         * @param nibbles The nibbles to wrap.
         */
        fun of(nibbles: ByteArray): NibbleArray = NibbleArray(nibbles, nibbles.indices)

        /**
         * Convert an array of bytes (which contains two nibbles per byte) into a [NibbleArray].
         *
         * @param bytes The bytes to break into nibbles to build the [NibbleArray].
         */
        fun fromBytes(bytes: ByteArray): NibbleArray {
            val result = ByteArray(bytes.size shl 1)

            bytes.forEachIndexed { byteIndex, byte ->
                val nibbleIndex = byteIndex shl 1
                result[nibbleIndex] = (((byte.toInt() shr 4).toByte()).toInt() and 0x0F).toByte()
                result[nibbleIndex + 1] = (byte.toInt() and 0x0F).toByte()
            }

            return of(result)
        }
    }

    /**
     * The number of nibbles in the array.
     */
    val size: Int get() = range.last + 1 - range.first

    /**
     * Returns a copy of the [NibbleArray] minus a given number of nibbles removed from the start.
     *
     * @param count The number of nibbles to drop from the start of the array.
     */
    fun dropFirst(count: Int): NibbleArray = when {
        count == size -> empty
        count <= size -> NibbleArray(values, IntRange(range.first + count, range.last))
        else -> throw IndexOutOfBoundsException()
    }

    /**
     * Returns a [NibbleArray] containing only the first n nibbles from this array.
     *
     * @param count The number of nibbles to take from the start of the array.
     */
    fun takeFirst(count: Int): NibbleArray = when {
        count == 0 -> empty
        count < size -> NibbleArray(values, range.first until (range.first + count))
        count == size -> this
        else -> throw IndexOutOfBoundsException()
    }

    /**
     * Get the nibble at the provided index.
     *
     * @param index The index of the nibble to retrieve.
     */
    operator fun get(index: Int): Byte = values[index + range.first]

    /**
     * Obtain the nibbles in this [NibbleArray] as a [Sequence<Byte>].
     */
    fun asSequence(): Sequence<Byte> = (0 until size).asSequence().map { this[it] }

    /**
     * Return a [NibbleArray] containing the nibbles after (and not including) the nibble at the provided index.
     *
     * @param index The index of the nibble to split this nibble array at.
     */
    fun remainingAfter(index: Int): NibbleArray = dropFirst(index + 1)

    /**
     * Return the first element of this array, or throw [IllegalStateException] if it is empty.
     */
    val head: Byte get() = if (isEmpty()) throw IllegalStateException("head called on empty nibble array") else this[0]

    /**
     * Return the suffix of this array after the first element has been removed.
     */
    val tail: NibbleArray get() = dropFirst(1)

    /**
     * Returns true if this array is empty, and false otherwise.
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * Calculates the length of the match between the supplied [NibbleArray] and the prefix of this [NibbleArray].
     *
     * @param other The [NibbleArray] to compare.
     */
    fun prefixMatchingLength(other: NibbleArray): Int {
        var ptr = 0
        while (ptr < size && ptr < other.size) {
            if (this[ptr] != other[ptr]) break
            ptr++
        }
        return ptr
    }

    /**
     * Returns true if this [NibbleArray] starts with the supplied [NibbleArray], and false otherwise.
     *
     * @param other The [NibbleArray] to compare.
     */
    fun startsWith(other: NibbleArray): Boolean = prefixMatchingLength(other) == other.size

    // Two NibbleArrays are equal if they have the same values at the same indices
    override fun equals(other: Any?): Boolean =
        (other is NibbleArray) && (size == other.size) && (0 until size).all { this[it] == other[it] }

    override fun hashCode(): Int = asSequence().fold(0) { a, i -> a + 31 * i.hashCode() }
}

/**
 * Represents the result of comparing the prefixes of a key and a path, capturing useful matching and non-matching parts.
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
                    path[0].toInt(), path.remainingAfter(0), key[0].toInt(), key.remainingAfter(0)
                )

                key.size -> KeyPrefixesPath(path.dropFirst(matchesUpTo))
                path.size -> PathPrefixesKey(key.dropFirst(matchesUpTo))
                else -> PartialMatch(
                    path.takeFirst(matchesUpTo), path.dropFirst(matchesUpTo), key.dropFirst(matchesUpTo)
                )
            }
        }
    }

    /**
     * The path and the key are entirely equal.
     */
    object Equals : PathPrefixMatch()

    /**
     * The path and the key are entirely unequal.
     */
    data class NoMatch(
        val pathHead: Int, val pathTail: NibbleArray, val keyHead: Int, val keyTail: NibbleArray
    ) : PathPrefixMatch()

    /**
     * The entire key (e.g. 1, 2, 3) is a prefix to the path (e.g. 1, 2, 3, 4)
     */
    data class KeyPrefixesPath(val pathRemainder: NibbleArray) : PathPrefixMatch() {
        val pathRemainderHead: Int get() = pathRemainder.head.toInt()
        val pathRemainderTail: NibbleArray get() = pathRemainder.tail
    }

    /**
     * The entire path (e.g. 1, 2, 3) is a prefix to the entire key (e.g. 1, 2, 3, 4)
     */
    data class PathPrefixesKey(val keyRemainder: NibbleArray) : PathPrefixMatch() {
        val keyRemainderHead: Int get() = keyRemainder.head.toInt()
        val keyRemainderTail: NibbleArray get() = keyRemainder.tail
    }

    /**
     * The key and the path have a shared prefix (e.g. 1, 2) not equal to the entire key (e.g. 1, 2, 3)
     * or the entire path (e.g. 1, 2, 4)
     */
    data class PartialMatch(
        val sharedPrefix: NibbleArray, val pathRemainder: NibbleArray, val keyRemainder: NibbleArray
    ) : PathPrefixMatch() {
        val pathRemainderHead: Int get() = pathRemainder.head.toInt()
        val pathRemainderTail: NibbleArray get() = pathRemainder.tail
        val keyRemainderHead: Int get() = keyRemainder.head.toInt()
        val keyRemainderTail: NibbleArray get() = keyRemainder.tail
    }
}