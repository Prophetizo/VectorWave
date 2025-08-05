package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
// DWT imports removed - using only MODWT

import java.util.Arrays;

/**
 * Comprehensive demonstration of MODWT (Maximal Overlap Discrete Wavelet Transform) functionality.
 * 
 * <p>This demo shows the key differences between standard DWT and MODWT:</p>
 * <ul>
 *   <li>MODWT can handle arbitrary length signals (not just power-of-2)</li>
 *   <li>MODWT produces same-length output as input (non-decimated)</li>
 *   <li>MODWT is shift-invariant</li>
 *   <li>MODWT provides better time-frequency localization</li>
 * </ul>
 * 
 * <h2>When to Use MODWT:</h2>
 * <ul>
 *   <li><strong>Time series analysis:</strong> Financial data, economic indicators</li>
 *   <li><strong>Pattern detection:</strong> When shift-invariance is critical</li>
 *   <li><strong>Feature extraction:</strong> Machine learning applications</li>
 *   <li><strong>Arbitrary length signals:</strong> Real-world data that isn't power-of-2</li>
 * </ul>
 * 
 * <h2>Performance Characteristics:</h2>
 * <p>MODWT has O(N*L) complexity where N is signal length and L is filter length,
 * compared to O(N) for DWT. However, MODWT provides significant advantages for
 * many signal analysis tasks.</p>
 * 
 * @see ai.prophetizo.wavelet.modwt.MODWTTransform
 * @see ai.prophetizo.wavelet.modwt.MODWTTransform
 */
public class MODWTDemo {
    
    public static void main(String[] args) {
        System.out.println("MODWT (Maximal Overlap Discrete Wavelet Transform) Demo");
        System.out.println("======================================================");
        System.out.println();
        
        demonstrateArbitraryLength();
        System.out.println();
        
        demonstrateNonDecimated();
        System.out.println();
        
        demonstrateShiftInvariance();
        System.out.println();
        
        demonstrateMultipleWavelets();
        System.out.println();
        
        demonstrateDWTvsMODWT();
        System.out.println();
        
        demonstrateTimeSeriesAnalysis();
        System.out.println();
        
        demonstratePerformanceCharacteristics();
    }
    
    private static void demonstrateArbitraryLength() {
        System.out.println("1. Arbitrary Length Signals");
        System.out.println("==========================");
        
        // MODWT can handle signals that are not power-of-2 length
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // length 7
        
        System.out.println("Original signal (length " + signal.length + "): " + Arrays.toString(signal));
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(signal);
        double[] reconstructed = modwt.inverse(result);
        
        System.out.println("Approximation coeffs (length " + result.approximationCoeffs().length + "): " + 
                         formatArray(result.approximationCoeffs()));
        System.out.println("Detail coeffs (length " + result.detailCoeffs().length + "): " + 
                         formatArray(result.detailCoeffs()));
        System.out.println("Reconstructed signal: " + formatArray(reconstructed));
        
        double maxError = calculateMaxError(signal, reconstructed);
        System.out.println("Max reconstruction error: " + String.format("%.2e", maxError));
    }
    
    private static void demonstrateNonDecimated() {
        System.out.println("2. Non-decimated Transform");
        System.out.println("=========================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}; // length 8
        
        System.out.println("Original signal: " + Arrays.toString(signal));
        System.out.println("Signal length: " + signal.length);
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(signal);
        
        System.out.println("MODWT approximation length: " + result.approximationCoeffs().length + 
                         " (same as input)");
        System.out.println("MODWT detail length: " + result.detailCoeffs().length + 
                         " (same as input)");
        
        // Compare with what DWT would produce
        System.out.println("Standard DWT would produce: " + (signal.length / 2) + 
                         " approximation + " + (signal.length / 2) + " detail coefficients");
    }
    
    private static void demonstrateShiftInvariance() {
        System.out.println("3. Shift Invariance");
        System.out.println("==================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] shiftedSignal = {8.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // circular shift left by 1
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        MODWTResult result1 = modwt.forward(signal);
        MODWTResult result2 = modwt.forward(shiftedSignal);
        
        System.out.println("Original signal:     " + formatArray(signal));
        System.out.println("Shifted signal:      " + formatArray(shiftedSignal));
        System.out.println();
        System.out.println("Original approx:     " + formatArray(result1.approximationCoeffs()));
        System.out.println("Shifted approx:      " + formatArray(result2.approximationCoeffs()));
        System.out.println();
        System.out.println("Original detail:     " + formatArray(result1.detailCoeffs()));
        System.out.println("Shifted detail:      " + formatArray(result2.detailCoeffs()));
        
        System.out.println("\nNote: MODWT coefficients shift with the input signal, preserving temporal relationships.");
    }
    
    private static void demonstrateMultipleWavelets() {
        System.out.println("4. Different Wavelets");
        System.out.println("====================");
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        System.out.println("Original signal: " + Arrays.toString(signal));
        System.out.println();
        
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4
        };
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform modwt = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            MODWTResult result = modwt.forward(signal);
            double[] reconstructed = modwt.inverse(result);
            
            double maxError = calculateMaxError(signal, reconstructed);
            
            System.out.println(wavelet.name() + " wavelet:");
            System.out.println("  Approximation: " + formatArray(result.approximationCoeffs()));
            System.out.println("  Detail:        " + formatArray(result.detailCoeffs()));
            System.out.println("  Max error:     " + String.format("%.2e", maxError));
            System.out.println();
        }
    }
    
