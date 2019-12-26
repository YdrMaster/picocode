import java.io.EOFException
import java.io.InputStream

internal fun InputStream.readU16LE(): Int {
    val b1: Int = read()
    val b2: Int = read()
    if (b1 or b2 < 0) throw EOFException()
    return (b2 shl 8) or b1
}

internal fun InputStream.readU32LE() =
    readI32LE().toLong() and 0xff_ff_ff_ff

internal fun InputStream.readI16LE() =
    readU16LE().toShort()

internal fun InputStream.readI32LE(): Int {
    val b1: Int = read()
    val b2: Int = read()
    val b3: Int = read()
    val b4: Int = read()
    if (b1 or b2 or b3 or b4 < 0) throw EOFException()
    return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
}
