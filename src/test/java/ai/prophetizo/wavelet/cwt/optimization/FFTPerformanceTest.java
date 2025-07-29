package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

/**
 * Performance comparison tests between standard and optimized FFT implementations.
 * 
 * <p>These tests are disabled by default to avoid affecting regular test runs.
 * Enable them to validate performance improvements.</p>
 */
class FFTPerformanceTest {
    
    @Test
    @DisplayName("Performance comparison for FFT operations")
    @Disabled("Performance test - enable manually for benchmarking")
    void compareFFTPerformance() {
        // Test different sizes
        int[] sizes = {64, 128, 256, 512, 1024, 2048};
        
        System.out.println("FFT Performance Comparison");
        System.out.println("Size\tStandard (ms)\tOptimized (ms)\tSpeedup");
        System.out.println("----\t-------------\t--------------\t-------");
        
        for (int size : sizes) {
            double[] signal = generateTestSignal(size);
            
            // Warm up JVM
            FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
            FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
            
            for (int i = 0; i < 100; i++) {
                standardFFT.fft(signal);
                optimizedFFT.fft(signal);
            }
            
            // Measure performance
            int iterations = 1000;
            
            long standardTime = measureFFTTime(standardFFT, signal, iterations);
            long optimizedTime = measureFFTTime(optimizedFFT, signal, iterations);
            
            double speedup = (double) standardTime / optimizedTime;
            
            System.out.printf("%d\t%.2f\t\t%.2f\t\t%.2fx%n", 
                size, 
                standardTime / 1_000_000.0, 
                optimizedTime / 1_000_000.0, 
                speedup);
        }
    }
    
    @Test
    @DisplayName("Performance comparison for IFFT operations")
    @Disabled("Performance test - enable manually for benchmarking")
    void compareIFFTPerformance() {
        // Test different sizes
        int[] sizes = {64, 128, 256, 512, 1024, 2048};
        
        System.out.println("IFFT Performance Comparison");
        System.out.println("Size\tStandard (ms)\tOptimized (ms)\tSpeedup");
        System.out.println("----\t-------------\t--------------\t-------");
        
        for (int size : sizes) {
            Complex[] spectrum = generateTestSpectrum(size);
            
            // Warm up JVM
            FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
            FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
            
            for (int i = 0; i < 100; i++) {
                standardFFT.ifft(spectrum);
                optimizedFFT.ifft(spectrum);
            }
            
            // Measure performance
            int iterations = 1000;
            
            long standardTime = measureIFFTTime(standardFFT, spectrum, iterations);
            long optimizedTime = measureIFFTTime(optimizedFFT, spectrum, iterations);
            
            double speedup = (double) standardTime / optimizedTime;
            
            System.out.printf("%d\t%.2f\t\t%.2f\t\t%.2fx%n", 
                size, 
                standardTime / 1_000_000.0, 
                optimizedTime / 1_000_000.0, 
                speedup);
        }
    }
    
    @Test
    @DisplayName("Memory allocation comparison")
    @Disabled("Performance test - enable manually for benchmarking")
    void compareMemoryUsage() {
        System.out.println("Memory Usage Analysis");
        System.out.println("This test demonstrates reduced garbage collection with optimized implementation");
        
        int size = 1024;
        int iterations = 10000;
        
        double[] signal = generateTestSignal(size);
        
        // Test standard implementation
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        FFTAcceleratedCWT standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            standardFFT.fft(signal);
        }
        long standardTime = System.nanoTime() - startTime;
        
        System.gc();
        long memoryAfterStandard = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Test optimized implementation
        FFTAcceleratedCWT optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            optimizedFFT.fft(signal);
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        System.gc();
        long memoryAfterOptimized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        System.out.printf("Standard Implementation: %.2f ms%n", standardTime / 1_000_000.0);
        System.out.printf("Optimized Implementation: %.2f ms%n", optimizedTime / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", (double) standardTime / optimizedTime);
        System.out.printf("Memory usage change (standard): %d KB%n", (memoryAfterStandard - memoryBefore) / 1024);
        System.out.printf("Memory usage change (optimized): %d KB%n", (memoryAfterOptimized - memoryAfterStandard) / 1024);
    }
    
    private double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / size) + 0.5 * Math.cos(4 * Math.PI * i / size);
        }
        return signal;
    }
    
    private Complex[] generateTestSpectrum(int size) {
        Complex[] spectrum = new Complex[size];
        for (int i = 0; i < size; i++) {
            double phase = 2 * Math.PI * i / size;
            spectrum[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        return spectrum;
    }
    
    private long measureFFTTime(FFTAcceleratedCWT fft, double[] signal, int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fft.fft(signal);
        }
        return System.nanoTime() - startTime;
    }
    
    private long measureIFFTTime(FFTAcceleratedCWT fft, Complex[] spectrum, int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fft.ifft(spectrum);
        }
        return System.nanoTime() - startTime;
    }
}