import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure

class PicoZense {
    @Suppress("FunctionName")
    private interface NativeFunctions : Library {
        // 创建环境
        fun Ps2_Initialize(): Int

        // 释放环境
        fun Ps2_Shutdown(): Int

        // 获取设备数量
        // pointer for int reference
        fun Ps2_GetDeviceCount(pointer: Pointer): Int
    }
}
