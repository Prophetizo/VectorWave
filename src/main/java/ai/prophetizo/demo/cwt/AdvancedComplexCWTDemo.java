package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;

/**
 * Advanced demonstration of complex wavelet analysis with practical applications.
 * Shows phase-based analysis, coherence, and financial market applications.
 */
public class AdvancedComplexCWTDemo {
    
    public static void main(String[] args) {
        System.out.println("Advanced Complex CWT Analysis Demo");
        System.out.println("==================================\n");
        
        // 1. Phase synchronization between coupled signals
        demonstratePhaseSynchronization();
        
        // 2. Financial market microstructure analysis
        demonstrateMarketMicrostructure();
        
        // 3. Signal decomposition using phase information
        demonstratePhaseBasedDecomposition();
        
        // 4. Wavelet ridge extraction and reconstruction
        demonstrateRidgeAnalysis();
        
        // 5. Cross-wavelet analysis (simplified)
        demonstrateCrossWaveletAnalysis();
    }
    
    private static void demonstratePhaseSynchronization() {
        System.out.println("1. Phase Synchronization Analysis");
        System.out.println("---------------------------------");
        
        int N = 1024;
        double dt = 0.01;
        
        // Create two coupled oscillators with varying coupling strength
        double[] signal1 = new double[N];
        double[] signal2 = new double[N];
        
        double freq1 = 10.0; // Hz
        double freq2 = 11.0; // Hz (slightly different)
        double couplingStrength = 0.3;
        
        for (int i = 0; i < N; i++) {
            double t = i * dt;
            signal1[i] = Math.cos(2 * Math.PI * freq1 * t);
            
            // Second signal influenced by first (phase coupling)
            double phase2 = 2 * Math.PI * freq2 * t + couplingStrength * Math.sin(2 * Math.PI * freq1 * t);
            signal2[i] = Math.cos(phase2);
        }
        
        // Analyze with complex wavelets
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        CWTTransform cwt = new CWTTransform(wavelet);
        double[] scales = generateScales(0.5, 2.0, 20); // Focus on oscillation scales
        
        ComplexCWTResult result1 = cwt.analyzeComplex(signal1, scales);
        ComplexCWTResult result2 = cwt.analyzeComplex(signal2, scales);
        
        // Calculate phase difference
        double[][] phase1 = result1.getPhase();
        double[][] phase2 = result2.getPhase();
        
        // Find scale with maximum energy (dominant frequency)
        int dominantScale = findDominantScale(result1.getMagnitude());
        
        // Calculate phase synchronization index
        double syncIndex = calculatePhaseSynchronization(
            phase1[dominantScale], phase2[dominantScale]
        );
        
        System.out.printf("Coupling strength: %.1f\n", couplingStrength);
        System.out.printf("Phase synchronization index: %.3f\n", syncIndex);
        System.out.println("Interpretation: Values close to 1 indicate strong phase locking");
        System.out.println();
    }
    
    private static void demonstrateMarketMicrostructure() {
        System.out.println("2. Market Microstructure Analysis");
        System.out.println("---------------------------------");
        
        // Create synthetic high-frequency trading data
        int N = 2000;
        double[] prices = createHFTData(N);
        double[] returns = calculateReturns(prices);
        
        // Use Paul wavelet for sharp transitions
        PaulWavelet paul = new PaulWavelet(4);
        CWTTransform cwt = new CWTTransform(paul);
        
        // Scales corresponding to different trading frequencies
        // 1-5: Ultra high frequency (< 1 second)
        // 5-20: High frequency (1-10 seconds)
        // 20-100: Low frequency (10 seconds - 1 minute)
        double[] scales = generateLogScales(1, 100, 50);
        
        ComplexCWTResult result = cwt.analyzeComplex(returns, scales);
        
        // Analyze different frequency bands
        analyzeTradingFrequencies(result);
        
        // Detect liquidity events using phase jumps
        int[] liquidityEvents = detectLiquidityEvents(result);
        System.out.printf("Detected %d liquidity events\n", liquidityEvents.length);
        
        // Calculate market efficiency at different scales
        calculateMarketEfficiency(result);
        System.out.println();
    }
    
