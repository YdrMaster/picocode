package cn.autolabor

sealed class MarkTree<T> {
    abstract val index: T

    data class Leaf<T>(override val index: T) : MarkTree<T>()
    class Branch<T>(override val index: T, children: List<MarkTree<T>>) : MarkTree<T>()

    companion object {
        // 构建序号树
        fun <T> build(
            root: T,
            struct: Map<T, List<T>>
        ): MarkTree<T> =
            struct[root]
                ?.map { build(it, struct) }
                ?.let { Branch(root, it) }
            ?: Leaf(root)
    }
}
