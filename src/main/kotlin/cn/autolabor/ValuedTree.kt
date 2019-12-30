package cn.autolabor

/** 所有节点带 [T] 类型值的多叉树 */
sealed class ValuedTree<T> {
    abstract val value: T
    abstract fun <U> map(block: (T) -> U): ValuedTree<U>
    abstract fun flattenAsSequence(): Sequence<ValuedTree<T>>

    data class Leaf<T>(
        override val value: T
    ) : ValuedTree<T>() {
        override fun <U> map(block: (T) -> U) =
            Leaf(block(value))

        override fun flattenAsSequence() =
            sequenceOf(this)
    }

    data class Branch<T>(
        override val value: T,
        val children: List<ValuedTree<T>>
    ) : ValuedTree<T>() {
        override fun <U> map(block: (T) -> U) =
            Branch(block(value), children.map { it.map(block) })

        override fun flattenAsSequence() =
            sequence {
                yield(this@Branch)
                for (child in children)
                    yieldAll(children.asSequence())
            }
    }

    companion object {
        // 构建带值多叉树
        fun <T> build(
            root: T,
            struct: Map<T, List<T>>
        ): ValuedTree<T> =
            struct[root]
                ?.map { build(it, struct) }
                ?.let { Branch(root, it) }
            ?: Leaf(root)
    }
}
