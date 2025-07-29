package ai.prophetizo.demo;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates financial applications of wavelet transforms.
 * Shows how wavelets can be used for financial time series analysis,
 * noise reduction, and trend detection.
 */
public class FinancialOptimizationDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Financial Optimization Demo");
        System.out.println("=======================================");
        
        // Simulate financial time series data
        double[] stockPrices = generateStockPriceData();
        double[] volatilityData = generateVolatilityData();
        
        demonstrateNoiseReduction(stockPrices);
        demonstrateTrendAnalysis(stockPrices);
        demonstrateVolatilityAnalysis(volatilityData);
        demonstrateMultiResolutionAnalysis(stockPrices);
    }
    
    /**
     * Demonstrates noise reduction in financial data using wavelets.
     */
    private static void demonstrateNoiseReduction(double[] noisyPrices) {
        System.out.println("\n1. Noise Reduction in Stock Prices:");
        System.out.println("   Original data points: " + noisyPrices.length);
        System.out.println("   Original price range: [" + 
            String.format("%.2f", Arrays.stream(noisyPrices).min().orElse(0)) + ", " +
            String.format("%.2f", Arrays.stream(noisyPrices).max().orElse(0)) + "]");
        
        try {
            // Use Daubechies DB4 for better frequency localization
            WaveletTransform transform = new WaveletTransformFactory()
                    .withBoundaryMode(BoundaryMode.PERIODIC)
                    .create(Daubechies.DB4);
            
            TransformResult result = transform.forward(noisyPrices);
            
            // Apply soft thresholding to reduce noise
            double[] denoisedApprox = applySoftThresholding(result, 0.1);
            double[] denoisedDetail = new double[result.detailCoeffs().length];
            System.arraycopy(result.detailCoeffs(), 0, denoisedDetail, 0, denoisedDetail.length);
            
            // Create a new result with denoised coefficients using the existing implementation
            // Since TransformResult is sealed, create it through the transform operation
            double[] tempSignal = new double[denoisedApprox.length * 2];
            System.arraycopy(denoisedApprox, 0, tempSignal, 0, denoisedApprox.length);
            System.arraycopy(denoisedDetail, 0, tempSignal, denoisedApprox.length, denoisedDetail.length);
            
            TransformResult denoisedResult = transform.forward(tempSignal);
            double[] reconstructed = transform.inverse(denoisedResult);
            
            double noiseReduction = calculateNoiseReduction(noisyPrices, reconstructed);
            
            System.out.println("   ✓ Wavelet: " + Daubechies.DB4.name());
            System.out.println("   ✓ Noise reduction: " + String.format("%.2f%%", noiseReduction * 100));
            System.out.println("   ✓ Denoised price range: [" + 
                String.format("%.2f", Arrays.stream(reconstructed).min().orElse(0)) + ", " +
                String.format("%.2f", Arrays.stream(reconstructed).max().orElse(0)) + "]");
            
        } catch (Exception e) {
            System.out.println("   ! Error in noise reduction: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates trend analysis using wavelet approximation coefficients.
     */
    private static void demonstrateTrendAnalysis(double[] prices) {
        System.out.println("\n2. Trend Analysis:");
        
        try {
            // Use Symlet for balanced time-frequency localization
            WaveletTransform transform = new WaveletTransformFactory()
                    .withBoundaryMode(BoundaryMode.PERIODIC)
                    .create(Symlet.SYM2);
            
            TransformResult result = transform.forward(prices);
            double[] trend = result.approximationCoeffs();
            double[] details = result.detailCoeffs();
            
            // Analyze trend characteristics
            double trendSlope = calculateTrendSlope(trend);
            double trendStrength = calculateTrendStrength(trend, details);
            
            System.out.println("   ✓ Wavelet: " + Symlet.SYM2.name());
            System.out.println("   ✓ Trend slope: " + String.format("%.4f", trendSlope));
            System.out.println("   ✓ Trend strength: " + String.format("%.2f%%", trendStrength * 100));
            
            if (trendSlope > 0.01) {
                System.out.println("   → Market shows BULLISH trend");
            } else if (trendSlope < -0.01) {
                System.out.println("   → Market shows BEARISH trend");
            } else {
                System.out.println("   → Market shows SIDEWAYS movement");
            }
            
        } catch (Exception e) {
            System.out.println("   ! Error in trend analysis: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates volatility analysis using detail coefficients.
     */
    private static void demonstrateVolatilityAnalysis(double[] returns) {
        System.out.println("\n3. Volatility Analysis:");
        
        try {
            // Use Coiflet for volatility analysis
            WaveletTransform transform = new WaveletTransformFactory()
                    .withBoundaryMode(BoundaryMode.ZERO_PADDING)
                    .create(Coiflet.COIF1);
            
            TransformResult result = transform.forward(returns);
            double[] details = result.detailCoeffs();
            
            // Calculate volatility metrics
            double volatility = calculateVolatility(details);
            double[] volatilityClusters = detectVolatilityClusters(details);
            
            System.out.println("   ✓ Wavelet: " + Coiflet.COIF1.name());
            System.out.println("   ✓ Wavelet-based volatility: " + String.format("%.4f", volatility));
            System.out.println("   ✓ Volatility clusters detected: " + volatilityClusters.length);
            
            if (volatility > 0.02) {
                System.out.println("   → HIGH volatility period detected");
            } else if (volatility > 0.01) {
                System.out.println("   → MODERATE volatility period");
            } else {
                System.out.println("   → LOW volatility period");
            }
            
        } catch (Exception e) {
            System.out.println("   ! Error in volatility analysis: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates multi-resolution analysis for different time scales.
     */
    private static void demonstrateMultiResolutionAnalysis(double[] prices) {
        System.out.println("\n4. Multi-Resolution Analysis:");
        
        Wavelet[] wavelets = {new Haar(), Daubechies.DB2, Symlet.SYM2};
        
        for (Wavelet wavelet : wavelets) {
            try {
                WaveletTransform transform = new WaveletTransformFactory()
                        .withBoundaryMode(BoundaryMode.PERIODIC)
                        .create(wavelet);
                
                TransformResult result = transform.forward(prices);
                
                double approximationEnergy = calculateEnergy(result.approximationCoeffs());
                double detailEnergy = calculateEnergy(result.detailCoeffs());
                double totalEnergy = approximationEnergy + detailEnergy;
                
                System.out.println("   " + wavelet.name() + ":");
                System.out.println("     - Low frequency energy: " + 
                    String.format("%.2f%%", (approximationEnergy / totalEnergy) * 100));
                System.out.println("     - High frequency energy: " + 
                    String.format("%.2f%%", (detailEnergy / totalEnergy) * 100));
                
            } catch (Exception e) {
                System.out.println("   ! Error with " + wavelet.name() + ": " + e.getMessage());
            }
        }
    }
    
    // Helper methods for financial calculations
    
    private static double[] generateStockPriceData() {
        // Simulate stock price data with trend and noise
        double[] prices = new double[16];
        double basePrice = 100.0;
        double trend = 0.5;
        
        for (int i = 0; i < prices.length; i++) {
            double trendComponent = basePrice + (trend * i);
            double noise = (Math.random() - 0.5) * 5.0; // Random noise
            prices[i] = trendComponent + noise;
        }
        
        return prices;
    }
    
    private static double[] generateVolatilityData() {
        // Simulate return data for volatility analysis
        double[] returns = new double[16];
        
        for (int i = 0; i < returns.length; i++) {
            // Simulate daily returns with varying volatility
            double volatility = 0.01 + 0.02 * Math.sin(i * Math.PI / 8);
            returns[i] = volatility * (Math.random() - 0.5) * 2;
        }
        
        return returns;
    }
    
    private static double[] applySoftThresholding(TransformResult result, double threshold) {
        double[] approx = result.approximationCoeffs();
        double[] details = result.detailCoeffs();
        
        // Apply soft thresholding to detail coefficients
        for (int i = 0; i < details.length; i++) {
            if (Math.abs(details[i]) < threshold) {
                details[i] = 0;
            } else {
                details[i] = Math.signum(details[i]) * (Math.abs(details[i]) - threshold);
            }
        }
        
        return approx; // Return approximation coefficients for reconstruction
    }
    
    private static double calculateNoiseReduction(double[] original, double[] denoised) {
        double originalVariance = calculateVariance(original);
        double denoisedVariance = calculateVariance(denoised);
        return Math.max(0, (originalVariance - denoisedVariance) / originalVariance);
    }
    
    private static double calculateVariance(double[] data) {
        double mean = Arrays.stream(data).average().orElse(0);
        return Arrays.stream(data).map(x -> Math.pow(x - mean, 2)).average().orElse(0);
    }
    
    private static double calculateTrendSlope(double[] trend) {
        if (trend.length < 2) return 0;
        
        // Simple linear regression slope
        double n = trend.length;
        double sumX = n * (n - 1) / 2;
        double sumY = Arrays.stream(trend).sum();
        double sumXY = 0;
        double sumX2 = n * (n - 1) * (2 * n - 1) / 6;
        
        for (int i = 0; i < trend.length; i++) {
            sumXY += i * trend[i];
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    private static double calculateTrendStrength(double[] trend, double[] details) {
        double trendEnergy = calculateEnergy(trend);
        double detailEnergy = calculateEnergy(details);
        double totalEnergy = trendEnergy + detailEnergy;
        
        return totalEnergy > 0 ? trendEnergy / totalEnergy : 0;
    }
    
    private static double calculateVolatility(double[] details) {
        return Math.sqrt(Arrays.stream(details).map(x -> x * x).average().orElse(0));
    }
    
    private static double[] detectVolatilityClusters(double[] details) {
        // Simple volatility clustering detection
        return Arrays.stream(details).filter(x -> Math.abs(x) > 0.01).toArray();
    }
    
    private static double calculateEnergy(double[] coefficients) {
        return Arrays.stream(coefficients).map(x -> x * x).sum();
    }
}