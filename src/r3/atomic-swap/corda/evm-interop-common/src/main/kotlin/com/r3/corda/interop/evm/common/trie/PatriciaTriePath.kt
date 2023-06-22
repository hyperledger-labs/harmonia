package com.r3.corda.interop.evm.common.trie

import com.r3.corda.interop.evm.common.trie.PatriciaTriePathPrefix.*

enum class PatriciaTriePathPrefix(vararg  prefixNibbles: Byte) {
    LEAF_ODD(3),
    LEAF_EVEN(2, 0),
    EXTENSION_ODD(1),
    EXTENSION_EVEN(0, 0);

    val prefix = NibbleArray(prefixNibbles)

    companion object {
        fun fromNibbles(nibbles: NibbleArray): PatriciaTriePathPrefix =
            values().firstOrNull { nibbles.startsWith(it.prefix) } ?:
                throw IllegalArgumentException("Nibbles $nibbles do not start with a valid prefix")
    }

    fun addToNibbles(nibbles: NibbleArray): NibbleArray = nibbles.prepend(prefix)

    val size: Int get() = prefix.size
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
        (if (nibbles.isEvenSized) evenPrefix else oddPrefix).addToNibbles(nibbles)
}

object PatriciaTriePath {

    fun fromBytes(bytes: ByteArray): Pair<PatriciaTriePathType, NibbleArray> {
        val allNibbles = NibbleArray.fromBytes(bytes)

        val prefix = PatriciaTriePathPrefix.fromNibbles(allNibbles)

        return PatriciaTriePathType.forPrefix(prefix) to allNibbles.dropFirst(prefix.size)
    }

}

