package cn.autolabor;

import Jama.Matrix;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.bytedeco.opencv.opencv_core.*;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.CV_8U;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


public class PicoProcess {
    private static Mat qrBinary;

    public static void test() {
        Mat src = imread("C:\\Users\\user\\Documents\\GitHub\\picocode\\pic\\writer1577435455394.jpg");
        QRRecognize(src);
        waitKey();
    }

    public static void process(@NotNull Mat src) {


        imshow(" ", src);
        waitKey(2);

    }

    public static void QRRecognize(Mat src) {
        long startTime = System.currentTimeMillis();    //获取开始时间
        Mat qrGray = new Mat();
        cvtColor(src, qrGray, COLOR_BGR2GRAY);
        qrBinary = new Mat(qrGray.size(), CV_8U);
        threshold(qrGray, qrBinary, 50, 255, THRESH_OTSU | THRESH_BINARY);
        imwrite("qrBinary.jpg", qrBinary);
        MatVector list = new MatVector();
        Mat hierarchy = new Mat();
        findContours(qrBinary, list, hierarchy, RETR_TREE, CHAIN_APPROX_NONE);
        int[][] tree = Utils.parseHierarchy(hierarchy);
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
            if (area2 <= 50 || area2 >= Math.pow(Math.min(src.rows(), src.cols()), 2) * 30.0 / 9.0 / 49.0) {
                continue;
            }
            drawContours(qrContour, list, k, new Scalar(255, 255, 255, 0), 1, LINE_AA, hierarchy, Integer.MAX_VALUE, new Point());

            if (tree[k][2] == -1 || tree[k][3] == -1) {
                continue;
            }
            Matrix centroid = new Matrix(new double[][]{{mc.get(k)[0], mc.get(k)[1], 1}, {mc.get(tree[k][3])[0], mc.get(tree[k][3])[1], 1}, {mc.get(tree[k][3])[0], mc.get(tree[k][3])[1], 1}});
            int[] blockLabel = new int[4];
            double area1 = contourArea(list.get(tree[k][3]));
            double area3 = contourArea(list.get(tree[k][2]));
            double ratio1 = area1 / area2;
            double ratio2 = area2 / area3;

            if (ratio1 <= 64.0 / 25.0 && ratio1 >= 34.0 / 25.0 && ratio2 >= 16.0 / 9.0 && ratio2 <= 34.0 / 9.0 && Math.abs(centroid.det()) <= 1) {
                blockLabel[1] = k;
                blockLabel[2] = tree[k][3];
                blockLabel[0] = tree[k][2];
                int[] dsOut = tree[blockLabel[2]];
                blockLabel[3] = dsOut[3];
                blockConfid.add(blockLabel);
            }

        }


