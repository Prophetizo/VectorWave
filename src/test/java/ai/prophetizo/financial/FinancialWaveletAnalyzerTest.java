package ai.prophetizo.financial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.BoundaryMode;

/**
 * Unit tests for FinancialWaveletAnalyzer class.
 */
class FinancialWaveletAnalyzerTest {
    
    private FinancialWaveletAnalyzer analyzer;
    private static final double TOLERANCE = 1e-10;
    
    @BeforeEach
    void setUp() {
        // Use 3% risk-free rate for testing
        FinancialConfig config = new FinancialConfig(0.03);
        analyzer = new FinancialWaveletAnalyzer(config);
    }
    
    
    @Test
    @DisplayName("Constructor with config should set configuration correctly")
    void testConstructorWithConfig() {
        FinancialConfig config = new FinancialConfig(0.03);
        FinancialWaveletAnalyzer configAnalyzer = new FinancialWaveletAnalyzer(config);
        assertEquals(0.03, configAnalyzer.getConfig().getRiskFreeRate(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Constructor should reject null configuration")
    void testConstructorRejectsNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> new FinancialWaveletAnalyzer(null));
    }
    
    @Test
    @DisplayName("Constructor with config and transform should set both correctly")
    void testConstructorWithConfigAndTransform() {
        FinancialConfig config = new FinancialConfig(0.025);
        WaveletTransform transform = new WaveletTransformFactory()
                .boundaryMode(BoundaryMode.PERIODIC)
                .create(Daubechies.DB2);
        
        FinancialWaveletAnalyzer customAnalyzer = new FinancialWaveletAnalyzer(config, transform);
        assertEquals(0.025, customAnalyzer.getConfig().getRiskFreeRate(), TOLERANCE);
        assertSame(transform, customAnalyzer.getTransform());
    }
    
    @Test
    @DisplayName("Constructor should reject null transform")
    void testConstructorRejectsNullTransform() {
        FinancialConfig config = new FinancialConfig(0.04); // 4% risk-free rate
        assertThrows(IllegalArgumentException.class, () -> new FinancialWaveletAnalyzer(config, null));
    }
    
    // Sharpe Ratio Tests
    
    @Test
    @DisplayName("Calculate Sharpe ratio with positive excess return")
    void testCalculateSharpeRatioPositiveExcess() {
        double[] returns = {0.10, 0.08, 0.12, 0.06}; // 10%, 8%, 12%, 6%
        double riskFreeRate = 0.04; // 4%
        
        // Mean return: (0.10 + 0.08 + 0.12 + 0.06) / 4 = 0.09
        // Excess return: 0.09 - 0.04 = 0.05
        // Sample std dev of [0.10, 0.08, 0.12, 0.06]: ~0.0258
        // Expected Sharpe ratio: 0.05 / 0.0258 ≈ 1.937
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns, riskFreeRate);
        assertEquals(1.937, sharpeRatio, 0.01);
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio with negative excess return")
    void testCalculateSharpeRatioNegativeExcess() {
        double[] returns = {0.02, 0.01, 0.03, 0.00}; // Low returns
        double riskFreeRate = 0.04; // Higher than mean return
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns, riskFreeRate);
        assertTrue(sharpeRatio < 0, "Sharpe ratio should be negative when excess return is negative");
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio with zero risk-free rate")
    void testCalculateSharpeRatioZeroRiskFree() {
        double[] returns = {0.05, 0.03, 0.07, 0.01};
        double riskFreeRate = 0.0;
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns, riskFreeRate);
        assertTrue(sharpeRatio > 0, "Sharpe ratio should be positive with positive mean return and zero risk-free rate");
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio with zero standard deviation")
    void testCalculateSharpeRatioZeroStdDev() {
        double[] constantReturns = {0.05, 0.05, 0.05, 0.05}; // No variance
        double riskFreeRate = 0.03;
        
        double sharpeRatio = analyzer.calculateSharpeRatio(constantReturns, riskFreeRate);
        assertEquals(Double.POSITIVE_INFINITY, sharpeRatio);
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio with zero std dev and equal mean and risk-free rate")
    void testCalculateSharpeRatioZeroStdDevZeroExcess() {
        double[] constantReturns = {0.04, 0.04, 0.04, 0.04};
        double riskFreeRate = 0.04;
        
        double sharpeRatio = analyzer.calculateSharpeRatio(constantReturns, riskFreeRate);
        assertEquals(0.0, sharpeRatio, TOLERANCE);
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio using configured risk-free rate")
    void testCalculateSharpeRatioWithConfiguredRate() {
        FinancialConfig config = new FinancialConfig(0.03);
        FinancialWaveletAnalyzer configAnalyzer = new FinancialWaveletAnalyzer(config);
        
        double[] returns = {0.08, 0.06, 0.10, 0.04};
        double sharpeRatio = configAnalyzer.calculateSharpeRatio(returns);
        
        // Should use 0.03 as risk-free rate
        double expectedSharpe = configAnalyzer.calculateSharpeRatio(returns, 0.03);
        assertEquals(expectedSharpe, sharpeRatio, TOLERANCE);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.02, 0.04, 0.06, 0.08})
    @DisplayName("Calculate Sharpe ratio with various risk-free rates")
    void testCalculateSharpeRatioVariousRates(double riskFreeRate) {
        double[] returns = {0.10, 0.08, 0.12, 0.06};
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns, riskFreeRate);
        assertTrue(Double.isFinite(sharpeRatio), "Sharpe ratio should be finite for valid inputs");
        
        // Higher risk-free rate should result in lower Sharpe ratio
        if (riskFreeRate < 0.08) { // Less than the minimum return
            assertTrue(sharpeRatio > 0, "Sharpe ratio should be positive when risk-free rate is reasonable");
        }
    }
    
    // Input validation tests
    
    @Test
    @DisplayName("Calculate Sharpe ratio should reject null returns")
    void testCalculateSharpeRatioRejectsNullReturns() {
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateSharpeRatio(null, 0.04));
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio should reject empty returns")
    void testCalculateSharpeRatioRejectsEmptyReturns() {
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateSharpeRatio(new double[0], 0.04));
    }
    
