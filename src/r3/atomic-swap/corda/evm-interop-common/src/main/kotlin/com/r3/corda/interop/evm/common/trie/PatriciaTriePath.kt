package com.r3.corda.interop.evm.common.trie

import com.r3.corda.interop.evm.common.trie.PatriciaTriePathPrefix.*

enum class PatriciaTriePathPrefix(vararg  nibbleValues: Byte) {
    LEAF_ODD(3),
    LEAF_EVEN(2, 0),
    EXTENSION_ODD(1),
    EXTENSION_EVEN(0, 0);

    companion object {
        fun fromNibbles(nibbles: NibbleArray): PatriciaTriePathPrefix =
            values().firstOrNull { nibbles.startsWith(it.prefixNibbles) } ?:
                throw IllegalArgumentException("Nibbles $nibbles do not start with a valid prefix")
    }

    val prefixNibbles = NibbleArray(nibbleValues)
}

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

    fun applyPrefix(nibbles: NibbleArray): NibbleArray =
        nibbles.prepend((if (nibbles.isEvenSized) evenPrefix else oddPrefix).prefixNibbles)
}

object PatriciaTriePath {

    fun fromBytes(bytes: ByteArray): Pair<PatriciaTriePathType, NibbleArray> {
        val allNibbles = NibbleArray.fromBytes(bytes)

        val prefix = PatriciaTriePathPrefix.fromNibbles(allNibbles)

        return PatriciaTriePathType.forPrefix(prefix) to allNibbles.dropFirst(prefix.prefixNibbles.size)
    }

}