        if (blockConfid.size() <= 1) {
            System.out.println("Without QR");
        }
        // 收集所有二维码边框，获得图中二维码数量
        int outLabelTemp = -1;
        List<Integer> outLabel = new ArrayList<>();
        for (int[] ints : blockConfid) {
            int outLabelNow = ints[3];
            if (outLabelNow != outLabelTemp) {
                outLabel.add(outLabelNow);//outLabel外框的边缘的索引，size代表一共有多少个qr
                outLabelTemp = outLabelNow;
            }
        }
        // 所有特征块索引归类
        int[][] outAll = new int[outLabel.size()][4];
        List<Mat> qrCode = new ArrayList<>();
        for (int i = 0; i < outLabel.size(); i++) {
            outAll[i][0] = outLabel.get(i);
        }
        //归类
        int[] num = new int[outLabel.size()];
        for (int[] ints : blockConfid) {
            for (int i1 = 0; i1 < outLabel.size(); i1++) {
                if (ints[3] == outLabel.get(i1)) {
                    outAll[i1][num[i1] + 1] = ints[2];
                    num[i1]++;
                    break;
                }
            }
        }
        // 画出二维码外边框 和 特征块最外层边框
        int num1 = 0;
        for (int[] ints : outAll) {//getQuaCorner是得到外框四角坐标，perspectiveTran是二维码的透视校正，deCode是二维码解码
            Random ble = new Random();
            Random grn = new Random();
            Random rd = new Random();
            System.out.println("QR!");
            Scalar color = new Scalar(255, 0, 0, 0);
            for (int i = 1; i < 4; i++) {
                drawContours(src, list, ints[i], color, 1, LINE_AA, hierarchy, Integer.MAX_VALUE, new Point());
            }

            double area4 = contourArea(list.get(ints[0]));
            double area31 = contourArea(list.get(ints[1]));
            double area32 = contourArea(list.get(ints[2]));
            double area33 = contourArea(list.get(ints[3]));
//            if (area4 > 15.0 * Math.max(Math.max(area31, area32), area33)) {
//                continue;
//            }
            Point[] qrQuaCor = getQuaCorner(list.get(ints[0]));
            for (int i = 0; i < qrQuaCor.length; i++) {
                System.out.println(qrQuaCor[i].x() + " " + qrQuaCor[i].y());
            }
//            qrCode.add(perspectiveTran(qrQuaCor));//
//            String qrInfo = deCode(qrCode.get(num1));
//            num1++;
            assert qrQuaCor != null;
            line(src, qrQuaCor[0], qrQuaCor[1], color, 4, LINE_AA, 0);
            line(src, qrQuaCor[1], qrQuaCor[3], color, 4, LINE_AA, 0);
            line(src, qrQuaCor[3], qrQuaCor[2], color, 4, LINE_AA, 0);
            line(src, qrQuaCor[2], qrQuaCor[0], color, 4, LINE_AA, 0);
//            Imgproc.drawContours(src, list, ints[0], color, 4, Imgproc.LINE_AA);

//            if (qrInfo != null) {
//
//                putText(src, qrInfo, qrQuaCor[0], FONT_HERSHEY_SIMPLEX, 1.0, color, 1, LINE_AA, false);
//            } else System.out.println("二维码数据失真");
        }
        long endTime = System.currentTimeMillis();    //获取结束时间
        System.out.println("RunTime：" + (endTime - startTime) + "ms");    //输出程序运行时间
        imshow("QRRecognize", src);
        imshow("QRContours", qrContour);
        waitKey(2);
        imwrite("D:\\Users\\user\\OpenCV_test\\src\\QR\\QRDetect\\qrNew.jpg", src);
    }

    public static Point[] getQuaCorner(Mat pointOfContour) {
        /*
        p0    p2
        |   / |
        |  /  |
        | /   |
        p1    p3
         */

        if (pointOfContour.rows() < 4) {
            return null;
        }
        int[][] corPoint = new int[4][2];
        double len13 = 0.0;
        int itemp = 0;
        int jtemp = 0;
        for (int i = 0; i < pointOfContour.rows() / 2 + 1; i++) {
            for (int j = pointOfContour.rows() / 2; j < pointOfContour.rows() - 1; j++) {
                double lentemp = Math.sqrt(Math.pow(pointOfContour.ptr(i, 0).getInt(0) - pointOfContour.ptr(j, 0).getInt(0), 2) +
                        Math.pow(pointOfContour.ptr(i, 0).getInt(4) - pointOfContour.ptr(j, 0).getInt(4), 2));
                if (lentemp > len13) {
                    len13 = lentemp;
                    itemp = i;
                    jtemp = j;
                }

            }

        }
        corPoint[0][0] = pointOfContour.ptr(itemp, 0).getInt(0);
        corPoint[0][1] = pointOfContour.ptr(itemp, 0).getInt(4);
        corPoint[2][0] = pointOfContour.ptr(jtemp, 0).getInt(0);
        corPoint[2][1] = pointOfContour.ptr(jtemp, 0).getInt(4);


        double area123 = 0.0;
        int k2temp = 0;
        int k22;
        for (int k2 = jtemp; k2 < itemp + pointOfContour.rows(); k2++) {

            if (k2 >= pointOfContour.rows()) {
                k22 = k2 - pointOfContour.rows();
            } else {
                k22 = k2;
            }
            corPoint[1][0] = pointOfContour.ptr(k22, 0).getInt(0);
            corPoint[1][1] = pointOfContour.ptr(k22, 0).getInt(4);


            double area123temp = Math.abs(((corPoint[0][0] - corPoint[1][0]) * (corPoint[2][1] - corPoint[1][1]) - (corPoint[2][0] - corPoint[1][0]) * (corPoint[0][1] - corPoint[1][1])));
            if (area123temp > area123) {
                area123 = area123temp;
                k2temp = k22;
            }
        }
        corPoint[1][0] = pointOfContour.ptr(k2temp, 0).getInt(0);
        corPoint[1][1] = pointOfContour.ptr(k2temp, 0).getInt(4);
        double area143 = 0.0;
        int k4temp = 0;
        int k44;
        for (int k4 = itemp; k4 < jtemp; k4++) {
            k44 = k4;

            corPoint[3][0] = pointOfContour.ptr(k44, 0).getInt(0);
            corPoint[3][1] = pointOfContour.ptr(k44, 0).getInt(4);
            double area143temp = Math.abs(((corPoint[0][0] - corPoint[3][0]) * (corPoint[2][1] - corPoint[3][1]) - (corPoint[2][0] - corPoint[3][0]) * (corPoint[0][1] - corPoint[3][1])));
            if (area143temp > area143) {
                area143 = area143temp;
                k4temp = k44;
            }
        }
        corPoint[3][0] = pointOfContour.ptr(k4temp, 0).getInt(0);
        corPoint[3][1] = pointOfContour.ptr(k4temp, 0).getInt(4);


        java.util.Arrays.sort(corPoint, Comparator.comparingInt(o -> o[0]));
        java.util.Arrays.sort(corPoint, 0, 2, Comparator.comparingInt(o -> o[1]));
        Arrays.sort(corPoint, 2, 4, Comparator.comparingInt(o -> o[1]));
        Point[] corPointp = new Point[4];
        for (int i = 0; i < 4; i++) {
            corPointp[i] = new Point(corPoint[i][0], corPoint[i][1]);
        }
//        System.out.println(Arrays.deepToString(corPointp));
        return corPointp;

    }

