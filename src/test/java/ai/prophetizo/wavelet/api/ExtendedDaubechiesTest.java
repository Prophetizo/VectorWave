package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Extended Daubechies wavelets (DB12-DB45).
 */
class ExtendedDaubechiesTest {

    @Test
    @DisplayName("Verify all extended Daubechies wavelets are accessible")
    void testAllExtendedDaubechiesAccessible() {
        // DB12-DB20
        for (int order = 12; order <= 20; order += 2) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "DB" + order + " should be accessible");
            assertTrue(wavelet instanceof Daubechies, "DB" + order + " should be Daubechies instance");
            assertEquals(2 * order, wavelet.lowPassDecomposition().length, 
                "DB" + order + " should have " + (2 * order) + " coefficients");
        }
        
        // DB22-DB30
        for (int order = 22; order <= 30; order += 2) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "DB" + order + " should be accessible");
            assertTrue(wavelet instanceof Daubechies, "DB" + order + " should be Daubechies instance");
            assertEquals(2 * order, wavelet.lowPassDecomposition().length, 
                "DB" + order + " should have " + (2 * order) + " coefficients");
        }
        
        // DB32-DB38 (PyWavelets maximum)
        for (int order = 32; order <= 38; order += 2) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "DB" + order + " should be accessible");
            assertTrue(wavelet instanceof Daubechies, "DB" + order + " should be Daubechies instance");
            assertEquals(2 * order, wavelet.lowPassDecomposition().length, 
                "DB" + order + " should have " + (2 * order) + " coefficients");
        }
        
        // DB40-DB45 (MATLAB maximum)
        int[] matlabOrders = {40, 42, 44, 45};
        for (int order : matlabOrders) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "DB" + order + " should be accessible");
            assertTrue(wavelet instanceof Daubechies, "DB" + order + " should be Daubechies instance");
            assertEquals(2 * order, wavelet.lowPassDecomposition().length, 
                "DB" + order + " should have " + (2 * order) + " coefficients");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 18, 24, 30, 38, 45})
    @DisplayName("Test MODWT transform with extended Daubechies wavelets")
    void testMODWTWithExtendedDaubechies(int order) {
        WaveletName name = WaveletName.valueOf("DB" + order);
        Wavelet wavelet = WaveletRegistry.getWavelet(name);
        
        // Create test signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32.0);
        }
        
        // Test transform
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);
        
        assertNotNull(result, "Transform result should not be null");
        assertEquals(signal.length, result.approximationCoeffs().length, 
            "Approximation coefficients length should match signal");
        assertEquals(signal.length, result.detailCoeffs().length, 
            "Detail coefficients length should match signal");
        
        // Test reconstruction
        double[] reconstructed = transform.inverse(result);
        assertNotNull(reconstructed, "Reconstructed signal should not be null");
        assertEquals(signal.length, reconstructed.length, 
            "Reconstructed signal length should match original");
        
        // Check reconstruction error
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            double error = Math.abs(signal[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        
        assertTrue(maxError < 1e-10, 
            String.format("DB%d reconstruction error too large: %.2e", order, maxError));
    }

    @Test
    @DisplayName("Test coefficient normalization for extended Daubechies")
    void testCoefficientNormalization() {
        int[] orders = {12, 20, 30, 38, 45};
        
        for (int order : orders) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            double[] coeffs = wavelet.lowPassDecomposition();
            
            // Check sum equals sqrt(2)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 1e-10, 
                String.format("DB%d coefficients should sum to sqrt(2)", order));
            
            // Check sum of squares equals 1
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, 1e-10, 
                String.format("DB%d coefficients sum of squares should equal 1", order));
        }
    }

    @Test
    @DisplayName("Test filter length progression")
    void testFilterLengthProgression() {
        // Verify filter lengths follow the pattern: length = 2 * order
        for (int order = 12; order <= 20; order += 2) {
            verifyFilterLength(order);
        }
        for (int order = 22; order <= 30; order += 2) {
            verifyFilterLength(order);
        }
        for (int order = 32; order <= 38; order += 2) {
            verifyFilterLength(order);
        }
        int[] matlabOrders = {40, 42, 44, 45};
        for (int order : matlabOrders) {
            verifyFilterLength(order);
        }
    }
    
    private void verifyFilterLength(int order) {
        WaveletName name = WaveletName.valueOf("DB" + order);
        Wavelet wavelet = WaveletRegistry.getWavelet(name);
        int expectedLength = 2 * order;
        assertEquals(expectedLength, wavelet.lowPassDecomposition().length,
            String.format("DB%d should have filter length %d", order, expectedLength));
    }

    @Test
    @DisplayName("Compare computational complexity of different orders")
    void testComputationalComplexity() {
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        System.out.println("\n=== Extended Daubechies Computational Complexity ===");
        System.out.println("Signal length: " + signal.length);
        System.out.printf("%-10s %-15s %-20s%n", "Order", "Filter Length", "Transform Time (ms)");
        System.out.println("-".repeat(50));
        
        int[] testOrders = {12, 18, 24, 30, 38, 45};
        for (int order : testOrders) {
            WaveletName name = WaveletName.valueOf("DB" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            // Warm up
            for (int i = 0; i < 10; i++) {
                transform.forward(signal);
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                transform.forward(signal);
            }
            double avgTime = (System.nanoTime() - startTime) / 1e6 / 100;
            
            System.out.printf("DB%-7d %-15d %-20.2f%n", 
                order, wavelet.lowPassDecomposition().length, avgTime);
        }
    }
}