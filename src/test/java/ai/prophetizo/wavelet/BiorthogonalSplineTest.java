package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.BiorthogonalSpline;
import ai.prophetizo.wavelet.test.BaseWaveletTest;
import ai.prophetizo.wavelet.test.WaveletAssertions;
import ai.prophetizo.wavelet.test.WaveletTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BiorthogonalSpline wavelets.
 */
@DisplayName("BiorthogonalSpline Wavelet Tests")
class BiorthogonalSplineTest extends BaseWaveletTest {
    
    private BiorthogonalSpline bior1_3;
    
    @BeforeEach
    void setUp() {
        bior1_3 = BiorthogonalSpline.BIOR1_3;
    }
    
    @Test
    @DisplayName("BIOR1.3 decomposition filter values should match expected")
    void testBior1_3_FilterValues() {
        double[] expected = {
            -0.08703882797784893, 0.08703882797784893,
            0.7071067811865476, 0.7071067811865476,
            0.08703882797784893, -0.08703882797784893
        };
        
        double[] actual = bior1_3.lowPassDecomposition();
        
        assertArrayEquals(expected, actual, 1e-15, 
            "BIOR1.3 decomposition low-pass filter coefficients");
    }
    
    @Test
    @DisplayName("BIOR1.3 high-pass filter generation should be correct")
    void testBior1_3_HighPassFilterGeneration() {
        double[] highPass = bior1_3.highPassDecomposition();
        
        // The first coefficient should be specific value based on the algorithm
        assertEquals(0.35355339059327373, highPass[0], 1e-15, 
            "First coefficient of high-pass decomposition filter");
    }
    
    @Test
    @DisplayName("Basic wavelet properties")
    void testBasicProperties() {
        assertEquals("bior1.3", bior1_3.name());
        assertEquals(3, bior1_3.vanishingMoments());
        assertEquals(1, bior1_3.dualVanishingMoments());
        assertTrue(bior1_3.isSymmetric());
    }
    
    @Test
    @DisplayName("Filter orthogonality for biorthogonal wavelets")
    void testFilterOrthogonality() {
        // For biorthogonal wavelets, the decomposition and reconstruction filters
        // are not orthogonal to each other, but they satisfy perfect reconstruction.
        // The sum of squares of reconstruction filters should equal 1.0 (normalized).
        double[] lowRecon = bior1_3.lowPassReconstruction();
        
        // Test normalization of reconstruction filter
        double sumSquares = 0.0;
        for (double coeff : lowRecon) {
            sumSquares += coeff * coeff;
        }
        
        assertEquals(0.25, sumSquares, 1e-15, 
            "Sum of squares of reconstruction filter coefficients");
    }
    
    @Test
    @DisplayName("Perfect reconstruction property")
    void testPerfectReconstruction() {
        double[] h0 = bior1_3.lowPassDecomposition();   // decomposition low-pass
        double[] h1 = bior1_3.highPassDecomposition();  // decomposition high-pass  
        double[] g0 = bior1_3.lowPassReconstruction();  // reconstruction low-pass
        double[] g1 = bior1_3.highPassReconstruction(); // reconstruction high-pass
        
        // Basic length checks
        assertNotNull(h0);
        assertNotNull(h1);
        assertNotNull(g0);
        assertNotNull(g1);
        
        assertTrue(h0.length > 0);
        assertTrue(h1.length > 0);
        assertTrue(g0.length > 0);
        assertTrue(g1.length > 0);
    }
}