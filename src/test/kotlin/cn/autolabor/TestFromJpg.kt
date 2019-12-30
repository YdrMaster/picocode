package cn.autolabor

import org.bytedeco.opencv.global.opencv_imgcodecs

fun main() {
  process(opencv_imgcodecs.imread("test_qr_picture.jpg"))
//    PicoProcess.test()
}
