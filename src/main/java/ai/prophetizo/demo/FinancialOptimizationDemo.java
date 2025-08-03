package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTResultImpl;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;

import java.util.Arrays;

/**
 * Demonstrates financial applications of MODWT transforms.
 * Shows how MODWT can be used for financial time series analysis,
 * noise reduction, and trend detection with shift-invariance.
 */
public class FinancialOptimizationDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Financial Optimization Demo (MODWT)");
        System.out.println("===============================================");
        
        // Simulate financial time series data
        double[] stockPrices = generateStockPriceData();
        double[] volatilityData = generateVolatilityData();
        
        demonstrateNoiseReduction(stockPrices);
        demonstrateTrendAnalysis(stockPrices);
        demonstrateVolatilityAnalysis(volatilityData);
        demonstrateMultiResolutionAnalysis(stockPrices);
    }
    
    /**
     * Demonstrates noise reduction in financial data using MODWT.
     */
    private static void demonstrateNoiseReduction(double[] noisyPrices) {
        System.out.println("\n1. Noise Reduction in Stock Prices:");
        System.out.println("   Original data points: " + noisyPrices.length);
        System.out.println("   Original price range: [" + 
            String.format("%.2f", Arrays.stream(noisyPrices).min().orElse(0)) + ", " +
            String.format("%.2f", Arrays.stream(noisyPrices).max().orElse(0)) + "]");
        
        try {
            // Use WaveletDenoiser which internally uses MODWT
            WaveletDenoiser denoiser = WaveletDenoiser.forFinancialData();
            
            double[] denoised = denoiser.denoise(noisyPrices, 
                WaveletDenoiser.ThresholdMethod.SURE);
            
            double noiseReduction = calculateNoiseReduction(noisyPrices, denoised);
            
            System.out.println("   ✓ Using MODWT with DB4 wavelet");
            System.out.println("   ✓ Shift-invariant denoising preserves timing");
            System.out.println("   ✓ Noise reduction: " + String.format("%.2f%%", noiseReduction * 100));
            System.out.println("   ✓ Denoised price range: [" + 
                String.format("%.2f", Arrays.stream(denoised).min().orElse(0)) + ", " +
                String.format("%.2f", Arrays.stream(denoised).max().orElse(0)) + "]");
            
        } catch (Exception e) {
            System.out.println("   ! Error in noise reduction: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates trend analysis using MODWT approximation coefficients.
     */
    private static void demonstrateTrendAnalysis(double[] prices) {
        System.out.println("\n2. Trend Analysis:");
        
        try {
            // Use MODWT for shift-invariant trend analysis
            MODWTTransform transform = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
            
            MODWTResult result = transform.forward(prices);
            double[] trend = result.approximationCoeffs();
            double[] details = result.detailCoeffs();
            
            // Calculate trend strength
            double trendEnergy = calculateEnergy(trend);
            double detailEnergy = calculateEnergy(details);
            double trendRatio = trendEnergy / (trendEnergy + detailEnergy);
            
            System.out.println("   ✓ MODWT preserves signal length: " + trend.length);
            System.out.println("   ✓ Trend strength: " + String.format("%.2f%%", trendRatio * 100));
            System.out.println("   ✓ Detail volatility: " + String.format("%.4f", Math.sqrt(detailEnergy / details.length)));
            
            // Identify trend direction
            double trendDirection = trend[trend.length - 1] - trend[0];
            System.out.println("   ✓ Overall trend: " + (trendDirection > 0 ? "Upward" : "Downward"));
            
        } catch (Exception e) {
            System.out.println("   ! Error in trend analysis: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates volatility analysis using MODWT detail coefficients.
     */
    private static void demonstrateVolatilityAnalysis(double[] volatilityData) {
        System.out.println("\n3. Volatility Analysis:");
        
        try {
            MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(volatilityData);
            
            // Analyze high-frequency components for volatility clustering
            double[] details = result.detailCoeffs();
            
            // Calculate rolling volatility from detail coefficients
            int windowSize = 20;
            double[] rollingVol = new double[details.length - windowSize + 1];
            
            for (int i = 0; i < rollingVol.length; i++) {
                double sum = 0;
                for (int j = 0; j < windowSize; j++) {
                    sum += details[i + j] * details[i + j];
                }
                rollingVol[i] = Math.sqrt(sum / windowSize);
            }
            
            double avgVol = Arrays.stream(rollingVol).average().orElse(0);
            double maxVol = Arrays.stream(rollingVol).max().orElse(0);
            
            System.out.println("   ✓ Average volatility: " + String.format("%.4f", avgVol));
            System.out.println("   ✓ Maximum volatility: " + String.format("%.4f", maxVol));
            System.out.println("   ✓ Volatility ratio (max/avg): " + String.format("%.2f", maxVol / avgVol));
            System.out.println("   ✓ MODWT preserves temporal alignment for volatility");
            
        } catch (Exception e) {
            System.out.println("   ! Error in volatility analysis: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates multi-resolution analysis for different time scales.
     */
    private static void demonstrateMultiResolutionAnalysis(double[] prices) {
        System.out.println("\n4. Multi-Resolution Analysis:");
        
        try {
            MODWTTransform transform = new MODWTTransform(Daubechies.DB6, BoundaryMode.PERIODIC);
            
            // Perform multiple levels of decomposition
            System.out.println("   Analyzing at different time scales:");
            
            double[] currentSignal = prices;
            for (int level = 1; level <= 3; level++) {
                MODWTResult result = transform.forward(currentSignal);
                
                double approxEnergy = calculateEnergy(result.approximationCoeffs());
                double detailEnergy = calculateEnergy(result.detailCoeffs());
                
                System.out.printf("   Level %d: Approx energy %.2f%%, Detail energy %.2f%%\n",
                    level,
                    100 * approxEnergy / (approxEnergy + detailEnergy),
                    100 * detailEnergy / (approxEnergy + detailEnergy));
                
                // Use approximation for next level
                currentSignal = result.approximationCoeffs();
            }
            
            System.out.println("   ✓ MODWT maintains signal length at all levels");
            System.out.println("   ✓ Shift-invariant decomposition for accurate timing");
            
        } catch (Exception e) {
            System.out.println("   ! Error in multi-resolution analysis: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private static double[] generateStockPriceData() {
        int days = 252; // One trading year
        double[] prices = new double[days];
        double price = 100.0;
        double drift = 0.0002; // Small positive drift
        double volatility = 0.02;
        
        for (int i = 0; i < days; i++) {
            double dailyReturn = drift + volatility * (Math.random() - 0.5) * 2;
            price *= (1 + dailyReturn);
            prices[i] = price;
            
            // Add microstructure noise
            prices[i] += 0.01 * (Math.random() - 0.5);
        }
        
        return prices;
    }
    
    private static double[] generateVolatilityData() {
        int days = 100;
        double[] volatility = new double[days];
        double currentVol = 0.15;
        double meanReversion = 0.1;
        double volOfVol = 0.05;
        double longTermMean = 0.20;
        
        for (int i = 0; i < days; i++) {
            // GARCH-like volatility clustering
            double innovation = volOfVol * (Math.random() - 0.5) * 2;
            currentVol += meanReversion * (longTermMean - currentVol) + innovation;
            currentVol = Math.max(0.05, Math.min(0.50, currentVol)); // Bounds
            volatility[i] = currentVol;
        }
        
        return volatility;
    }
    
    private static double calculateNoiseReduction(double[] original, double[] denoised) {
        double sumOriginal = 0;
        double sumDiff = 0;
        
        for (int i = 1; i < original.length; i++) {
            double origDiff = Math.abs(original[i] - original[i-1]);
            double denoisedDiff = Math.abs(denoised[i] - denoised[i-1]);
            sumOriginal += origDiff;
            sumDiff += denoisedDiff;
        }
        
        return 1.0 - (sumDiff / sumOriginal);
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
}