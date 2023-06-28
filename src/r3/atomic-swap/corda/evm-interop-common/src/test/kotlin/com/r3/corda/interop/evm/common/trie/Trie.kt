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

import org.junit.Assert.*
import org.junit.Test
import org.web3j.crypto.Hash
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric

class TrieTests {

    @Test
    fun testEmptyNodeHashCalculation() {
        val emptyNode = EmptyNode
        val expectedHash = "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"

        assertEquals(expectedHash, Numeric.toHexString(emptyNode.hash))
    }

    @Test
    fun testEmptyLeafNodeHashCalculation() {
        val leaf = leafNodeFromKey().withValue()
        val expectedHash = "0xf9be828fd675253c2e3ecdff4379debab459f376b7554fac193747c676f10f0a"

        assertEquals(expectedHash, Numeric.toHexString(leaf.hash))
    }

    @Test
    fun testDummyLeafNodeHashCalculation() {
        val leaf = leafNodeFromKey(1, 2, 3, 4).withValue("leaf")
        val expectedHash = "0x2fc0c91eb10b756afb03c8ceafc121c9c2f4eb47b6ef974ba808f8b46067a6d0"

        assertEquals(expectedHash, Numeric.toHexString(leaf.hash))
    }

    @Test
    fun testEmptyBranchNodeHashCalculation() {
        val branch = branchNode().empty()
        val expectedHash = "0xbe0f4440e293a47160b9b148d49212d0616ec5b0a70c99de9bf36515d52e0901"

        assertEquals(expectedHash, Numeric.toHexString(branch.hash))
    }

    @Test
    fun testEmptyExtensionNodeHashCalculation() {
        val branch = extensionNodeFromKey().empty()
        val expectedHash = "0x9650f4d88ca5e8628148a9c9ced5c6063f7419b93a0bf854258890ddea8f233d"

        assertEquals(expectedHash, Numeric.toHexString(branch.hash))
    }

    @Test
    fun testEmptyTrieRootNode() {
        val trie = trie { }
        val expectedHash = "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"

        assert(trie.root is EmptyNode)
        assertEquals(expectedHash, Numeric.toHexString(trie.root.hash))
    }

    @Test
    fun testTrieWithSingleLeafNode() {
        val trie = trie {
            "leaf".at(1, 2, 3, 4)
        }

        val key = byteArrayOf(1, 2, 3, 4)
        val value = "leaf".toByteArray()

        trie.put(key, value)

        val leaf = Node.leaf(key, value)
        val expectedHash = "0x2fc0c91eb10b756afb03c8ceafc121c9c2f4eb47b6ef974ba808f8b46067a6d0"

        assertTrue(trie.root is LeafNode)
        assertArrayEquals(leaf.hash, trie.root.hash)
        assertEquals(expectedHash, Numeric.toHexString(trie.root.hash))
    }

