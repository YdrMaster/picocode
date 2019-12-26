import org.bytedeco.opencv.opencv_core.Mat;

public class Main {
    public static void main(String[] args) {
        PicoZense.INSTANCE.initialize();
        try (PicoZense.PicoCamera camera = PicoZense.INSTANCE.get(0)) {
            while (true) {
                Mat mat = camera.next();
                if (mat == null) continue;
                // TODO
            }
        }
    }
}
