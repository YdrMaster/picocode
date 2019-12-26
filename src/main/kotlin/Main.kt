import org.bytedeco.opencv.global.opencv_core.CV_8UC3
import org.bytedeco.opencv.opencv_core.Mat
import java.util.*
import kotlin.random.Random

fun main() {
//    val jpg = imread("""C:\Users\User\Desktop\Lenna.jpg""")
    val mat = Mat(640, 360, CV_8UC3)
    val buffer = Random.nextBytes(mat.rows() * mat.cols() * 3)
    mat.data().put(*buffer)
    mat.data().get(buffer)
    Arrays.toString(buffer).let(::println)
}
