package cn.autolabor

import org.bytedeco.opencv.opencv_core.Mat

fun parseHierarchy(hierarchy: Mat): IndexTree {
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
    // 组织兄弟结构
    val struct = indices.groupBy(parents::get)
    TODO()
}
