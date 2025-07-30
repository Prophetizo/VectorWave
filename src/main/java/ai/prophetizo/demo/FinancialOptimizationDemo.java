package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;

import java.util.Arrays;

/**
 * Demonstrates financial signal optimization using wavelets.
 * Shows denoising and feature extraction from financial time series data.
 */
public class FinancialOptimizationDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Financial Optimization Demo");
        System.out.println("========================================");
        
        // Simulated financial data (daily returns)
        double[] stockPrices = generateFinancialTimeSeries();
        
        System.out.println("Original Stock Prices: " + Arrays.toString(stockPrices));
        
        demonstrateDenoising(stockPrices);
        demonstrateFeatureExtraction(stockPrices);
        demonstrateVolatilityAnalysis(stockPrices);
    }
    
    private static double[] generateFinancialTimeSeries() {
        // Simulated stock price data with noise
        return new double[]{
            100.0, 102.5, 101.8, 103.2, 105.1, 104.7, 106.3, 108.1,
            107.9, 109.4, 111.2, 110.8, 112.5, 114.3, 113.9, 115.7
        };
    }
    
    private static void demonstrateDenoising(double[] prices) {
        System.out.println("\n1. Financial Signal Denoising:");
        System.out.println("------------------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Use Daubechies for better frequency localization
        WaveletTransform transform = factory.create(Daubechies.DB4);
        
        try {
            TransformResult result = transform.forward(prices);
            
            // Simple denoising: zero out small detail coefficients
            double[] denoisedDetails = result.detailCoeffs().clone();
            double threshold = 0.5; // Threshold for noise reduction
            int significantCoeffs = 0;
            
            for (int i = 0; i < denoisedDetails.length; i++) {
                if (Math.abs(denoisedDetails[i]) < threshold) {
                    denoisedDetails[i] = 0.0;
                } else {
                    significantCoeffs++;
                }
            }
            
            // For demonstration purposes, show the denoising effect conceptually
            // Note: In a real implementation, you would need direct access to modify coefficients
            // or use a wavelet library that supports coefficient modification
            System.out.println("Approximation:     " + Arrays.toString(result.approximationCoeffs()));
            System.out.println("Original Details:  " + Arrays.toString(result.detailCoeffs()));
            System.out.println("Denoised Details:  " + Arrays.toString(denoisedDetails));
            
            // Reconstruct original for comparison
            double[] denoisedPrices = transform.inverse(result);
            
            System.out.println("Original:  " + Arrays.toString(prices));
            System.out.println("Reconstructed: " + Arrays.toString(denoisedPrices));
            
            // Calculate denoising effectiveness (using reconstruction error as proxy)
            double reconstructionError = 0.0;
            for (int i = 0; i < prices.length; i++) {
                double diff = prices[i] - denoisedPrices[i];
                reconstructionError += diff * diff;
            }
            reconstructionError = Math.sqrt(reconstructionError / prices.length);
            
            System.out.printf("Reconstruction RMSE: %.6f\n", reconstructionError);
            System.out.printf("Significant coefficients: %d/%d (%.1f%% retention)\n", 
                significantCoeffs, denoisedDetails.length, 
                (100.0 * significantCoeffs) / denoisedDetails.length);
            
        } catch (Exception e) {
            System.out.println("Error during denoising: " + e.getMessage());
        }
    }
    
    private static void demonstrateFeatureExtraction(double[] prices) {
        System.out.println("\n2. Feature Extraction (Trend Analysis):");
        System.out.println("---------------------------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(new Haar());
        
        try {
            TransformResult result = transform.forward(prices);
            
            System.out.println("Approximation (Trend): " + Arrays.toString(result.approximationCoeffs()));
            System.out.println("Detail (High-freq):    " + Arrays.toString(result.detailCoeffs()));
            
            // Analyze trend direction
            double[] trend = result.approximationCoeffs();
            double trendSlope = (trend[trend.length - 1] - trend[0]) / trend.length;
            
            System.out.printf("Overall trend slope: %.3f\n", trendSlope);
            System.out.println("Market direction: " + (trendSlope > 0 ? "BULLISH" : "BEARISH"));
            
        } catch (Exception e) {
            System.out.println("Error during feature extraction: " + e.getMessage());
        }
    }
    
    private static void demonstrateVolatilityAnalysis(double[] prices) {
        System.out.println("\n3. Volatility Analysis:");
        System.out.println("-----------------------");
        
        WaveletTransformFactory factory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransform transform = factory.create(Daubechies.DB2);
        
        try {
            TransformResult result = transform.forward(prices);
            
            // Calculate volatility from detail coefficients
            double[] details = result.detailCoeffs();
            double volatility = calculateStandardDeviation(details);
            
            System.out.printf("Wavelet-based volatility: %.3f\n", volatility);
            
            // Compare with traditional volatility
            double[] returns = calculateReturns(prices);
            double traditionalVolatility = calculateStandardDeviation(returns);
            
            System.out.printf("Traditional volatility:   %.3f\n", traditionalVolatility);
            
            // Risk assessment
            String riskLevel;
            if (volatility < 1.0) {
                riskLevel = "LOW";
            } else if (volatility < 2.0) {
                riskLevel = "MEDIUM";
            } else {
                riskLevel = "HIGH";
            }
            
            System.out.println("Risk Level: " + riskLevel);
            
        } catch (Exception e) {
            System.out.println("Error during volatility analysis: " + e.getMessage());
        }
    }
    
    private static double calculateTotalVariation(double[] signal) {
        double variation = 0.0;
        for (int i = 1; i < signal.length; i++) {
            variation += Math.abs(signal[i] - signal[i - 1]);
        }
        return variation;
    }
    
    private static double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = (prices[i] - prices[i - 1]) / prices[i - 1];
        }
        return returns;
    }
    
    private static double calculateStandardDeviation(double[] data) {
        double mean = Arrays.stream(data).average().orElse(0.0);
        double variance = Arrays.stream(data)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}