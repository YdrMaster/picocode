package cn.autolabor;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;

public class Utils {
    public static int[][] parseHierarchy(Mat hierarchy) {
        int[][] result = new int[hierarchy.cols()][4];
        for (int i = 0; i < hierarchy.cols(); ++i) {
            BytePointer data = hierarchy.ptr(0, i);
            for (int k = 0; k < 4; ++k)
                result[i][k] = data.getInt(k * 4);
        }
        return result;
    }
}
