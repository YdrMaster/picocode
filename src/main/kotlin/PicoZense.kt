import PicoZense.NativeFunctions.PsDeviceInfo
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import java.io.Closeable
import java.lang.reflect.Proxy

object PicoZense : Closeable {
    private const val OK = 0

    private val native =
        with(NativeFunctions::class.java) {
            cast(Proxy.newProxyInstance(
                classLoader,
                arrayOf(this),
                Library.Handler("picozense_api",
                                this,
                                emptyMap<String, NativeFunctions>())))
        }

    fun initialize(): Int {
        require(OK == native.Ps2_Initialize()) { "initialize failed" }
        val count = IntByReference()
        require(OK == native.Ps2_GetDeviceCount(count.pointer)) { "get devices count failed" }
        require(count.value > 0) { "no device" }
        return count.value
    }

    operator fun get(index: Int): PicoCamera {
        val info = PsDeviceInfo()
        require(OK == native.Ps2_GetDeviceInfo(info, index))
        val uri = info.uri.takeWhile { it != 0.toByte() }.toByteArray().toString(Charsets.US_ASCII)

        val handler = LongByReference()
        require(OK == native.Ps2_OpenDevice(uri, handler.pointer))
        return PicoCamera(uri.split(':').first(), handler.value)
    }

    override fun close() {
        native.Ps2_Shutdown()
    }

    /** 相机资源 */
    class PicoCamera(
        val deviceType: String,
        private val handler: Long
    ) : Closeable {
        init {
            require(OK == native.Ps2_StartStream(handler, 0))
        }

        override fun close() {
            native.Ps2_StopStream(handler, 0)
            native.Ps2_CloseDevice(handler)
        }
    }

    @Suppress("FunctionName")
    private interface NativeFunctions : Library {
        // 创建环境
        fun Ps2_Initialize(): Int

        // 释放环境
        fun Ps2_Shutdown(): Int

        // 获取设备数量
        // pDeviceCount for int*
        fun Ps2_GetDeviceCount(pDeviceCount: Pointer): Int

        // 获取设备信息
        // pDevices for PsDeviceInfo*
        fun Ps2_GetDeviceInfo(pDevices: PsDeviceInfo, deviceIndex: Int): Int

        @FieldOrder("SessionCount", "devicetype", "uri", "fw", "status")
        class PsDeviceInfo : Structure() {
            @JvmField
            var SessionCount: Int = -1
            @JvmField
            var devicetype: Int = -1
            @JvmField
            var uri = ByteArray(256) { -1 }
            @JvmField
            var fw = ByteArray(50) { -1 }
            @JvmField
            var status: Int = 0x0f

            private fun uriPair() =
                uri.takeWhile { it != 0.toByte() }
                    .toByteArray()
                    .toString(Charsets.US_ASCII)
                    .split(':')
                    .let { (name, code) -> name to code }

            override fun toString() =
                "$SessionCount, $devicetype, $status, ${uriPair()}"
        }

        // 打开设备
        // pDevice for PsDeviceHandle*
        fun Ps2_OpenDevice(uri: String, pDevice: Pointer): Int

        // 关闭设备
        // device for device handler
        fun Ps2_CloseDevice(device: Long): Int

        // 启动数据流
        // device for device handler
        fun Ps2_StartStream(device: Long, sessionIndex: Int): Int

        // 停止数据流
        // device for device handler
        fun Ps2_StopStream(device: Long, sessionIndex: Int): Int
    }
}
