package com.r3.corda.interop.evm.common.trie

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