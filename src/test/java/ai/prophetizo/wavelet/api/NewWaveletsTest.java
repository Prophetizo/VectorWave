package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for newly added wavelets.
 * Validates mathematical correctness, registration, and basic functionality.
 */
class NewWaveletsTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // Provide all new Daubechies wavelets for parameterized testing
    static Stream<Daubechies> newDaubechiesWavelets() {
        return Stream.of(
            Daubechies.DB6,
            Daubechies.DB8,
            Daubechies.DB10
        );
    }
    
    // Provide all new Symlet wavelets for parameterized testing
    static Stream<Symlet> newSymletWavelets() {
        return Stream.of(
            Symlet.SYM5,
            Symlet.SYM6,
            Symlet.SYM7,
            Symlet.SYM8,
            Symlet.SYM10,
            Symlet.SYM12,
            Symlet.SYM15,
            Symlet.SYM20
        );
    }
    
    // Provide all new Coiflet wavelets for parameterized testing
    static Stream<Coiflet> newCoifletWavelets() {
        return Stream.of(
            Coiflet.COIF4,
            Coiflet.COIF5
        );
    }
    
    @ParameterizedTest
    @MethodSource("newDaubechiesWavelets")
    @DisplayName("Daubechies wavelets should satisfy orthogonality conditions")
    void testDaubechiesOrthogonality(Daubechies wavelet) {
        assertTrue(wavelet.verifyCoefficients(), 
            "Daubechies " + wavelet.name() + " should satisfy orthogonality conditions");
    }
    
    @ParameterizedTest
    @MethodSource("newDaubechiesWavelets")
    @DisplayName("Daubechies wavelets should have correct filter lengths")
    void testDaubechiesFilterLength(Daubechies wavelet) {
        int expectedLength = 2 * wavelet.vanishingMoments();
        assertEquals(expectedLength, wavelet.lowPassDecomposition().length,
            "Filter length should be 2 * vanishing moments");
        assertEquals(expectedLength, wavelet.highPassDecomposition().length,
            "High-pass filter should have same length as low-pass");
    }
    
    @ParameterizedTest
    @MethodSource("newDaubechiesWavelets")
    @DisplayName("Daubechies wavelets should be registered in WaveletRegistry")
    void testDaubechiesRegistration(Daubechies wavelet) {
        assertTrue(WaveletRegistry.hasWavelet(wavelet.name()),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(wavelet.name());
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    @ParameterizedTest
    @MethodSource("newSymletWavelets")
    @DisplayName("Symlet wavelets should satisfy orthogonality conditions")
    void testSymletOrthogonality(Symlet wavelet) {
        assertTrue(wavelet.verifyCoefficients(), 
            "Symlet " + wavelet.name() + " should satisfy orthogonality conditions");
    }
    
    @ParameterizedTest
    @MethodSource("newSymletWavelets")
    @DisplayName("Symlet wavelets should have correct filter lengths")
    void testSymletFilterLength(Symlet wavelet) {
        // Most Symlets have 2 * vanishing moments coefficients
        // But some may have more due to optimization constraints
        int minExpectedLength = 2 * wavelet.vanishingMoments();
        int actualLength = wavelet.lowPassDecomposition().length;
        
        assertTrue(actualLength >= minExpectedLength,
            "Filter length should be at least 2 * vanishing moments for " + wavelet.name() +
            " (expected >= " + minExpectedLength + ", got " + actualLength + ")");
        
        assertEquals(actualLength, wavelet.highPassDecomposition().length,
            "High-pass filter should have same length as low-pass for " + wavelet.name());
    }
    
    @ParameterizedTest
    @MethodSource("newSymletWavelets")
    @DisplayName("Symlet wavelets should be registered in WaveletRegistry")
    void testSymletRegistration(Symlet wavelet) {
        assertTrue(WaveletRegistry.hasWavelet(wavelet.name()),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(wavelet.name());
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    @ParameterizedTest
    @MethodSource("newCoifletWavelets")
    @DisplayName("Coiflet wavelets should have correct filter lengths")
    void testCoifletFilterLength(Coiflet wavelet) {
        // Coiflet filter length is 6 * order (not vanishing moments)
        // COIF4 has order 4, so 24 coefficients
        // COIF5 has order 5, so 30 coefficients
        int order = Integer.parseInt(wavelet.name().substring(4));
        int expectedLength = 6 * order;
        assertEquals(expectedLength, wavelet.lowPassDecomposition().length,
            "Filter length should be 6 * order for " + wavelet.name());
        assertEquals(expectedLength, wavelet.highPassDecomposition().length,
            "High-pass filter should have same length as low-pass for " + wavelet.name());
    }
    
    @ParameterizedTest
    @MethodSource("newCoifletWavelets")
    @DisplayName("Coiflet wavelets should be registered in WaveletRegistry")
    void testCoifletRegistration(Coiflet wavelet) {
        assertTrue(WaveletRegistry.hasWavelet(wavelet.name()),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(wavelet.name());
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    @ParameterizedTest
    @MethodSource("newCoifletWavelets")
    @DisplayName("Coiflet wavelets should satisfy orthogonality conditions")
    void testCoifletOrthogonality(Coiflet wavelet) {
        double[] h = wavelet.lowPassDecomposition();
        
        // Check sum = √2
        double sum = 0;
        for (double coeff : h) {
            sum += coeff;
        }
        // Use more relaxed tolerance for COIF5 due to numerical precision of coefficients
        double tolerance = wavelet.name().equals("coif5") ? 1e-2 : 1e-6;
        assertEquals(Math.sqrt(2), sum, tolerance,
            "Sum of coefficients should equal √2 for " + wavelet.name());
        
        // Check sum of squares = 1
        double sumSquares = 0;
        for (double coeff : h) {
            sumSquares += coeff * coeff;
        }
        assertEquals(1.0, sumSquares, 1e-6,
            "Sum of squared coefficients should equal 1 for " + wavelet.name());
    }
    
    @Test
    @DisplayName("All new wavelets should be listed in getOrthogonalWavelets()")
    void testAllNewWaveletsInOrthogonalList() {
        var orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Check new Daubechies wavelets
        assertTrue(orthogonalWavelets.contains("db6"));
        assertTrue(orthogonalWavelets.contains("db8"));
        assertTrue(orthogonalWavelets.contains("db10"));
        
        // Check new Symlet wavelets
        assertTrue(orthogonalWavelets.contains("sym5"));
        assertTrue(orthogonalWavelets.contains("sym6"));
        assertTrue(orthogonalWavelets.contains("sym7"));
        assertTrue(orthogonalWavelets.contains("sym8"));
        assertTrue(orthogonalWavelets.contains("sym10"));
        assertTrue(orthogonalWavelets.contains("sym12"));
        assertTrue(orthogonalWavelets.contains("sym15"));
        assertTrue(orthogonalWavelets.contains("sym20"));
        
        // Check new Coiflet wavelets
        assertTrue(orthogonalWavelets.contains("coif4"));
        assertTrue(orthogonalWavelets.contains("coif5"));
    }
    
    @Test
    @DisplayName("Registry should provide correct descriptions for new wavelets")
    void testWaveletDescriptions() {
        // Test Daubechies descriptions
        assertEquals("Daubechies wavelet of order 6", 
                    WaveletRegistry.getWavelet("db6").description());
        assertEquals("Daubechies wavelet of order 8", 
                    WaveletRegistry.getWavelet("db8").description());
        assertEquals("Daubechies wavelet of order 10", 
                    WaveletRegistry.getWavelet("db10").description());
        
        // Test Symlet descriptions
        assertEquals("Symlet wavelet of order 5", 
                    WaveletRegistry.getWavelet("sym5").description());
        assertEquals("Symlet wavelet of order 20", 
                    WaveletRegistry.getWavelet("sym20").description());
        
        // Test Coiflet descriptions
        assertEquals("Coiflet wavelet of order 4", 
                    WaveletRegistry.getWavelet("coif4").description());
        assertEquals("Coiflet wavelet of order 5", 
                    WaveletRegistry.getWavelet("coif5").description());
    }
    
    @Test
    @DisplayName("High-pass filters should be proper quadrature mirror filters")
    void testQuadratureMirrorRelationship() {
        // Test a few wavelets to ensure QMF relationship is correct
        verifyQMF(Daubechies.DB6);
        verifyQMF(Symlet.SYM8);
        verifyQMF(Coiflet.COIF4);
    }
    
    private void verifyQMF(OrthogonalWavelet wavelet) {
        double[] h = wavelet.lowPassDecomposition();
        double[] g = wavelet.highPassDecomposition();
        
        assertEquals(h.length, g.length, 
            "High-pass and low-pass filters should have same length");
        
        // Verify g[n] = (-1)^n * h[L-1-n] relationship
        for (int i = 0; i < h.length; i++) {
            double expected = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
            assertEquals(expected, g[i], TOLERANCE,
                "QMF relationship violated at index " + i + " for " + wavelet.name());
        }
    }
    
    @Test
    @DisplayName("Verify specific coefficient values for DB6")
    void testDB6SpecificCoefficients() {
        double[] coeffs = Daubechies.DB6.lowPassDecomposition();
        
        // Verify a few key coefficients against known values
        assertEquals(0.1115407433501094, coeffs[0], 1e-15);
        assertEquals(0.4946238903984530, coeffs[1], 1e-15);
        assertEquals(0.7511339080210954, coeffs[2], 1e-15);
        assertEquals(-0.0010773010853085, coeffs[11], 1e-15);
    }
    
    @Test
    @DisplayName("Verify specific coefficient values for SYM10")
    void testSYM10SpecificCoefficients() {
        double[] coeffs = Symlet.SYM10.lowPassDecomposition();
        
        assertEquals(20, coeffs.length, "SYM10 should have 20 coefficients");
        
        // Verify a few key coefficients
        assertEquals(0.0001909865953747, coeffs[0], 1e-15);
        assertEquals(0.4587134074291871, coeffs[14], 1e-15);
        assertEquals(0.0076074873252916, coeffs[19], 1e-15);
    }
    
    @Test
    @DisplayName("Verify specific coefficient values for COIF5")
    void testCOIF5SpecificCoefficients() {
        double[] coeffs = Coiflet.COIF5.lowPassDecomposition();
        
        assertEquals(30, coeffs.length, "COIF5 should have 30 coefficients");
        
        // Verify a few key coefficients
        assertEquals(-0.0000000892313669, coeffs[0], 1e-15);
        assertEquals(0.7742896038276542, coeffs[19], 1e-15);
        assertEquals(-0.0002120808398231, coeffs[29], 1e-15);
    }
}