    private static void demonstratePhaseBasedDecomposition() {
        System.out.println("3. Phase-Based Signal Decomposition");
        System.out.println("-----------------------------------");
        
        // Create multi-component signal
        int N = 512;
        double[] signal = new double[N];
        
        // Component 1: Low frequency trend
        double[] component1 = new double[N];
        // Component 2: Medium frequency oscillation
        double[] component2 = new double[N];
        // Component 3: High frequency + amplitude modulation
        double[] component3 = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            component1[i] = 0.5 * Math.sin(2 * Math.PI * 2 * t);
            component2[i] = 0.3 * Math.cos(2 * Math.PI * 10 * t);
            component3[i] = 0.2 * (1 + 0.5 * Math.sin(2 * Math.PI * t)) * 
                           Math.sin(2 * Math.PI * 50 * t);
            
            signal[i] = component1[i] + component2[i] + component3[i];
        }
        
        // Analyze with complex Morlet
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        CWTTransform cwt = new CWTTransform(morlet);
        double[] scales = generateScales(2, 100, 50);
        
        ComplexCWTResult result = cwt.analyzeComplex(signal, scales);
        
        // Decompose using phase information
        double[][] decomposed = decomposeUsingPhase(result, signal);
        
        System.out.println("Signal decomposition using phase-locked reconstruction:");
        System.out.printf("Component 1 (trend): Energy = %.3f\n", 
            calculateEnergy(decomposed[0]));
        System.out.printf("Component 2 (medium): Energy = %.3f\n", 
            calculateEnergy(decomposed[1]));
        System.out.printf("Component 3 (high): Energy = %.3f\n", 
            calculateEnergy(decomposed[2]));
        
