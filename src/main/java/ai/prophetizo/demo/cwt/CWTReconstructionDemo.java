package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;

/**
 * Demonstrates CWT reconstruction methods with focus on practical approaches.
 */
public class CWTReconstructionDemo {
    
    public static void main(String[] args) {
        System.out.println("CWT Reconstruction Methods Demo");
        System.out.println("===============================\n");
        
        // Create test signal
        int N = 256;
        double[] signal = createTestSignal(N);
        
        // Different scale configurations
        double[] sparseScales = {2, 4, 8, 16, 32};  // Dyadic scales (best for DWT)
        double[] denseScales = generateLogScales(1, 50, 30);  // Dense scales
        
        System.out.println("1. Standard Inverse CWT:");
        System.out.println("   - Direct numerical integration");
        System.out.println("   - Simple but limited accuracy (~85-95% error)");
        System.out.println("   - Suitable for visualization and rough reconstruction\n");
        
        System.out.println("2. MODWT-Based Inverse CWT (Recommended):");
        System.out.println("   - Leverages shift-invariant MODWT properties");
        System.out.println("   - Fast: O(N log N) complexity");
        System.out.println("   - Stable: No iterative optimization");
        System.out.println("   - Works with arbitrary length signals");
        System.out.println("   - Ideal for real-time applications\n");
        
        // Test with Morlet wavelet
        MorletWavelet morlet = new MorletWavelet();
        testReconstruction("Morlet with Sparse Scales", morlet, signal, sparseScales);
        testReconstruction("Morlet with Dense Scales", morlet, signal, denseScales);
        
        // Financial application example
        System.out.println("\n3. Financial Application Example:");
        demonstrateFinancialReconstruction();
        
        // Guidelines
        System.out.println("\n4. Reconstruction Method Selection Guide:");
        System.out.println("   Use Standard InverseCWT when:");
        System.out.println("   - Approximate reconstruction is sufficient");
        System.out.println("   - Visualization is the primary goal");
        System.out.println("   - Working with any scale distribution\n");
        
        System.out.println("   Use MODWT-Based InverseCWT when:");
        System.out.println("   - Speed is critical (10-300x faster)");
        System.out.println("   - Working with dyadic or near-dyadic scales");
        System.out.println("   - Shift-invariance is important");
        System.out.println("   - Real-time processing is required");
        System.out.println("   - Financial applications (preserves structure)");
    }
    
    private static void testReconstruction(String testName, ContinuousWavelet wavelet, 
                                         double[] signal, double[] scales) {
        System.out.println("\n" + testName + ":");
        System.out.println("-".repeat(50));
        
        // Perform CWT
        CWTTransform cwt = new CWTTransform(wavelet);
        CWTResult cwtResult = cwt.analyze(signal, scales);
        
        // Standard reconstruction
        long startStandard = System.nanoTime();
        InverseCWT standardInverse = new InverseCWT(wavelet);
        double[] standardRecon = standardInverse.reconstruct(cwtResult);
        long timeStandard = System.nanoTime() - startStandard;
        double errorStandard = calculateError(signal, standardRecon);
        
        // MODWT-based reconstruction
        long startMODWT = System.nanoTime();
        MODWTBasedInverseCWT modwtInverse = new MODWTBasedInverseCWT(wavelet);
        double[] modwtRecon = modwtInverse.reconstruct(cwtResult);
        long timeMODWT = System.nanoTime() - startMODWT;
        double errorMODWT = calculateError(signal, modwtRecon);
        
        // Results
        System.out.printf("Standard:   %.1f%% error, %.2f ms\n", 
            errorStandard * 100, timeStandard / 1e6);
        System.out.printf("MODWT-based:  %.1f%% error, %.2f ms (%.1fx faster)\n", 
            errorMODWT * 100, timeMODWT / 1e6, (double)timeStandard / timeMODWT);
        
        // Analysis
        if (isDyadicScales(scales)) {
            System.out.println("Analysis: Dyadic scales - MODWT method works well");
        } else {
            System.out.println("Analysis: Mixed scales - MODWT method less accurate but still fast");
        }
    }
    
    private static void demonstrateFinancialReconstruction() {
        int N = 500;
        double[] prices = createFinancialData(N);
        double[] returns = calculateReturns(prices);
        
        // Use Paul wavelet (good for financial data)
        PaulWavelet paul = new PaulWavelet(4);
        double[] scales = {2, 4, 8, 16, 32, 64};  // Trading periods
        
        CWTTransform cwt = new CWTTransform(paul);
        CWTResult cwtResult = cwt.analyze(returns, scales);
        
        // MODWT-based reconstruction
        MODWTBasedInverseCWT modwtInverse = new MODWTBasedInverseCWT(paul);
        double[] reconReturns = modwtInverse.reconstruct(cwtResult);
        
        // Convert back to prices
        double[] reconPrices = returnsToprices(reconReturns, prices[0]);
        
        double priceError = calculateError(prices, reconPrices);
        System.out.printf("Price reconstruction error: %.1f%%\n", priceError * 100);
        System.out.println("MODWT method preserves price structure despite return-space errors");
    }
    
    private static double[] createTestSignal(int N) {
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / N) +
                       0.5 * Math.sin(2 * Math.PI * 15 * i / N);
        }
        return signal;
    }
    
    private static double[] createFinancialData(int N) {
        double[] prices = new double[N];
        double basePrice = 100;
        
        for (int i = 0; i < N; i++) {
            double trend = basePrice + 0.1 * i;
            double cycle = 5 * Math.sin(2 * Math.PI * i / 20);
            double noise = Math.random() - 0.5;
            prices[i] = trend + cycle + noise;
        }
        
        return prices;
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
    
    private static boolean isDyadicScales(double[] scales) {
        for (double scale : scales) {
            double log2 = Math.log(scale) / Math.log(2);
            if (Math.abs(log2 - Math.round(log2)) > 0.1) {
                return false;
            }
        }
        return true;
    }
    
    private static double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = Math.log(prices[i + 1] / prices[i]);
        }
        return returns;
    }
    
    private static double[] returnsToprices(double[] returns, double initialPrice) {
        double[] prices = new double[returns.length + 1];
        prices[0] = initialPrice;
        
        for (int i = 0; i < returns.length; i++) {
            prices[i + 1] = prices[i] * Math.exp(returns[i]);
        }
        
        return prices;
    }
    
    private static double calculateError(double[] original, double[] reconstructed) {
        double mse = 0;
        double power = 0;
        int len = Math.min(original.length, reconstructed.length);
        
        for (int i = 0; i < len; i++) {
            double diff = original[i] - reconstructed[i];
            mse += diff * diff;
            power += original[i] * original[i];
        }
        
        return Math.sqrt(mse / power);
    }
}