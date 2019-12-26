fun main() {
    PicoZense.use {
        it.runCatching {
            initialize()
            this[0].use { camera ->
                println("type: ${camera.deviceType}")
                while (true) camera.next()
            }
        }
    }.getOrElse { it.printStackTrace() }
}
