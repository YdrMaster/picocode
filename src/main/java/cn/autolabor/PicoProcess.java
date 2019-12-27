package cn.autolabor;

import org.bytedeco.opencv.opencv_core.Mat;
import org.jetbrains.annotations.NotNull;

import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;

public class PicoProcess {
    public static void process(@NotNull Mat rgb) {
        imshow("pico", rgb);
        waitKey(1);
    }
}
