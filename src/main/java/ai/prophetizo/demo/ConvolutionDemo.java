package ai.prophetizo.demo;

import ai.prophetizo.wavelet.util.SignalProcessor;

/**
 * Demonstrates the FFT-based linear convolution avoiding circular artifacts.
 */
public class ConvolutionDemo {
    public static void main(String[] args) {
        // Test signal with clear boundary characteristics
        double[] signal = {1.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] kernel = {0.5, -0.5};
        
        System.out.println("=== FFT Convolution Demonstration ===");
        System.out.println("Signal: " + java.util.Arrays.toString(signal));
        System.out.println("Kernel: " + java.util.Arrays.toString(kernel));
        System.out.println();
        
        // Linear convolution using SignalProcessor (no artifacts)
        double[] linearResult = SignalProcessor.convolveFFT(signal, kernel);
        System.out.println("Linear convolution result:");
        System.out.println("  Length: " + linearResult.length + " (= " + signal.length + " + " + kernel.length + " - 1)");
        System.out.println("  Values: " + java.util.Arrays.toString(linearResult));
        System.out.println();
        
        // Demonstrate scaled convolution (simulating CWT behavior)
        double scale = 4.0;
        double sqrtScale = Math.sqrt(scale);
        
        // Scale the kernel
        double[] scaledKernel = new double[kernel.length];
        for (int i = 0; i < kernel.length; i++) {
            scaledKernel[i] = kernel[i] / sqrtScale;
        }
        
        double[] scaledResult = SignalProcessor.convolveFFT(signal, scaledKernel);
        System.out.println("Scaled convolution result (scale=4.0):");
        System.out.println("  Values: " + java.util.Arrays.toString(scaledResult));
        System.out.println("  Note: Kernel scaled by 1/sqrt(4) = 0.5");
        System.out.println();
        
        System.out.println("Key Features:");
        System.out.println("- SignalProcessor.convolveFFT performs linear convolution (no circular artifacts)");
        System.out.println("- Automatic zero-padding to next power of 2 for FFT efficiency");
        System.out.println("- Scaling by 1/sqrt(scale) simulates CWT normalization");  
        System.out.println("- FFT acceleration provides O(N log N) performance");
    }
}