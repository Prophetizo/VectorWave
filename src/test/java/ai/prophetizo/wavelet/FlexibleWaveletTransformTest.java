package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.finance.FinancialAnalysisParameters;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import ai.prophetizo.wavelet.util.ValidationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for flexible wavelet transform with automatic padding.
 */
class FlexibleWaveletTransformTest extends BaseWaveletTest {
    
    private static final double EPSILON = 1e-10;
    
    static Stream<Arguments> waveletAndPaddingProvider() {
        return Stream.of(
            Arguments.of(new Haar(), new ZeroPaddingStrategy(), "Haar/Zero"),
            Arguments.of(new Haar(), new SymmetricPaddingStrategy(), "Haar/Symmetric"),
            Arguments.of(Daubechies.DB2, new ZeroPaddingStrategy(), "DB2/Zero"),
            Arguments.of(Daubechies.DB2, new PeriodicPaddingStrategy(), "DB2/Periodic"),
            Arguments.of(Daubechies.DB4, new ReflectPaddingStrategy(), "DB4/Reflect")
        );
    }
    
    @ParameterizedTest(name = "{2} transform with arbitrary length")
    @MethodSource("waveletAndPaddingProvider")
    @DisplayName("Perfect reconstruction for non-power-of-2 signals")
    void testArbitraryLengthReconstruction(Wavelet wavelet, PaddingStrategy padding, String name) {
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            wavelet, BoundaryMode.PERIODIC, padding);
        
        // Test various non-power-of-2 lengths
        int[] lengths = {10, 15, 31, 63, 100, 127, 200, 255};
        
