package ai.prophetizo.financial;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class specifically for validating the wavelet denoising implementation
 * in FinancialWaveletAnalyzer.
 */
class FinancialWaveletDenoisingTest {
    
    private FinancialWaveletAnalyzer analyzer;
    private WaveletTransform transform;
    
    @BeforeEach
    void setUp() {
        FinancialConfig config = new FinancialConfig(0.02); // 2% risk-free rate
        analyzer = new FinancialWaveletAnalyzer(config);
        transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
    }
    
    @Test
    void testTransformResultCreateValidation() {
        // Test that TransformResult.create properly validates matching lengths
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.0, 0.0, 0.0, 0.0};
        
        // Should succeed with matching lengths
        TransformResult result = TransformResult.create(approx, detail);
        assertNotNull(result);
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
    }
    
    @Test
    void testDenoisingWithZeroDetails() {
        // Create a noisy signal (power of 2 length)
        double[] noisyReturns = {0.05, -0.02, 0.08, -0.03, 0.01, 0.06, -0.04, 0.02};
        
        // Apply forward transform
        TransformResult result = transform.forward(noisyReturns);
        
        // Verify we can create a denoised result with zero details
        double[] approxCoeffs = result.approximationCoeffs();
        double[] zeroDetails = new double[result.detailCoeffs().length];
        
        // This should not throw any exception
        TransformResult denoisedResult = TransformResult.create(approxCoeffs, zeroDetails);
        
        // Verify the denoised result has the expected structure
        assertArrayEquals(approxCoeffs, denoisedResult.approximationCoeffs());
        assertArrayEquals(zeroDetails, denoisedResult.detailCoeffs());
        
        // Verify all detail coefficients are zero
        for (double detail : denoisedResult.detailCoeffs()) {
            assertEquals(0.0, detail);
        }
    }
    
    @Test
    void testWaveletSharpeRatioDenoising() {
        // Test the full wavelet Sharpe ratio calculation
        double[] returns = {0.05, -0.02, 0.08, -0.03, 0.01, 0.06, -0.04, 0.02};
        
        // Calculate both regular and wavelet Sharpe ratios
        double regularSharpe = analyzer.calculateSharpeRatio(returns);
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);
        
        // Both should be valid numbers
        assertTrue(Double.isFinite(regularSharpe));
        assertTrue(Double.isFinite(waveletSharpe));
        
        // The wavelet Sharpe ratio should typically be different (often higher)
        // due to noise removal, but we can't guarantee this for all signals
        assertNotEquals(regularSharpe, waveletSharpe, 0.0001);
    }
    
    @Test
    void testDenoisingPreservesSignalLength() {
        double[] returns = {0.1, -0.05, 0.08, -0.02, 0.06, -0.03, 0.04, 0.01};
        
        // Apply transform and denoise
        TransformResult result = transform.forward(returns);
        double[] denoisedReturns = transform.inverse(
            TransformResult.create(
                result.approximationCoeffs(),
                new double[result.detailCoeffs().length]
            )
        );
        
        // Denoised signal should have the same length as original
        assertEquals(returns.length, denoisedReturns.length);
    }
    
    @Test
    void testInvalidArrayLengthsThrowsException() {
        // Test that providing arrays of different lengths fails appropriately
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.0, 0.0}; // Wrong length
        
        // This should throw an AssertionError when assertions are enabled
        // or IllegalArgumentException when full validation is enabled
        assertThrows(AssertionError.class, () -> {
            TransformResult.create(approx, detail);
        });
    }
}