package cn.autolabor

import cn.autolabor.ValuedTree.Branch
import cn.autolabor.ValuedTree.Companion.build
import cn.autolabor.ValuedTree.Companion.tree
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_highgui.imshow
import org.bytedeco.opencv.global.opencv_highgui.waitKey
import org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Scalar
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.implement.vector.Vector2D
import org.mechdancer.algebra.implement.vector.vector2DOf
import kotlin.math.abs

private const val minCount0 = (7 * 3 * 2 - 1) * 4
private const val minCount1 = (7 * 2 - 1) * 4
private const val minCount2 = (5 * 2 - 1) * 4
private const val minCount3 = (3 * 2 - 1) * 4

private const val ratio0 = minCount0.toDouble() / minCount1
private const val ratio1 = minCount1.toDouble() / minCount2
private const val ratio2 = minCount2.toDouble() / minCount3

private const val tolerance = .7
private val range0 = ratio0 * tolerance..ratio0 / tolerance
private val range1 = ratio1 * tolerance..ratio1 / tolerance
private val range2 = ratio2 * tolerance..ratio2 / tolerance

private val COLOR_R = Scalar(.0, .0, 255.0, .0)
private val COLOR_G = Scalar(255.0, .0, .0, .0)
private val COLOR_B = Scalar(.0, 255.0, .0, .0)

internal fun process(mat: Mat) {
    // TODO 需要进一步预处理，滤波？
    // 找轮廓
    val contours = MatVector()
    val hierarchy = Mat()
    findContours(binary(mat), contours, hierarchy, RETR_TREE, CHAIN_APPROX_NONE)
    // 筛选轮廓
    val begin = System.nanoTime()
    val candidates =
        buildForest(hierarchy)
            // 展平
            .asSequence()
            .flatMap { it.map { i -> contours.get(i.toLong()) }.flattenAsSequence() }
            .ofType<Branch<Mat>>()
            // 外框轮廓特征
            .filter { (border, _) -> border.rows() >= minCount0 }
            // 定位点轮廓特征
            .mapNotNull { (border, outers) ->
                // 外环特征
                outers
                    .asTypedSequence<Branch<Mat>>()
                    .filter { it.checkRation(border, range0) }
                    .mapNotNull { (outer, mediums) ->
                        // 中环特征
                        mediums
                            .asTypedSequence<Branch<Mat>>()
                            .singleOrNull { it.checkRation(outer, range1) }
                            ?.let { (medium, inners) ->
                                // 内环特征
                                inners
                                    .singleOrNull { it.checkRation(medium, range2) }
                                    ?.value
                                    ?.let { tree(outer, tree(medium, tree(it))) }
                            }
                    }
                    .takeIf { it.size >= 3 }
                    ?.let { tree(border, it) }
            }
            .toList()
    println(1E9 / (System.nanoTime() - begin))
    candidates
        .asSequence()
        .flatMap { it.flattenAsSequence() }
        .forEach { drawContours(mat, MatVector(it.value), -1, COLOR_R, 2, 0, Mat(), Int.MAX_VALUE, Point()) }
    candidates
        .forEach { rectangleOf(it.value) }
    mat.show()
}

private var last = 0L
private fun Mat.show(title: String = "test") {
    val now = System.currentTimeMillis()
    if (now - last > 1000) {
        last = now
        imwrite("test/$now.jpg", this)
    }
    imshow(title, this)
    waitKey()
}

private val hsvBlackL = Mat(0.0, 0.0, 144.0)
private val hsvBlackH = Mat(180.0, 255.0, 255.0)

// 二值化
private fun binary(src: Mat) =
    Mat().also { dst ->
        val hsv = Mat()
        cvtColor(src, hsv, COLOR_BGR2HSV)
        opencv_core.inRange(hsv, hsvBlackL, hsvBlackH, dst)
        dst.show("binary")
    }

// 从四向链表构造树
private fun buildForest(hierarchy: Mat): List<ValuedTree<Int>> {
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

// 检查相对上一级轮廓的比例
private fun ValuedTree<Mat>.checkRation(
    parent: Mat,
    range: ClosedFloatingPointRange<Double>
) =
    parent.rows().toDouble() / this.value.rows() in range

private inline fun <reified U : Any>
    Sequence<*>.ofType() =
    mapNotNull { it as? U }

private inline fun <reified U : Any>
    Iterable<*>.asTypedSequence() =
    mapNotNull { it as? U }

// 向量叉乘，用于求平行四边形面积
private infix fun Vector2D.cross(others: Vector2D): Double {
    val (x0, y0) = this
    val (x1, y1) = others
    return x0 * y1 - x1 * y0
}

// 根据一条对角线求另一条对角线
private fun List<Vector2D>.maxByArea(a: Int, c: Int): Pair<Int, Int> {
    val b = (a + 1 until c)
        .maxBy { b -> abs((this[a] - this[b]) cross (this[c] - this[b])) }!!
    val d = ((0 until a) + (c + 1 until size))
        .maxBy { d -> abs((this[a] - this[d]) cross (this[c] - this[d])) }!!
    return b to d
}

// 针对轮廓的方形检测
private fun rectangleOf(contour: Mat) {
    // 规范化点序列
    val points =
        (0 until contour.rows()).map { i ->
            val ptr = contour.ptr(i, 0)
            val x = ptr.getInt(0)
            val y = ptr.getInt(4)
            vector2DOf(x, y)
        }
    // 找到四个角点
    val (a, c) = intArrayOf(0, 0).apply {
        sequence {
            while (true) {
                yield(0 to 1)
                yield(1 to 0)
            }
        }.first { (a, b) ->
            null == points
                .indices
                .maxBy { points[it] euclid points[this[a]] }!!
                .takeIf { this[b] != it }
                ?.let { this[b] = it }
        }
        sort()
    }
    val (b, d) = points.maxByArea(a, c)
    println(listOf(a, b, c, d).sorted())
}
