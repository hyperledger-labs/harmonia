package com.r3.corda.interop.evm.common.trie

class LeafNodeBuilder(val nibbles: NibbleArray) {
    fun withValue(stringValue: String) = LeafNode(nibbles, stringValue.toByteArray())
    fun withValue(vararg byteArray: Byte) = LeafNode(nibbles, byteArray)
}

class ExtensionNodeBuilder(val pathNibbles: NibbleArray) {
    fun empty() = ExtensionNode(pathNibbles, EmptyNode())
    fun withInner(inner: Node) = ExtensionNode(pathNibbles, inner)
    fun toBranches(vararg branches: Pair<Int, Node>) =
        InnerBranchNodeBuilder(this, branchNode(*branches))
}

class InnerBranchNodeBuilder(val builder: ExtensionNodeBuilder, val branchBuilder: BranchNodeBuilder) {
    fun withValue(value: String) = builder.withInner(branchBuilder.withValue(value))
    fun empty() = builder.withInner(branchBuilder.empty())
}

class BranchNodeBuilder(val branches: List<Pair<Byte, Node>>) {
    fun withValue(value: String) = BranchNode.createWithBranches(
        branches = *(branches.toTypedArray()),
        value = value.toByteArray())

    fun empty() = BranchNode.createWithBranches(
        branches = *(branches.toTypedArray()))
}

class TrieBuilder(val trie: PatriciaTrie) {
    fun build(): PatriciaTrie = trie
    fun put(vararg keyBytes: Byte) = TrieValueBuilder(this, keyBytes)
}

class TrieValueBuilder(val builder: TrieBuilder, val keyBytes: ByteArray) {
    fun withValue(stringValue: String) = builder.apply { trie.put(keyBytes, stringValue.toByteArray()) }
}

fun trie(): TrieBuilder = TrieBuilder(PatriciaTrie())

fun leafNodeFromKey(vararg pathBytes: Byte): LeafNodeBuilder = LeafNodeBuilder(NibbleArray.fromBytes(pathBytes))
fun leafNode(vararg nibbles: Byte): LeafNodeBuilder = LeafNodeBuilder(NibbleArray(nibbles))

fun extensionNodeFromKey(vararg pathBytes: Byte): ExtensionNodeBuilder =
    ExtensionNodeBuilder(NibbleArray.fromBytes(pathBytes))
fun extensionNode(vararg nibbles: Byte): ExtensionNodeBuilder =
    ExtensionNodeBuilder(NibbleArray(nibbles))

fun branchNode(vararg branches: Pair<Int, Node>): BranchNodeBuilder =
    BranchNodeBuilder(branches.map { (a, b) -> a.toByte() to b })