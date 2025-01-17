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

import static cn.autolabor.Utils.binary;
import static cn.autolabor.Utils.center;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class PicoProcess {
    public static void process(@NotNull Mat src) {
        imshow(" ", src);
        waitKey(1);
    }

    public static void test() {
        QRRecognize(imread("test_qr_picture.jpg"));
        waitKey();
    }

    static void testShow(@NotNull Mat mat) {
//        imshow("test", mat);
//        imwrite("test.bmp", mat);
//        waitKey();
    }

    private static void QRRecognize(@NotNull Mat src) {
        // 计时
        long startTime = System.currentTimeMillis();
        // 二值化
        Mat qrBinary = binary(src);
        // TODO 需要进一步预处理，滤波？
        // 找轮廓
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(qrBinary, contours, hierarchy, RETR_TREE, CHAIN_APPROX_NONE);
        int[][] tree = Utils.parseHierarchy(hierarchy);
        // 求各轮廓中心
        List<Point2d> mc = center(contours);
        //根据特征块特征，获取所有特征块索引和二维码边框索引
        List<int[]> blockConfid = new ArrayList<>();
        Mat qrContour = qrBinary.clone();
        // 每个点按最小 2x2 = 4 考虑，中环至少 5x5x4 = 100
        final double minArea = 100;
        // 整体 = (7x3)^2，中环 = 5^2，中环/整体 = 25/441
        final double maxArea = Math.pow(Math.min(src.rows(), src.cols()), 2) * 25 / 441;
        for (int k = 0; k < contours.size(); k++) {
            // final int next = tree[k][0];
            // final int prior = tree[k][1];
            final int firstChild = tree[k][2];
            final int parent = tree[k][3];
            // 先排除孤立的轮廓
            if (firstChild < 0 || parent < 0) continue;
            final int uncle0 = tree[parent][0];
            final int uncle1 = tree[parent][1];
            final int grand = tree[parent][3];
            if ((uncle0 < 0 && uncle1 < 0) || grand < 0) continue;
            // 再按面积过滤
            double area = contourArea(contours.get(k));
            if (area < minArea || maxArea < area) continue;
            // 求环面积比
            double
                // 试图提高可读性
                // childLevel = contourArea(contours.get(firstChild)) / 9,
                // level = area / 25,
                // parentLevel = contourArea(contours.get(parent)) / 49,
                ratio1 = contourArea(contours.get(parent)) / area,
                ratio2 = area / contourArea(contours.get(firstChild));
            Matrix centroid = new Matrix(new double[][]{
                {mc.get(k).x(), mc.get(k).y(), 1},
                {mc.get(parent).x(), mc.get(parent).y(), 1},
                {mc.get(parent).x(), mc.get(parent).y(), 1}});
            if (34.0 / 25.0 <= ratio1 && ratio1 <= 64.0 / 25.0
                && 16.0 / 9.0 <= ratio2 && ratio2 <= 34.0 / 9.0
                && Math.abs(centroid.det()) <= 1
            ) blockConfid.add(new int[]{firstChild, k, parent, grand});
        }
        if (blockConfid.size() <= 1) System.out.println("Without QR");
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
        for (int i = 0; i < outLabel.size(); i++) outAll[i][0] = outLabel.get(i);
        //归类
        int[] num = new int[outLabel.size()];
        for (int[] ints : blockConfid)
            for (int i1 = 0; i1 < outLabel.size(); i1++)
                if (ints[3] == outLabel.get(i1)) {
                    outAll[i1][num[i1] + 1] = ints[2];
                    num[i1]++;
                    break;
                }
        // 画出二维码外边框 和 特征块最外层边框
        int num1 = 0;
        for (int[] ints : outAll) {//getQuaCorner是得到外框四角坐标，perspectiveTran是二维码的透视校正，deCode是二维码解码
            Random ble = new Random();
            Random grn = new Random();
            Random rd = new Random();
            System.out.println("QR!");
            Scalar color = new Scalar(255, 0, 0, 0);
            for (int i = 1; i < 4; i++)
                drawContours(src, contours, ints[i], color, 1, LINE_AA, hierarchy, Integer.MAX_VALUE, new Point());

            double area4 = contourArea(contours.get(ints[0]));
            double area31 = contourArea(contours.get(ints[1]));
            double area32 = contourArea(contours.get(ints[2]));
            double area33 = contourArea(contours.get(ints[3]));
//            if (area4 > 15.0 * Math.max(Math.max(area31, area32), area33)) {
//                continue;
//            }
            Point[] qrQuaCor = getQuaCorner(contours.get(ints[0]));
            assert qrQuaCor != null;
            for (Point point : qrQuaCor) System.out.println(point.x() + " " + point.y());
//            qrCode.add(perspectiveTran(qrQuaCor));//
//            String qrInfo = deCode(qrCode.get(num1));
//            num1++;
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

    private static Point[] getQuaCorner(Mat pointOfContour) {
        /*
        p0    p2
        |   / |
        |  /  |
        | /   |
        p1    p3
         */

        if (pointOfContour.rows() < 4) return null;
        int[][] corPoint = new int[4][2];
        double len13 = 0.0;
        int itemp = 0;
        int jtemp = 0;
        for (int i = 0; i < pointOfContour.rows() / 2 + 1; i++)
            for (int j = pointOfContour.rows() / 2; j < pointOfContour.rows() - 1; j++) {
                double lentemp = Math.sqrt(Math.pow(pointOfContour.ptr(i, 0).getInt(0) - pointOfContour.ptr(j, 0).getInt(0), 2) +
                    Math.pow(pointOfContour.ptr(i, 0).getInt(4) - pointOfContour.ptr(j, 0).getInt(4), 2));
                if (lentemp > len13) {
                    len13 = lentemp;
                    itemp = i;
                    jtemp = j;
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

            if (k2 >= pointOfContour.rows()) k22 = k2 - pointOfContour.rows();
            else k22 = k2;
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
        for (int i = 0; i < 4; i++) corPointp[i] = new Point(corPoint[i][0], corPoint[i][1]);
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

    private static BufferedImage toBufferedImage(Mat matrix) {
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        matrix.data().get(buffer);
        // get all pixel from martix
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), BufferedImage.TYPE_BYTE_BINARY);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, bufferSize);
        return image;
    }

    private static void light(org.opencv.core.Mat src, float alpha, float beta) {
        int channal = src.channels();
        double[] pixel;
        for (int i = 0, rlength = src.rows(); i < rlength; i++)
            for (int j = 0, clen = src.cols(); j < clen; j++) {
                pixel = src.get(i, j).clone();
                if (channal == 3) {
                    pixel[0] = (pixel[0] - beta) * alpha;
                    pixel[1] = (pixel[1] - beta) * alpha;
                    pixel[2] = (pixel[2] - beta) * alpha;
                    src.put(i, j, pixel);
                } else src.put(i, j, (pixel[0] - beta) * alpha);
            }
    }
}
