package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiorthogonalSplineTest {

    @Test
    void testBior1_3_Properties() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals("bior1.3", bior.name());
        assertEquals(WaveletType.BIORTHOGONAL, bior.getType());
        assertTrue(bior.description().contains("Biorthogonal spline wavelet bior1.3"));
        assertTrue(bior.description().contains("reconstruction order 1"));
        assertTrue(bior.description().contains("decomposition order 3"));
    }

    @Test
    void testBior1_3_FilterLengths() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals(6, bior.lowPassDecomposition().length);
        assertEquals(2, bior.highPassDecomposition().length); // Generated from lowPassRecon which has length 2
        assertEquals(2, bior.lowPassReconstruction().length);
        assertEquals(6, bior.highPassReconstruction().length); // Generated from lowPassDecomp which has length 6
    }

    @Test
    void testBior1_3_FilterValues() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Test decomposition low-pass filter (normalized to L2 norm = 1)
        double[] expectedLowPassDecomp = {
            -0.08703882797784893, 0.08703882797784893,
            0.6963106238227914, 0.6963106238227914,
            0.08703882797784893, -0.08703882797784893
        };
        assertArrayEquals(expectedLowPassDecomp, bior.lowPassDecomposition(), 1e-15);
        
        // Test reconstruction low-pass filter (normalized to L2 norm = 1)
        double[] expectedLowPassRecon = {
            0.7071067811865476, 0.7071067811865476
        };
        assertArrayEquals(expectedLowPassRecon, bior.lowPassReconstruction(), 1e-15);
    }

    @Test
    void testBior1_3_HighPassFilterGeneration() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // High-pass decomposition is generated from low-pass reconstruction
        double[] highPassDecomp = bior.highPassDecomposition();
        assertEquals(2, highPassDecomp.length);
        
        // Verify alternating sign pattern (using normalized values)
        assertEquals(0.7071067811865476, highPassDecomp[0], 1e-15);
        assertEquals(-0.7071067811865476, highPassDecomp[1], 1e-15);
        
        // High-pass reconstruction is generated from low-pass decomposition
        double[] highPassRecon = bior.highPassReconstruction();
        assertEquals(6, highPassRecon.length);
        
        // Check first and last values to verify the generation logic
        // highPassRecon[0] = (0 % 2 == 0 ? 1 : -1) * lowPassDecomp[6-1-0] = 1 * lowPassDecomp[5] = 1 * (-0.08703882797784893) = -0.08703882797784893
        assertEquals(-0.08703882797784893, highPassRecon[0], 1e-15);
        // highPassRecon[5] = (5 % 2 == 0 ? 1 : -1) * lowPassDecomp[6-1-5] = -1 * lowPassDecomp[0] = -1 * (-0.08703882797784893) = 0.08703882797784893
        assertEquals(0.08703882797784893, highPassRecon[5], 1e-15);
    }

    @Test
    void testBior1_3_VanishingMoments() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals(3, bior.vanishingMoments()); // Decomposition order
        assertEquals(1, bior.dualVanishingMoments()); // Reconstruction order
    }

    @Test
    void testBior1_3_SplineOrder() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Spline order is max of reconstruction and decomposition orders
        assertEquals(3, bior.splineOrder());
    }

    @Test
    void testBior1_3_Symmetry() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertTrue(bior.isSymmetric());
    }

    @Test
    void testFilterDefensiveCopies() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Get filters
        double[] lowPassDecomp1 = bior.lowPassDecomposition();
        double[] lowPassDecomp2 = bior.lowPassDecomposition();
        double[] lowPassRecon1 = bior.lowPassReconstruction();
        double[] lowPassRecon2 = bior.lowPassReconstruction();
        
        // Should be different arrays (defensive copies)
        assertNotSame(lowPassDecomp1, lowPassDecomp2);
        assertNotSame(lowPassRecon1, lowPassRecon2);
        
        // But with same content
        assertArrayEquals(lowPassDecomp1, lowPassDecomp2);
        assertArrayEquals(lowPassRecon1, lowPassRecon2);
        
        // Modifying returned array shouldn't affect internal state
        lowPassDecomp1[0] = 999.0;
        assertNotEquals(999.0, bior.lowPassDecomposition()[0]);
    }

    @Test
    void testInterfaceImplementation() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Verify it implements all required interfaces
        assertTrue(bior instanceof BiorthogonalWavelet);
        assertTrue(bior instanceof DiscreteWavelet);
        assertTrue(bior instanceof Wavelet);
    }

    @Test
    void testFilterOrthogonality() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // For biorthogonal wavelets, decomposition and reconstruction filters
        // have different lengths and are not orthogonal to themselves,
        // but satisfy biorthogonality conditions
        
        double[] lowDecomp = bior.lowPassDecomposition();
        double[] highDecomp = bior.highPassDecomposition();
        double[] lowRecon = bior.lowPassReconstruction();
        double[] highRecon = bior.highPassReconstruction();
        
        // Basic sanity checks
        assertNotEquals(lowDecomp.length, lowRecon.length);
        assertNotEquals(highDecomp.length, highRecon.length);
        
        // Check that filters are normalized (sum of squares = 1 for L2 normalized filters)
        double sumSquaresRecon = 0;
        for (double v : lowRecon) {
            sumSquaresRecon += v * v;
        }
        assertEquals(1.0, sumSquaresRecon, 1e-10); // L2 normalized filter
    }

    @Test
    void testHighPassGenerationLogic() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Verify the alternating sign pattern in high-pass generation
        double[] lowRecon = bior.lowPassReconstruction();
        double[] highDecomp = bior.highPassDecomposition();
        
        // highDecomp[i] = (i % 2 == 0 ? 1 : -1) * lowRecon[n-1-i]
        for (int i = 0; i < highDecomp.length; i++) {
            double expected = (i % 2 == 0 ? 1 : -1) * lowRecon[lowRecon.length - 1 - i];
            assertEquals(expected, highDecomp[i], 1e-15);
        }
        
        // Similarly for the other high-pass filter
        double[] lowDecomp = bior.lowPassDecomposition();
        double[] highRecon = bior.highPassReconstruction();
        
        for (int i = 0; i < highRecon.length; i++) {
            double expected = (i % 2 == 0 ? 1 : -1) * lowDecomp[lowDecomp.length - 1 - i];
            assertEquals(expected, highRecon[i], 1e-15);
        }
    }

    @Test
    void testStaticConstant() {
        // Verify BIOR1_3 is properly accessible as a static constant
        assertNotNull(BiorthogonalSpline.BIOR1_3);
        assertEquals("bior1.3", BiorthogonalSpline.BIOR1_3.name());
        
        // Multiple accesses should return the same instance
        assertSame(BiorthogonalSpline.BIOR1_3, BiorthogonalSpline.BIOR1_3);
    }
}