package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;

/**
 * Demonstrates the FFT-based linear convolution avoiding circular artifacts.
 */
public class ConvolutionDemo {
    public static void main(String[] args) {
        FFTAcceleratedCWT fft = new FFTAcceleratedCWT();
        
        // Test signal with clear boundary characteristics
        double[] signal = {1.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] kernel = {0.5, -0.5};
        
        System.out.println("=== FFT Convolution Demonstration ===");
        System.out.println("Signal: " + java.util.Arrays.toString(signal));
        System.out.println("Kernel: " + java.util.Arrays.toString(kernel));
        System.out.println();
        
        // Linear convolution (no artifacts)
        double[] linearResult = fft.convolveLinear(signal, kernel);
        System.out.println("Linear convolution result:");
        System.out.println("  Length: " + linearResult.length + " (= " + signal.length + " + " + kernel.length + " - 1)");
        System.out.println("  Values: " + java.util.Arrays.toString(linearResult));
        System.out.println();
        
        // Demonstrate CWT convolution
        double[] cwtResult = fft.convolveCWT(signal, kernel, 1.0);
        System.out.println("CWT convolution result (scale=1.0):");
        System.out.println("  Values: " + java.util.Arrays.toString(cwtResult));
        System.out.println();
        
        // Different scales
        double[] cwtResult2 = fft.convolveCWT(signal, kernel, 4.0);
        System.out.println("CWT convolution result (scale=4.0):");
        System.out.println("  Values: " + java.util.Arrays.toString(cwtResult2));
        System.out.println("  Note: Scaled by 1/sqrt(4) = 0.5 relative to scale=1.0");
        System.out.println();
        
        System.out.println("Key Features:");
        System.out.println("- Linear convolution eliminates circular artifacts at signal boundaries");
        System.out.println("- Proper zero-padding ensures mathematically correct results");
        System.out.println("- CWT scaling maintains correct normalization");  
        System.out.println("- FFT acceleration provides O(N log N) performance");
    }
}