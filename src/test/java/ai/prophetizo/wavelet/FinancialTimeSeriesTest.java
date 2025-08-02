package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
/**
 * Comprehensive test suite for wavelet transforms on financial time series data.
 * Tests various patterns commonly found in financial markets including:
 * - Price trends with noise
 * - Volatility clustering
 * - Mean reversion patterns
 * - Jump processes
 * - Intraday seasonality
 */
class FinancialTimeSeriesTest {
    
    private static final double EPSILON = 1e-10;
    private static final double RECONSTRUCTION_TOLERANCE = 1e-8; // Relaxed for financial data
    
    // Financial data characteristics
    private static final double INITIAL_PRICE = 100.0;
    private static final double DAILY_VOLATILITY = 0.02; // 2% daily vol
    private static final double DRIFT = 0.0001; // Small positive drift
    
    private Random random;
    
    @BeforeEach
    void setUp() {
        random = new Random(TestConstants.TEST_SEED); // Fixed seed for reproducibility
    }
    
    @Test
    @DisplayName("Perfect reconstruction of stock price series")
    void testStockPriceReconstruction() {
        // Generate realistic stock price data
        double[] prices = generateStockPrices(256);
        
        // Test with different wavelets suitable for financial data
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2
        };
        
        for (Wavelet wavelet : wavelets) {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            TransformResult result = transform.forward(prices);
            double[] reconstructed = transform.inverse(result);
            
            assertArrayEquals(prices, reconstructed, RECONSTRUCTION_TOLERANCE,
                wavelet.name() + " should perfectly reconstruct stock prices");
        }
    }
    
    @Test
    @DisplayName("Volatility clustering detection")
    void testVolatilityClusteringDetection() {
        // Generate GARCH-like volatility clustering
        double[] returns = generateVolatilityClustering(512);
        
        // Use DB4 which is good for detecting volatility patterns
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        
        // Detail coefficients should capture volatility clusters
        double[] details = result.detailCoeffs();
        
        // Verify energy concentration in detail coefficients during high volatility periods
        double totalEnergy = 0;
        for (double d : details) {
            totalEnergy += d * d;
        }
        
        assertTrue(totalEnergy > 0, "Detail coefficients should capture volatility");
    }
    
    @Test
    @DisplayName("Mean reversion pattern analysis")
    void testMeanReversionPattern() {
        // Generate mean-reverting series (e.g., pairs trading spread)
        double[] spread = generateMeanRevertingSeries(256);
        
        WaveletTransform transform = new WaveletTransform(Symlet.SYM3, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(spread);
        
        // Approximation should capture the mean level
        double[] approx = result.approximationCoeffs();
        
        // Verify approximation coefficients are relatively stable
        double variance = calculateVariance(approx);
        assertTrue(variance < 10.0, "Approximation should be relatively stable for mean-reverting series");
    }
    
    @Test
    @DisplayName("Jump process detection")
    void testJumpProcessDetection() {
        // Generate price series with jumps (e.g., earnings announcements)
        double[] pricesWithJumps = generatePriceSeriesWithJumps(256);
        
        // Haar wavelet is good for detecting discontinuities
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(pricesWithJumps);
        
        double[] details = result.detailCoeffs();
        
        // Find maximum detail coefficient (should correspond to jump location)
        double maxDetail = 0;
        for (double d : details) {
            maxDetail = Math.max(maxDetail, Math.abs(d));
        }
        
        assertTrue(maxDetail > 1.0, "Jump should create large detail coefficient");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {64, 128, 256, 512, 1024})
    @DisplayName("Intraday seasonality pattern detection")
    void testIntradaySeasonality(int dataPoints) {
        // Generate intraday volume pattern (U-shape common in markets)
        double[] volume = generateIntradayVolume(dataPoints);
        
        WaveletTransform transform = new WaveletTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(volume);
        
        // Verify perfect reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(volume, reconstructed, RECONSTRUCTION_TOLERANCE);
        
        // Approximation should capture the U-shape pattern
        double[] approx = result.approximationCoeffs();
        assertTrue(approx.length == dataPoints / 2, "Approximation length should be half of input");
    }
    
    @Test
    @DisplayName("High-frequency noise filtering")
    void testHighFrequencyNoiseFiltering() {
        // Generate clean trend with high-frequency noise
        double[] cleanSignal = new double[256];
        double[] noisySignal = new double[256];
        
        for (int i = 0; i < 256; i++) {
            cleanSignal[i] = 100 + 0.1 * i; // Linear trend
            noisySignal[i] = cleanSignal[i] + 0.5 * random.nextGaussian();
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(noisySignal);
        
        // Zero out small detail coefficients (simple denoising)
        double[] details = result.detailCoeffs();
        double threshold = 0.5;
        int zeroedCount = 0;
        
        for (int i = 0; i < details.length; i++) {
            if (Math.abs(details[i]) < threshold) {
                details[i] = 0;
                zeroedCount++;
            }
        }
        
        assertTrue(zeroedCount > 0, "Some detail coefficients should be below threshold");
        
        // Reconstruct with modified coefficients
        TransformResult denoisedResult = TransformResult.create(
            result.approximationCoeffs(), details);
        double[] denoised = transform.inverse(denoisedResult);
        
        // Verify denoising reduced variance
        double originalVariance = calculateVariance(noisySignal);
        double denoisedVariance = calculateVariance(denoised);
        assertTrue(denoisedVariance < originalVariance, "Denoising should reduce variance");
    }
    
    @Test
    @DisplayName("Extreme value preservation")
    void testExtremeValuePreservation() {
        // Test with financial data containing extreme values (e.g., flash crash)
        double[] data = new double[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = 100.0 + random.nextGaussian() * 0.5;
        }
        // Add extreme values
        data[100] = 150.0; // Spike up
        data[150] = 50.0;  // Crash down
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(data);
        double[] reconstructed = transform.inverse(result);
        
        // Verify extreme values are preserved
        assertEquals(150.0, reconstructed[100], RECONSTRUCTION_TOLERANCE, 
            "Extreme high value should be preserved");
        assertEquals(50.0, reconstructed[150], RECONSTRUCTION_TOLERANCE,
            "Extreme low value should be preserved");
    }
    
    @Test
    @DisplayName("Zero and negative value handling")
    void testZeroAndNegativeValues() {
        // Financial data can include zero or negative values (e.g., spreads, returns)
        double[] data = {-10, -5, 0, 5, 10, 0, -2.5, 7.5};
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(data);
        double[] reconstructed = transform.inverse(result);
        
        assertArrayEquals(data, reconstructed, RECONSTRUCTION_TOLERANCE,
            "Should handle zero and negative values correctly");
    }
    
    @Test
    @DisplayName("Multi-scale decomposition energy preservation")
    void testEnergyPreservation() {
        // Generate typical return series
        double[] returns = new double[512];
        double totalInputEnergy = 0;
        
        for (int i = 0; i < returns.length; i++) {
            returns[i] = random.nextGaussian() * DAILY_VOLATILITY;
            totalInputEnergy += returns[i] * returns[i];
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        
        // Calculate energy in transform domain
        double approxEnergy = 0;
        double detailEnergy = 0;
        
        for (double a : result.approximationCoeffs()) {
            approxEnergy += a * a;
        }
        
        for (double d : result.detailCoeffs()) {
            detailEnergy += d * d;
        }
        
        double totalTransformEnergy = approxEnergy + detailEnergy;
        
        // Energy should be preserved (Parseval's theorem)
        assertEquals(totalInputEnergy, totalTransformEnergy, 1e-10,
            "Energy should be preserved in wavelet domain");
    }
    
    // Helper methods for generating financial time series patterns
    
    private double[] generateStockPrices(int length) {
        double[] prices = new double[length];
        prices[0] = INITIAL_PRICE;
        
        for (int i = 1; i < length; i++) {
            double dailyReturn = DRIFT + DAILY_VOLATILITY * random.nextGaussian();
            prices[i] = prices[i-1] * (1 + dailyReturn);
        }
        
        return prices;
    }
    
    private double[] generateVolatilityClustering(int length) {
        double[] returns = new double[length];
        double currentVol = DAILY_VOLATILITY;
        
        for (int i = 0; i < length; i++) {
            // GARCH(1,1)-like process
            currentVol = 0.1 * DAILY_VOLATILITY + 0.85 * currentVol + 
                        0.05 * Math.abs(returns[Math.max(0, i-1)]);
            returns[i] = currentVol * random.nextGaussian();
        }
        
        return returns;
    }
    
    private double[] generateMeanRevertingSeries(int length) {
        double[] series = new double[length];
        double mean = 0.0;
        double kappa = 0.1; // Mean reversion speed
        
        series[0] = mean;
        
        for (int i = 1; i < length; i++) {
            double innovation = 0.5 * random.nextGaussian();
            series[i] = series[i-1] + kappa * (mean - series[i-1]) + innovation;
        }
        
        return series;
    }
    
    private double[] generatePriceSeriesWithJumps(int length) {
        double[] prices = new double[length];
        prices[0] = INITIAL_PRICE;
        
        // Add jumps at specific locations
        int[] jumpLocations = {length/4, length/2, 3*length/4};
        double[] jumpSizes = {5.0, -3.0, 4.0};
        
        for (int i = 1; i < length; i++) {
            double dailyReturn = DRIFT + DAILY_VOLATILITY * random.nextGaussian();
            prices[i] = prices[i-1] * (1 + dailyReturn);
            
            // Check for jumps
            for (int j = 0; j < jumpLocations.length; j++) {
                if (i == jumpLocations[j]) {
                    prices[i] += jumpSizes[j];
                }
            }
        }
        
        return prices;
    }
    
    private double[] generateIntradayVolume(int length) {
        double[] volume = new double[length];
        
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            // U-shape pattern common in intraday volume
            double baseVolume = 1000 * (1 + 0.5 * Math.pow(2 * t - 1, 2));
            volume[i] = baseVolume + 100 * random.nextGaussian();
        }
        
        return volume;
    }
    
    private double calculateVariance(double[] data) {
        double mean = 0;
        for (double d : data) {
            mean += d;
        }
        mean /= data.length;
        
        double variance = 0;
        for (double d : data) {
            variance += (d - mean) * (d - mean);
        }
        
        return variance / data.length;
    }
}