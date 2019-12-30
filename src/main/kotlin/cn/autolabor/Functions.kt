package cn.autolabor

import cn.autolabor.ValuedTree.Branch
import cn.autolabor.ValuedTree.Companion.build
import cn.autolabor.ValuedTree.Leaf
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_highgui.imshow
import org.bytedeco.opencv.global.opencv_highgui.waitKey
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.global.opencv_imgproc.drawContours
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.Scalar

private const val minCount0 = (7 * 3 * 2 - 1) * 4
private const val minCount1 = (7 * 2 - 1) * 4
private const val minCount2 = (5 * 2 - 1) * 4
private const val minCount3 = (3 * 2 - 1) * 4

private const val ratio0 = minCount0.toDouble() / minCount1
private const val ratio1 = minCount1.toDouble() / minCount2
private const val ratio2 = minCount2.toDouble() / minCount3

private val range0 = ratio0 * .8..ratio0 / .8
private val range1 = ratio1 * .8..ratio1 / .8
private val range2 = ratio2 * .8..ratio2 / .8

private val COLOR_R = Scalar(.0, .0, 255.0, .0)
private val COLOR_G = Scalar(255.0, .0, .0, .0)
private val COLOR_B = Scalar(.0, 255.0, .0, .0)

internal fun process(mat: Mat) {
    // TODO 需要进一步预处理，滤波？
    // 找轮廓
    val contours = MatVector()
    val hierarchy = Mat()
    opencv_imgproc.findContours(
        binary(mat),
        contours,
        hierarchy,
        opencv_imgproc.RETR_TREE,
        opencv_imgproc.CHAIN_APPROX_NONE)

    val candidates =
        buildForest(hierarchy).asSequence()
            // 展平
            .flatMap { it.map { i -> contours.get(i.toLong()) }.flattenAsSequence() }
            .ofType<Branch<Mat>>()
            // 外框轮廓特征
            .filter { (border, _) -> border.rows() >= minCount0 }
            // 定位点轮廓特征
            .mapNotNull { (border, outers) ->
                outers
                    .asTypedSequence<Branch<Mat>>()
                    .filter { (outer, _) -> border.rows().toDouble() / outer.rows() in range0 }
                    .mapNotNull { (outer, mediums) ->
                        mediums
                            .asTypedSequence<Branch<Mat>>()
                            .singleOrNull { outer.rows().toDouble() / it.value.rows() in range1 }
                            ?.let { (medium, inners) ->
                                inners
                                    .singleOrNull { medium.rows().toDouble() / it.value.rows() in range2 }
                                    ?.let { Branch(medium, listOf(Leaf(it.value))) }
                            }
                            ?.let { Branch(outer, listOf(it)) }
                    }
                    .takeIf { it.size >= 3 }
                    ?.let { Branch(border, it) }
            }
            .toList()
    candidates.asSequence().flatMap { it.flattenAsSequence() }
        .forEach { drawContours(mat, MatVector(it.value), -1, COLOR_R) }
    testShow(mat)
}

private fun testShow(mat: Mat) {
    imshow("test", mat)
//  imwrite("test.bmp", mat);
    waitKey()
}

private val hsvBlackL = Mat(0.0, 0.0, 144.0)
private val hsvBlackH = Mat(180.0, 255.0, 255.0)

// 二值化
private fun binary(src: Mat) =
    Mat().also { dst ->
        val hsv = Mat()
        opencv_imgproc.cvtColor(src, hsv, opencv_imgproc.COLOR_BGR2HSV)
        opencv_core.inRange(hsv, hsvBlackL, hsvBlackH, dst)
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

private inline fun <reified U : Any>
    Sequence<*>.ofType() =
    mapNotNull { it as? U }

private inline fun <reified U : Any>
    Iterable<*>.asTypedSequence() =
    mapNotNull { it as? U }