        for (int length : lengths) {
            double[] signal = generateSignal(length);
            
            // Forward transform (automatically pads)
            TransformResult result = transform.forward(signal);
            
            // Verify result is marked as padded
            assertTrue(result instanceof PaddedTransformResult,
                "Result should be PaddedTransformResult for length " + length);
            
            // Inverse transform (automatically trims)
            double[] reconstructed = transform.inverse(result);
            
            // Verify correct length
            assertEquals(length, reconstructed.length,
                "Reconstructed signal should have original length");
            
            // Verify reconstruction accuracy
            assertArrayEquals(signal, reconstructed, 1e-9,
                "Failed reconstruction for " + name + " with length " + length);
        }
    }
    
    @Test
    @DisplayName("Power-of-2 signals work without padding")
    void testPowerOfTwoSignals() {
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        TransformResult result = transform.forward(signal);
        
        // Should not be padded
        assertFalse(result instanceof PaddedTransformResult,
            "Power-of-2 signal should not produce PaddedTransformResult");
        
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 7, 10, 15, 31, 50, 63, 100, 127, 200})
    @DisplayName("Correct padding to next power of 2")
    void testPaddingToNextPowerOfTwo(int originalLength) {
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = new double[originalLength];
        for (int i = 0; i < originalLength; i++) {
            signal[i] = i + 1;
        }
        
        TransformResult result = transform.forward(signal);
        
        if (result instanceof PaddedTransformResult padded) {
            assertEquals(originalLength, padded.originalLength());
            
            // Padded length should be next power of 2
            int paddedLength = padded.paddedLength();
            assertTrue(ValidationUtils.isPowerOfTwo(paddedLength));
            assertTrue(paddedLength >= originalLength);
            assertTrue(paddedLength < 2 * originalLength || originalLength == 1);
        }
    }
    
    @Test
    @DisplayName("Different padding strategies produce different coefficients")
    void testPaddingStrategyEffects() {
        double[] signal = {1, 2, 3, 4, 5}; // Length 5, will pad to 8
        
        // Test with different padding strategies
        PaddingStrategy[] strategies = {
            new ZeroPaddingStrategy(),
            new SymmetricPaddingStrategy(),
            new ReflectPaddingStrategy(),
            new PeriodicPaddingStrategy()
        };
        
        TransformResult[] results = new TransformResult[strategies.length];
        
        for (int i = 0; i < strategies.length; i++) {
            FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
                new Haar(), BoundaryMode.PERIODIC, strategies[i]);
            results[i] = transform.forward(signal);
        }
        
        // Different padding should produce different coefficients
        for (int i = 0; i < strategies.length - 1; i++) {
            for (int j = i + 1; j < strategies.length; j++) {
                assertFalse(
                    arraysEqual(results[i].detailCoeffs(), results[j].detailCoeffs(), EPSILON),
                    "Different padding strategies should produce different coefficients"
                );
            }
        }
    }
    
    @Test
    @DisplayName("Financial time series with arbitrary length")
    void testFinancialTimeSeries() {
        // Typical financial time series lengths (not power of 2)
        int tradingDays = FinancialAnalysisParameters.TRADING_DAYS_PER_YEAR; // One year of trading days
        double[] prices = new double[tradingDays];
        
        // Simulate price series with trend and volatility
        for (int i = 0; i < tradingDays; i++) {
            prices[i] = 100 + 0.1 * i + 5 * Math.sin(2 * Math.PI * i / 20) + 
                       0.5 * (Math.random() - 0.5);
        }
        
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, new SymmetricPaddingStrategy());
        
        // Transform and reconstruct
        TransformResult result = transform.forward(prices);
        double[] reconstructed = transform.inverse(result);
        
        // Verify dimensions preserved
        assertEquals(tradingDays, reconstructed.length);
        
        // Calculate returns
        double[] returns = new double[tradingDays - 1];
        double[] reconstructedReturns = new double[tradingDays - 1];
        
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
            reconstructedReturns[i] = (reconstructed[i + 1] - reconstructed[i]) / reconstructed[i];
        }
        
        // Verify returns are preserved (important for financial analysis)
        assertArrayEquals(returns, reconstructedReturns, 1e-8);
    }
    
    @Test
    @DisplayName("Edge cases")
    void testEdgeCases() {
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Single element
        double[] single = {42.0};
        TransformResult result1 = transform.forward(single);
        double[] reconstructed1 = transform.inverse(result1);
        assertArrayEquals(single, reconstructed1, EPSILON);
        
        // Very small signal
        double[] tiny = {1, 2};
        TransformResult result2 = transform.forward(tiny);
        double[] reconstructed2 = transform.inverse(result2);
        assertArrayEquals(tiny, reconstructed2, EPSILON);
    }
    
    @Test
    @DisplayName("Multi-level decomposition with flexible length")
    void testMultiLevelWithPadding() {
        // Non-power-of-2 length
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 25.0);
        }
        
        FlexibleWaveletTransform flexTransform = new FlexibleWaveletTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, new ZeroPaddingStrategy());
        
        // First level
        TransformResult level1 = flexTransform.forward(signal);
        assertTrue(level1 instanceof PaddedTransformResult);
        
        // Can still do multi-level on the coefficients
        MultiLevelWaveletTransform mwt = new MultiLevelWaveletTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // The approximation coefficients can be further decomposed
        double[] approx = level1.approximationCoeffs();
        if (ValidationUtils.isPowerOfTwo(approx.length) && approx.length > 4) {
            MultiLevelTransformResult multiResult = mwt.decompose(approx, 2);
            assertEquals(2, multiResult.levels());
        }
    }
    
    @Test
    @DisplayName("Invalid inputs")
    void testInvalidInputs() {
        FlexibleWaveletTransform transform = new FlexibleWaveletTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Null signal
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(null));
        
        // Empty signal
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(new double[0]));
        
        // Non-finite values
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(new double[]{1, 2, Double.NaN, 4}));
        
        assertThrows(InvalidSignalException.class,
            () -> transform.forward(new double[]{1, 2, Double.POSITIVE_INFINITY, 4}));
    }
    
    private double[] generateSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / (length / 4.0)) + 
                       0.5 * Math.cos(2 * Math.PI * i / (length / 8.0));
        }
        return signal;
    }
    
    private boolean arraysEqual(double[] a, double[] b, double tolerance) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > tolerance) return false;
        }
        return true;
    }
}