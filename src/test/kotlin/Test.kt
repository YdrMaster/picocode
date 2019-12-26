fun main() {
    PicoZense.use {
        it.runCatching {
            initialize()
            val camera = this[0]
            println("type: ${camera.deviceType}")
        }
    }.getOrElse { it.printStackTrace() }
}
