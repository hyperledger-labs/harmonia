/*
 * Copyright 2023, R3 LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.r3.corda.interop.evm.common.trie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Hash
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric

class ProofTests {

    @Test
    fun testMerkleProofWithOneLeafNodeAndOneShorterLeafNode() {
        assertProofIsValidForTrieWith(
            "hello".at(1, 2, 3, 4),
            "world".at(1, 2, 3)
        )
    }

    @Test
    fun testMerkleProofWithOneLeafNodeAndOneLongerLeafNode() {
        assertProofIsValidForTrieWith(
            "hello".at(1, 2, 3, 4),
            "world".at(1, 2, 3, 4, 5, 6)
        )
    }

    @Test
    fun testMerkleProofWithPrefixMatchingLeafNodesOfSameAndLongerLengths() {
        assertProofIsValidForTrieWith(
            "hello1".at(1, 2, 3, 4),
            "hello2".at(1, 2, 3, 5),
            "world".at(1, 2, 3)
        )
    }

    @Test
    fun testMerkleProofWithPrefixMatchingLeafNodesOfSameAndShorterLengths() {
        assertProofIsValidForTrieWith(
            "hello1".at(1, 2, 3, 4),
            "hello2".at(1, 2, 3, 5),
            "world".at(1, 2, 5)
        )
    }

    @Test
    fun testMerkleProofWithPartiallyMatchingLeafNodesOfSameAndShorterLengths() {
        assertProofIsValidForTrieWith(
            "hello1".at(1, 2, 3, 4),
            "hello2".at(1, 2, 3, 5),
            "world".at(16, 2, 5)
        )
    }

    @Test
    fun testMerkleProofWithFullyMatchingExtensionNodes() {
        assertProofIsValidForTrieWith(
            "hello1".at(1, 2, 3, 4),
            "hello2".at(1, 2, 3, 80),
            "world".at(1, 2, 3)
        )
    }

    @Test
    fun testMerkleProofOfPatriciaTrieWithStaticTransactionData() {
        fun buildStringPair(key: Long, data: String): Pair<String, String> =
            Numeric.toHexString(RlpEncoder.encode(RlpString.create(key))) to data

        // Source TX data: https://etherscan.io/block/10593417
        val p1 = buildStringPair(
            0L,
            "0xf8ab81a5852e90edd00083012bc294a3bed4e1c75d00fa6f4e5e6922db7261b5e9acd280b844a9059cbb0000000000000000000000008bda8b9823b8490e8cf220dc7b91d97da1c54e250000000000000000000000000000000000000000000000056bc75e2d6310000026a06c89b57113cf7da8aed7911310e03d49be5e40de0bd73af4c9c54726c478691ba056223f039fab98d47c71f84190cf285ce8fc7d9181d6769387e5efd0a970e2e9"
        )

        val p2 = buildStringPair(
            1L,
            "0xf8ab81a6852e90edd00083012bc294a3bed4e1c75d00fa6f4e5e6922db7261b5e9acd280b844a9059cbb0000000000000000000000008bda8b9823b8490e8cf220dc7b91d97da1c54e250000000000000000000000000000000000000000000000056bc75e2d6310000026a0d77c66153a661ecc986611dffda129e14528435ed3fd244c3afb0d434e9fd1c1a05ab202908bf6cbc9f57c595e6ef3229bce80a15cdf67487873e57cc7f5ad7c8a"
        )
        val p3 = buildStringPair(
            2L,
            "0xf86d8229f185199c82cc008252089488e9a2d38e66057e18545ce03b3ae9ce4fc360538702ce7de1537c008025a096e7a1d9683b205f697b4073a3e2f0d0ad42e708f03e899c61ed6a894a7f916aa05da238fbb96d41a4b5ec0338c86cfcb627d0aa8e556f21528e62f31c32f7e672"
        )
        val p4 = buildStringPair(
            3L,
            "0xf86f826b2585199c82cc0083015f9094e955ede0a3dbf651e2891356ecd0509c1edb8d9c8801051fdc4efdc0008025a02190f26e70a82d7f66354a13cda79b6af1aa808db768a787aeb348d425d7d0b3a06a82bd0518bc9b69dc551e20d772a1b06222edfc5d39b6973e4f4dc46ed8b196"
        )

        val expectedHashes = mapOf(
            p1 to "0xb0c43213c86c2cacce8ceef965b881529d31e5be93ad6cefcef2f319a20ef1b5",
            p2 to "0x5bbbf64bd0f08465acbe30adb2be807488c3847c94a7dfabaffa3e25ab3a604a",
            p3 to "0x7d965a103dbb8e2027682e45bd371cf92bb9e15b84d5b2fa0dfa45333879ed12",
            p4 to "0x0b41fc4c1d8518cdeda9812269477256bdc415eb39c4531885ff9728d6ad096b"
        )

        expectedHashes.forEach { (pair, hash) ->
            assertEquals(hash, Hash.sha3(pair.second))
        }

        assertProofIsValidForTrieWith(expectedHashes.keys.map { (first, second) ->
            Numeric.hexStringToByteArray(first) to Numeric.hexStringToByteArray(second)
        })
    }

    // Helper functions for building test cases

    private fun String.at(vararg key: Byte): Pair<ByteArray, ByteArray> = key to this.toByteArray()

    private fun assertProofIsValidForTrieWith(vararg entries: Pair<ByteArray, ByteArray>) {
        assertProofIsValidForTrieWith(entries.toList())
    }

    private fun assertProofIsValidForTrieWith(entries: Collection<Pair<ByteArray, ByteArray>>) {
        val trie = PatriciaTrie()
        entries.forEach { (key, value) ->
            trie.put(key, value)
        }

        assertTrue(entries.all { (key, value) ->
            val proof = trie.generateMerkleProof(key)

            PatriciaTrie.verifyMerkleProof(
                rootHash = trie.root.hash,
                key = key,
                expectedValue = value,
                proof = proof)
        })
    }
}
