package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.PaulWavelet;

/**
 * Comprehensive demonstration of adaptive scale selection strategies in CWT analysis.
 * 
 * <p>Shows different approaches to automatic scale selection:
 * <ul>
 *   <li>Dyadic scale selection for multi-resolution analysis</li>
 *   <li>Signal-adaptive selection based on frequency content</li>
 *   <li>Optimal spacing algorithms (logarithmic, linear, mel-scale)</li>
 *   <li>Wavelet-specific optimizations</li>
 *   <li>Performance and accuracy comparisons</li>
 * </ul>
 * </p>
 */
public class AdaptiveScaleSelectionDemo {
    
    public static void main(String[] args) {
        System.out.println("Adaptive Scale Selection for CWT Analysis");
        System.out.println("=========================================\n");
        
        // Create test signals with different characteristics
        TestSignal[] testSignals = createTestSignals();
        
        for (TestSignal testSignal : testSignals) {
            System.out.println("Analyzing: " + testSignal.name);
            System.out.println("Description: " + testSignal.description);
            System.out.println("Signal length: " + testSignal.signal.length + " samples");
            System.out.println("Sampling rate: " + testSignal.samplingRate + " Hz\n");
            
            // Compare different scale selection strategies
            compareScaleSelectionStrategies(testSignal);
            
            System.out.println("=" + "=".repeat(60) + "\n");
        }
        
        // Demonstrate advanced applications
        demonstrateAdvancedApplications();
    }
    
    private static void compareScaleSelectionStrategies(TestSignal testSignal) {
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        CWTTransform cwt = new CWTTransform(morlet);
        
        System.out.println("Scale Selection Strategy Comparison:");
        System.out.println("-----------------------------------");
        
        // 1. Manual logarithmic scales (baseline)
        double[] manualScales = generateManualScales(2, 128, 40);
        long startTime = System.nanoTime();
        CWTResult manualResult = cwt.analyze(testSignal.signal, manualScales);
        long manualTime = System.nanoTime() - startTime;
        
        System.out.printf("1. Manual Logarithmic (%d scales):\n", manualScales.length);
        System.out.printf("   Computation time: %.2f ms\n", manualTime / 1e6);
        analyzeResult(manualResult, "Manual");
        
        // 2. Dyadic scale selection
        DyadicScaleSelector dyadicSelector = DyadicScaleSelector.create();
        startTime = System.nanoTime();
        double[] dyadicScales = dyadicSelector.selectScales(testSignal.signal, morlet, testSignal.samplingRate);
        CWTResult dyadicResult = cwt.analyze(testSignal.signal, dyadicScales);
        long dyadicTime = System.nanoTime() - startTime;
        
        System.out.printf("\n2. Dyadic Selection (%d scales):\n", dyadicScales.length);
        System.out.printf("   Computation time: %.2f ms\n", dyadicTime / 1e6);
        System.out.printf("   Scale range: %.2f - %.2f\n", dyadicScales[0], dyadicScales[dyadicScales.length-1]);
        analyzeResult(dyadicResult, "Dyadic");
        
        // 3. Signal-adaptive selection
        SignalAdaptiveScaleSelector adaptiveSelector = new SignalAdaptiveScaleSelector();
        startTime = System.nanoTime();
        double[] adaptiveScales = adaptiveSelector.selectScales(testSignal.signal, morlet, testSignal.samplingRate);
        CWTResult adaptiveResult = cwt.analyze(testSignal.signal, adaptiveScales);
        long adaptiveTime = System.nanoTime() - startTime;
        
        System.out.printf("\n3. Signal-Adaptive Selection (%d scales):\n", adaptiveScales.length);
        System.out.printf("   Computation time: %.2f ms\n", adaptiveTime / 1e6);
        System.out.printf("   Scale range: %.2f - %.2f\n", adaptiveScales[0], adaptiveScales[adaptiveScales.length-1]);
        analyzeResult(adaptiveResult, "Adaptive");
        
        // 4. Optimal spacing strategies
        OptimalScaleSelector optimalSelector = OptimalScaleSelector.logarithmic(12);
        
        AdaptiveScaleSelector.ScaleSelectionConfig logConfig = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(testSignal.samplingRate)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.LOGARITHMIC)
                .scalesPerOctave(12)
                .maxScales(50)
                .build();
        
