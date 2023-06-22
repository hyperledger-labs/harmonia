package com.r3.corda.interop.evm.common.trie

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PatriciaTriePathTests {

    @Test
    fun oddNumberedLeafPathRoundTripsCorrectly() {
        assertRoundTripsCorrectly(
            PatriciaTriePathType.LEAF,
            0x1, 0xC, 0x5)
    }

    @Test
    fun evenNumberedLeafPathRoundTripsCorrectly() {
        assertRoundTripsCorrectly(
            PatriciaTriePathType.LEAF,
            0x1, 0xC, 0x5, 0x6)
    }

    @Test
    fun oddNumberedExtensionPathRoundTripsCorrectly() {
        assertRoundTripsCorrectly(
            PatriciaTriePathType.EXTENSION,
            0x1, 0xC, 0x5)
    }

    @Test
    fun evenNumberedExtensionPathRoundTripsCorrectly() {
        assertRoundTripsCorrectly(
            PatriciaTriePathType.EXTENSION,
            0x1, 0xC, 0x5, 0x6)
    }

    private fun assertRoundTripsCorrectly(type: PatriciaTriePathType, vararg nibbles: Byte) {
        val path = PatriciaTriePath(type, NibbleArray(nibbles))

        assertEquals(path, PatriciaTriePath.fromBytes(path.type.prefixNibbles(path.pathNibbles).toBytes()))
    }
}