fun main() {
    PicoZense.use {
        it.runCatching {
            initialize()
        }
    }.getOrElse { System.err.println(it.message) }
}
