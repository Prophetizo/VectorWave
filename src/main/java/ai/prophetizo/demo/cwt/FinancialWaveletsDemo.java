package ai.prophetizo.demo.cwt;

import ai.prophetizo.wavelet.cwt.finance.*;
import ai.prophetizo.wavelet.cwt.*;

/**
 * Demonstrates financial wavelets for market analysis.
 * 
 * <p>This demo showcases specialized wavelets designed for financial time series
 * analysis, including market crash detection and volatility analysis.</p>
 */
public class FinancialWaveletsDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Financial Wavelets Demo ===\n");
        
        // Create synthetic market data
        double[] priceData = createMarketData();
        double[] volumeData = createVolumeData(priceData.length);
        
        System.out.printf("Generated market data: %d days%n%n", priceData.length);
        
        // Demo 1: Paul wavelet for volatility detection
        paulWaveletAnalysis(priceData);
        
        // Demo 2: Shannon wavelet for frequency analysis
        shannonWaveletAnalysis(priceData);
        
        // Demo 3: DOG wavelet for edge detection
        dogWaveletAnalysis(priceData);
        
        // Demo 4: Comprehensive financial analysis
        comprehensiveFinancialAnalysis(priceData, volumeData);
    }
    
    /**
     * Creates synthetic market price data with trends, cycles, and crashes.
     */
    private static double[] createMarketData() {
        int days = 512;
        double[] prices = new double[days];
        double basePrice = 100.0;
        
        for (int i = 0; i < days; i++) {
            double t = (double) i / days;
            
            // Base upward trend
            prices[i] = basePrice + 0.5 * i;
            
            // Weekly cycle (5-day work week)
            prices[i] += 5.0 * Math.sin(2 * Math.PI * i / 5);
            
            // Monthly cycle (22 trading days)
            prices[i] += 3.0 * Math.sin(2 * Math.PI * i / 22);
            
            // Quarterly earnings cycle
            prices[i] += 8.0 * Math.sin(2 * Math.PI * i / 66);
            
            // Market crash simulation at day 200
            if (i >= 200 && i < 220) {
                double crashFactor = Math.exp(-(i - 200) / 5.0);
                prices[i] -= 25.0 * crashFactor;
            }
            
            // Bull run at day 350
            if (i >= 350 && i < 400) {
                double bullFactor = Math.exp(-(Math.abs(i - 375)) / 10.0);
                prices[i] += 15.0 * bullFactor;
            }
            
            // Random noise (volatility)
            prices[i] += 2.0 * (Math.random() - 0.5);
        }
        
        return prices;
    }
    
    /**
     * Creates synthetic volume data correlated with price movements.
     */
    private static double[] createVolumeData(int length) {
        double[] volume = new double[length];
        double baseVolume = 1000000; // 1M shares base
        
        for (int i = 0; i < length; i++) {
            volume[i] = baseVolume + 100000 * Math.random();
            
            // Higher volume during crash (days 195-225)
            if (i >= 195 && i < 225) {
                volume[i] *= 3.0;
            }
            
            // Higher volume during bull run (days 345-405)
            if (i >= 345 && i < 405) {
                volume[i] *= 2.0;
            }
        }
        
        return volume;
    }
    
    /**
     * Demonstrates Paul wavelet for detecting market volatility.
     */
    private static void paulWaveletAnalysis(double[] prices) {
        System.out.println("1. Paul Wavelet Analysis (Volatility Detection)");
        System.out.println("===============================================");
        
        // Paul wavelet is excellent for detecting rapid changes and volatility
        PaulWavelet paulWavelet = new PaulWavelet(4); // Order 4 for good time-frequency resolution
        
        System.out.printf("Using Paul wavelet of order %d%n", paulWavelet.getOrder());
        System.out.printf("Bandwidth: %.3f, Center frequency: %.3f%n", 
            paulWavelet.bandwidth(), paulWavelet.centerFrequency());
        
        CWTTransform cwt = new CWTTransform(paulWavelet);
        
        // Focus on short to medium-term volatility (1-50 day scales)
        double[] scales = ScaleSpace.logarithmic(1.0, 50.0, 30).getScales();
        
        long startTime = System.nanoTime();
        CWTResult result = cwt.analyze(prices, scales);
        long endTime = System.nanoTime();
        
        System.out.printf("Analysis completed in %.2f ms%n", (endTime - startTime) / 1e6);
        
        // Find periods of high volatility
        double[][] coeffs = result.getCoefficients();
        double[] volatilityIndex = computeVolatilityIndex(coeffs);
        
        System.out.println("\nHigh volatility periods detected:");
        findVolatilityPeaks(volatilityIndex, prices.length);
        
        System.out.println();
    }
    
    /**
     * Demonstrates Shannon wavelet for frequency domain analysis.
     */
    private static void shannonWaveletAnalysis(double[] prices) {
        System.out.println("2. Shannon-Gabor Wavelet Analysis (Frequency Components)");
        System.out.println("========================================================");
        
        // Shannon-Gabor wavelet provides excellent frequency localization
        ShannonGaborWavelet shannonWavelet = new ShannonGaborWavelet();
        
        System.out.printf("Using Shannon wavelet%n");
        System.out.printf("Bandwidth: %.3f, Center frequency: %.3f%n", 
            shannonWavelet.bandwidth(), shannonWavelet.centerFrequency());
        
        CWTTransform cwt = new CWTTransform(shannonWavelet);
        
        // Analyze cycles from daily to quarterly
        double[] scales = ScaleSpace.logarithmic(1.0, 100.0, 25).getScales();
        
        CWTResult result = cwt.analyze(prices, scales);
        double[][] coeffs = result.getCoefficients();
        
        // Analyze frequency content at different scales
        System.out.println("\nDominant frequency components:");
        analyzeFrequencyComponents(coeffs, scales);
        
        System.out.println();
    }
    
    /**
     * Demonstrates DOG wavelet for edge detection in price movements.
     */
    private static void dogWaveletAnalysis(double[] prices) {
        System.out.println("3. DOG Wavelet Analysis (Price Movement Edges)");
        System.out.println("==============================================");
        
        // DOG (Difference of Gaussians) wavelet is excellent for edge detection
        DOGWavelet dogWavelet = new DOGWavelet(2); // σ = 2 for medium-term edge detection
        
        System.out.printf("Using DOG wavelet with σ = %.1f%n", 2.0);
        System.out.printf("Bandwidth: %.3f%n", dogWavelet.bandwidth());
        
        CWTTransform cwt = new CWTTransform(dogWavelet);
        
        // Focus on short-term price movements (1-20 day scales)
        double[] scales = ScaleSpace.logarithmic(1.0, 20.0, 20).getScales();
        
        CWTResult result = cwt.analyze(prices, scales);
        double[][] coeffs = result.getCoefficients();
        
        // Find significant price movement edges
        System.out.println("\nSignificant price movement edges:");
        findPriceEdges(coeffs, scales, prices.length);
        
        System.out.println();
    }
    
    /**
     * Demonstrates comprehensive financial analysis using FinancialWaveletAnalyzer.
     */
    private static void comprehensiveFinancialAnalysis(double[] prices, double[] volume) {
        System.out.println("4. Comprehensive Financial Analysis");
        System.out.println("===================================");
        
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
        
        // Market crash detection
        System.out.println("Market Crash Detection:");
        double crashThreshold = 15.0; // Threshold for crash detection
        
        FinancialWaveletAnalyzer.CrashDetectionResult crashResult = 
            analyzer.detectMarketCrashes(prices, crashThreshold);
        
        System.out.printf("Crash detection threshold: %.1f%n", crashThreshold);
        System.out.printf("Number of crash points detected: %d%n", 
            crashResult.getCrashPoints().size());
        
        if (!crashResult.getCrashPoints().isEmpty()) {
            System.out.println("Detected crash points:");
            for (int point : crashResult.getCrashPoints()) {
                System.out.printf("  Day %d: Price = %.2f%n", point, prices[point]);
            }
        }
        
        // Basic volatility analysis (compute standard deviation in windows)
        System.out.println("\nVolatility Analysis:");
        double averageVolatility = computeVolatility(prices, 20);
        int maxVolatilityIndex = findMaxVolatilityPeriod(prices, 20);
        
        System.out.printf("Average volatility: %.4f%n", averageVolatility);
        System.out.printf("Maximum volatility period around day %d%n", maxVolatilityIndex);
        
        // Simple trend analysis
        System.out.println("\nTrend Analysis:");
        double overallTrend = computeTrend(prices);
        
        System.out.printf("Overall trend direction: %s%n", 
            overallTrend > 0 ? "BULLISH" : "BEARISH");
        System.out.printf("Trend strength: %.4f%n", Math.abs(overallTrend));
        
        // Volume-price correlation
        System.out.println("\nVolume-Price Correlation:");
        double correlation = computeCorrelation(prices, volume);
        System.out.printf("Volume-price correlation: %.4f%n", correlation);
        if (Math.abs(correlation) > 0.3) {
            System.out.printf("Strong %s correlation detected%n", 
                correlation > 0 ? "positive" : "negative");
        } else {
            System.out.println("Weak correlation between volume and price");
        }
    }
    
    // Helper methods
    
    /**
     * Computes a volatility index from CWT coefficients.
     */
    private static double[] computeVolatilityIndex(double[][] coeffs) {
        int timePoints = coeffs[0].length;
        double[] volatility = new double[timePoints];
        
        for (int t = 0; t < timePoints; t++) {
            double energy = 0;
            for (int s = 0; s < coeffs.length; s++) {
                energy += coeffs[s][t] * coeffs[s][t];
            }
            volatility[t] = Math.sqrt(energy);
        }
        
        return volatility;
    }
    
    /**
     * Finds peaks in the volatility index.
     */
    private static void findVolatilityPeaks(double[] volatility, int totalDays) {
        // Compute statistics
        double mean = 0, max = 0;
        for (double v : volatility) {
            mean += v;
            max = Math.max(max, v);
        }
        mean /= volatility.length;
        
        double threshold = mean + 2.0 * computeStdDev(volatility, mean);
        
        System.out.printf("  Volatility threshold: %.3f (mean + 2σ)%n", threshold);
        
        int peakCount = 0;
        for (int i = 1; i < volatility.length - 1; i++) {
            if (volatility[i] > threshold && 
                volatility[i] > volatility[i-1] && 
                volatility[i] > volatility[i+1]) {
                
                int day = (int)((double)i / volatility.length * totalDays);
                System.out.printf("  Day %d: Volatility = %.3f%n", day, volatility[i]);
                peakCount++;
            }
        }
        
        if (peakCount == 0) {
            System.out.println("  No significant volatility peaks detected");
        }
    }
    
    /**
     * Analyzes frequency components in the CWT result.
     */
    private static void analyzeFrequencyComponents(double[][] coeffs, double[] scales) {
        // Compute energy at each scale
        double[] scaleEnergy = new double[scales.length];
        
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < coeffs[s].length; t++) {
                scaleEnergy[s] += coeffs[s][t] * coeffs[s][t];
            }
            scaleEnergy[s] = Math.sqrt(scaleEnergy[s]);
        }
        
        // Find dominant scales
        double maxEnergy = 0;
        for (double energy : scaleEnergy) {
            maxEnergy = Math.max(maxEnergy, energy);
        }
        
        double threshold = maxEnergy * 0.3; // 30% of maximum
        
        for (int s = 0; s < scales.length; s++) {
            if (scaleEnergy[s] > threshold) {
                double period = scales[s]; // Approximate period in days
                System.out.printf("  Scale %.1f (~%.1f day period): Energy = %.2e%n", 
                    scales[s], period, scaleEnergy[s]);
            }
        }
    }
    
    /**
     * Finds significant edges in price movements.
     */
    private static void findPriceEdges(double[][] coeffs, double[] scales, int totalDays) {
        // Focus on short-term scales (first few scales)
        int maxScale = Math.min(5, scales.length);
        
        for (int s = 0; s < maxScale; s++) {
            double[] scaleCoeffs = coeffs[s];
            
            // Find local maxima (edges)
            double threshold = computeThreshold(scaleCoeffs, 2.0);
            
            System.out.printf("  Scale %.1f (threshold: %.3f):%n", scales[s], threshold);
            
            int edgeCount = 0;
            for (int t = 1; t < scaleCoeffs.length - 1; t++) {
                if (Math.abs(scaleCoeffs[t]) > threshold &&
                    Math.abs(scaleCoeffs[t]) > Math.abs(scaleCoeffs[t-1]) &&
                    Math.abs(scaleCoeffs[t]) > Math.abs(scaleCoeffs[t+1])) {
                    
                    int day = (int)((double)t / scaleCoeffs.length * totalDays);
                    System.out.printf("    Day %d: Edge strength = %.3f%n", 
                        day, scaleCoeffs[t]);
                    edgeCount++;
                    
                    if (edgeCount >= 5) { // Limit output
                        System.out.printf("    ... and %d more edges%n", 
                            countRemainingEdges(scaleCoeffs, t+1, threshold));
                        break;
                    }
                }
            }
            
            if (edgeCount == 0) {
                System.out.println("    No significant edges detected");
            }
        }
    }
    
    private static double computeStdDev(double[] values, double mean) {
        double variance = 0;
        for (double value : values) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.length);
    }
    
    private static double computeThreshold(double[] values, double sigmas) {
        double mean = 0;
        for (double value : values) {
            mean += Math.abs(value);
        }
        mean /= values.length;
        
        double stdDev = 0;
        for (double value : values) {
            double diff = Math.abs(value) - mean;
            stdDev += diff * diff;
        }
        stdDev = Math.sqrt(stdDev / values.length);
        
        return mean + sigmas * stdDev;
    }
    
    private static int countRemainingEdges(double[] coeffs, int startIndex, double threshold) {
        int count = 0;
        for (int t = startIndex; t < coeffs.length - 1; t++) {
            if (Math.abs(coeffs[t]) > threshold &&
                Math.abs(coeffs[t]) > Math.abs(coeffs[t-1]) &&
                Math.abs(coeffs[t]) > Math.abs(coeffs[t+1])) {
                count++;
            }
        }
        return count;
    }
    
    // Additional helper methods for financial analysis
    
    private static double computeVolatility(double[] prices, int windowSize) {
        double totalVolatility = 0;
        int count = 0;
        
        for (int i = windowSize; i < prices.length; i++) {
            double mean = 0;
            for (int j = i - windowSize; j < i; j++) {
                mean += prices[j];
            }
            mean /= windowSize;
            
            double variance = 0;
            for (int j = i - windowSize; j < i; j++) {
                double diff = prices[j] - mean;
                variance += diff * diff;
            }
            variance /= windowSize;
            
            totalVolatility += Math.sqrt(variance);
            count++;
        }
        
        return totalVolatility / count;
    }
    
    private static int findMaxVolatilityPeriod(double[] prices, int windowSize) {
        double maxVolatility = 0;
        int maxIndex = 0;
        
        for (int i = windowSize; i < prices.length; i++) {
            double variance = 0;
            double mean = 0;
            
            for (int j = i - windowSize; j < i; j++) {
                mean += prices[j];
            }
            mean /= windowSize;
            
            for (int j = i - windowSize; j < i; j++) {
                double diff = prices[j] - mean;
                variance += diff * diff;
            }
            variance /= windowSize;
            
            double volatility = Math.sqrt(variance);
            if (volatility > maxVolatility) {
                maxVolatility = volatility;
                maxIndex = i - windowSize / 2;
            }
        }
        
        return maxIndex;
    }
    
    private static double computeTrend(double[] prices) {
        // Simple linear regression slope
        int n = prices.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += prices[i];
            sumXY += i * prices[i];
            sumX2 += i * i;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    private static double computeCorrelation(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        
        return denominator != 0 ? numerator / denominator : 0;
    }
}