//    public static Mat perspectiveTran(Point[] quaCorner) {
//        Point2f[] srcProjection = new Point2f[4];
////        srcProjection[0] = new Point2f(0, 0);
////        srcProjection[1] = new Point2f(0, 279);
////        srcProjection[2] = new Point2f(279, 0);
////        srcProjection[3] = new Point2f(279, 279);
//        Mat dst = new Mat();
//        Mat tran = getPerspectiveTransform(new Point2f(quaCorner), new Mat(srcProjection));
//        warpPerspective(qrBinary, dst, tran, new Size(280, 280));
//        imwrite("D:\\Users\\user\\OpenCV_test\\src\\QR\\QRDetect\\qr.jpg", dst);
//        return dst;
//
//    }

    public static String deCode(Mat qrCode) {
        try {
            MultiFormatReader formatReader = new MultiFormatReader();
            //读取指定的二维码文件

            BufferedImage bufferedImage = toBufferedImage(qrCode);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
            //定义二维码参数
            Map<DecodeHintType, String> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
            Result result = formatReader.decode(binaryBitmap, hints);
            //输出相关的二维码信息
            System.out.println("QRText:" + result.getText());
            bufferedImage.flush();
            return result.getText();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedImage toBufferedImage(Mat matrix) {
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        matrix.data().get(buffer);
        // get all pixel from martix
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), BufferedImage.TYPE_BYTE_BINARY);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, bufferSize);
        return image;
    }

    public static void light(org.opencv.core.Mat src, float alpha, float beta) {

        int channal = src.channels();
        double[] pixel;
        for (int i = 0, rlength = src.rows(); i < rlength; i++) {
            for (int j = 0, clen = src.cols(); j < clen; j++) {
                pixel = src.get(i, j).clone();
                if (channal == 3) {
                    pixel[0] = (pixel[0] - beta) * alpha;
                    pixel[1] = (pixel[1] - beta) * alpha;
                    pixel[2] = (pixel[2] - beta) * alpha;
                    src.put(i, j, pixel);
                } else {
                    src.put(i, j, (pixel[0] - beta) * alpha);
                }
            }
        }
    }
}
