import org.bytedeco.opencv.global.opencv_highgui.imshow
import org.bytedeco.opencv.global.opencv_highgui.waitKey
import org.bytedeco.opencv.global.opencv_imgcodecs.imread

fun main() {
    val jpg = imread("""C:\Users\User\Desktop\Lenna.jpg""")
    imshow("Lenna", jpg)
    waitKey()
}
