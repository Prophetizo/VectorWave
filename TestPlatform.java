import ai.prophetizo.wavelet.util.PlatformDetector;

public class TestPlatform {
    public static void main(String[] args) {
        System.out.println("Platform: " + PlatformDetector.getPlatform());
        System.out.println("OS Arch: " + System.getProperty("os.arch"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
    }
}
