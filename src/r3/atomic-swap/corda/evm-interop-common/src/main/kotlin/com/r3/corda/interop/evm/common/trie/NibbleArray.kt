package com.r3.corda.interop.evm.common.trie

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

    fun isNotEmpty(): Boolean = values.isNotEmpty()

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