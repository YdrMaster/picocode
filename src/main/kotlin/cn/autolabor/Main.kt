package cn.autolabor

import cn.autolabor.PicoZense.PicoCamera.RGBResolution.R1920_1080
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import org.bytedeco.opencv.opencv_core.Mat

@ObsoleteCoroutinesApi
fun main() = runBlocking<Unit>(Dispatchers.Default) {
    // 等待 Pico 初始化
    val waiting = launch {
        print("pico initializing")
        while (true) {
            print('.')
            delay(800L)
        }
    }
    // 等待设备上线
    while (PicoZense.deviceCount <= 0)
        delay(1000L)
    // 处理
    PicoZense.use { zense ->
        zense.runCatching {
            // 打开相机设备
            open {
                rgbResolution = R1920_1080
            }.use { camera ->
                waiting.cancelAndJoin()
                println()
                println("type: ${camera.deviceType}")
                // 处理图像
                actor<Mat>(capacity = 1) {
                    for (rgb in this) PicoProcess.process(rgb)
                }.apply {
                    while (true) camera.nextRgb()?.let { send(it) }
                }
            }
        }.onFailure { it.printStackTrace() }
    }
}