    private static String formatArray(double[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < Math.min(array.length, 8); i++) { // Show first 8 elements max
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.3f", array[i]));
        }
        if (array.length > 8) {
            sb.append(", ...");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0.0;
        for (int i = 0; i < original.length; i++) {
            double error = Math.abs(original[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        return maxError;
    }
    
    private static void demonstrateDWTvsMODWT() {
        System.out.println("5. DWT vs MODWT Comparison");
        System.out.println("==========================");
        
        // Use a power-of-2 length signal for fair comparison
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.sin(6 * Math.PI * i / 16);
        }
        
        System.out.println("Original signal length: " + signal.length);
        System.out.println();
        
        // DWT
        // DWT comparison removed - focusing on MODWT capabilities
        // For DWT comparison, see historical documentation
        
        // MODWT
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MODWTResult modwtResult = modwt.forward(signal);
        
        System.out.println("DWT Results:");
        System.out.println("  Approximation length: " + dwtResult.approximationCoeffs().length);
        System.out.println("  Detail length: " + dwtResult.detailCoeffs().length);
        System.out.println("  Total coefficients: " + 
                         (dwtResult.approximationCoeffs().length + dwtResult.detailCoeffs().length));
        
        System.out.println("\nMODWT Results:");
        System.out.println("  Approximation length: " + modwtResult.approximationCoeffs().length);
        System.out.println("  Detail length: " + modwtResult.detailCoeffs().length);
        System.out.println("  Total coefficients: " + 
                         (modwtResult.approximationCoeffs().length + modwtResult.detailCoeffs().length));
        
        System.out.println("\nKey Difference: MODWT has 2x redundancy but preserves temporal information");
    }
    
    private static void demonstrateTimeSeriesAnalysis() {
        System.out.println("6. Time Series Analysis with MODWT");
        System.out.println("==================================");
        
        // Create a financial-like time series with trend and noise
        int length = 100;
        double[] timeSeries = new double[length];
        for (int i = 0; i < length; i++) {
            double trend = 100 + i * 0.5;  // Upward trend
            double seasonal = 10 * Math.sin(2 * Math.PI * i / 20);  // Seasonal component
            double noise = 2 * (Math.random() - 0.5);  // Random noise
            timeSeries[i] = trend + seasonal + noise;
        }
        
        MODWTTransform modwt = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(timeSeries);
        
        // Analyze components
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // Calculate variance contributions
        double totalVar = calculateVariance(timeSeries);
        double approxVar = calculateVariance(approx);
        double detailVar = calculateVariance(detail);
        
        System.out.println("Time series length: " + length);
        System.out.println("Total variance: " + String.format("%.2f", totalVar));
        System.out.println("Approximation variance: " + String.format("%.2f", approxVar) + 
                         " (" + String.format("%.1f%%", 100 * approxVar / totalVar) + ")");
        System.out.println("Detail variance: " + String.format("%.2f", detailVar) + 
                         " (" + String.format("%.1f%%", 100 * detailVar / totalVar) + ")");
        System.out.println("\nInterpretation: Approximation captures trend, detail captures high-frequency changes");
    }
    
    private static void demonstratePerformanceCharacteristics() {
        System.out.println("7. Performance Characteristics");
        System.out.println("=============================");
        
        // Test different signal sizes
        int[] sizes = {100, 500, 1000, 5000};
        Wavelet wavelet = new Haar();
        
        System.out.println("Signal Size | MODWT Time | Processing Rate | Vector Speedup");
        System.out.println("------------|------------|-----------------|---------------");
        
        for (int size : sizes) {
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.random();
            }
            
            MODWTTransform modwt = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Warm up
            for (int i = 0; i < 10; i++) {
                modwt.forward(signal);
            }
            
            // Measure
            long startTime = System.nanoTime();
            int iterations = 100;
            for (int i = 0; i < iterations; i++) {
                modwt.forward(signal);
            }
            long endTime = System.nanoTime();
            
            double avgTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
            double samplesPerMs = size / avgTimeMs;
            
            // Get performance info
            var perfInfo = modwt.getPerformanceInfo();
            double speedup = perfInfo.estimateSpeedup(size);
            
            System.out.printf("%11d | %9.3f ms | %13.0f/ms | %13.1fx%n", 
                            size, avgTimeMs, samplesPerMs, speedup);
        }
        
        System.out.println("\nNote: MODWT is computationally more intensive than DWT but provides");
        System.out.println("      important advantages for signal analysis applications.");
    }
    
    private static double calculateVariance(double[] data) {
        double mean = 0.0;
        for (double value : data) {
            mean += value;
        }
        mean /= data.length;
        
        double variance = 0.0;
        for (double value : data) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return variance / data.length;
    }
}