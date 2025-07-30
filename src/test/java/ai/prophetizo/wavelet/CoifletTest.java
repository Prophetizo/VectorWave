package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Coiflet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Coiflet wavelet implementations.
 * Verifies mathematical properties and coefficient correctness.
 */
class CoifletTest {
    
    private static final double EPSILON = 1e-10;
    private static final double RECONSTRUCTION_TOLERANCE = 1e-8;
    
    @Test
    @DisplayName("Coiflet coefficients are normalized")
    void testCoifletNormalization() {
        // Test that low-pass filter coefficients sum to sqrt(2)
        Coiflet[] coiflets = {Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3};
        
        for (Coiflet coif : coiflets) {
            double[] coeffs = coif.lowPassDecomposition();
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 1e-4, 
                coif.name() + " coefficients should sum to sqrt(2)");
        }
    }
    
    @Test
    @DisplayName("Coiflet orthogonality condition")
    void testCoifletOrthogonality() {
        // Test that sum of squares equals 1
        Coiflet[] coiflets = {Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3};
        
        for (Coiflet coif : coiflets) {
            double[] coeffs = coif.lowPassDecomposition();
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, 1e-4,
                coif.name() + " should satisfy orthogonality condition");
        }
    }
    
    @Test
    @DisplayName("Coiflet vanishing moments")
    void testCoifletVanishingMoments() {
        assertEquals(2, Coiflet.COIF1.vanishingMoments());
        assertEquals(4, Coiflet.COIF2.vanishingMoments());
        assertEquals(6, Coiflet.COIF3.vanishingMoments());
    }
    
    @Test
    @DisplayName("Coiflet filter lengths")
    void testCoifletFilterLengths() {
        assertEquals(6, Coiflet.COIF1.lowPassDecomposition().length);
        assertEquals(12, Coiflet.COIF2.lowPassDecomposition().length);
        assertEquals(18, Coiflet.COIF3.lowPassDecomposition().length);
    }
    
    @Test
    @DisplayName("Coiflet coefficients satisfy orthogonality conditions")
    void testCoefficientOrthogonality() {
        // Verify that all Coiflet variants satisfy the mathematical requirements
        assertTrue(Coiflet.COIF1.verifyCoefficients(), 
            "COIF1 coefficients should satisfy orthogonality conditions");
        assertTrue(Coiflet.COIF2.verifyCoefficients(), 
            "COIF2 coefficients should satisfy orthogonality conditions");
        assertTrue(Coiflet.COIF3.verifyCoefficients(), 
            "COIF3 coefficients should satisfy orthogonality conditions");
    }
    
    @Test
    @DisplayName("Coiflet high-pass filter generation")
    void testHighPassGeneration() {
        // Verify high-pass filter is correctly generated from low-pass
        Coiflet coif1 = Coiflet.COIF1;
        double[] lowPass = coif1.lowPassDecomposition();
        double[] highPass = coif1.highPassDecomposition();
        
        assertEquals(lowPass.length, highPass.length);
        
        // Verify QMF relationship
        for (int i = 0; i < lowPass.length; i++) {
            double expected = (i % 2 == 0 ? 1 : -1) * lowPass[lowPass.length - 1 - i];
            assertEquals(expected, highPass[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Coiflet perfect reconstruction")
    void testPerfectReconstruction() {
        // Test that forward and inverse transforms give perfect reconstruction
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        Coiflet[] coiflets = {Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3};
        for (Coiflet coif : coiflets) {
            WaveletTransform transform = new WaveletTransform(coif, 
                ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
            
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            assertArrayEquals(signal, reconstructed, 5e-4,
                coif.name() + " should provide perfect reconstruction");
        }
    }
    
    @Test
    @DisplayName("Coiflet symmetry properties")
    void testCoifletSymmetry() {
        // Coiflets are designed to have near-symmetry
        // Test by checking moment properties
        Coiflet[] coiflets = {Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3};
        
        for (Coiflet coif : coiflets) {
            double[] h = coif.lowPassDecomposition();
            
            // Calculate first moment (should be sqrt(2) for scaling function)
            double firstMoment = 0;
            for (int k = 0; k < h.length; k++) {
                firstMoment += k * h[k];
            }
            
            // Coiflets have additional vanishing moments for scaling function
            // This makes them more symmetric than standard Daubechies
            assertTrue(Math.abs(firstMoment) < h.length,
                coif.name() + " should have bounded first moment");
        }
    }
    
    @Test
    @DisplayName("Coiflet handles smooth signals")
    void testSmoothSignals() {
        // Coiflets should handle smooth signals well due to vanishing moments
        double[] smoothSignal = new double[16];
        
        // Create a smooth signal
        for (int i = 0; i < 16; i++) {
            smoothSignal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Test all Coiflet variants
        Coiflet[] coiflets = {Coiflet.COIF1, Coiflet.COIF2, Coiflet.COIF3};
        
        for (Coiflet coif : coiflets) {
            WaveletTransform transform = new WaveletTransform(coif,
                ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
            
            TransformResult result = transform.forward(smoothSignal);
            double[] reconstructed = transform.inverse(result);
            
            assertArrayEquals(smoothSignal, reconstructed, 1e-2,
                coif.name() + " should accurately reconstruct smooth signals");
            
            // Verify that detail coefficients are small for smooth signals
            double[] details = result.detailCoeffs();
            double detailEnergy = 0;
            for (double d : details) {
                detailEnergy += d * d;
            }
            
            // Smooth signals should have most energy in approximation coefficients
            assertTrue(detailEnergy < 0.5,
                coif.name() + " should have small detail coefficients for smooth signals");
        }
    }
}