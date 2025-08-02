package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for financial time series wavelet analysis.
 * Tests extreme scenarios that can occur in financial markets.
 */
class FinancialEdgeCasesTest {
    
    private static final double EPSILON = 1e-8; // Relaxed for financial data reconstruction
    
    @Test
    @DisplayName("Handle market halt (constant values)")
    void testMarketHaltConstantValues() {
        // When markets halt, prices remain constant
        double[] haltedPrices = new double[256];
        java.util.Arrays.fill(haltedPrices, 100.0);
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(haltedPrices);
        
        // All detail coefficients should be zero
        double[] details = result.detailCoeffs();
        for (double d : details) {
            assertEquals(0.0, d, EPSILON, "Detail coefficients should be zero for constant signal");
        }
        
        // Perfect reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(haltedPrices, reconstructed, EPSILON);
    }
    
    @Test
    @DisplayName("Handle limit moves (maximum daily change)")
    void testLimitMoves() {
        // Simulate limit up/down moves (e.g., 10% circuit breaker)
        double[] prices = new double[256];
        prices[0] = 100.0;
        
        for (int i = 1; i < prices.length; i++) {
            if (i == 50) {
                prices[i] = prices[i-1] * 1.10; // Limit up
            } else if (i == 150) {
                prices[i] = prices[i-1] * 0.90; // Limit down
            } else {
                prices[i] = prices[i-1] * (1 + 0.001 * (Math.random() - 0.5));
            }
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(prices);
        double[] reconstructed = transform.inverse(result);
        
        // Verify limit moves are preserved
        assertEquals(prices[50], reconstructed[50], 1e-10, "Limit up move should be preserved");
        assertEquals(prices[150], reconstructed[150], 1e-10, "Limit down move should be preserved");
    }
    
    @Test
    @DisplayName("Handle overnight gaps")
    void testOvernightGaps() {
        // Simulate overnight gaps in price data
        double[] prices = new double[64]; // Smaller for clarity
        double lastClose = 100.0;
        
        for (int i = 0; i < prices.length; i++) {
            if (i % 8 == 0 && i > 0) {
                // Overnight gap
                double gapPercent = 0.02 * (Math.random() - 0.5); // +/- 1% gap
                prices[i] = lastClose * (1 + gapPercent);
            } else {
                // Intraday movement
                prices[i] = (i == 0) ? lastClose : prices[i-1] * (1 + 0.001 * (Math.random() - 0.5));
            }
            
            if ((i + 1) % 8 == 0) {
                lastClose = prices[i]; // End of day
            }
        }
        
        WaveletTransform transform = new WaveletTransform(Symlet.SYM2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(prices);
        double[] reconstructed = transform.inverse(result);
        
        assertArrayEquals(prices, reconstructed, 1e-10, "Overnight gaps should be preserved");
    }
    
    @Test
    @DisplayName("Handle sparse trading (many zeros)")
    void testSparseTrading() {
        // Simulate illiquid asset with sparse trades
        double[] volumes = new double[256];
        double baseVolume = 1000;
        
        for (int i = 0; i < volumes.length; i++) {
            // Only 20% chance of trading
            if (Math.random() < 0.2) {
                volumes[i] = baseVolume * (0.5 + Math.random());
            } else {
                volumes[i] = 0.0; // No trading
            }
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(volumes);
        double[] reconstructed = transform.inverse(result);
        
        // Count zeros
        int originalZeros = 0;
        int reconstructedZeros = 0;
        
        for (int i = 0; i < volumes.length; i++) {
            if (volumes[i] == 0.0) originalZeros++;
            if (Math.abs(reconstructed[i]) < EPSILON) reconstructedZeros++;
        }
        
        // Note: Wavelet transforms may not preserve exact zeros due to filter convolution
        // But the reconstruction should be very close to the original
        assertArrayEquals(volumes, reconstructed, 1e-6, "Sparse data should be accurately reconstructed");
        
        // Verify that most zero values remain very small
        int nearZeros = 0;
        for (int i = 0; i < volumes.length; i++) {
            if (volumes[i] == 0.0 && Math.abs(reconstructed[i]) < 1e-6) {
                nearZeros++;
            }
        }
        assertTrue(nearZeros > originalZeros * 0.8, 
            "Most zero values should remain very small after reconstruction");
    }
    
    @Test
    @DisplayName("Handle extreme outliers (fat tails)")
    void testExtremeOutliers() {
        // Generate returns with fat tails (common in finance)
        double[] returns = new double[512];
        
        for (int i = 0; i < returns.length; i++) {
            double u = Math.random();
            if (u < 0.01) {
                // 1% chance of extreme event
                returns[i] = -0.10 + 0.05 * Math.random(); // -10% to -5% crash
            } else if (u > 0.99) {
                // 1% chance of extreme positive
                returns[i] = 0.05 + 0.05 * Math.random(); // +5% to +10% jump
            } else {
                // Normal returns
                returns[i] = 0.001 * (Math.random() - 0.5);
            }
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(returns);
        double[] reconstructed = transform.inverse(result);
        
        // Find and verify extreme values
        double minOriginal = Double.MAX_VALUE;
        double maxOriginal = -Double.MAX_VALUE;
        int minIndex = -1;
        int maxIndex = -1;
        
        for (int i = 0; i < returns.length; i++) {
            if (returns[i] < minOriginal) {
                minOriginal = returns[i];
                minIndex = i;
            }
            if (returns[i] > maxOriginal) {
                maxOriginal = returns[i];
                maxIndex = i;
            }
        }
        
        assertEquals(minOriginal, reconstructed[minIndex], EPSILON,
            "Extreme negative value should be preserved");
        assertEquals(maxOriginal, reconstructed[maxIndex], EPSILON,
            "Extreme positive value should be preserved");
    }
    
    @Test
    @DisplayName("Handle tick data precision")
    void testTickDataPrecision() {
        // Financial tick data often has specific decimal precision
        double[] ticks = new double[128];
        double basePrice = 123.45;
        
        for (int i = 0; i < ticks.length; i++) {
            // Prices move in minimum tick size (e.g., $0.01)
            int tickMove = (int)(3 * (Math.random() - 0.5)); // -1, 0, or 1 tick
            ticks[i] = Math.round((basePrice + tickMove * 0.01) * 100) / 100.0;
            basePrice = ticks[i];
        }
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(ticks);
        double[] reconstructed = transform.inverse(result);
        
        // Check precision is maintained
        for (int i = 0; i < ticks.length; i++) {
            double rounded = Math.round(reconstructed[i] * 100) / 100.0;
            assertEquals(ticks[i], reconstructed[i], 1e-10,
                "Tick precision should be perfectly preserved at index " + i);
        }
    }
    
    @Test
    @DisplayName("Handle currency pairs (small decimal values)")
    void testCurrencyPairs() {
        // Forex data often has very small changes
        double[] eurUsd = new double[256];
        eurUsd[0] = 1.1850; // Starting EUR/USD rate
        
        for (int i = 1; i < eurUsd.length; i++) {
            // Typical forex movement in pips (0.0001)
            double pipMove = (Math.random() - 0.5) * 10; // +/- 5 pips
            eurUsd[i] = eurUsd[i-1] + pipMove * 0.0001;
        }
        
        WaveletTransform transform = new WaveletTransform(Symlet.SYM3, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(eurUsd);
        double[] reconstructed = transform.inverse(result);
        
        // Verify pip-level precision
        for (int i = 0; i < eurUsd.length; i++) {
            assertEquals(eurUsd[i], reconstructed[i], 1e-10,
                "Forex precision should be maintained at index " + i);
        }
    }
    
    @Test
    @DisplayName("Handle negative prices (spreads)")
    void testNegativePrices() {
        // Some financial instruments can have negative values (e.g., spreads, basis)
        double[] spread = new double[256];
        
        for (int i = 0; i < spread.length; i++) {
            // Oscillating around zero
            spread[i] = 2.0 * Math.sin(2 * Math.PI * i / 50) + 0.5 * (Math.random() - 0.5);
        }
        
        WaveletTransform transform = new WaveletTransform(Daubechies.DB2, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(spread);
        double[] reconstructed = transform.inverse(result);
        
        // Count sign changes
        int originalNegatives = 0;
        int reconstructedNegatives = 0;
        
        for (int i = 0; i < spread.length; i++) {
            if (spread[i] < 0) originalNegatives++;
            if (reconstructed[i] < 0) reconstructedNegatives++;
        }
        
        assertEquals(originalNegatives, reconstructedNegatives,
            "Number of negative values should be approximately preserved");
        assertArrayEquals(spread, reconstructed, EPSILON,
            "Negative values should be perfectly reconstructed");
    }
    
    @Test
    @DisplayName("Handle high-frequency data patterns")
    void testHighFrequencyPatterns() {
        // Simulate high-frequency trading patterns
        double[] microstructure = new double[1024];
        
        // Base price with microstructure noise
        for (int i = 0; i < microstructure.length; i++) {
            double trend = 100.0 + 0.001 * i;
            double bidAskBounce = 0.01 * ((i % 2 == 0) ? 1 : -1); // Bid-ask bounce
            double noise = 0.005 * (Math.random() - 0.5);
            microstructure[i] = trend + bidAskBounce + noise;
        }
        
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(microstructure);
        
        // High-frequency components should be in detail coefficients
        double[] details = result.detailCoeffs();
        double detailEnergy = 0;
        for (double d : details) {
            detailEnergy += d * d;
        }
        
        assertTrue(detailEnergy > 0, "Detail coefficients should capture high-frequency noise");
        
        // Perfect reconstruction despite high-frequency components
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(microstructure, reconstructed, EPSILON,
            "High-frequency patterns should be preserved");
    }
}