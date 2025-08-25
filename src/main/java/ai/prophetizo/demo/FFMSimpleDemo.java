package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.modwt.*;

import java.util.Random;

/**
 * Simple demonstration of MODWT's memory-efficient features.
 * 
 * <p>Note: Full FFM (Foreign Function & Memory) API integration with MODWT
 * is planned for future releases. This demo shows current memory-efficient
 * patterns available with MODWT.</p>
 * 
 */
public class FFMSimpleDemo {
    
    public static void main(String[] args) {
        System.out.println("=== MODWT Simple Memory Efficiency Demo ===\n");
        
        // Generate test signal (MODWT works with any size!)
        int signalSize = 777; // Not a power of 2!
        double[] signal = generateSignal(signalSize);
        
        // Use Haar wavelet (simplest)
        Wavelet wavelet = new Haar();
        
        System.out.println("1. MODWT vs DWT Memory Comparison");
        System.out.println("---------------------------------");
        
        // MODWT approach - works with any size
        MODWTTransform modwt = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        long modwtStart = System.nanoTime();
        MODWTResult modwtResult = modwt.forward(signal);
        double[] modwtRecon = modwt.inverse(modwtResult);
        long modwtTime = System.nanoTime() - modwtStart;
        
        // Show memory advantage
        int nextPowerOf2 = nextPowerOfTwo(signalSize);
        int dwtPadding = nextPowerOf2 - signalSize;
        double memoryWaste = (double) dwtPadding / nextPowerOf2 * 100;
        
        System.out.printf("Signal size: %d%n", signalSize);
        System.out.printf("DWT would need: %d (padding: %d samples)%n", nextPowerOf2, dwtPadding);
        System.out.printf("Memory waste avoided: %.1f%%%n", memoryWaste);
        System.out.printf("MODWT time: %.2f ms%n", modwtTime / 1e6);
        
        // Verify perfect reconstruction
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - modwtRecon[i]));
        }
        System.out.printf("Reconstruction error: %.2e%n", maxError);
        System.out.printf("Perfect reconstruction: %s%n", maxError < 1e-14 ? "YES" : "NO");
        
        System.out.println("\n2. Arbitrary Length Processing");
        System.out.println("-------------------------------");
        
        // Process signals of various non-power-of-2 lengths
        int[] sizes = {100, 333, 555, 777, 999};
        System.out.println("Processing various signal lengths:");
        
        for (int size : sizes) {
            double[] testSignal = generateSignal(size);
            MODWTResult result = modwt.forward(testSignal);
            System.out.printf("  Size %4d: coeffs=%d (preserved length: %s)%n", 
                            size, result.getSignalLength(),
                            size == result.getSignalLength() ? "YES" : "NO");
        }
        
        System.out.println("\n3. Streaming-Like Processing");
        System.out.println("----------------------------");
        
        // Process signal in chunks (simulating streaming)
        int chunkSize = 64;
        int processed = 0;
        int chunkCount = 0;
        
        System.out.println("Processing signal in chunks:");
        while (processed < signalSize) {
            int size = Math.min(chunkSize, signalSize - processed);
            
            // Extract chunk
            double[] chunk = new double[size];
            System.arraycopy(signal, processed, chunk, 0, size);
            
            // Process chunk with MODWT
            MODWTResult chunkResult = modwt.forward(chunk);
            chunkCount++;
            
            if (chunkCount <= 3 || processed + size >= signalSize) {
                System.out.printf("  Chunk %2d: size=%3d, approx_energy=%.4f%n", 
                                chunkCount, size, calculateEnergy(chunkResult.approximationCoeffs()));
            } else if (chunkCount == 4) {
                System.out.println("  ...");
            }
            
            processed += size;
        }
        
        System.out.println("\n4. Performance Information");
        System.out.println("-------------------------");
        
        // Show performance characteristics
        var perfInfo = modwt.getPerformanceInfo();
        System.out.println(perfInfo.description());
        
        // Estimate processing time for larger signals
        int[] testSizes = {1000, 10000, 100000};
        System.out.println("\nEstimated processing times:");
        for (int size : testSizes) {
            long estimate = modwt.estimateProcessingTime(size);
            System.out.printf("  %6d samples: ~%.2f ms%n", size, estimate / 1e6);
        }
        
        System.out.println("\nDemo completed successfully!");
        System.out.println("\nKey MODWT advantages demonstrated:");
        System.out.println("- Works with ANY signal length (not just powers of 2)");
        System.out.println("- No memory waste from padding");
        System.out.println("- Shift-invariant (better for streaming/chunked processing)");
        System.out.println("- Preserves signal length in coefficients");
    }
    
    private static double[] generateSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian() * 0.1 +
                       Math.sin(2 * Math.PI * i / 50);
        }
        
        return signal;
    }
    
    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }
}