package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Symlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Symlet wavelet implementations.
 * Verifies mathematical properties and coefficient correctness.
 */
class SymletTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Symlet coefficients are normalized")
    void testSymletNormalization() {
        // Test that low-pass filter coefficients sum to sqrt(2)
        String[] wavelets = {"sym2", "sym3", "sym4"};
        Symlet[] symlets = {Symlet.SYM2, Symlet.SYM3, Symlet.SYM4};
        
        for (int i = 0; i < symlets.length; i++) {
            double[] coeffs = symlets[i].lowPassDecomposition();
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, EPSILON, 
                wavelets[i] + " coefficients should sum to sqrt(2)");
        }
    }
    
    @Test
    @DisplayName("Symlet orthogonality condition")
    void testSymletOrthogonality() {
        // Test that sum of squares equals 1
        Symlet[] symlets = {Symlet.SYM2, Symlet.SYM3, Symlet.SYM4};
        
        for (Symlet sym : symlets) {
            double[] coeffs = sym.lowPassDecomposition();
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, EPSILON,
                sym.name() + " should satisfy orthogonality condition");
        }
    }
    
    @Test
    @DisplayName("Symlet high-pass filter generation")
    void testHighPassGeneration() {
        // Verify high-pass filter is correctly generated from low-pass
        Symlet sym2 = Symlet.SYM2;
        double[] lowPass = sym2.lowPassDecomposition();
        double[] highPass = sym2.highPassDecomposition();
        
        assertEquals(lowPass.length, highPass.length);
        
        // Verify QMF relationship
        for (int i = 0; i < lowPass.length; i++) {
            double expected = (i % 2 == 0 ? 1 : -1) * lowPass[lowPass.length - 1 - i];
            assertEquals(expected, highPass[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Symlet perfect reconstruction")
    void testPerfectReconstruction() {
        // Test that forward and inverse transforms give perfect reconstruction
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        Symlet[] symlets = {Symlet.SYM2, Symlet.SYM3, Symlet.SYM4};
        for (Symlet sym : symlets) {
            WaveletTransform transform = new WaveletTransform(sym, 
                ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC);
            
            TransformResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            assertArrayEquals(signal, reconstructed, EPSILON,
                sym.name() + " should provide perfect reconstruction");
        }
    }
}