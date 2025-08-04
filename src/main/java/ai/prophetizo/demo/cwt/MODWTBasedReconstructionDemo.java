package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.finance.*;

/**
 * Demonstrates MODWT-based CWT reconstruction for practical applications.
 */
public class MODWTBasedReconstructionDemo {
    
    public static void main(String[] args) {
        System.out.println("MODWT-Based CWT Reconstruction Demo");
        System.out.println("===================================\n");
        
        // Create test signal
        int N = 256;
        double[] signal = createTestSignal(N);
        
        // Use dyadic scales for better MODWT compatibility
        double[] scales = generateDyadicScales(1, 5); // 2^1 to 2^5
        
        System.out.println("Test Configuration:");
        System.out.printf("Signal length: %d samples\n", N);
        System.out.printf("Scales: ");
        for (double s : scales) {
            System.out.printf("%.0f ", s);
        }
        System.out.println("\n");
        
        // Test with different wavelets
        testWavelet("Morlet", new MorletWavelet(), signal, scales);
        testWavelet("Mexican Hat", new DOGWavelet(2), signal, scales);
        testWavelet("Paul-4", new PaulWavelet(4), signal, scales);
        
        // Financial data test
        System.out.println("\n4. Financial Data Test:");
        double[] prices = createFinancialData(N);
        testFinancialReconstruction(prices, scales);
        
        // Mixed scales test (dyadic + non-dyadic)
        System.out.println("\n5. Mixed Scales Test:");
        double[] mixedScales = {2, 3, 4, 6, 8, 12, 16, 24, 32}; // Mix of dyadic and non-dyadic
        testMixedScales(signal, mixedScales);
    }
    
    private static void testWavelet(String name, ContinuousWavelet wavelet, 
                                   double[] signal, double[] scales) {
        System.out.printf("\n%s Wavelet Test:\n", name);
        System.out.println("-".repeat(40));
        
        // Perform CWT
        CWTTransform cwt = new CWTTransform(wavelet);
        CWTResult cwtResult = cwt.analyze(signal, scales);
        
        // Standard reconstruction
        long startStandard = System.nanoTime();
        InverseCWT standardInverse = new InverseCWT(wavelet);
        double[] standardReconstructed = standardInverse.reconstruct(cwtResult);
        long timeStandard = System.nanoTime() - startStandard;
        double errorStandard = calculateRelativeError(signal, standardReconstructed);
        
        // MODWT-based reconstruction
        long startMODWT = System.nanoTime();
        MODWTBasedInverseCWT modwtInverse = new MODWTBasedInverseCWT(wavelet);
        double[] modwtReconstructed = modwtInverse.reconstruct(cwtResult);
        long timeMODWT = System.nanoTime() - startMODWT;
        double errorMODWT = calculateRelativeError(signal, modwtReconstructed);
        
        // Results
        System.out.println("Method         | Error (%) | Time (ms) | Speedup");
        System.out.println("---------------|-----------|-----------|--------");
        System.out.printf("Standard       | %9.1f | %9.2f | %7s\n", 
            errorStandard * 100, timeStandard / 1e6, "1.0x");
        System.out.printf("MODWT-based    | %9.1f | %9.2f | %7.1fx\n", 
            errorMODWT * 100, timeMODWT / 1e6, (double)timeStandard / timeMODWT);
        
        // Show improvement for specific use cases
        if (name.equals("Paul-4")) {
            System.out.println("\nNote: MODWT-based method is ideal for financial applications");
            System.out.println("      where speed and stability are more important than");
            System.out.println("      perfect mathematical reconstruction accuracy.");
        }
    }
    
    private static void testFinancialReconstruction(double[] prices, double[] scales) {
        // Use specialized financial wavelet
        PaulWavelet paulWavelet = new PaulWavelet(4);
        CWTTransform cwt = new CWTTransform(paulWavelet);
        
        // Analyze returns instead of prices
        double[] returns = calculateReturns(prices);
        CWTResult cwtResult = cwt.analyze(returns, scales);
        
        // MODWT-based reconstruction
        MODWTBasedInverseCWT modwtInverse = new MODWTBasedInverseCWT(paulWavelet);
        double[] reconstructedReturns = modwtInverse.reconstruct(cwtResult);
        
        // Convert back to prices
        double[] reconstructedPrices = returnsToprices(reconstructedReturns, prices[0]);
        
        double error = calculateRelativeError(prices, reconstructedPrices);
        System.out.printf("Returns reconstruction error: %.1f%%\n", 
            calculateRelativeError(returns, reconstructedReturns) * 100);
        System.out.printf("Price reconstruction error: %.1f%%\n", error * 100);
        System.out.println("Note: DWT-based method preserves financial data structure better");
    }
    
    private static void testMixedScales(double[] signal, double[] scales) {
        MorletWavelet wavelet = new MorletWavelet();
        CWTTransform cwt = new CWTTransform(wavelet);
        CWTResult cwtResult = cwt.analyze(signal, scales);
        
        // Test with and without refinement
        MODWTBasedInverseCWT modwtNoRefine = new MODWTBasedInverseCWT(
            wavelet, Daubechies.DB4, false);
        MODWTBasedInverseCWT modwtWithRefine = new MODWTBasedInverseCWT(
            wavelet, Daubechies.DB4, true);
        
        double[] noRefine = modwtNoRefine.reconstruct(cwtResult);
        double[] withRefine = modwtWithRefine.reconstruct(cwtResult);
        
        System.out.printf("Without refinement: %.1f%% error\n", 
            calculateRelativeError(signal, noRefine) * 100);
        System.out.printf("With refinement: %.1f%% error\n", 
            calculateRelativeError(signal, withRefine) * 100);
        System.out.println("Refinement uses non-dyadic scales to improve accuracy");
    }
    
    private static double[] createTestSignal(int N) {
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / N) +
                       0.5 * Math.sin(2 * Math.PI * 15 * i / N) +
                       0.3 * Math.cos(2 * Math.PI * 30 * i / N);
        }
        return signal;
    }
    
    private static double[] createFinancialData(int N) {
        double[] prices = new double[N];
        double basePrice = 100;
        
        for (int i = 0; i < N; i++) {
            double trend = basePrice + 0.05 * i;
            double cycle = 2 * Math.sin(2 * Math.PI * i / 20);
            double noise = 0.5 * (Math.random() - 0.5);
            prices[i] = trend + cycle + noise;
        }
        
        return prices;
    }
    
    private static double[] generateDyadicScales(int minLevel, int maxLevel) {
        int numScales = maxLevel - minLevel + 1;
        double[] scales = new double[numScales];
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.pow(2, minLevel + i);
        }
        
        return scales;
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
    
    private static double calculateRelativeError(double[] original, double[] reconstructed) {
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