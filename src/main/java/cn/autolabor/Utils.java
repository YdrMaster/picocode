package cn.autolabor;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2d;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static cn.autolabor.PicoProcess.testShow;
import static org.bytedeco.opencv.global.opencv_core.inRange;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

class Utils {
    private final static Mat hsvBlackL = new Mat(new double[]{0, 0, 144});
    private final static Mat hsvBlackH = new Mat(new double[]{180, 255, 255});

    static int[][] parseHierarchy(Mat hierarchy) {
        int[][] result = new int[hierarchy.cols()][4];
        for (int i = 0; i < hierarchy.cols(); ++i) {
            BytePointer data = hierarchy.ptr(0, i);
            for (int k = 0; k < 4; ++k)
                result[i][k] = data.getInt(k * 4);
        }
        return result;
    }

    // 二值化
    @NotNull
    static Mat binary(@NotNull Mat src) {
        // 原版 先转灰度
        // Mat gray = new Mat();
        // Mat dst = new Mat();
        // cvtColor(src, gray, COLOR_BGR2GRAY);
        // threshold(gray, dst, 50, 255, THRESH_OTSU | THRESH_BINARY);
        // 直接在 HSV 色域阈值化
        // 效果不是很一样，也许是阈值选择的问题，灰度图更锐利，有少量噪点，hsv 会产生圆角，噪点少
        Mat hsv = new Mat();
        Mat dst = new Mat();
        cvtColor(src, hsv, COLOR_BGR2HSV);
        inRange(hsv, hsvBlackL, hsvBlackH, dst);
        testShow(hsv);
        testShow(dst);
        return dst;
    }

    // MatVector 转 Stream
    @NotNull
    static Stream<Mat> toStream(@NotNull MatVector mats) {
        return LongStream.range(0, mats.size()).mapToObj(mats::get);
    }

    // 求轮廓中心
    @NotNull
    static List<Point2d> center(@NotNull MatVector contours) {
        return toStream(contours)
            .map(opencv_imgproc::moments)
            .map(m -> new Point2d(m.m10() / m.m00(), m.m01() / m.m00()))
            .collect(Collectors.toList());
    }
}
