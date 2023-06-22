package com.r3.corda.interop.evm.common.trie

import com.r3.corda.interop.evm.common.trie.PatriciaTriePathPrefix.*

enum class PatriciaTriePathPrefix(private val prefixBytes: ByteArray) {
    LEAF_ODD(byteArrayOf(3)),
    LEAF_EVEN(byteArrayOf(2, 0)),
    EXTENSION_ODD(byteArrayOf(1)),
    EXTENSION_EVEN(byteArrayOf(0, 0));

    companion object {
        fun fromNibbles(nibbles: NibbleArray): PatriciaTriePathPrefix =
            values().firstOrNull { nibbles.values.startsWith(it.prefixBytes) } ?:
                throw IllegalArgumentException("Nibbles $nibbles do not start with a valid prefix")
    }

    fun addToNibbles(nibbles: NibbleArray): NibbleArray = nibbles.prepend(prefixBytes)

    val size: Int get() = prefixBytes.size
}

enum class PatriciaTriePathType(private val oddPrefix: PatriciaTriePathPrefix, private val evenPrefix: PatriciaTriePathPrefix) {
    LEAF(LEAF_ODD, LEAF_EVEN),
    EXTENSION(EXTENSION_ODD, EXTENSION_EVEN);

    companion object {
        fun forPrefix(prefix: PatriciaTriePathPrefix): PatriciaTriePathType = when(prefix) {
            LEAF_ODD -> LEAF
            LEAF_EVEN -> LEAF
            EXTENSION_ODD -> EXTENSION
            EXTENSION_EVEN -> EXTENSION
        }
    }

    fun prefixNibbles(nibbles: NibbleArray): NibbleArray =
        (if (nibbles.values.size.isEven) evenPrefix else oddPrefix).addToNibbles(nibbles)
}

/**
 * Represents an array of nibbles (values between 0-15 inclusive)
 */
data class NibbleArray(val values: ByteArray) {

    companion object {
        fun fromBytes(bytes: ByteArray): NibbleArray {
            val result = ByteArray(bytes.size shl 1)

            bytes.forEachIndexed { byteIndex, byte ->
                val nibbleIndex = byteIndex shl 1
                result[nibbleIndex] = (byte shr 4) and 0x0F
                result[nibbleIndex + 1] = byte and 0x0F
            }

            return NibbleArray(result)
        }

        fun ofNibbles(vararg values: Byte): NibbleArray = NibbleArray(values)
    }

    fun toBytes(): ByteArray {
        require(values.size.isEven) {
            "Cannot convert odd-sized nibble array to bytes"
        }

        val result = ByteArray(values.size shr 1)

        values.forEachIndexed { nibbleIndex, nibble ->
            val resultIndex = nibbleIndex shr 1
            result[resultIndex] = if (nibbleIndex.isEven) nibble shl 4
            else result[resultIndex] or nibble
        }

        return result;
    }

    fun prepend(prefixNibbles: ByteArray): NibbleArray {
        val prepended = ByteArray(prefixNibbles.size + values.size)

        prefixNibbles.copyInto(prepended, 0)
        values.copyInto(prepended, prefixNibbles.size)

        return NibbleArray(prepended)
    }

    fun dropFirst(numberOfNibbles: Int): NibbleArray =
        NibbleArray(values.copyOfRange(numberOfNibbles, values.size))

    fun takeFirst(numberOfNibbles: Int): NibbleArray =
        NibbleArray(values.copyOfRange(0, numberOfNibbles))

    val size: Int get() = values.size

    val head: Byte get() = values.firstOrNull() ?:
        throw IllegalStateException("head called on empty nibble array")

    val tail: NibbleArray get() = dropFirst(1)

    fun isEmpty(): Boolean = values.isEmpty()

    fun startsWith(other: NibbleArray): Boolean = values.startsWith(other.values)

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

data class PatriciaTriePath(val type: PatriciaTriePathType, val pathNibbles: NibbleArray) {

    companion object {

        fun fromBytes(bytes: ByteArray): PatriciaTriePath {
            val allNibbles = NibbleArray.fromBytes(bytes)

            val prefix = PatriciaTriePathPrefix.fromNibbles(allNibbles)

            return PatriciaTriePath(
                PatriciaTriePathType.forPrefix(prefix),
                allNibbles.dropFirst(prefix.size)
            )
        }

        fun forLeaf(pathNibbles: NibbleArray): PatriciaTriePath =
            PatriciaTriePath(PatriciaTriePathType.LEAF, pathNibbles)

        fun forExtension(pathNibbles: NibbleArray): PatriciaTriePath =
            PatriciaTriePath(PatriciaTriePathType.EXTENSION, pathNibbles)
    }

    fun toBytes(): ByteArray = type.prefixNibbles(pathNibbles).toBytes()

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

/**
 * Checks if the ByteArray starts with the specified ByteArray.
 *
 * The function iterates over the elements in the original ByteArray and
 * the other ByteArray simultaneously. If the original ByteArray is shorter
 * than the other ByteArray, or if any element in the original ByteArray
 * doesn't match the corresponding element in the other ByteArray, the
 * function returns false. If all elements match, the function returns true.
 *
 * @param other the ByteArray to check at the start of this ByteArray
 * @return true if this ByteArray starts with the specified ByteArray, false otherwise
 */
private fun ByteArray.startsWith(other: ByteArray): Boolean {
    if (other.size > this.size) {
        return false
    }
    for (i in other.indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}