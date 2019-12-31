package cn.autolabor

import cn.autolabor.PicoZense.PicoCamera.RGBResolution.R1280_720
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import org.bytedeco.opencv.opencv_core.Mat

@ObsoleteCoroutinesApi
fun main() = runBlocking<Unit>(Dispatchers.Default) {
    require(PicoZense.deviceCount > 0) { "Please plugin the camera." }
    // 等待 Pico 初始化
    val waiting = launch {
        print("Pico initializing")
        while (true) {
            print('.')
            delay(800L)
        }
    }
    // 处理
    PicoZense.use { zense ->
        zense.runCatching {
            // 打开相机设备
            open {
                rgbResolution = R1280_720
            }.use { camera ->
                // 处理图像
                actor<Mat>(capacity = 1) {
                    for (rgb in this) process(rgb)
                }.apply {
                    waiting.cancelAndJoin()
                    println()
                    println("Camera opened, type: ${camera.deviceType}")
                    while (true) camera.nextRgb()?.let { send(it) }
                }
            }
        }.onFailure { it.printStackTrace() }
    }
}