    @Test
    @DisplayName("Calculate Sharpe ratio should reject single return")
    void testCalculateSharpeRatioRejectsSingleReturn() {
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateSharpeRatio(new double[]{0.05}, 0.04));
    }
    
    // Wavelet Sharpe Ratio Tests
    
    @Test
    @DisplayName("Calculate wavelet Sharpe ratio with power-of-2 length")
    void testCalculateWaveletSharpeRatio() {
        // Create noisy returns data (power of 2 length)
        double[] noisyReturns = {0.10, 0.08, 0.12, 0.06, 0.09, 0.07, 0.11, 0.05};
        double riskFreeRate = 0.04;
        
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(noisyReturns, riskFreeRate);
        double regularSharpe = analyzer.calculateSharpeRatio(noisyReturns, riskFreeRate);
        
        assertTrue(Double.isFinite(waveletSharpe), "Wavelet Sharpe ratio should be finite");
        assertNotEquals(regularSharpe, waveletSharpe, "Wavelet and regular Sharpe ratios should differ due to denoising");
    }
    
    @Test
    @DisplayName("Calculate wavelet Sharpe ratio with configured risk-free rate")
    void testCalculateWaveletSharpeRatioConfiguredRate() {
        FinancialConfig config = new FinancialConfig(0.025);
        FinancialWaveletAnalyzer configAnalyzer = new FinancialWaveletAnalyzer(config);
        
        double[] returns = {0.08, 0.06, 0.10, 0.04, 0.09, 0.07, 0.11, 0.05};
        double waveletSharpe = configAnalyzer.calculateWaveletSharpeRatio(returns);
        
        // Should use configured risk-free rate
        double expectedSharpe = configAnalyzer.calculateWaveletSharpeRatio(returns, 0.025);
        assertEquals(expectedSharpe, waveletSharpe, TOLERANCE);
    }
    
    @Test
    @DisplayName("Calculate wavelet Sharpe ratio should reject non-power-of-2 length")
    void testCalculateWaveletSharpeRatioRejectsNonPowerOfTwo() {
        double[] returns = {0.08, 0.06, 0.10}; // Length 3, not power of 2
        
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateWaveletSharpeRatio(returns, 0.04));
    }
    
    @Test
    @DisplayName("Calculate wavelet Sharpe ratio should reject null returns")
    void testCalculateWaveletSharpeRatioRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateWaveletSharpeRatio(null, 0.04));
    }
    
    @Test
    @DisplayName("Calculate wavelet Sharpe ratio should reject empty returns")
    void testCalculateWaveletSharpeRatioRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> 
                analyzer.calculateWaveletSharpeRatio(new double[0], 0.04));
    }
    
    // Edge cases and robustness tests
    
    @Test
    @DisplayName("Should handle very small returns")
    void testHandleVerySmallReturns() {
        double[] smallReturns = {0.001, 0.002, 0.0015, 0.0008};
        double riskFreeRate = 0.001;
        
        double sharpeRatio = analyzer.calculateSharpeRatio(smallReturns, riskFreeRate);
        assertTrue(Double.isFinite(sharpeRatio), "Should handle very small returns");
    }
    
    @Test
    @DisplayName("Should handle large returns")
    void testHandleLargeReturns() {
        double[] largeReturns = {0.50, 0.60, 0.45, 0.55};
        double riskFreeRate = 0.04;
        
        double sharpeRatio = analyzer.calculateSharpeRatio(largeReturns, riskFreeRate);
        assertTrue(Double.isFinite(sharpeRatio), "Should handle large returns");
        assertTrue(sharpeRatio > 0, "Should be positive with large excess returns");
    }
    
    @Test
    @DisplayName("Mathematical accuracy test with known values")
    void testMathematicalAccuracy() {
        // Test case with known mathematical result
        double[] returns = {0.10, 0.05, 0.15, 0.00}; // Mean = 0.075
        double riskFreeRate = 0.03; // Excess return = 0.045
        
        // Manual calculation:
        // Mean = 0.075
        // Variance = ((0.10-0.075)² + (0.05-0.075)² + (0.15-0.075)² + (0.00-0.075)²) / (n-1)
        //          = (0.000625 + 0.000625 + 0.005625 + 0.005625) / 3 = 0.004166667
        // Std Dev = sqrt(0.004166667) = 0.064549722
        // Sharpe = 0.045 / 0.064549722 = 0.697137
        
        double sharpeRatio = analyzer.calculateSharpeRatio(returns, riskFreeRate);
        assertEquals(0.697137, sharpeRatio, 0.001);
    }
}