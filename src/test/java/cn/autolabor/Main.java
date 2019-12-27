package cn.autolabor;

import Jama.Matrix;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_8U;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Main {
    private static Mat qrBinary;

    public static void main(String[] args) {
        Mat src = imread("C:\\Users\\user\\Pictures\\Camera Roll\\WIN_20191224_17_54_25_Pro.jpg");

        QRRecognize(src);
    }

    public static void QRRecognize(Mat src) {
        long startTime = System.currentTimeMillis();    //获取开始时间
        Mat qrGray = new Mat();
        cvtColor(src, qrGray, COLOR_BGR2GRAY);
        qrBinary = new Mat(qrGray.size(), CV_8U);
        threshold(qrGray, qrBinary, 50, 255, THRESH_OTSU | THRESH_BINARY);
        MatVector list = new MatVector();
        Mat hierarchy = new Mat();
        findContours(qrBinary, list, hierarchy, RETR_TREE, CHAIN_APPROX_NONE);
        Mat qrContour = new Mat(qrBinary.size(), qrBinary.type());
        List<Moments> mu = new ArrayList<>();
        List<double[]> mc = new ArrayList<>();  //求各轮廓中心
        for (int i = 0; i < list.size(); i++) {
            mu.add(moments(list.get(i)));
            double[] center = new double[]{mu.get(i).m10() / mu.get(i).m00(), mu.get(i).m10() / mu.get(i).m00()};
            mc.add(center);
        }
        List<int[]> blockConfid = new ArrayList<>();
        //根据特征块特征，获取所有特征块索引 和 二维码边框索引
        for (int k = 0; k < list.size(); k++) {
            double area2 = contourArea(list.get(k));
            if (area2 <= 50 || area2 >= Math.pow(Math.min(src.rows(), src.cols()), 2) * 25.0 / 9.0 / 49.0) {
                continue;
            }
            drawContours(qrContour, list, k, new Scalar(255, 255, 255, 0), 1, LINE_AA, hierarchy, Integer.MAX_VALUE, new Point());
            BytePointer ds = hierarchy.ptr(0, k);
            if (ds.get(2) == -1 || ds.get(3) == -1) {
                continue;
            }
            Matrix centroid = new Matrix(new double[][]{{mc.get(k)[0], mc.get(k)[1], 1}, {mc.get(ds.get(3))[0], mc.get(ds.get(3))[1], 1}, {mc.get(ds.get(3))[0], mc.get(ds.get(3))[1], 1}});
            int[] blockLabel = new int[4];
            double area1 = contourArea(list.get((int) ds.get(3)));
            double area3 = contourArea(list.get((int) ds.get(2)));
            double ratio1 = area1 / area2;
            double ratio2 = area2 / area3;
            if (ratio1 <= 64.0 / 25.0 && ratio1 >= 34.0 / 25.0 && ratio2 >= 16.0 / 9.0 && ratio2 <= 34.0 / 9.0 && Math.abs(centroid.det()) <= 1) {
                blockLabel[1] = k;
                blockLabel[2] = ds.get(3);
                blockLabel[0] = ds.get(2);
                BytePointer dsOut = hierarchy.ptr(0, blockLabel[2]);
                blockLabel[3] = dsOut.get(3);
                blockConfid.add(blockLabel);
            }

        }
        imshow(" ", qrContour);
        waitKey(0);
        imwrite("C:\\Users\\user\\Documents\\GitHub\\picocode\\src\\test\\java\\cn\\autolabor\\contour.jpg", qrContour);
    }
}