        startTime = System.nanoTime();
        double[] optimalScales = optimalSelector.selectScales(testSignal.signal, morlet, logConfig);
        CWTResult optimalResult = cwt.analyze(testSignal.signal, optimalScales);
        long optimalTime = System.nanoTime() - startTime;
        
        System.out.printf("\n4. Optimal Logarithmic (%d scales):\n", optimalScales.length);
        System.out.printf("   Computation time: %.2f ms\n", optimalTime / 1e6);
        System.out.printf("   Scale range: %.2f - %.2f\n", optimalScales[0], optimalScales[optimalScales.length-1]);
        analyzeResult(optimalResult, "Optimal");
        
        // 5. Mel-scale spacing
        AdaptiveScaleSelector.ScaleSelectionConfig melConfig = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(testSignal.samplingRate)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.MEL_SCALE)
                .scalesPerOctave(10)
                .maxScales(40)
                .build();
        
        startTime = System.nanoTime();
        double[] melScales = optimalSelector.selectScales(testSignal.signal, morlet, melConfig);
        CWTResult melResult = cwt.analyze(testSignal.signal, melScales);
        long melTime = System.nanoTime() - startTime;
        
        System.out.printf("\n5. Mel-Scale Spacing (%d scales):\n", melScales.length);
        System.out.printf("   Computation time: %.2f ms\n", melTime / 1e6);
        System.out.printf("   Scale range: %.2f - %.2f\n", melScales[0], melScales[melScales.length-1]);
        analyzeResult(melResult, "Mel-scale");
        
        // Compare frequency coverage
        compareFrequencyCoverage(testSignal, morlet, manualScales, dyadicScales, adaptiveScales, optimalScales, melScales);
    }
    
    private static void analyzeResult(CWTResult result, String method) {
        double[][] coeffs = result.getCoefficients();
        
        // Calculate energy distribution
        double totalEnergy = 0;
        double maxCoeff = 0;
        
        for (int s = 0; s < coeffs.length; s++) {
            for (int t = 0; t < coeffs[0].length; t++) {
                double mag = Math.abs(coeffs[s][t]);
                totalEnergy += mag * mag;
                maxCoeff = Math.max(maxCoeff, mag);
            }
        }
        
        System.out.printf("   Total energy: %.2e, Max coefficient: %.3f\n", totalEnergy, maxCoeff);
        
        // Find dominant scale
        double maxScaleEnergy = 0;
        int dominantScale = 0;
        
        for (int s = 0; s < coeffs.length; s++) {
            double scaleEnergy = 0;
            for (int t = 0; t < coeffs[0].length; t++) {
                scaleEnergy += coeffs[s][t] * coeffs[s][t];
            }
            if (scaleEnergy > maxScaleEnergy) {
                maxScaleEnergy = scaleEnergy;
                dominantScale = s;
            }
        }
        
        System.out.printf("   Dominant scale index: %d (energy: %.2e)\n", dominantScale, maxScaleEnergy);
    }
    
    private static void compareFrequencyCoverage(TestSignal testSignal, MorletWavelet morlet,
                                               double[]... scaleArrays) {
        System.out.println("\nFrequency Coverage Comparison:");
        System.out.println("-----------------------------");
        
        String[] methods = {"Manual", "Dyadic", "Adaptive", "Optimal", "Mel-scale"};
        
        for (int i = 0; i < scaleArrays.length && i < methods.length; i++) {
            double[] scales = scaleArrays[i];
            double[] freqRange = new OptimalScaleSelector().getFrequencyRange(scales, morlet, testSignal.samplingRate);
            
            System.out.printf("%s: %.1f - %.1f Hz (%.1f octaves, %.1f Hz/scale)\n",
                methods[i],
                freqRange[0], freqRange[1],
                Math.log(freqRange[1] / freqRange[0]) / Math.log(2),
                (freqRange[1] - freqRange[0]) / scales.length);
        }
    }
    
    private static void demonstrateAdvancedApplications() {
        System.out.println("Advanced Applications");
        System.out.println("====================\n");
        
        // 1. Financial data analysis with Paul wavelet
        demonstrateFinancialAnalysis();
        
        // 2. Multi-component signal analysis
        demonstrateMultiComponentAnalysis();
        
        // 3. Real-time adaptive selection
        demonstrateRealTimeAdaptation();
    }
    
    private static void demonstrateFinancialAnalysis() {
        System.out.println("1. Financial Data Analysis:");
        System.out.println("   Using Paul wavelet with adaptive scale selection for market analysis");
        
        // Create synthetic high-frequency trading data
        double[] prices = createFinancialData(2000, 1000); // 2000 samples, $1000 initial price
        double[] returns = calculateReturns(prices);
        
        // Use Paul wavelet (asymmetric, good for financial analysis)
        PaulWavelet paul = new PaulWavelet(4);
        CWTTransform cwt = new CWTTransform(paul);
        
        // Configure for financial time scales
        AdaptiveScaleSelector.ScaleSelectionConfig financialConfig = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(1.0) // 1 Hz (1 sample per second)
                .frequencyRange(1.0/3600, 1.0/10) // 1 hour to 10 seconds
                .spacing(AdaptiveScaleSelector.ScaleSpacing.LOGARITHMIC)
                .scalesPerOctave(8)
                .useSignalAdaptation(true)
                .build();
        
        SignalAdaptiveScaleSelector financialSelector = new SignalAdaptiveScaleSelector();
        double[] financialScales = financialSelector.selectScales(returns, paul, financialConfig);
        
        CWTResult financialResult = cwt.analyze(returns, financialScales);
        
        System.out.printf("   Generated %d scales for financial analysis\n", financialScales.length);
        System.out.printf("   Scale range: %.1f - %.1f (time periods: %.0fs - %.0fmin)\n",
            financialScales[0], financialScales[financialScales.length-1],
            financialScales[0], financialScales[financialScales.length-1] / 60);
        
        // Detect volatility clusters
        detectVolatilityClusters(financialResult);
        System.out.println();
    }
    
    private static void demonstrateMultiComponentAnalysis() {
        System.out.println("2. Multi-Component Signal Analysis:");
        System.out.println("   Adaptive selection for signals with multiple frequency components");
        
        // Create multi-component signal
        int N = 1024;
        double samplingRate = 100; // 100 Hz
        double[] signal = new double[N];
        
        // Component 1: Low frequency (2 Hz)
        // Component 2: Medium frequency (10 Hz)
        // Component 3: High frequency (35 Hz)
        // Component 4: Chirp (5-20 Hz)
        
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            signal[i] = 0.8 * Math.sin(2 * Math.PI * 2 * t) +
                       0.6 * Math.cos(2 * Math.PI * 10 * t) +
                       0.4 * Math.sin(2 * Math.PI * 35 * t) +
                       0.3 * Math.cos(2 * Math.PI * (5 + 15 * t / (N/samplingRate)) * t);
        }
        
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        
        // Compare different selectors
        DyadicScaleSelector dyadicSelector = new DyadicScaleSelector();
        SignalAdaptiveScaleSelector adaptiveSelector = new SignalAdaptiveScaleSelector();
        
        double[] dyadicScales = dyadicSelector.selectScales(signal, morlet, samplingRate);
        double[] adaptiveScales = adaptiveSelector.selectScales(signal, morlet, samplingRate);
        
        System.out.printf("   Dyadic selector: %d scales\n", dyadicScales.length);
        System.out.printf("   Adaptive selector: %d scales\n", adaptiveScales.length);
        
        // Check how well each captures the known frequencies
        double[] knownFreqs = {2, 10, 35}; // Excluding chirp
        evaluateFrequencyCapture(dyadicScales, adaptiveScales, knownFreqs, morlet, samplingRate);
        System.out.println();
    }
    
    private static void demonstrateRealTimeAdaptation() {
        System.out.println("3. Real-Time Adaptive Selection:");
        System.out.println("   Simulating streaming data with changing characteristics");
        
        // Simulate streaming scenario
        int blockSize = 256;
        int numBlocks = 8;
        double samplingRate = 200; // 200 Hz
        
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        SignalAdaptiveScaleSelector adaptiveSelector = new SignalAdaptiveScaleSelector();
        
        System.out.println("   Processing streaming blocks:");
        
        for (int block = 0; block < numBlocks; block++) {
            // Create block with changing characteristics
            double[] blockData = createEvolvingSignalBlock(blockSize, block, samplingRate);
            
            long startTime = System.nanoTime();
            double[] adaptiveScales = adaptiveSelector.selectScales(blockData, morlet, samplingRate);
            long adaptationTime = System.nanoTime() - startTime;
            
            // Analyze frequency content of this block
            double dominantFreq = estimateDominantFrequency(blockData, samplingRate);
            
            System.out.printf("   Block %d: %d scales, dominant freq: %.1f Hz, adaptation time: %.1f μs\n",
                block + 1, adaptiveScales.length, dominantFreq, adaptationTime / 1000.0);
        }
        
        System.out.println("   Adaptive selection enables real-time frequency tracking\n");
    }
    
    // Helper methods
    
    private static TestSignal[] createTestSignals() {
        return new TestSignal[]{
            createChirpSignal(),
            createMultiToneSignal(),
            createNoisySignal(),
            createTransientSignal()
        };
    }
    
    private static TestSignal createChirpSignal() {
        int N = 512;
        double samplingRate = 100;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            double freq = 5 + 20 * t / (N / samplingRate); // 5-25 Hz chirp
            signal[i] = Math.cos(2 * Math.PI * freq * t);
        }
        
        return new TestSignal("Linear Chirp", 
            "Frequency increases linearly from 5 to 25 Hz", 
            signal, samplingRate);
    }
    
    private static TestSignal createMultiToneSignal() {
        int N = 512;
        double samplingRate = 100;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            signal[i] = 0.6 * Math.sin(2 * Math.PI * 8 * t) +
                       0.4 * Math.cos(2 * Math.PI * 15 * t) +
                       0.3 * Math.sin(2 * Math.PI * 25 * t) +
                       0.2 * Math.cos(2 * Math.PI * 40 * t);
        }
        
        return new TestSignal("Multi-Tone Signal",
            "Multiple discrete frequency components (8, 15, 25, 40 Hz)",
            signal, samplingRate);
    }
    
    private static TestSignal createNoisySignal() {
        int N = 512;
        double samplingRate = 100;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            double cleanSignal = Math.sin(2 * Math.PI * 12 * t);
            double noise = 0.3 * (Math.random() - 0.5);
            signal[i] = cleanSignal + noise;
        }
        
        return new TestSignal("Noisy Sinusoid",
            "12 Hz sinusoid with additive white noise (SNR ≈ 5 dB)",
            signal, samplingRate);
    }
    
    private static TestSignal createTransientSignal() {
        int N = 512;
        double samplingRate = 100;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            
            // Base oscillation
            signal[i] = 0.5 * Math.sin(2 * Math.PI * 10 * t);
            
            // Add transients
            if (i >= N/4 && i <= N/4 + 20) {
                signal[i] += 2.0 * Math.exp(-(i - N/4) * 0.1) * Math.sin(2 * Math.PI * 30 * t);
            }
            if (i >= 3*N/4 && i <= 3*N/4 + 30) {
                signal[i] += 1.5 * Math.exp(-(i - 3*N/4) * 0.05) * Math.cos(2 * Math.PI * 45 * t);
            }
        }
        
        return new TestSignal("Transient Signal",
            "10 Hz base with high-frequency transients",
            signal, samplingRate);
    }
    
    private static double[] generateManualScales(double min, double max, int count) {
        double[] scales = new double[count];
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        
        for (int i = 0; i < count; i++) {
            double logScale = logMin + (logMax - logMin) * i / (count - 1);
            scales[i] = Math.exp(logScale);
        }
        
        return scales;
    }
    
    private static double[] createFinancialData(int N, double initialPrice) {
        double[] prices = new double[N];
        prices[0] = initialPrice;
        
        for (int i = 1; i < N; i++) {
            double drift = 0.0001;
            double volatility = 0.02;
            
            // Add volatility clustering
            if (i > N/3 && i < 2*N/3) {
                volatility *= 2.0; // High volatility period
            }
            
            double random = Math.random() - 0.5;
            double return_ = drift + volatility * random;
            
            prices[i] = prices[i-1] * (1 + return_);
        }
        
        return prices;
    }
    
    private static double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = Math.log(prices[i + 1] / prices[i]);
        }
        return returns;
    }
    
    private static void detectVolatilityClusters(CWTResult result) {
        double[][] coeffs = result.getCoefficients();
        double[] scales = result.getScales();
        
        // Look at medium-term scales (around index 10-15)
        int scaleIdx = Math.min(10, coeffs.length - 1);
        double[] scaleCoeffs = coeffs[scaleIdx];
        
        // Detect periods of high activity
        double meanActivity = 0;
        for (double coeff : scaleCoeffs) {
            meanActivity += Math.abs(coeff);
        }
        meanActivity /= scaleCoeffs.length;
        
        int clusters = 0;
        boolean inCluster = false;
        
        for (double coeff : scaleCoeffs) {
            if (Math.abs(coeff) > 2 * meanActivity) {
                if (!inCluster) {
                    clusters++;
                    inCluster = true;
                }
            } else {
                inCluster = false;
            }
        }
        
        System.out.printf("   Detected %d volatility clusters at scale %.1f\n", clusters, scales[scaleIdx]);
    }
    
    private static void evaluateFrequencyCapture(double[] dyadicScales, double[] adaptiveScales,
                                               double[] knownFreqs, MorletWavelet morlet, double samplingRate) {
        System.out.println("   Frequency capture evaluation:");
        
        double centerFreq = morlet.centerFrequency();
        
        for (double freq : knownFreqs) {
            double targetScale = centerFreq * samplingRate / freq;
            
            // Find closest scales
            double dyadicDist = findClosestScaleDistance(dyadicScales, targetScale);
            double adaptiveDist = findClosestScaleDistance(adaptiveScales, targetScale);
            
            System.out.printf("   %.0f Hz: Dyadic error=%.1f%%, Adaptive error=%.1f%%\n",
                freq, 
                100 * dyadicDist / targetScale,
                100 * adaptiveDist / targetScale);
        }
    }
    
    private static double findClosestScaleDistance(double[] scales, double targetScale) {
        double minDistance = Double.MAX_VALUE;
        
        for (double scale : scales) {
            double distance = Math.abs(scale - targetScale);
            minDistance = Math.min(minDistance, distance);
        }
        
        return minDistance;
    }
    
    private static double[] createEvolvingSignalBlock(int blockSize, int blockIndex, double samplingRate) {
        double[] block = new double[blockSize];
        
        // Frequency evolves over blocks
        double baseFreq = 10 + 5 * Math.sin(2 * Math.PI * blockIndex / 8.0);
        
        for (int i = 0; i < blockSize; i++) {
            double t = i / samplingRate;
            block[i] = Math.sin(2 * Math.PI * baseFreq * t);
            
            // Add some harmonics that vary
            if (blockIndex % 3 == 0) {
                block[i] += 0.3 * Math.cos(2 * Math.PI * 2 * baseFreq * t);
            }
            if (blockIndex % 4 == 0) {
                block[i] += 0.2 * Math.sin(2 * Math.PI * 3 * baseFreq * t);
            }
        }
        
        return block;
    }
    
    private static double estimateDominantFrequency(double[] signal, double samplingRate) {
        // Simple spectral peak detection
        int N = signal.length;
        double maxPower = 0;
        int maxIndex = 0;
        
        // Compute power at different frequencies
        for (int k = 1; k < N/2; k++) {
            double real = 0, imag = 0;
            
            for (int n = 0; n < N; n++) {
                double angle = -2 * Math.PI * k * n / N;
                real += signal[n] * Math.cos(angle);
                imag += signal[n] * Math.sin(angle);
            }
            
            double power = real * real + imag * imag;
            if (power > maxPower) {
                maxPower = power;
                maxIndex = k;
            }
        }
        
        return maxIndex * samplingRate / N;
    }
    
    private static class TestSignal {
        final String name;
        final String description;
        final double[] signal;
        final double samplingRate;
        
        TestSignal(String name, String description, double[] signal, double samplingRate) {
            this.name = name;
            this.description = description;
            this.signal = signal;
            this.samplingRate = samplingRate;
        }
    }
}