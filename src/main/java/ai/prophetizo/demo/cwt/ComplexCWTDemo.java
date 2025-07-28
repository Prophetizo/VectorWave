package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;

/**
 * Demonstrates complex wavelet transform capabilities including
 * phase analysis and instantaneous frequency extraction.
 */
public class ComplexCWTDemo {
    
    public static void main(String[] args) {
        System.out.println("Complex CWT Analysis Demo");
        System.out.println("========================\n");
        
        // Create test signal with varying frequency
        int N = 512;
        double[] signal = createChirpSignal(N);
        
        // Setup wavelet and scales
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        double[] scales = generateScales(2, 64, 32);
        
        System.out.println("Signal: Linear chirp (frequency increases linearly)");
        System.out.println("Wavelet: Morlet (ω₀=6.0, σ=1.0)");
        System.out.printf("Scales: %.1f to %.1f (%d scales)\n\n", 
            scales[0], scales[scales.length-1], scales.length);
        
        // Perform complex CWT
        CWTTransform cwt = new CWTTransform(morlet);
        ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);
        
        // Extract various representations
        demonstrateMagnitudeAnalysis(complexResult);
        demonstratePhaseAnalysis(complexResult);
        demonstrateInstantaneousFrequency(complexResult);
        demonstrateRidgeExtraction(complexResult);
        
        // Compare with real-only analysis
        compareWithRealAnalysis(signal, scales, cwt);
        
