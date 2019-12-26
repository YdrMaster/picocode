fun main() {
    PicoZense.use {
        it.runCatching {
            initialize()
            val (name, code) = this[0]
            val handler = open("$name:$code")
            start(handler)
            stop(handler)
            close()
            println("$name:$code")
        }
    }.getOrElse { it.printStackTrace() }
}