        // Verify reconstruction
        double reconstructionError = calculateReconstructionError(
            signal, decomposed
        );
        System.out.printf("Reconstruction error: %.1f%%\n", 
            reconstructionError * 100);
        System.out.println();
    }
    
    private static void demonstrateRidgeAnalysis() {
        System.out.println("4. Wavelet Ridge Analysis");
        System.out.println("-------------------------");
        
        // Create signal with time-varying frequency
        int N = 1024;
        double[] signal = new double[N];
        double[] trueInstFreq = new double[N];
        
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            // Quadratic chirp
            double freq = 5 + 20 * t * t;
            trueInstFreq[i] = freq;
            
            double phase = 2 * Math.PI * (5 * t + 20 * t * t * t / 3);
            signal[i] = Math.cos(phase);
        }
        
        // High-resolution CWT
        MorletWavelet morlet = new MorletWavelet(10.0, 1.0); // Higher omega0 for better frequency resolution
        CWTTransform cwt = new CWTTransform(morlet);
        double[] scales = generateScales(1, 50, 100);
        
        ComplexCWTResult result = cwt.analyzeComplex(signal, scales);
        
        // Extract ridge
        Ridge ridge = extractRidge(result);
        
        System.out.println("Ridge extraction results:");
        System.out.printf("Number of ridge points: %d\n", ridge.length);
        
        // Convert ridge scales to frequencies
        double[] ridgeFrequencies = new double[ridge.length];
        for (int i = 0; i < ridge.length; i++) {
            // Approximate frequency from scale
            ridgeFrequencies[i] = morlet.centerFrequency() * N / scales[ridge.scaleIndices[i]];
        }
        
        // Compare with true instantaneous frequency
        double avgError = 0;
        for (int i = N/4; i < 3*N/4; i++) { // Middle portion
            avgError += Math.abs(ridgeFrequencies[i] - trueInstFreq[i]);
        }
        avgError /= (N/2);
        
        System.out.printf("Average frequency estimation error: %.2f Hz\n", avgError);
        
        // Reconstruct from ridge
        double[] ridgeReconstruction = reconstructFromRidge(result, ridge);
        double ridgeEnergy = calculateEnergy(ridgeReconstruction) / calculateEnergy(signal);
        System.out.printf("Ridge captures %.1f%% of signal energy\n", ridgeEnergy * 100);
        System.out.println();
    }
    
    private static void demonstrateCrossWaveletAnalysis() {
        System.out.println("5. Cross-Wavelet Analysis");
        System.out.println("-------------------------");
        
        // Create two related financial time series
        int N = 1000;
        double[] stockA = createStockPrice(N, 100, 0.02, 0.15);
        double[] stockB = createCorrelatedStock(stockA, 0.7, 0.1);
        
        double[] returnsA = calculateReturns(stockA);
        double[] returnsB = calculateReturns(stockB);
        
        // Complex wavelet analysis
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        CWTTransform cwt = new CWTTransform(morlet);
        double[] scales = generateScales(5, 100, 30);
        
        ComplexCWTResult resultA = cwt.analyzeComplex(returnsA, scales);
        ComplexCWTResult resultB = cwt.analyzeComplex(returnsB, scales);
        
        // Compute cross-wavelet spectrum
        ComplexNumber[][] crossSpectrum = computeCrossSpectrum(resultA, resultB);
        
        // Wavelet coherence
        double[][] coherence = computeWaveletCoherence(crossSpectrum, resultA, resultB);
        
        // Find scales with high coherence
        System.out.println("Wavelet coherence analysis:");
        for (int s = 0; s < scales.length; s += 5) {
            double avgCoherence = 0;
            for (int t = 0; t < N - 1; t++) {
                avgCoherence += coherence[s][t];
            }
            avgCoherence /= (N - 1);
            
            if (avgCoherence > 0.7) {
                System.out.printf("High coherence at scale %.1f: %.3f\n", 
                    scales[s], avgCoherence);
            }
        }
        
        // Phase difference analysis
        double[][] phaseDiff = computePhaseDifference(resultA, resultB);
        analyzeLeadLagRelationship(phaseDiff, scales);
        System.out.println();
    }
    
    // Helper methods
    
    private static double[] generateScales(double min, double max, int count) {
        double[] scales = new double[count];
        for (int i = 0; i < count; i++) {
            scales[i] = min + (max - min) * i / (count - 1);
        }
        return scales;
    }
    
    private static double[] generateLogScales(double min, double max, int count) {
        double[] scales = new double[count];
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        
        for (int i = 0; i < count; i++) {
            double logScale = logMin + (logMax - logMin) * i / (count - 1);
            scales[i] = Math.exp(logScale);
        }
        return scales;
    }
    
    private static int findDominantScale(double[][] magnitude) {
        double maxEnergy = 0;
        int dominantScale = 0;
        
        for (int s = 0; s < magnitude.length; s++) {
            double energy = 0;
            for (int t = 0; t < magnitude[0].length; t++) {
                energy += magnitude[s][t] * magnitude[s][t];
            }
            if (energy > maxEnergy) {
                maxEnergy = energy;
                dominantScale = s;
            }
        }
        
        return dominantScale;
    }
    
    private static double calculatePhaseSynchronization(double[] phase1, double[] phase2) {
        // Phase locking value (PLV)
        double sumCos = 0, sumSin = 0;
        
        for (int i = 0; i < phase1.length; i++) {
            double phaseDiff = phase1[i] - phase2[i];
            sumCos += Math.cos(phaseDiff);
            sumSin += Math.sin(phaseDiff);
        }
        
        sumCos /= phase1.length;
        sumSin /= phase1.length;
        
        return Math.sqrt(sumCos * sumCos + sumSin * sumSin);
    }
    
    private static double[] createHFTData(int N) {
        double[] prices = new double[N];
        prices[0] = 100;
        
        for (int i = 1; i < N; i++) {
            // Base trend
            double trend = 0.00001 * i;
            
            // Market maker quotes (high frequency)
            double hf = 0.001 * Math.sin(2 * Math.PI * i / 5);
            
            // Liquidity events
            double event = 0;
            if (Math.random() < 0.01) { // 1% chance
                event = 0.005 * (Math.random() - 0.5);
            }
            
            // Microstructure noise
            double noise = 0.0001 * (Math.random() - 0.5);
            
            prices[i] = prices[i-1] * (1 + trend + hf + event + noise);
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
    
    private static void analyzeTradingFrequencies(ComplexCWTResult result) {
        double[][] magnitude = result.getMagnitude();
        double[] scales = result.getScales();
        
        System.out.println("Trading frequency analysis:");
        
        // Define frequency bands
        double[] bandLimits = {1, 5, 20, 100};
        String[] bandNames = {"Ultra-HF", "High-freq", "Low-freq"};
        
        for (int b = 0; b < bandNames.length; b++) {
            double bandEnergy = 0;
            int count = 0;
            
            for (int s = 0; s < scales.length; s++) {
                if (scales[s] >= bandLimits[b] && scales[s] < bandLimits[b + 1]) {
                    for (int t = 0; t < magnitude[0].length; t++) {
                        bandEnergy += magnitude[s][t] * magnitude[s][t];
                    }
                    count++;
                }
            }
            
            if (count > 0) {
                bandEnergy /= count;
                System.out.printf("%s: Relative energy = %.3f\n", 
                    bandNames[b], bandEnergy);
            }
        }
    }
    
    private static int[] detectLiquidityEvents(ComplexCWTResult result) {
        double[][] phase = result.getPhase();
        java.util.List<Integer> events = new java.util.ArrayList<>();
        
        // Look for phase discontinuities
        for (int t = 1; t < phase[0].length - 1; t++) {
            int jumps = 0;
            for (int s = 0; s < phase.length / 2; s++) { // Focus on high frequencies
                double phaseDiff = phase[s][t] - phase[s][t-1];
                // Unwrap phase
                if (phaseDiff > Math.PI) phaseDiff -= 2 * Math.PI;
                if (phaseDiff < -Math.PI) phaseDiff += 2 * Math.PI;
                
                if (Math.abs(phaseDiff) > Math.PI / 2) {
                    jumps++;
                }
            }
            
            if (jumps > phase.length / 4) {
                events.add(t);
            }
        }
        
        return events.stream().mapToInt(i -> i).toArray();
    }
    
    private static void calculateMarketEfficiency(ComplexCWTResult result) {
        double[][] instFreq = result.getInstantaneousFrequency();
        
        System.out.println("Market efficiency by time scale:");
        
        int[] scaleIndices = {5, 10, 20}; // Different time scales
        String[] scaleNames = {"1-sec", "10-sec", "1-min"};
        
        for (int i = 0; i < scaleIndices.length; i++) {
            int s = scaleIndices[i];
            if (s < instFreq.length) {
                // Efficiency measured as stability of instantaneous frequency
                double variance = 0;
                double mean = 0;
                
                for (int t = 0; t < instFreq[0].length; t++) {
                    mean += instFreq[s][t];
                }
                mean /= instFreq[0].length;
                
                for (int t = 0; t < instFreq[0].length; t++) {
                    variance += Math.pow(instFreq[s][t] - mean, 2);
                }
                variance /= instFreq[0].length;
                
                double efficiency = 1.0 / (1.0 + Math.sqrt(variance));
                System.out.printf("%s scale: Efficiency = %.3f\n", 
                    scaleNames[i], efficiency);
            }
        }
    }
    
    private static double[][] decomposeUsingPhase(ComplexCWTResult result, double[] signal) {
        // Simplified phase-based decomposition
        double[][] components = new double[3][signal.length];
        ComplexNumber[][] coeffs = result.getCoefficients();
        double[] scales = result.getScales();
        
        // Define scale bands for components
        int[] bands = {0, scales.length/3, 2*scales.length/3, scales.length};
        
        // Reconstruct each band
        for (int band = 0; band < 3; band++) {
            for (int t = 0; t < signal.length; t++) {
                double sum = 0;
                
                for (int s = bands[band]; s < bands[band + 1]; s++) {
                    // Use complex coefficients for reconstruction
                    ComplexNumber c = coeffs[s][t];
                    sum += c.real() / Math.sqrt(scales[s]);
                }
                
                components[band][t] = sum * 2.0 / (bands[band + 1] - bands[band]);
            }
        }
        
        return components;
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double s : signal) {
            energy += s * s;
        }
        return Math.sqrt(energy / signal.length);
    }
    
    private static double calculateReconstructionError(double[] original, double[][] components) {
        double error = 0;
        
        for (int i = 0; i < original.length; i++) {
            double reconstructed = 0;
            for (int c = 0; c < components.length; c++) {
                reconstructed += components[c][i];
            }
            error += Math.pow(original[i] - reconstructed, 2);
        }
        
        return Math.sqrt(error / original.length) / calculateEnergy(original);
    }
    
    private static class Ridge {
        int[] scaleIndices;
        double[] magnitudes;
        int length;
        
        Ridge(int length) {
            this.length = length;
            this.scaleIndices = new int[length];
            this.magnitudes = new double[length];
        }
    }
    
    private static Ridge extractRidge(ComplexCWTResult result) {
        double[][] magnitude = result.getMagnitude();
        Ridge ridge = new Ridge(result.getNumSamples());
        
        // Simple ridge extraction - find maximum at each time
        for (int t = 0; t < result.getNumSamples(); t++) {
            double maxMag = 0;
            int maxScale = 0;
            
            for (int s = 0; s < result.getNumScales(); s++) {
                if (magnitude[s][t] > maxMag) {
                    maxMag = magnitude[s][t];
                    maxScale = s;
                }
            }
            
            ridge.scaleIndices[t] = maxScale;
            ridge.magnitudes[t] = maxMag;
        }
        
        return ridge;
    }
    
    private static double[] reconstructFromRidge(ComplexCWTResult result, Ridge ridge) {
        double[] reconstruction = new double[ridge.length];
        ComplexNumber[][] coeffs = result.getCoefficients();
        double[] scales = result.getScales();
        
        for (int t = 0; t < ridge.length; t++) {
            int s = ridge.scaleIndices[t];
            ComplexNumber c = coeffs[s][t];
            
            // Simplified reconstruction from ridge
            reconstruction[t] = c.real() * ridge.magnitudes[t] / Math.sqrt(scales[s]);
        }
        
        return reconstruction;
    }
    
    private static double[] createStockPrice(int N, double initial, double drift, double volatility) {
        double[] prices = new double[N];
        prices[0] = initial;
        
        for (int i = 1; i < N; i++) {
            double dt = 1.0 / FinancialAnalysisParameters.TRADING_DAYS_PER_YEAR; // Daily
            double dW = Math.sqrt(dt) * (Math.random() - 0.5) * 2.0;
            prices[i] = prices[i-1] * Math.exp((drift - 0.5 * volatility * volatility) * dt + 
                                               volatility * dW);
        }
        
        return prices;
    }
    
    private static double[] createCorrelatedStock(double[] baseStock, double correlation, double additionalVol) {
        int N = baseStock.length;
        double[] correlatedStock = new double[N];
        correlatedStock[0] = baseStock[0];
        
        for (int i = 1; i < N; i++) {
            double baseReturn = Math.log(baseStock[i] / baseStock[i-1]);
            double independentReturn = (Math.random() - 0.5) * 2.0 * additionalVol / Math.sqrt(FinancialAnalysisParameters.TRADING_DAYS_PER_YEAR);
            
            double correlatedReturn = correlation * baseReturn + 
                                    Math.sqrt(1 - correlation * correlation) * independentReturn;
            
            correlatedStock[i] = correlatedStock[i-1] * Math.exp(correlatedReturn);
        }
        
        return correlatedStock;
    }
    
    private static ComplexNumber[][] computeCrossSpectrum(ComplexCWTResult resultA, 
                                                         ComplexCWTResult resultB) {
        ComplexNumber[][] coeffsA = resultA.getCoefficients();
        ComplexNumber[][] coeffsB = resultB.getCoefficients();
        
        int numScales = coeffsA.length;
        int numSamples = coeffsA[0].length;
        ComplexNumber[][] crossSpectrum = new ComplexNumber[numScales][numSamples];
        
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples; t++) {
                // Cross-spectrum = A * conj(B)
                crossSpectrum[s][t] = coeffsA[s][t].multiply(coeffsB[s][t].conjugate());
            }
        }
        
        return crossSpectrum;
    }
    
    private static double[][] computeWaveletCoherence(ComplexNumber[][] crossSpectrum,
                                                     ComplexCWTResult resultA,
                                                     ComplexCWTResult resultB) {
        double[][] powerA = resultA.getPower();
        double[][] powerB = resultB.getPower();
        
        int numScales = crossSpectrum.length;
        int numSamples = crossSpectrum[0].length;
        double[][] coherence = new double[numScales][numSamples];
        
        // Smooth over time window
        int smoothWindow = 5;
        
        for (int s = 0; s < numScales; s++) {
            for (int t = smoothWindow; t < numSamples - smoothWindow; t++) {
                double crossPowerSum = 0;
                double powerASum = 0;
                double powerBSum = 0;
                
                for (int w = -smoothWindow; w <= smoothWindow; w++) {
                    crossPowerSum += crossSpectrum[s][t + w].magnitude();
                    powerASum += powerA[s][t + w];
                    powerBSum += powerB[s][t + w];
                }
                
                coherence[s][t] = crossPowerSum / 
                    (Math.sqrt(powerASum) * Math.sqrt(powerBSum) + 1e-10);
            }
        }
        
        return coherence;
    }
    
    private static double[][] computePhaseDifference(ComplexCWTResult resultA, 
                                                    ComplexCWTResult resultB) {
        double[][] phaseA = resultA.getPhase();
        double[][] phaseB = resultB.getPhase();
        
        int numScales = phaseA.length;
        int numSamples = phaseA[0].length;
        double[][] phaseDiff = new double[numScales][numSamples];
        
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples; t++) {
                double diff = phaseA[s][t] - phaseB[s][t];
                
                // Wrap to [-π, π]
                while (diff > Math.PI) diff -= 2 * Math.PI;
                while (diff < -Math.PI) diff += 2 * Math.PI;
                
                phaseDiff[s][t] = diff;
            }
        }
        
        return phaseDiff;
    }
    
    private static void analyzeLeadLagRelationship(double[][] phaseDiff, double[] scales) {
        System.out.println("\nLead-lag relationship analysis:");
        
        for (int s = 0; s < scales.length; s += 10) {
            double avgPhaseDiff = 0;
            int count = 0;
            
            for (int t = 100; t < phaseDiff[0].length - 100; t++) {
                avgPhaseDiff += phaseDiff[s][t];
                count++;
            }
            
            avgPhaseDiff /= count;
            
            if (Math.abs(avgPhaseDiff) > 0.1) {
                String leadLag = avgPhaseDiff > 0 ? "A leads B" : "B leads A";
                double timeLag = Math.abs(avgPhaseDiff) / (2 * Math.PI) * scales[s];
                
                System.out.printf("Scale %.1f: %s by %.2f time units\n", 
                    scales[s], leadLag, timeLag);
            }
        }
    }
}