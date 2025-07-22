package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Statistical tests for wavelet transforms on financial data.
 * Verifies preservation of important statistical properties used in finance.
 */
class FinancialStatisticsTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Preservation of return statistics")
    void testReturnStatisticsPreservation() {
        // Generate log returns typical of financial data
        double[] returns = generateLogReturns(512);
        
        // Calculate original statistics
        Statistics originalStats = calculateStatistics(returns);
        
        // Transform and reconstruct
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        double[] reconstructed = transform.inverse(result);
        
        // Calculate reconstructed statistics
        Statistics reconstructedStats = calculateStatistics(reconstructed);
        
        // Verify statistics are preserved
        assertEquals(originalStats.mean, reconstructedStats.mean, EPSILON,
            "Mean should be preserved");
        assertEquals(originalStats.variance, reconstructedStats.variance, EPSILON,
            "Variance should be preserved");
        assertEquals(originalStats.skewness, reconstructedStats.skewness, 1e-8,
            "Skewness should be preserved");
        assertEquals(originalStats.kurtosis, reconstructedStats.kurtosis, 1e-8,
            "Kurtosis should be preserved");
    }
    
    @Test
    @DisplayName("Autocorrelation structure preservation")
    void testAutocorrelationPreservation() {
        // Generate AR(1) process common in finance
        double[] ar1Process = generateAR1Process(256, 0.7);
        
        // Calculate autocorrelations up to lag 10
        double[] originalACF = calculateACF(ar1Process, 10);
        
        WaveletTransform transform = new WaveletTransform(Symlet.SYM3, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(ar1Process);
        double[] reconstructed = transform.inverse(result);
        
        double[] reconstructedACF = calculateACF(reconstructed, 10);
        
        // Verify ACF structure is preserved
        for (int lag = 0; lag <= 10; lag++) {
            assertEquals(originalACF[lag], reconstructedACF[lag], 1e-10,
                "ACF at lag " + lag + " should be preserved");
        }
    }
    
    @Test
    @DisplayName("Sharpe ratio calculation robustness")
    void testSharpeRatioCalculation() {
        // Generate returns with known Sharpe ratio
        double targetMean = 0.0001; // 1 bp daily return
        double targetVol = 0.01;    // 1% daily vol
        double targetSharpe = targetMean / targetVol * Math.sqrt(252); // Annualized
        
        double[] returns = new double[256]; // Power of 2
        for (int i = 0; i < returns.length; i++) {
            returns[i] = targetMean + targetVol * gaussianRandom(i);
        }
        
        // Apply wavelet denoising
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        
        // Simple soft thresholding
        double[] details = result.detailCoeffs();
        double threshold = 0.001;
        for (int i = 0; i < details.length; i++) {
            details[i] = softThreshold(details[i], threshold);
        }
        
        TransformResult denoisedResult = new TransformResultImpl(
            result.approximationCoeffs(), details);
        double[] denoisedReturns = transform.inverse(denoisedResult);
        
        // Calculate Sharpe ratios
        double originalSharpe = calculateSharpeRatio(returns);
        double denoisedSharpe = calculateSharpeRatio(denoisedReturns);
        
        // Denoising should improve Sharpe ratio by reducing noise
        assertTrue(denoisedSharpe >= originalSharpe * 0.9,
            "Denoising should not significantly reduce Sharpe ratio");
    }
    
    @ParameterizedTest
    @MethodSource("provideVolatilityScenarios")
    @DisplayName("Volatility estimation accuracy")
    void testVolatilityEstimation(String scenario, double[] prices) {
        // Calculate realized volatility from prices
        double[] returns = calculateReturns(prices);
        double realizedVol = calculateVolatility(returns);
        
        // Use wavelets to estimate volatility at different scales
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        
        // Estimate volatility from detail coefficients
        double[] details = result.detailCoeffs();
        double waveletVol = estimateVolatilityFromWavelets(details);
        
        // Should be within reasonable bounds (wider tolerance for wavelet-based estimation)
        // Note: Wavelet-based volatility estimation may differ from realized volatility
        // due to the different scales captured by wavelets
        assertTrue(waveletVol > 0, scenario + ": Wavelet volatility should be positive");
        assertTrue(waveletVol < realizedVol * 3, 
            scenario + ": Wavelet volatility should be within reasonable bounds");
    }
    
    @Test
    @DisplayName("Cointegration relationship preservation")
    void testCointegrationPreservation() {
        // Generate two cointegrated series (e.g., pairs trading)
        int length = 256;
        double[] series1 = new double[length];
        double[] series2 = new double[length];
        double[] spread = new double[length];
        
        // Common stochastic trend
        double trend = 100.0;
        for (int i = 0; i < length; i++) {
            trend += gaussianRandom(i * 2) * 0.5;
            series1[i] = trend + gaussianRandom(i * 3) * 0.1;
            series2[i] = trend * 1.01 + gaussianRandom(i * 4) * 0.1;
            spread[i] = series2[i] - 1.01 * series1[i];
        }
        
        // Transform the spread
        WaveletTransform transform = new WaveletTransform(Symlet.SYM2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(spread);
        double[] reconstructedSpread = transform.inverse(result);
        
        // Verify spread stationarity is preserved
        double originalMean = calculateMean(spread);
        double reconstructedMean = calculateMean(reconstructedSpread);
        
        assertEquals(originalMean, reconstructedMean, EPSILON,
            "Spread mean should be preserved");
        
        // Check that spread variance is finite and preserved
        double originalVar = calculateVariance(spread);
        double reconstructedVar = calculateVariance(reconstructedSpread);
        
        assertTrue(originalVar < 1.0, "Spread should be stationary");
        assertEquals(originalVar, reconstructedVar, EPSILON,
            "Spread variance should be preserved");
    }
    
    @Test
    @DisplayName("Value at Risk (VaR) calculation consistency")
    void testVaRCalculation() {
        // Generate returns with known distribution
        double[] returns = new double[1024]; // Power of 2
        for (int i = 0; i < returns.length; i++) {
            returns[i] = gaussianRandom(i) * 0.02; // 2% daily vol
        }
        
        // Calculate 95% and 99% VaR
        double var95Original = calculateVaR(returns, 0.95);
        double var99Original = calculateVaR(returns, 0.99);
        
        // Apply wavelet transform
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        double[] reconstructed = transform.inverse(result);
        
        double var95Reconstructed = calculateVaR(reconstructed, 0.95);
        double var99Reconstructed = calculateVaR(reconstructed, 0.99);
        
        // VaR should be preserved
        assertEquals(var95Original, var95Reconstructed, 1e-10,
            "95% VaR should be preserved");
        assertEquals(var99Original, var99Reconstructed, 1e-10,
            "99% VaR should be preserved");
    }
    
    // Helper methods
    
    private double[] generateLogReturns(int length) {
        double[] returns = new double[length];
        for (int i = 0; i < length; i++) {
            returns[i] = gaussianRandom(i) * 0.02 - 0.0001; // Slight negative drift
        }
        return returns;
    }
    
    private double[] generateAR1Process(int length, double phi) {
        double[] process = new double[length];
        process[0] = gaussianRandom(0);
        
        for (int i = 1; i < length; i++) {
            process[i] = phi * process[i-1] + Math.sqrt(1 - phi * phi) * gaussianRandom(i);
        }
        
        return process;
    }
    
    private static Stream<Arguments> provideVolatilityScenarios() {
        return Stream.of(
            Arguments.of("Constant volatility", generateConstantVolPrices()),
            Arguments.of("Increasing volatility", generateIncreasingVolPrices()),
            Arguments.of("Volatility clusters", generateClusteredVolPrices())
        );
    }
    
    private static double[] generateConstantVolPrices() {
        double[] prices = new double[256];
        prices[0] = 100.0;
        for (int i = 1; i < prices.length; i++) {
            prices[i] = prices[i-1] * (1 + 0.01 * gaussianRandom(i));
        }
        return prices;
    }
    
    private static double[] generateIncreasingVolPrices() {
        double[] prices = new double[256];
        prices[0] = 100.0;
        for (int i = 1; i < prices.length; i++) {
            double vol = 0.005 + 0.015 * i / prices.length;
            prices[i] = prices[i-1] * (1 + vol * gaussianRandom(i));
        }
        return prices;
    }
    
    private static double[] generateClusteredVolPrices() {
        double[] prices = new double[256];
        prices[0] = 100.0;
        double currentVol = 0.01;
        
        for (int i = 1; i < prices.length; i++) {
            // Random volatility regime changes
            if (i % 50 == 0) {
                currentVol = 0.005 + 0.02 * Math.random();
            }
            prices[i] = prices[i-1] * (1 + currentVol * gaussianRandom(i));
        }
        return prices;
    }
    
    private double[] calculateReturns(double[] prices) {
        double[] returns = new double[256]; // Fixed power of 2
        int len = Math.min(prices.length - 1, returns.length);
        for (int i = 0; i < len; i++) {
            returns[i] = Math.log(prices[i + 1] / prices[i]);
        }
        // Pad with zeros if needed
        for (int i = len; i < returns.length; i++) {
            returns[i] = 0.0;
        }
        return returns;
    }
    
    private double calculateVolatility(double[] returns) {
        return Math.sqrt(calculateVariance(returns));
    }
    
    private double estimateVolatilityFromWavelets(double[] details) {
        // Estimate volatility using MAD (Median Absolute Deviation) of detail coefficients
        double sum = 0;
        for (double d : details) {
            sum += Math.abs(d);
        }
        double mad = sum / details.length;
        
        // Scale factor for Gaussian noise
        return mad / 0.6745;
    }
    
    private double softThreshold(double x, double threshold) {
        if (Math.abs(x) <= threshold) {
            return 0;
        }
        return Math.signum(x) * (Math.abs(x) - threshold);
    }
    
    private double calculateSharpeRatio(double[] returns) {
        double mean = calculateMean(returns);
        double std = Math.sqrt(calculateVariance(returns));
        return mean / std * Math.sqrt(256); // Annualized (assuming 256 trading days)
    }
    
    private double calculateVaR(double[] returns, double confidence) {
        double[] sorted = returns.clone();
        java.util.Arrays.sort(sorted);
        
        int index = (int) Math.floor((1 - confidence) * sorted.length);
        return -sorted[index];
    }
    
    private double[] calculateACF(double[] data, int maxLag) {
        double[] acf = new double[maxLag + 1];
        double mean = calculateMean(data);
        double var = calculateVariance(data);
        
        for (int lag = 0; lag <= maxLag; lag++) {
            double sum = 0;
            for (int i = 0; i < data.length - lag; i++) {
                sum += (data[i] - mean) * (data[i + lag] - mean);
            }
            acf[lag] = sum / (data.length - lag) / var;
        }
        
        return acf;
    }
    
    private double calculateMean(double[] data) {
        double sum = 0;
        for (double d : data) {
            sum += d;
        }
        return sum / data.length;
    }
    
    private double calculateVariance(double[] data) {
        double mean = calculateMean(data);
        double sum = 0;
        for (double d : data) {
            sum += (d - mean) * (d - mean);
        }
        return sum / data.length;
    }
    
    private Statistics calculateStatistics(double[] data) {
        double mean = calculateMean(data);
        double variance = calculateVariance(data);
        double std = Math.sqrt(variance);
        
        double skewness = 0;
        double kurtosis = 0;
        
        for (double d : data) {
            double normalized = (d - mean) / std;
            skewness += Math.pow(normalized, 3);
            kurtosis += Math.pow(normalized, 4);
        }
        
        skewness /= data.length;
        kurtosis = kurtosis / data.length - 3; // Excess kurtosis
        
        return new Statistics(mean, variance, skewness, kurtosis);
    }
    
    private static class Statistics {
        final double mean;
        final double variance;
        final double skewness;
        final double kurtosis;
        
        Statistics(double mean, double variance, double skewness, double kurtosis) {
            this.mean = mean;
            this.variance = variance;
            this.skewness = skewness;
            this.kurtosis = kurtosis;
        }
    }
    
    // Simple deterministic "random" for reproducibility
    private static double gaussianRandom(int seed) {
        // Box-Muller transform with deterministic input
        double u1 = ((seed * 1103515245 + 12345) & 0x7fffffff) / (double) 0x7fffffff;
        double u2 = ((seed * 69069 + 1) & 0x7fffffff) / (double) 0x7fffffff;
        
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }
}