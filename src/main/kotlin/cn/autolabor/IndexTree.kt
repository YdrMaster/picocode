package cn.autolabor

sealed class IndexTree {
    abstract val index: Int

    data class Leaf(override val index: Int) : IndexTree()
    class Branch(override val index: Int, children: List<IndexTree>) : IndexTree()
}
