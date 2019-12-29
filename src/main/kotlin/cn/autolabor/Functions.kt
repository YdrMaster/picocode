package cn.autolabor

import cn.autolabor.ValuedTree.Companion.build
import org.bytedeco.opencv.opencv_core.Mat

// 从四向链表构造树
internal fun buildTree(hierarchy: Mat): List<ValuedTree<Int>> {
    // 获取序号序列
    val indices = 0 until hierarchy.cols()
    // 获取亲代序号
    val parents = indices.map { c ->
        // 0: next brother
        // 4: previous brother
        // 8: first child
        // 12: parent
        hierarchy.ptr(0, c).getInt(12)
    }
    val groups = indices.groupBy(parents::get)
    // 组织兄弟结构
    return groups[-1]?.map { build(it, groups) } ?: emptyList()
}
