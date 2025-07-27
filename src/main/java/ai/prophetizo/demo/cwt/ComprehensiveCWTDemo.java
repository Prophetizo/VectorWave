package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;

/**
 * Comprehensive CWT demonstration showcasing all major features.
 * 
 * <p>This demo provides a complete overview of VectorWave's CWT capabilities,
 * combining all major features in realistic use cases.</p>
 */
public class ComprehensiveCWTDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Comprehensive CWT Demo ===\n");
        
        // Demo 1: Real-world signal analysis
        realWorldSignalAnalysis();
        
        // Demo 2: Time-frequency analysis
        timeFrequencyAnalysis();
        
        // Demo 3: Multi-wavelet comparison
        multiWaveletComparison();
        
        // Demo 4: Advanced configuration showcase
        advancedConfiguration();
        
        System.out.println("Demo completed! Check out individual demos for more details:");
        System.out.println("- CWTBasicsDemo: Basic CWT usage and concepts");
        System.out.println("- FinancialWaveletsDemo: Financial analysis wavelets");
        System.out.println("- GaussianDerivativeDemo: Feature detection wavelets");
        System.out.println("- CWTPerformanceDemo: Performance optimization techniques");
    }
    
    /**
     * Analyzes a realistic biomedical signal (simulated ECG).
     */
    private static void realWorldSignalAnalysis() {
        System.out.println("1. Real-World Signal Analysis: Simulated ECG");
        System.out.println("============================================");
        
        // Create simulated ECG signal
        double[] ecgSignal = createECGSignal();
        System.out.printf("Generated ECG signal: %d samples (%.1f seconds @ 250 Hz)%n", 
            ecgSignal.length, ecgSignal.length / 250.0);
        
        // Use Morlet wavelet for biomedical signal analysis
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0); // Good time-frequency resolution
        
        // Configure for optimal analysis
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .normalizeScales(true)
            .paddingStrategy(CWTConfig.PaddingStrategy.REFLECT)
            .build();
        
        CWTTransform cwt = new CWTTransform(morlet, config);
        
        // Analyze frequencies from 1 Hz to 50 Hz (physiologically relevant)
        ScaleSpace scaleSpace = ScaleSpace.forFrequencyRange(1.0, 50.0, 250.0, morlet, 40);
        
        long startTime = System.nanoTime();
        CWTResult result = cwt.analyze(ecgSignal, scaleSpace);
        long endTime = System.nanoTime();
        
        System.out.printf("Analysis completed in %.2f ms%n", (endTime - startTime) / 1e6);
        
        // Analyze results
        analyzeECGResults(result, scaleSpace);
        
        System.out.println();
    }
    
    /**
     * Demonstrates time-frequency analysis of a chirp signal.
     */
    private static void timeFrequencyAnalysis() {
        System.out.println("2. Time-Frequency Analysis: Chirp Signal");
        System.out.println("========================================");
        
        // Create chirp signal (frequency increases linearly)
        double[] chirpSignal = createChirpSignal();
        System.out.printf("Generated chirp signal: %d samples%n", chirpSignal.length);
        System.out.println("Frequency increases from 5 Hz to 50 Hz over time");
        
        MorletWavelet morlet = new MorletWavelet();
        CWTTransform cwt = new CWTTransform(morlet);
        
        // Use scales corresponding to the frequency range
        double[] scales = ScaleSpace.logarithmic(1.0, 32.0, 30).getScales();
        
        CWTResult result = cwt.analyze(chirpSignal, scales);
        
        // Find the ridge (maximum energy) across time
        analyzeTimeFrequencyRidge(result, scales);
        
        System.out.println();
    }
    
    /**
     * Compares different wavelets on the same signal.
     */
    private static void multiWaveletComparison() {
        System.out.println("3. Multi-Wavelet Comparison");
        System.out.println("===========================");
        
        // Create a test signal with multiple components
        double[] testSignal = createMultiComponentSignal();
        System.out.printf("Test signal: %d samples with multiple frequency components%n", 
            testSignal.length);
        
        // Test different wavelets - using Morlet with different parameters
        MorletWavelet[] wavelets = {
            new MorletWavelet(6.0, 1.0),  // Standard Morlet
            new MorletWavelet(4.0, 1.0),  // Lower frequency resolution
            new MorletWavelet(8.0, 1.0),  // Higher frequency resolution
            new MorletWavelet(6.0, 2.0)   // Different bandwidth
        };
        
        String[] waveletNames = {"Morlet(6,1)", "Morlet(4,1)", "Morlet(8,1)", "Morlet(6,2)"};
        
        double[] scales = ScaleSpace.logarithmic(2.0, 16.0, 20).getScales();
        
        System.out.println("\nWavelet comparison results:");
        System.out.println("Wavelet      | Energy    | Max Coeff | Time (ms)");
        System.out.println("-------------|-----------|-----------|----------");
        
        for (int i = 0; i < wavelets.length; i++) {
            CWTTransform cwt = new CWTTransform(wavelets[i]);
            
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(testSignal, scales);
            long endTime = System.nanoTime();
            
            double[] stats = computeSignalStats(result.getCoefficients());
            double energy = stats[0];
            double maxCoeff = stats[1];
            double timeMs = (endTime - startTime) / 1e6;
            
            System.out.printf("%-12s | %9.2e | %9.3f | %8.2f%n", 
                waveletNames[i], energy, maxCoeff, timeMs);
        }
        
        System.out.println("\nMorlet wavelet parameter effects:");
        System.out.println("- (6,1): Standard configuration, good balance");
        System.out.println("- (4,1): Lower freq resolution, faster decay"); 
        System.out.println("- (8,1): Higher freq resolution, slower decay");
        System.out.println("- (6,2): Different bandwidth, wider frequency response");
        
        System.out.println();
    }
    
    /**
     * Showcases advanced CWT configuration options.
     */
    private static void advancedConfiguration() {
        System.out.println("4. Advanced Configuration Showcase");
        System.out.println("==================================");
        
        double[] signal = createTestSignal(1024);
        MorletWavelet wavelet = new MorletWavelet();
        double[] scales = ScaleSpace.logarithmic(2.0, 32.0, 25).getScales();
        
        // Test different configurations
        CWTConfig[] configs = {
            // Real-time processing config
            CWTConfig.forRealTimeProcessing(),
            
            // Batch processing config  
            CWTConfig.forBatchProcessing(),
            
            // Java 23 optimized config
            CWTConfig.optimizedForJava23(),
            
            // Custom high-precision config
            CWTConfig.builder()
                .enableFFT(true)
                .normalizeScales(true)
                .paddingStrategy(CWTConfig.PaddingStrategy.SYMMETRIC)
                .fftSize(2048)
                .build()
        };
        
        String[] configNames = {
            "Real-time", "Batch", "Java 23", "High-precision"
        };
        
        System.out.println("Configuration comparison:");
        System.out.println("Config        | Time (ms) | Memory | Features");
        System.out.println("--------------|-----------|--------|----------");
        
        for (int i = 0; i < configs.length; i++) {
            CWTTransform cwt = new CWTTransform(wavelet, configs[i]);
            
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            
            long startTime = System.nanoTime();
            CWTResult result = cwt.analyze(signal, scales);
            long endTime = System.nanoTime();
            
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            
            double timeMs = (endTime - startTime) / 1e6;
            double memoryMB = (memAfter - memBefore) / (1024.0 * 1024.0);
            
            String features = getConfigFeatures(configs[i]);
            
            System.out.printf("%-13s | %9.2f | %6.1f | %s%n", 
                configNames[i], timeMs, memoryMB, features);
        }
        
        System.out.println("\nConfiguration guidelines:");
        System.out.println("- Real-time: Low latency, direct convolution");
        System.out.println("- Batch: FFT acceleration, parallel processing");
        System.out.println("- Java 23: Latest JVM features, optimal performance");
        System.out.println("- High-precision: Custom FFT size, symmetric padding");
        
        System.out.println();
    }
    
    // Signal generation methods
    
    private static double[] createECGSignal() {
        int fs = 250; // 250 Hz sampling rate
        double duration = 4.0; // 4 seconds
        int N = (int)(fs * duration);
        double[] signal = new double[N];
        
        double heartRate = 75.0; // 75 BPM
        double beatInterval = 60.0 / heartRate; // seconds per beat
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / fs;
            
            // P wave, QRS complex, T wave simulation
            double beatTime = t % beatInterval;
            double beatPhase = beatTime / beatInterval * 2 * Math.PI;
            
            // QRS complex (dominant)
            if (beatTime > 0.1 && beatTime < 0.2) {
                double qrsPhase = (beatTime - 0.1) / 0.1 * Math.PI;
                signal[i] += 2.0 * Math.sin(qrsPhase);
            }
            
            // P wave
            if (beatTime > 0.05 && beatTime < 0.1) {
                double pPhase = (beatTime - 0.05) / 0.05 * Math.PI;
                signal[i] += 0.3 * Math.sin(pPhase);
            }
            
            // T wave
            if (beatTime > 0.3 && beatTime < 0.5) {
                double tPhase = (beatTime - 0.3) / 0.2 * Math.PI;
                signal[i] += 0.5 * Math.sin(tPhase);
            }
            
            // Add baseline noise
            signal[i] += 0.05 * (Math.random() - 0.5);
            
            // Add 50 Hz power line interference
            signal[i] += 0.02 * Math.sin(2 * Math.PI * 50 * t);
        }
        
        return signal;
    }
    
    private static double[] createChirpSignal() {
        int N = 512;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            
            // Linear chirp: frequency increases from 5 to 50
            double f0 = 5.0;  // Starting frequency
            double f1 = 50.0; // Ending frequency
            double freq = f0 + (f1 - f0) * t;
            
            // Instantaneous phase
            double phase = 2 * Math.PI * (f0 * t + 0.5 * (f1 - f0) * t * t);
            
            signal[i] = Math.sin(phase);
            
            // Add some noise
            signal[i] += 0.1 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createMultiComponentSignal() {
        int N = 256;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            
            // Low frequency component
            signal[i] += 2.0 * Math.sin(2 * Math.PI * 3 * t);
            
            // Medium frequency component (modulated)
            signal[i] += 1.5 * Math.sin(2 * Math.PI * 15 * t) * Math.exp(-Math.pow(t - 0.5, 2) / 0.1);
            
            // High frequency burst
            if (t > 0.6 && t < 0.8) {
                signal[i] += 1.0 * Math.sin(2 * Math.PI * 40 * t);
            }
            
            // Add noise
            signal[i] += 0.2 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    private static double[] createTestSignal(int size) {
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            double t = (double) i / size;
            
            signal[i] = Math.sin(2 * Math.PI * 10 * t) + 
                       0.5 * Math.sin(2 * Math.PI * 25 * t) +
                       0.2 * (Math.random() - 0.5);
        }
        
        return signal;
    }
    
    // Analysis methods
    
    private static void analyzeECGResults(CWTResult result, ScaleSpace scaleSpace) {
        double[][] coeffs = result.getCoefficients();
        double[] scales = scaleSpace.getScales();
        double[] frequencies = new double[scales.length];
        MorletWavelet morlet = new MorletWavelet();
        for (int i = 0; i < scales.length; i++) {
            frequencies[i] = morlet.centerFrequency() / scales[i]; // Approximate frequency
        }
        
        System.out.println("\nECG Analysis Results:");
        
        // Find dominant frequencies
        double[] frequencyEnergy = new double[frequencies.length];
        for (int f = 0; f < frequencies.length; f++) {
            for (int t = 0; t < coeffs[f].length; t++) {
                frequencyEnergy[f] += coeffs[f][t] * coeffs[f][t];
            }
            frequencyEnergy[f] = Math.sqrt(frequencyEnergy[f]);
        }
        
        // Find peaks in frequency energy
        System.out.println("Dominant frequency components:");
        for (int f = 1; f < frequencies.length - 1; f++) {
            if (frequencyEnergy[f] > frequencyEnergy[f-1] && 
                frequencyEnergy[f] > frequencyEnergy[f+1] &&
                frequencyEnergy[f] > 0.3 * getMaxValue(frequencyEnergy)) {
                
                System.out.printf("  %.1f Hz: Energy = %.2e%n", frequencies[f], frequencyEnergy[f]);
            }
        }
        
        // Detect heart rate
        double heartRateFreq = findHeartRateFrequency(frequencies, frequencyEnergy);
        if (heartRateFreq > 0) {
            double bpm = heartRateFreq * 60;
            System.out.printf("Estimated heart rate: %.1f BPM (%.2f Hz)%n", bpm, heartRateFreq);
        }
    }
    
    private static void analyzeTimeFrequencyRidge(CWTResult result, double[] scales) {
        double[][] coeffs = result.getCoefficients();
        int timePoints = coeffs[0].length;
        
        System.out.println("\nTime-frequency ridge analysis:");
        System.out.println("Time | Scale | Frequency | Energy");
        System.out.println("-----|-------|-----------|-------");
        
        // Sample every 10th time point for readability
        for (int t = 0; t < timePoints; t += timePoints / 10) {
            // Find scale with maximum energy at this time
            int maxScale = 0;
            double maxEnergy = 0;
            
            for (int s = 0; s < scales.length; s++) {
                double energy = coeffs[s][t] * coeffs[s][t];
                if (energy > maxEnergy) {
                    maxEnergy = energy;
                    maxScale = s;
                }
            }
            
            double timeRatio = (double) t / timePoints;
            double frequency = 1.0 / scales[maxScale]; // Approximate frequency
            
            System.out.printf("%4.1f | %5.1f | %9.1f | %6.2e%n", 
                timeRatio, scales[maxScale], frequency, maxEnergy);
        }
    }
    
    private static double[] computeSignalStats(double[][] coeffs) {
        double totalEnergy = 0;
        double maxCoeff = 0;
        
        for (double[] row : coeffs) {
            for (double value : row) {
                totalEnergy += value * value;
                maxCoeff = Math.max(maxCoeff, Math.abs(value));
            }
        }
        
        return new double[]{totalEnergy, maxCoeff};
    }
    
    private static String getConfigFeatures(CWTConfig config) {
        StringBuilder features = new StringBuilder();
        
        if (config.isFFTEnabled()) features.append("FFT ");
        if (config.isNormalizeAcrossScales()) features.append("Norm ");
        if (config.isUseStructuredConcurrency()) features.append("Parallel ");
        if (config.isUseStreamGatherers()) features.append("Stream ");
        
        return features.toString().trim();
    }
    
    private static double getMaxValue(double[] array) {
        double max = array[0];
        for (double value : array) {
            max = Math.max(max, value);
        }
        return max;
    }
    
    private static double findHeartRateFrequency(double[] frequencies, double[] energies) {
        // Look for dominant frequency in the 1-2 Hz range (60-120 BPM)
        double maxEnergy = 0;
        double heartRateFreq = 0;
        
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] >= 1.0 && frequencies[i] <= 2.0 && energies[i] > maxEnergy) {
                maxEnergy = energies[i];
                heartRateFreq = frequencies[i];
            }
        }
        
        return heartRateFreq;
    }
}