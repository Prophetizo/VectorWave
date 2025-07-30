package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;

/**
 * Demonstrates basic Continuous Wavelet Transform usage.
 * 
 * <p>This demo shows how to perform CWT analysis with different wavelets
 * and configurations, including FFT acceleration and scale space analysis.</p>
 */
public class CWTBasicsDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave CWT Basics Demo ===\n");
        
        // Create a synthetic signal for analysis
        double[] signal = createTestSignal();
        System.out.printf("Created test signal with %d samples%n%n", signal.length);
        
        // Demo 1: Basic CWT with Morlet wavelet
        basicCWTAnalysis(signal);
        
        // Demo 2: FFT-accelerated CWT
        fftAcceleratedCWT(signal);
        
        // Demo 3: Scale space analysis
        scaleSpaceAnalysis(signal);
        
        // Demo 4: CWT configuration options
        configurationOptions(signal);
    }
    
    /**
     * Creates a test signal with multiple frequency components.
     */
    private static double[] createTestSignal() {
        int N = 1024;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            
            // Low frequency component (5 Hz equivalent)
            signal[i] += 2.0 * Math.sin(2 * Math.PI * 5 * t);
            
            // Medium frequency component (20 Hz equivalent)
            signal[i] += 1.5 * Math.sin(2 * Math.PI * 20 * t);
            
            // High frequency burst in the middle
            if (t > 0.4 && t < 0.6) {
                signal[i] += 3.0 * Math.sin(2 * Math.PI * 50 * t);
            }
            
            // Add some noise
            signal[i] += 0.2 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    /**
     * Demonstrates basic CWT analysis with Morlet wavelet.
     */
    private static void basicCWTAnalysis(double[] signal) {
        System.out.println("1. Basic CWT Analysis");
        System.out.println("====================");
        
        // Create Morlet wavelet (optimal for time-frequency analysis)
        MorletWavelet wavelet = new MorletWavelet();
        System.out.printf("Using Morlet wavelet: %s%n", wavelet.name());
        System.out.printf("Bandwidth: %.2f, Central frequency: %.2f%n", 
            wavelet.bandwidth(), wavelet.centerFrequency());
        
        // Create CWT transform
        CWTTransform cwt = new CWTTransform(wavelet);
        
        // Define scales for analysis (logarithmic spacing)
        double[] scales = ScaleSpace.logarithmic(1.0, 32.0, 32).getScales();
        System.out.printf("Analyzing %d scales from %.1f to %.1f%n", 
            scales.length, scales[0], scales[scales.length - 1]);
        
        // Perform analysis
        long startTime = System.nanoTime();
        CWTResult result = cwt.analyze(signal, scales);
        long endTime = System.nanoTime();
        
        System.out.printf("Analysis completed in %.2f ms%n", (endTime - startTime) / 1e6);
        
        // Display results
        double[][] coefficients = result.getCoefficients();
        System.out.printf("Result matrix: %d scales × %d time points%n", 
            coefficients.length, coefficients[0].length);
        
        // Find maximum coefficient
        double maxCoeff = 0;
        int maxScale = 0, maxTime = 0;
        for (int s = 0; s < coefficients.length; s++) {
            for (int t = 0; t < coefficients[s].length; t++) {
                double coeff = Math.abs(coefficients[s][t]);
                if (coeff > maxCoeff) {
                    maxCoeff = coeff;
                    maxScale = s;
                    maxTime = t;
                }
            }
        }
        
        System.out.printf("Maximum coefficient: %.3f at scale %.2f, time %d%n%n", 
            maxCoeff, scales[maxScale], maxTime);
    }
    
    /**
     * Demonstrates FFT-accelerated CWT for improved performance.
     */
    private static void fftAcceleratedCWT(double[] signal) {
        System.out.println("2. FFT-Accelerated CWT");
        System.out.println("======================");
        
        MorletWavelet wavelet = new MorletWavelet();
        
        // Configure for FFT acceleration
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .normalizeScales(true)
            .paddingStrategy(CWTConfig.PaddingStrategy.REFLECT)
            .build();
        
        CWTTransform cwt = new CWTTransform(wavelet, config);
        
        double[] scales = ScaleSpace.logarithmic(1.0, 64.0, 40).getScales();
        
        // Compare with non-FFT version
        System.out.println("Performance comparison:");
        
        // FFT version
        long fftStart = System.nanoTime();
        CWTResult fftResult = cwt.analyze(signal, scales);
        long fftTime = System.nanoTime() - fftStart;
        
        // Direct convolution version  
        CWTConfig directConfig = CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(true)
            .build();
        CWTTransform directCwt = new CWTTransform(wavelet, directConfig);
        
        long directStart = System.nanoTime();
        CWTResult directResult = directCwt.analyze(signal, scales);
        long directTime = System.nanoTime() - directStart;
        
        System.out.printf("FFT method:    %.2f ms%n", fftTime / 1e6);
        System.out.printf("Direct method: %.2f ms%n", directTime / 1e6);
        System.out.printf("Speedup:       %.1fx%n", (double) directTime / fftTime);
        
        // Verify results are similar
        double mse = computeMSE(fftResult.getCoefficients(), directResult.getCoefficients());
        System.out.printf("MSE between methods: %.2e (should be very small)%n%n", mse);
    }
    
    /**
     * Demonstrates scale space analysis for different frequency ranges.
     */
    private static void scaleSpaceAnalysis(double[] signal) {
        System.out.println("3. Scale Space Analysis");
        System.out.println("======================");
        
        MorletWavelet wavelet = new MorletWavelet();
        CWTTransform cwt = new CWTTransform(wavelet);
        
        // Define different scale spaces for different frequency ranges
        ScaleSpace lowFreqScales = ScaleSpace.logarithmic(8.0, 64.0, 16);
        ScaleSpace midFreqScales = ScaleSpace.logarithmic(2.0, 16.0, 16);
        ScaleSpace highFreqScales = ScaleSpace.logarithmic(0.5, 4.0, 16);
        
        System.out.println("Analyzing different frequency ranges:");
        
        analyzeFrequencyRange(cwt, signal, lowFreqScales, "Low frequency (large scales)");
        analyzeFrequencyRange(cwt, signal, midFreqScales, "Medium frequency (medium scales)");
        analyzeFrequencyRange(cwt, signal, highFreqScales, "High frequency (small scales)");
        
        System.out.println();
    }
    
    /**
     * Demonstrates various CWT configuration options.
     */
    private static void configurationOptions(double[] signal) {
        System.out.println("4. Configuration Options");
        System.out.println("========================");
        
        MorletWavelet wavelet = new MorletWavelet();
        double[] scales = ScaleSpace.logarithmic(2.0, 16.0, 20).getScales();
        
        // Different padding strategies
        System.out.println("Testing padding strategies:");
        
        CWTConfig.PaddingStrategy[] strategies = {
            CWTConfig.PaddingStrategy.ZERO,
            CWTConfig.PaddingStrategy.REFLECT,
            CWTConfig.PaddingStrategy.SYMMETRIC,
            CWTConfig.PaddingStrategy.PERIODIC
        };
        
        for (CWTConfig.PaddingStrategy strategy : strategies) {
            CWTConfig config = CWTConfig.builder()
                .paddingStrategy(strategy)
                .enableFFT(true)
                .build();
            
            CWTTransform cwt = new CWTTransform(wavelet, config);
            
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(signal, scales);
            long endTime = System.nanoTime();
            
            double energy = computeEnergy(result.getCoefficients());
            System.out.printf("  %-10s: %.2f ms, Energy: %.2e%n", 
                strategy, (endTime - startTime) / 1e6, energy);
        }
        
        System.out.println("\nNormalization comparison:");
        
        // With normalization
        CWTConfig normalizedConfig = CWTConfig.builder()
            .normalizeScales(true)
            .enableFFT(true)
            .build();
        CWTTransform normalizedCwt = new CWTTransform(wavelet, normalizedConfig);
        CWTResult normalizedResult = normalizedCwt.analyze(signal, scales);
        
        // Without normalization
        CWTConfig unnormalizedConfig = CWTConfig.builder()
            .normalizeScales(false)
            .enableFFT(true)
            .build();
        CWTTransform unnormalizedCwt = new CWTTransform(wavelet, unnormalizedConfig);
        CWTResult unnormalizedResult = unnormalizedCwt.analyze(signal, scales);
        
        double normalizedEnergy = computeEnergy(normalizedResult.getCoefficients());
        double unnormalizedEnergy = computeEnergy(unnormalizedResult.getCoefficients());
        
        System.out.printf("  Normalized:    Energy = %.2e%n", normalizedEnergy);
        System.out.printf("  Unnormalized:  Energy = %.2e%n", unnormalizedEnergy);
        System.out.printf("  Ratio:         %.2f%n", unnormalizedEnergy / normalizedEnergy);
    }
    
    // Helper methods
    
    private static void analyzeFrequencyRange(CWTTransform cwt, double[] signal, 
                                            ScaleSpace scaleSpace, String description) {
        CWTResult result = cwt.analyze(signal, scaleSpace);
        double energy = computeEnergy(result.getCoefficients());
        
        double[] scales = scaleSpace.getScales();
        System.out.printf("  %s: scales %.1f-%.1f, energy = %.2e%n", 
            description, scales[scales.length-1], scales[0], energy);
    }
    
    private static double computeEnergy(double[][] coefficients) {
        double energy = 0;
        for (double[] row : coefficients) {
            for (double value : row) {
                energy += value * value;
            }
        }
        return energy;
    }
    
    private static double computeMSE(double[][] a, double[][] b) {
        double mse = 0;
        int count = 0;
        
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                double diff = a[i][j] - b[i][j];
                mse += diff * diff;
                count++;
            }
        }
        
        return mse / count;
    }
}