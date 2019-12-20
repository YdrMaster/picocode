import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
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

    override fun close() {
        native.Ps2_Shutdown()
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
        fun Ps2_GetDeviceInfo(pDevices: Pointer, deviceIndex: Int): Int

        class PsDeviceInfo : Structure() {
            var SessionCount: Int = -1
            var devicetype: Int = -1
            var uri = ByteArray(256) { -1 }
            var fw = ByteArray(50) { -1 }
            var status: Int = -1
        }

        // 打开设备
        // pDevice for PsDeviceHandle*
        fun Ps2_OpenDevice(uri: String, pDevice: Pointer): Int

        // 关闭设备
        // device for device handler
        fun Ps2_CloseDevice(device: Pointer): Int
    }
}
