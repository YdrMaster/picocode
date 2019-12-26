import org.bytedeco.opencv.global.opencv_highgui.imshow
import org.bytedeco.opencv.global.opencv_highgui.waitKey

fun main() {
    PicoZense.use { zense ->
        zense.runCatching {
            initialize()
            this[0].use { camera ->
                println("type: ${camera.deviceType}")
                while (true) {
                    camera.next()?.let { imshow("pico", it) }
                    waitKey(1)
                }
            }
        }
    }.getOrElse { it.printStackTrace() }
}
