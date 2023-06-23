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

    fun applyPrefix(nibbles: NibbleArray): NibbleArray =
        nibbles.prepend((if (nibbles.isEvenSized) evenPrefix else oddPrefix).prefixNibbles)
}