    @Test
    fun testTrieWithTwoLeafNodesSecondShorter() {
        val trie = trie {
            "hello".at(1, 2, 3, 4)
            "world".at(1, 2, 3)
        }

        val node = extensionNode(0, 1, 0, 2, 0, 3).toBranches(
            0 to leafNode(4).withValue("hello")
        ).withValue("world")

        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun testTrieWithTwoLeafNodesSecondLonger() {
        val trie = trie {
            "hello".at(1, 2, 3, 4)
            "world".at(1 ,2 ,3 ,4 ,5 ,6)
        }

        val node = extensionNode(0, 1, 0, 2, 0, 3, 0, 4).toBranches(
            0 to leafNode(5, 0, 6).withValue("world")
        ).withValue("hello")

        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun testTrieWithTwoLeafNodesSameLength() {
        val trie = trie {
            "hello".at(1, 2, 3, 4)
            "world".at(1, 2, 3, 4)
        }

        val leaf = leafNode(0, 1, 0, 2, 0, 3, 0, 4).withValue("world")

        assertArrayEquals(leaf.hash, trie.root.hash)
    }

    @Test
    fun testTrieWithPrefixMatchingLeafNodesThirdMatchingLengthLong() {
        val trie = trie {
            "hello1".at(1, 2, 3, 4)
            "hello2".at(1, 2, 3, 5)
            "world".at(1, 2, 3)
        }

        val node = extensionNode(0, 1, 0, 2, 0, 3).toBranches(
            0 to branchNode(
                4 to leafNode().withValue("hello1"),
                5 to leafNode().withValue("hello2")).empty())
            .withValue("world")

        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun testTrieWithPrefixMatchingLeafNodesThirdShorterPartiallyMatching() {
        val trie = trie {
            "hello1".at(1, 2, 3, 4)
            "hello2".at(1, 2, 3, 5)
            "world".at(1, 2, 5)
        }

        val node = extensionNode(0, 1, 0, 2, 0).toBranches(
            3 to extensionNode(0).toBranches(
                4 to leafNode().withValue("hello1"),
                5 to leafNode().withValue("hello2")
            ).empty(),
            5 to leafNode().withValue("world")).empty()


        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun testTrieWithPartiallyMatchingLeafNodesThirdNoMatch() {
        val trie = trie {
            "hello1".at(1, 2, 3, 4)
            "hello2".at(1, 2, 3, 5)
            "world".at(16, 2, 5)
        }

        val node = branchNode(
            0 to extensionNode(1, 0, 2, 0, 3, 0).toBranches(
                4 to leafNode().withValue("hello1"),
                5 to leafNode().withValue("hello2")
            ).empty(),
            1 to leafNode(0, 0, 2, 0, 5).withValue("world")
        ).empty()

        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun testPatriciaTrieWithStaticTransactionData() {
        val trie = trie {
            "hello1".at(1, 2, 3, 4)
            "hello2".at(1, 2, 3, 80)
            "world".at(1, 2, 3)
        }

        val node = extensionNode(0, 1, 0, 2, 0, 3).toBranches(
            0 to leafNode(4).withValue("hello1"),
            5 to leafNode(0).withValue("hello2"))
            .withValue("world")

        assertArrayEquals(node.hash, trie.root.hash)
    }

    @Test
    fun `should handle patricia trie with static data from existing transaction correctly`() {

        // Source TX data: https://etherscan.io/block/10593417

        val p1 = Pair(
            Numeric.toHexString(RlpEncoder.encode(RlpString.create(0L))),
            "0xf8ab81a5852e90edd00083012bc294a3bed4e1c75d00fa6f4e5e6922db7261b5e9acd280b844a9059cbb0000000000000000000000008bda8b9823b8490e8cf220dc7b91d97da1c54e250000000000000000000000000000000000000000000000056bc75e2d6310000026a06c89b57113cf7da8aed7911310e03d49be5e40de0bd73af4c9c54726c478691ba056223f039fab98d47c71f84190cf285ce8fc7d9181d6769387e5efd0a970e2e9"
        )
        val p2 = Pair(
            Numeric.toHexString(RlpEncoder.encode(RlpString.create(1L))),
            "0xf8ab81a6852e90edd00083012bc294a3bed4e1c75d00fa6f4e5e6922db7261b5e9acd280b844a9059cbb0000000000000000000000008bda8b9823b8490e8cf220dc7b91d97da1c54e250000000000000000000000000000000000000000000000056bc75e2d6310000026a0d77c66153a661ecc986611dffda129e14528435ed3fd244c3afb0d434e9fd1c1a05ab202908bf6cbc9f57c595e6ef3229bce80a15cdf67487873e57cc7f5ad7c8a"
        )
        val p3 = Pair(
            Numeric.toHexString(RlpEncoder.encode(RlpString.create(2L))),
            "0xf86d8229f185199c82cc008252089488e9a2d38e66057e18545ce03b3ae9ce4fc360538702ce7de1537c008025a096e7a1d9683b205f697b4073a3e2f0d0ad42e708f03e899c61ed6a894a7f916aa05da238fbb96d41a4b5ec0338c86cfcb627d0aa8e556f21528e62f31c32f7e672"
        )
        val p4 = Pair(
            Numeric.toHexString(RlpEncoder.encode(RlpString.create(3L))),
            "0xf86f826b2585199c82cc0083015f9094e955ede0a3dbf651e2891356ecd0509c1edb8d9c8801051fdc4efdc0008025a02190f26e70a82d7f66354a13cda79b6af1aa808db768a787aeb348d425d7d0b3a06a82bd0518bc9b69dc551e20d772a1b06222edfc5d39b6973e4f4dc46ed8b196"
        )

        assertEquals("0xb0c43213c86c2cacce8ceef965b881529d31e5be93ad6cefcef2f319a20ef1b5", Hash.sha3(p1.second))
        assertEquals("0x5bbbf64bd0f08465acbe30adb2be807488c3847c94a7dfabaffa3e25ab3a604a", Hash.sha3(p2.second))
        assertEquals("0x7d965a103dbb8e2027682e45bd371cf92bb9e15b84d5b2fa0dfa45333879ed12", Hash.sha3(p3.second))
        assertEquals("0x0b41fc4c1d8518cdeda9812269477256bdc415eb39c4531885ff9728d6ad096b", Hash.sha3(p4.second))

        val trie = PatriciaTrie()
        trie.put(Numeric.hexStringToByteArray(p1.first), Numeric.hexStringToByteArray(p1.second))
        trie.put(Numeric.hexStringToByteArray(p2.first), Numeric.hexStringToByteArray(p2.second))
        trie.put(Numeric.hexStringToByteArray(p3.first), Numeric.hexStringToByteArray(p3.second))
        trie.put(Numeric.hexStringToByteArray(p4.first), Numeric.hexStringToByteArray(p4.second))

        val treeRoot = Numeric.toHexString(trie.root.hash)
        assertEquals("0xab41f886be23cd786d8a69a72b0f988ea72e0b2e03970d0798f5e03763a442cc", treeRoot)
    }
}
