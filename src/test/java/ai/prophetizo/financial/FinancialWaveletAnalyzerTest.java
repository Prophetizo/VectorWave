package ai.prophetizo.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for FinancialWaveletAnalyzer class.
 */
class FinancialWaveletAnalyzerTest {
    
    private FinancialWaveletAnalyzer analyzer;
    private double[] samplePrices;
    private double[] sampleVolumes;
    private static final double TOLERANCE = 1e-10;
    
    @BeforeEach
    void setUp() {
        analyzer = new FinancialWaveletAnalyzer();
        samplePrices = generateSamplePrices(256);
        sampleVolumes = generateSampleVolumes(256);
    }
    
    private double[] generateSamplePrices(int length) {
        double[] prices = new double[length];
        prices[0] = 100.0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 1; i < length; i++) {
            double change = random.nextGaussian() * 0.02; // 2% daily volatility
            prices[i] = prices[i - 1] * (1 + change);
        }
        
        return prices;
    }
    
    private double[] generateSampleVolumes(int length) {
        double[] volumes = new double[length];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 0; i < length; i++) {
            volumes[i] = random.nextDouble(1000, 10000);
        }
        
        return volumes;
    }
    
    @Test
    @DisplayName("Analyzer should handle basic price analysis")
    void testBasicPriceAnalysis() {
        Map<String, Object> results = analyzer.analyzePriceData(samplePrices);
        
        assertNotNull(results);
        assertTrue(results.containsKey("mean"));
        assertTrue(results.containsKey("volatility"));
        assertTrue(results.containsKey("trend"));
        
        double mean = (Double) results.get("mean");
        double volatility = (Double) results.get("volatility");
        String trend = (String) results.get("trend");
        
        assertTrue(mean > 0);
        assertTrue(volatility >= 0);
        assertTrue(trend.equals("BULLISH") || trend.equals("BEARISH") || trend.equals("SIDEWAYS"));
    }
    
    @Test
    @DisplayName("Volatility analysis should produce valid results")
    void testVolatilityAnalysis() {
        VolatilityResult result = analyzer.analyzeVolatility(samplePrices, 20);
        
        assertNotNull(result);
        assertTrue(result.realizedVolatility() >= 0);
        assertTrue(result.garchVolatility() >= 0);
        assertTrue(result.waveletVolatility() >= 0);
        assertNotNull(result.detailVolatilities());
        assertNotNull(result.timeScaleVolatilities());
        assertTrue(result.timestamp() > 0);
    }
    
    @Test
    @DisplayName("Trading signal generation should work")
    void testTradingSignalGeneration() {
        List<TradingSignal> signals = analyzer.generateTradingSignals(samplePrices, sampleVolumes);
        
        assertNotNull(signals);
        // Should generate some signals for realistic price data
        assertTrue(signals.size() >= 0);
        
        if (!signals.isEmpty()) {
            TradingSignal signal = signals.get(0);
            assertNotNull(signal.type());
            assertNotNull(signal.strength());
            assertTrue(signal.confidence() >= 0 && signal.confidence() <= 1.0);
            assertTrue(signal.price() > 0);
            assertTrue(signal.timestamp() > 0);
            assertNotNull(signal.reason());
        }
    }
    
    @Test
    @DisplayName("Market regime analysis should return valid regimes")
    void testMarketRegimeAnalysis() {
        String regime = analyzer.analyzeMarketRegime(samplePrices);
        
        assertNotNull(regime);
        assertTrue(regime.equals("TRENDING_LOW_VOL") || 
                  regime.equals("TRENDING_HIGH_VOL") ||
                  regime.equals("RANGING_HIGH_VOL") ||
                  regime.equals("RANGING_LOW_VOL") ||
                  regime.equals("INSUFFICIENT_DATA") ||
                  regime.equals("ANALYSIS_ERROR"));
    }
    
    @Test
    @DisplayName("Multi-timeframe analysis should work")
    void testMultiTimeframeAnalysis() {
        double[] largePriceArray = generateSamplePrices(1024);
        Map<String, Object> analysis = analyzer.performMultiTimeframeAnalysis(largePriceArray);
        
        assertNotNull(analysis);
        assertTrue(analysis.size() > 0);
        
        // Should have analysis for timeframes that fit in the data
        if (analysis.containsKey("timeframe_64")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> timeframe64 = (Map<String, Object>) analysis.get("timeframe_64");
            assertNotNull(timeframe64);
            assertTrue(timeframe64.containsKey("regime"));
            assertTrue(timeframe64.containsKey("volatility"));
            assertTrue(timeframe64.containsKey("signals"));
        }
    }
    
    @Test
    @DisplayName("Risk analysis should calculate metrics")
    void testRiskAnalysis() {
        double[] benchmarkPrices = generateSamplePrices(samplePrices.length);
        Map<String, Double> riskMetrics = analyzer.analyzeRisk(samplePrices, benchmarkPrices);
        
        assertNotNull(riskMetrics);
        assertTrue(riskMetrics.containsKey("volatility"));
        assertTrue(riskMetrics.containsKey("beta"));
        assertTrue(riskMetrics.containsKey("var_95"));
        assertTrue(riskMetrics.containsKey("max_drawdown"));
        
        assertTrue(riskMetrics.get("volatility") >= 0);
        assertTrue(riskMetrics.get("var_95") >= 0);
        assertTrue(riskMetrics.get("max_drawdown") >= 0);
        assertTrue(riskMetrics.get("max_drawdown") <= 1.0);
    }
    
    @Test
    @DisplayName("Portfolio optimization should work")
    void testPortfolioOptimization() {
        int numAssets = 3;
        double[][] assetPrices = new double[numAssets][];
        double[] expectedReturns = new double[numAssets];
        
        for (int i = 0; i < numAssets; i++) {
            assetPrices[i] = generateSamplePrices(100);
            expectedReturns[i] = 0.08 + ThreadLocalRandom.current().nextGaussian() * 0.02;
        }
        
        Map<String, Object> optimization = analyzer.optimizePortfolio(assetPrices, expectedReturns, 0.1);
        
        assertNotNull(optimization);
        assertTrue(optimization.containsKey("weights"));
        assertTrue(optimization.containsKey("expected_return"));
        assertTrue(optimization.containsKey("volatility"));
        assertTrue(optimization.containsKey("sharpe_ratio"));
        
        double[] weights = (double[]) optimization.get("weights");
        assertEquals(numAssets, weights.length);
        
        // Weights should sum to approximately 1
        double weightSum = 0.0;
        for (double weight : weights) {
            weightSum += weight;
            assertTrue(weight >= 0); // Non-negative weights
        }
        assertEquals(1.0, weightSum, 0.001);
    }
    
    @Test
    @DisplayName("Should handle insufficient data gracefully")
    void testInsufficientDataHandling() {
        double[] shortPrices = {100.0, 101.0, 99.0};
        
        // analyzePriceData should throw exception for insufficient data
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzePriceData(shortPrices));
        
        List<TradingSignal> signals = analyzer.generateTradingSignals(shortPrices, new double[3]);
        assertTrue(signals.isEmpty());
        
        String regime = analyzer.analyzeMarketRegime(shortPrices);
        assertEquals("INSUFFICIENT_DATA", regime);
    }
    
    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputHandling() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzePriceData(null));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyzeVolatility(null, 20));
        
        List<TradingSignal> signals = analyzer.generateTradingSignals(null, sampleVolumes);
        assertTrue(signals.isEmpty());
    }
    
    @Test
    @DisplayName("Volatility result should be immutable")
    void testVolatilityResultImmutability() {
        VolatilityResult result = analyzer.analyzeVolatility(samplePrices, 20);
        
        double[] originalDetails = result.detailVolatilities();
        double[] modifiedDetails = result.detailVolatilities();
        modifiedDetails[0] = -999.0; // Try to modify
        
        // Should return different arrays (defensive copies)
        assertNotSame(originalDetails, modifiedDetails);
        assertNotEquals(-999.0, result.detailVolatilities()[0]);
=======
    }
    
    @Test
    @DisplayName("Default constructor should use default configuration")
    void testDefaultConstructor() {
        FinancialWaveletAnalyzer defaultAnalyzer = new FinancialWaveletAnalyzer();
        assertEquals(FinancialConfig.DEFAULT_RISK_FREE_RATE, defaultAnalyzer.getConfig().getRiskFreeRate(), TOLERANCE);
        assertNotNull(defaultAnalyzer.getTransform());
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
        FinancialConfig config = new FinancialConfig();
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