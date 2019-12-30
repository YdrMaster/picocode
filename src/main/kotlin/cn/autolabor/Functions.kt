package cn.autolabor

import cn.autolabor.ValuedTree.Branch
import cn.autolabor.ValuedTree.Companion.build
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
    buildForest(hierarchy)
        .asSequence()
        .flatMap {
            it.map { i -> contours.get(i.toLong()) }
                .flattenAsSequence()
        }
        // 外框结构特征
        .ofType<Branch<Mat>>()
        // 外框轮廓特征
        .filter { (border, _) -> border.rows() >= minCount0 }
        // 外环轮廓特征
        .mapNotNull { (border, children) ->
            children
                .ofType<Branch<Mat>>()
                .mapNotNull { child -> child as? Branch }
                .filter { child ->
                    val ratio = border.rows().toDouble() / child.value.rows()
                    ratio in range0
                }
                .takeIf { it.size >= 3 }
                ?.let { Branch(border, it) }
        }
        // 画图
        .onEach { (border, children) ->
            drawContours(mat, MatVector(border), -1, COLOR_R)
            for (outer in children)
                drawContours(mat, MatVector(outer.value), -1, COLOR_G)
        }
        .count()
        .let(::println)
    testShow(mat)
}

private fun testShow(mat: Mat) {
    imshow("test", mat)
//        imwrite("test.bmp", mat);
    waitKey()
}

private val hsvBlackL = Mat(0.0, 0.0, 144.0)
private val hsvBlackH = Mat(180.0, 255.0, 255.0)

// 二值化
private fun binary(src: Mat): Mat { // 原版 先转灰度
    // Mat gray = new Mat();
    // Mat dst = new Mat();
    // cvtColor(src, gray, COLOR_BGR2GRAY);
    // threshold(gray, dst, 50, 255, THRESH_OTSU | THRESH_BINARY);
    // 直接在 HSV 色域阈值化
    // 效果不是很一样，也许是阈值选择的问题，灰度图更锐利，有少量噪点，hsv 会产生圆角，噪点少
    val hsv = Mat()
    val dst = Mat()
    opencv_imgproc.cvtColor(src, hsv, opencv_imgproc.COLOR_BGR2HSV)
    opencv_core.inRange(hsv, hsvBlackL, hsvBlackH, dst)
    PicoProcess.testShow(hsv)
    PicoProcess.testShow(dst)
    return dst
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
    Iterable<*>.ofType() =
    mapNotNull { it as? U }
