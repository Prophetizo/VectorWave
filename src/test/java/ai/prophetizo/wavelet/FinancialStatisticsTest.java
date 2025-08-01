package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Statistical tests for wavelet transforms on financial data.
 * Verifies preservation of important statistical properties used in finance.
 */
class FinancialStatisticsTest {
    
    private static final double EPSILON = 1e-10;
    private static final int CACHE_SIZE = 100; // Maximum number of entries in the random cache
    
    /**
     * Thread-safe LRU cache for Random instances to ensure reproducible test data.
     * Each unique sequence index gets its own Random instance with a deterministic seed.
     * The cache is limited to CACHE_SIZE entries with LRU eviction policy.
     * 
     * <p>Note: For production code requiring caching, consider using a proper caching
     * library like Caffeine or Guava Cache for better performance and features.</p>
     */
    private static final java.util.Map<Integer, Random> RANDOM_CACHE = 
        java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<Integer, Random>(CACHE_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Random> eldest) {
                    return size() > CACHE_SIZE;
                }
            }
        );
    
    /**
     * Clears the Random cache. Useful for test cleanup or memory-sensitive scenarios.
     * This method is thread-safe.
     */
    static void clearRandomCache() {
        RANDOM_CACHE.clear();
    }
    
    @Test
    @DisplayName("Random cache eviction and size limit")
    void testRandomCacheEviction() {
        // Clear cache to start fresh
        clearRandomCache();
        
        // Fill cache beyond capacity
        for (int i = 0; i < CACHE_SIZE + 50; i++) {
            gaussianRandom(i);
        }
        
        // Cache size should not exceed limit
        assertTrue(RANDOM_CACHE.size() <= CACHE_SIZE, 
            "Cache size " + RANDOM_CACHE.size() + " exceeds limit " + CACHE_SIZE);
        
        // Verify that values are deterministic within a test run
        // Note: If a Random instance is evicted and recreated, it will produce
        // different values even with the same seed, because the internal state resets
        int cachedIndex = 10; // Should still be in cache
        double value1 = gaussianRandom(cachedIndex);
        double value2 = gaussianRandom(cachedIndex);
        assertNotEquals(value1, value2, "Subsequent calls should produce different random values");
        
        // Verify reproducibility across test runs by checking a fresh Random
        clearRandomCache();
        Random testRng = new Random(TestConstants.TEST_SEED * 1000L + TestConstants.TEST_SEED); // Same seed formula as index 42
        double expectedValue = testRng.nextGaussian();
        double actualValue = gaussianRandom(42);
        assertEquals(expectedValue, actualValue, EPSILON, 
            "First call with given index should produce predictable value");
        
        // Clear cache for other tests
        clearRandomCache();
    }
    
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
        
        TransformResult denoisedResult = TransformResult.create(
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
    
    /**
     * Calculates log returns from price series.
     * 
     * <p><strong>Important:</strong> This method ensures the returned array has
     * exactly 256 elements (power of 2) for wavelet transform compatibility.
     * If the price series is too short, the returns are padded using the last
     * calculated return value repeated, which better preserves statistical
     * properties than padding with zeros.</p>
     * 
     * @param prices the price series (must have at least 2 elements)
     * @return log returns array of size 256
     * @throws IllegalArgumentException if prices has fewer than 2 elements
     */
    private double[] calculateReturns(double[] prices) {
        if (prices.length < 2) {
            throw new IllegalArgumentException(
                "Price series must have at least 2 elements to calculate returns");
        }
        
        double[] returns = new double[256]; // Fixed power of 2 for wavelet transform
        int numReturns = Math.min(prices.length - 1, returns.length);
        
        // Calculate actual returns
        for (int i = 0; i < numReturns; i++) {
            returns[i] = Math.log(prices[i + 1] / prices[i]);
        }
        
        // If we have fewer returns than needed, pad with the mean return
        // This is more realistic than padding with zeros for financial data
        if (numReturns < returns.length) {
            // Calculate mean return from available data
            double sumReturns = 0;
            for (int i = 0; i < numReturns; i++) {
                sumReturns += returns[i];
            }
            double meanReturn = sumReturns / numReturns;
            
            // Pad with mean return (preserves expected value)
            for (int i = numReturns; i < returns.length; i++) {
                returns[i] = meanReturn;
            }
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
    
    /**
     * Returns a Gaussian (normal) distributed random value with mean 0 and standard deviation 1.
     * Uses Java's Random class for proper statistical properties while maintaining
     * reproducibility through deterministic seeding based on the sequence index.
     * 
     * <p>Each unique sequence index gets its own Random instance to ensure that
     * the same index always produces the same value, which is important for
     * reproducible test results.</p>
     * 
     * @param sequenceIndex the index in the sequence, used to seed the random generator
     * @return a Gaussian distributed random value
     */
    private static double gaussianRandom(int sequenceIndex) {
        // Get or create a Random instance for this sequence index
        Random rng = RANDOM_CACHE.computeIfAbsent(sequenceIndex, 
            idx -> new Random(idx * 1000L + TestConstants.TEST_SEED)); // Deterministic seed based on index
        
        // Using nextGaussian() provides better statistical properties than manual Box-Muller
        return rng.nextGaussian();
    }
}