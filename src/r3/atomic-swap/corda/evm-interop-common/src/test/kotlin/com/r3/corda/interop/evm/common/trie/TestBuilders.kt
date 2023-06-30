package com.r3.corda.interop.evm.common.trie

class LeafNodeBuilder(val nibbles: NibbleArray) {
    fun withValue(stringValue: String) = LeafNode(nibbles, stringValue.toByteArray())
    fun withValue(vararg byteArray: Byte) = LeafNode(nibbles, byteArray)
}

class ExtensionNodeBuilder(val pathNibbles: NibbleArray) {
    fun empty() = ExtensionNode(pathNibbles, EmptyNode)
    fun withInner(inner: Node) = ExtensionNode(pathNibbles, inner)
    fun toBranches(vararg branches: Pair<Int, Node>) =
        InnerBranchNodeBuilder(this, branchNode(*branches))
}

class InnerBranchNodeBuilder(val builder: ExtensionNodeBuilder, val branchBuilder: BranchNodeBuilder) {
    fun withValue(value: String) = builder.withInner(branchBuilder.withValue(value))
    fun empty() = builder.withInner(branchBuilder.empty())
}

class BranchNodeBuilder(val branches: List<Pair<Int, Node>>) {

    fun withValue(value: String): Node = withValue(value.toByteArray())

    fun withValue(value: ByteArray): Node = BranchNode.from(branches, value)

    fun empty() = BranchNode.from(branches)
}

fun trie(build: TrieBuilder.() -> Unit): PatriciaTrie {
    val builder = TrieBuilder()
    builder.build()
    return builder.trie
}

class TrieBuilder {
    val trie = PatriciaTrie()

    fun String.at(vararg bytes: Byte) {
        trie.put(bytes, this.toByteArray())
    }
}

fun leafNodeFromKey(vararg pathBytes: Byte): LeafNodeBuilder = LeafNodeBuilder(NibbleArray.fromBytes(pathBytes))
fun leafNode(vararg nibbles: Byte): LeafNodeBuilder = LeafNodeBuilder(NibbleArray(nibbles))

fun extensionNodeFromKey(vararg pathBytes: Byte): ExtensionNodeBuilder =
    ExtensionNodeBuilder(NibbleArray.fromBytes(pathBytes))
fun extensionNode(vararg nibbles: Byte): ExtensionNodeBuilder =
    ExtensionNodeBuilder(NibbleArray(nibbles))

fun branchNode(vararg branches: Pair<Int, Node>): BranchNodeBuilder =
    BranchNodeBuilder(branches.toList())