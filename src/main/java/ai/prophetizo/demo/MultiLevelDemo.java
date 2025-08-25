package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;

/**
 * Demonstrates multi-level MODWT decomposition for financial time series analysis.
 * MODWT provides shift-invariant decomposition and works with arbitrary length signals.
 */
public class MultiLevelDemo {

    public static void main(String[] args) {
        // Create a synthetic financial time series with multiple components
        int length = 512;
        double[] prices = generateFinancialSeries(length);

        // Create multi-level MODWT transform
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        System.out.println("=== Multi-Level MODWT Decomposition Demo ===\n");

        // 1. Full decomposition
        MultiLevelMODWTResult fullResult = mwt.decompose(prices);
        System.out.println("Full decomposition levels: " + fullResult.levels());
        System.out.println("Original signal length: " + length);
        System.out.println("Final approximation length: " + fullResult.finalApproximation().length);

        // 2. Analyze energy at each scale
        System.out.println("\nEnergy distribution by scale:");
        double totalEnergy = fullResult.totalDetailEnergy();
        for (int level = 1; level <= fullResult.levels(); level++) {
            double levelEnergy = fullResult.detailEnergyAtLevel(level);
            double percentage = (levelEnergy / totalEnergy) * 100;
            System.out.printf("Level %d (scale 2^%d): %.2f%%\n",
                    level, level, percentage);
        }

        // 3. Partial decomposition for specific analysis
        System.out.println("\n3-level decomposition for trend analysis:");
        MultiLevelMODWTResult partial = mwt.decompose(prices, 3);

        // 4. Denoising demonstration
        System.out.println("\nDenoising by removing finest scale details:");
        double[] denoised = mwt.reconstructFromLevel(partial, 2);

        // Calculate noise reduction
        double noiseReduction = calculateNoiseReduction(prices, denoised);
        System.out.printf("Noise reduction: %.2f%%\n", noiseReduction);

        // 5. Adaptive decomposition
        System.out.println("\nAdaptive decomposition (1% energy threshold):");
        MultiLevelTransformResult adaptive = mwt.decomposeAdaptive(prices, 0.01);
        System.out.println("Adaptive levels: " + adaptive.levels());

        // 6. Specific 7-level decomposition example
        System.out.println("\n7-Level Decomposition (as requested):");
        demonstrate7LevelDecomposition();

        // 7. Multi-scale volatility analysis
        System.out.println("\nVolatility at different time scales:");
        analyzeVolatilityByScale(fullResult);

        System.out.println("\n=== Demo Complete ===");
    }

    /**
     * Generates a synthetic financial time series with trend, cycles, and noise.
     */
    private static double[] generateFinancialSeries(int length) {
        double[] series = new double[length];

        for (int i = 0; i < length; i++) {
            // Long-term trend
            double trend = 100 + 0.05 * i;

            // Medium-term cycle (business cycle)
            double mediumCycle = 10 * Math.sin(2 * Math.PI * i / 64.0);

            // Short-term oscillations (daily fluctuations)
            double shortCycle = 2 * Math.sin(2 * Math.PI * i / 8.0);

            // Market noise
            double noise = 0.5 * (Math.random() - 0.5);

            // Volatility clustering (GARCH-like effect)
            double volatility = (i > length / 2) ? 2.0 : 1.0;

            series[i] = trend + mediumCycle + shortCycle + volatility * noise;
        }

        return series;
    }

    /**
     * Calculates the percentage reduction in high-frequency components.
     */
    private static double calculateNoiseReduction(double[] original, double[] denoised) {
        double diffSquared = 0.0;
        double originalSquared = 0.0;

        for (int i = 0; i < original.length; i++) {
            double diff = original[i] - denoised[i];
            diffSquared += diff * diff;
            originalSquared += original[i] * original[i];
        }

        return (diffSquared / originalSquared) * 100;
    }

    /**
     * Analyzes volatility structure across different time scales.
     */
    private static void analyzeVolatilityByScale(MultiLevelTransformResult result) {
        for (int level = 1; level <= Math.min(result.levels(), 5); level++) {
            double[] details = result.detailsAtLevel(level);

            // Calculate standard deviation of detail coefficients
            double mean = 0.0;
            for (double d : details) {
                mean += d;
            }
            mean /= details.length;

            double variance = 0.0;
            for (double d : details) {
                variance += (d - mean) * (d - mean);
            }
            variance /= details.length;

            double volatility = Math.sqrt(variance);
            int scale = 1 << level; // 2^level

            System.out.printf("Scale %3d samples (~%3d time units): volatility = %.4f\n",
                    scale, scale, volatility);
        }
    }

    /**
     * Demonstrates specific 7-level decomposition as requested.
     */
    private static void demonstrate7LevelDecomposition() {
        // Generate a signal with sufficient length for 7 levels
        int signalLength = 512;  // 2^9, supports up to 9 levels (we need 7)
        double[] signal = new double[signalLength];
        
        // Create a multi-frequency signal
        for (int i = 0; i < signalLength; i++) {
            double t = i / 512.0;
            signal[i] = Math.sin(2 * Math.PI * 2 * t)    // Low frequency
                      + 0.5 * Math.sin(2 * Math.PI * 8 * t)  // Medium frequency
                      + 0.25 * Math.sin(2 * Math.PI * 32 * t) // High frequency
                      + 0.1 * (Math.random() - 0.5);          // Noise
        }
        
        // Create transform with DB4 wavelet
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Decompose into exactly 7 levels
        MultiLevelMODWTResult result = mwt.decompose(signal, 7);
        
        System.out.println("  Signal length: " + signalLength);
        System.out.println("  Decomposition levels: " + result.levels());
        System.out.println("\n  Level details:");
        
        // Show details for each level
        for (int level = 1; level <= 7; level++) {
            double[] details = result.detailsAtLevel(level);
            double energy = result.detailEnergyAtLevel(level);
            System.out.printf("    Level %d: %3d coefficients, energy=%.6f\n", 
                              level, details.length, energy);
        }
        
        // Show final approximation
        double[] approximation = result.finalApproximation();
        System.out.printf("    Final approximation: %d coefficients\n", approximation.length);
        
        // Demonstrate reconstruction from different levels
        System.out.println("\n  Reconstruction examples:");
        double[] denoised1 = mwt.reconstructFromLevel(result, 1);  // Remove finest details
        double[] denoised3 = mwt.reconstructFromLevel(result, 3);  // Remove 3 finest levels
        System.out.println("    - Removing level 1 details: preserves lower frequencies");
        System.out.println("    - Removing levels 1-3: stronger denoising effect");
    }
}