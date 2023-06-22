package com.r3.corda.interop.evm.common.trie

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PatriciaTriePathTests {

    @Test
    fun oddNumberedLeafPathRoundTripsCorrectly() {
        val unit = PatriciaTriePath(
            PatriciaTriePathType.LEAF,
            NibbleArray.ofNibbles(0x1, 0xC, 0x5)
        )

        assertEquals(unit, PatriciaTriePath.fromBytes(unit.toBytes()))
    }

    @Test
    fun evenNumberedLeafPathRoundTripsCorrectly() {
        val unit = PatriciaTriePath(
            PatriciaTriePathType.LEAF,
            NibbleArray.ofNibbles(0x1, 0xC, 0x5, 0x6)
        )

        assertEquals(unit, PatriciaTriePath.fromBytes(unit.toBytes()))
    }

    @Test
    fun oddNumberedExtensionPathRoundTripsCorrectly() {
        val unit = PatriciaTriePath(
            PatriciaTriePathType.EXTENSION,
            NibbleArray.ofNibbles(0x1, 0xC, 0x5)
        )

        assertEquals(unit, PatriciaTriePath.fromBytes(unit.toBytes()))
    }

    @Test
    fun evenNumberedExtensionPathRoundTripsCorrectly() {
        val unit = PatriciaTriePath(
            PatriciaTriePathType.EXTENSION,
            NibbleArray.ofNibbles(0x1, 0xC, 0x5, 0x6)
        )

        assertEquals(unit, PatriciaTriePath.fromBytes(unit.toBytes()))
    }
}