        // Financial application example
        demonstrateFinancialApplication();
    }
    
    private static void demonstrateMagnitudeAnalysis(ComplexCWTResult result) {
        System.out.println("1. Magnitude Analysis:");
        System.out.println("   - Represents signal energy at each scale/time");
        
        double[][] magnitude = result.getMagnitude();
        
        // Find maximum coefficient
        double maxMag = 0;
        int maxScale = 0, maxTime = 0;
        
        for (int s = 0; s < result.getNumScales(); s++) {
            for (int t = 0; t < result.getNumSamples(); t++) {
                if (magnitude[s][t] > maxMag) {
                    maxMag = magnitude[s][t];
                    maxScale = s;
                    maxTime = t;
                }
            }
        }
        
        System.out.printf("   - Peak magnitude: %.3f at scale index %d, time %d\n", 
            maxMag, maxScale, maxTime);
        System.out.println("   - Use for: Energy distribution, feature detection\n");
    }
    
    private static void demonstratePhaseAnalysis(ComplexCWTResult result) {
        System.out.println("2. Phase Analysis:");
        System.out.println("   - Captures oscillation timing and synchronization");
        
        double[][] phase = result.getPhase();
        
        // Analyze phase at a specific scale
        int scaleIdx = 10;
        double[] phaseAtScale = phase[scaleIdx];
        
        // Calculate phase coherence (simplified)
        double coherence = calculatePhaseCoherence(phaseAtScale);
        
        System.out.printf("   - Phase coherence at scale %d: %.3f\n", scaleIdx, coherence);
        System.out.println("   - Use for: Synchronization analysis, mode coupling\n");
    }
    
    private static void demonstrateInstantaneousFrequency(ComplexCWTResult result) {
        System.out.println("3. Instantaneous Frequency:");
        System.out.println("   - Tracks frequency changes over time");
        
        double[][] instFreq = result.getInstantaneousFrequency();
        
        // Sample instantaneous frequency at middle scale
        int midScale = result.getNumScales() / 2;
        int sampleTime = result.getNumSamples() / 4;
        
        System.out.printf("   - Inst. frequency at scale %d, time %d: %.3f\n",
            midScale, sampleTime, instFreq[midScale][sampleTime]);
        
        // Show frequency progression
        System.out.print("   - Frequency progression: ");
        for (int t = 0; t < result.getNumSamples() - 1; t += 50) {
            System.out.printf("%.2f ", instFreq[midScale][t]);
        }
        System.out.println("\n   - Use for: Chirp detection, frequency modulation analysis\n");
    }
    
    private static void demonstrateRidgeExtraction(ComplexCWTResult result) {
        System.out.println("4. Ridge Extraction:");
        System.out.println("   - Identifies dominant frequency components");
        
        double[][] magnitude = result.getMagnitude();
        int[] ridge = new int[result.getNumSamples()];
        
        // Simple ridge extraction - find max magnitude at each time
        for (int t = 0; t < result.getNumSamples(); t++) {
            double maxMag = 0;
            int maxScale = 0;
            
            for (int s = 0; s < result.getNumScales(); s++) {
                if (magnitude[s][t] > maxMag) {
                    maxMag = magnitude[s][t];
                    maxScale = s;
                }
            }
            ridge[t] = maxScale;
        }
        
        // Show ridge progression
        System.out.print("   - Ridge scale indices: ");
        for (int t = 0; t < result.getNumSamples(); t += 50) {
            System.out.printf("%d ", ridge[t]);
        }
        System.out.println("\n   - Use for: Dominant frequency tracking, signal decomposition\n");
    }
    
    private static void compareWithRealAnalysis(double[] signal, double[] scales, CWTTransform cwt) {
        System.out.println("5. Complex vs Real CWT Comparison:");
        
        // Real CWT (magnitude only)
        CWTResult realResult = cwt.analyze(signal, scales);
        
        // Complex CWT
        ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);
        
        System.out.println("   - Real CWT: Magnitude information only");
        System.out.println("   - Complex CWT: Magnitude + Phase information");
        System.out.println("   - Complex provides richer signal representation");
        System.out.println("   - Essential for phase-dependent applications\n");
    }
    
    private static void demonstrateFinancialApplication() {
        System.out.println("6. Financial Application - Market Phase Analysis:");
        
        // Create synthetic market data with regime changes
        int N = 500;
        double[] prices = createMarketData(N);
        double[] returns = calculateReturns(prices);
        
        // Analyze with complex Morlet
        MorletWavelet morlet = new MorletWavelet(5.0, 1.5);
        CWTTransform cwt = new CWTTransform(morlet);
        double[] scales = generateScales(5, 50, 20); // Trading periods
        
        ComplexCWTResult result = cwt.analyzeComplex(returns, scales);
        
        // Extract phase information
        double[][] phase = result.getPhase();
        double[][] magnitude = result.getMagnitude();
        
        // Detect phase transitions (simplified)
        int transitionCount = 0;
        int scaleIdx = 10; // Medium-term cycles
        
        for (int t = 1; t < N - 1; t++) {
            double phaseDiff = phase[scaleIdx][t] - phase[scaleIdx][t-1];
            if (Math.abs(phaseDiff) > Math.PI) { // Phase wrap
                transitionCount++;
            }
        }
        
        System.out.printf("   - Detected %d phase transitions in market cycles\n", transitionCount);
        System.out.println("   - Phase analysis reveals market regime changes");
        System.out.println("   - Magnitude shows volatility clustering");
        System.out.println("   - Combined: Complete market microstructure view\n");
    }
    
    // Helper methods
    
    private static double[] createChirpSignal(int N) {
        double[] signal = new double[N];
        double f0 = 0.1;  // Starting frequency
        double f1 = 0.4;  // Ending frequency
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            double freq = f0 + (f1 - f0) * t;
            signal[i] = Math.cos(2 * Math.PI * freq * i);
        }
        
        return signal;
    }
    
    private static double[] createMarketData(int N) {
        double[] prices = new double[N];
        prices[0] = 100;
        
        for (int i = 1; i < N; i++) {
            double trend = 0.0001 * i;
            double cycle = 0.02 * Math.sin(2 * Math.PI * i / 20);
            double regime = i > N/2 ? 0.02 : 0.01; // Volatility regime change
            double noise = regime * (Math.random() - 0.5);
            
            prices[i] = prices[i-1] * (1 + trend + cycle + noise);
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
    
    private static double[] generateScales(double min, double max, int count) {
        double[] scales = new double[count];
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        
        for (int i = 0; i < count; i++) {
            double logScale = logMin + (logMax - logMin) * i / (count - 1);
            scales[i] = Math.exp(logScale);
        }
        
        return scales;
    }
    
    private static double calculatePhaseCoherence(double[] phase) {
        // Simple phase coherence measure
        double sumCos = 0, sumSin = 0;
        
        for (double p : phase) {
            sumCos += Math.cos(p);
            sumSin += Math.sin(p);
        }
        
        double meanCos = sumCos / phase.length;
        double meanSin = sumSin / phase.length;
        
        return Math.sqrt(meanCos * meanCos + meanSin * meanSin);
    }
}