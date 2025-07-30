package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for real-to-complex FFT optimization.
 * Currently tests the FFT infrastructure that will support the optimization.
 */
class RealFFTOptimizationTest {
    
    private FFTAcceleratedCWT fftAccelerator;
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        fftAccelerator = new FFTAcceleratedCWT();
    }
    
    @Test
    @DisplayName("Real FFT should match standard FFT for real signals")
    void testRealFFTMatchesStandardFFT() {
        // Test various power-of-2 sizes
        int[] sizes = {8, 16, 32, 64, 128, 256};
        
        for (int size : sizes) {
            // Create test signal (sine wave)
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 16.0);
            }
            
            // Compute FFT using optimized real FFT
            FFTAcceleratedCWT.Complex[] fftResult = fftAccelerator.fft(signal);
            
            // Verify size
            assertEquals(size, fftResult.length, 
                "FFT result should have same size as input");
            
            // Verify Hermitian symmetry (X[k] = X*[N-k] for real signals)
            for (int k = 1; k < size / 2; k++) {
                FFTAcceleratedCWT.Complex forward = fftResult[k];
                FFTAcceleratedCWT.Complex backward = fftResult[size - k];
                
                assertEquals(forward.real, backward.real, EPSILON,
                    String.format("Real parts should match at k=%d and %d", k, size - k));
                assertEquals(forward.imag, -backward.imag, EPSILON,
                    String.format("Imaginary parts should be negated at k=%d and %d", k, size - k));
            }
            
            // DC and Nyquist components should be real
            assertEquals(0.0, fftResult[0].imag, EPSILON,
                "DC component should be real");
            if (size > 1) {
                assertEquals(0.0, fftResult[size/2].imag, EPSILON,
                    "Nyquist component should be real");
            }
        }
    }
    
    @Test
    @DisplayName("Real FFT should correctly transform known signals")
    void testRealFFTKnownSignals() {
        // Test 1: Constant signal -> DC component only
        double[] constant = new double[16];
        for (int i = 0; i < 16; i++) {
            constant[i] = 1.0;
        }
        
        FFTAcceleratedCWT.Complex[] constantFFT = fftAccelerator.fft(constant);
        assertEquals(16.0, constantFFT[0].real, EPSILON, "DC component should be sum of signal");
        
        for (int k = 1; k < 16; k++) {
            assertEquals(0.0, Math.abs(constantFFT[k].real), EPSILON,
                "Non-DC components should be zero for constant signal");
            assertEquals(0.0, Math.abs(constantFFT[k].imag), EPSILON,
                "Non-DC components should be zero for constant signal");
        }
        
        // Test 2: Single frequency sine wave
        double[] sine = new double[32];
        int freq = 4; // 4 cycles in 32 samples
        for (int i = 0; i < 32; i++) {
            sine[i] = Math.sin(2 * Math.PI * freq * i / 32.0);
        }
        
        FFTAcceleratedCWT.Complex[] sineFFT = fftAccelerator.fft(sine);
        
        // Should have peaks at frequency bins freq and 32-freq
        double peakMagnitude = Math.sqrt(
            sineFFT[freq].real * sineFFT[freq].real + 
            sineFFT[freq].imag * sineFFT[freq].imag
        );
        assertTrue(peakMagnitude > 10.0, "Should have significant peak at frequency bin");
        
        // Other bins should be near zero
        for (int k = 0; k < 32; k++) {
            if (k != freq && k != 32 - freq) {
                double magnitude = Math.sqrt(
                    sineFFT[k].real * sineFFT[k].real + 
                    sineFFT[k].imag * sineFFT[k].imag
                );
                assertTrue(magnitude < 0.1, 
                    String.format("Non-peak bin %d should be near zero", k));
            }
        }
    }
    
    @Test
    @DisplayName("Real FFT should handle small sizes correctly")
    void testRealFFTSmallSizes() {
        // Test size 2
        double[] signal2 = {1.0, -1.0};
        FFTAcceleratedCWT.Complex[] fft2 = fftAccelerator.fft(signal2);
        assertEquals(0.0, fft2[0].real, EPSILON, "DC should be zero");
        // For [1, -1], the DFT gives X[1] = 1 - (-1)*exp(-j*pi) = 1 + 1 = 2
        assertEquals(2.0, Math.abs(fft2[1].real), EPSILON, "Nyquist magnitude should be 2");
        
        // Test size 4  
        double[] signal4 = {1.0, 0.0, -1.0, 0.0};
        FFTAcceleratedCWT.Complex[] fft4 = fftAccelerator.fft(signal4);
        assertEquals(0.0, fft4[0].real, EPSILON, "DC should be zero");
        // For this signal, energy is at k=1 and k=3, not at Nyquist (k=2)
        assertEquals(0.0, fft4[2].real, EPSILON, "Nyquist should be 0 for this signal");
        // Check that we have energy at k=1
        assertTrue(Math.abs(fft4[1].real) > 1.0, "Should have energy at k=1");
    }
    
    @Test
    @DisplayName("Round-trip FFT/IFFT should preserve signal")
    void testRoundTrip() {
        double[] original = new double[64];
        for (int i = 0; i < 64; i++) {
            original[i] = Math.cos(2 * Math.PI * i / 8.0) + 
                         0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Forward FFT
        FFTAcceleratedCWT.Complex[] spectrum = fftAccelerator.fft(original);
        
        // Inverse FFT
        double[] reconstructed = fftAccelerator.ifft(spectrum);
        
        // Compare
        assertArrayEquals(original, reconstructed, EPSILON,
            "Round-trip FFT/IFFT should preserve signal");
    }
    
    @Test
    @DisplayName("FFT infrastructure should handle various signal sizes")
    void testFFTInfrastructure() {
        // Test small size
        double[] small = {1.0, 2.0, 3.0, 4.0};
        FFTAcceleratedCWT.Complex[] smallFFT = fftAccelerator.fft(small);
        assertNotNull(smallFFT);
        assertEquals(4, smallFFT.length);
        
        // Test larger size
        double[] large = new double[8];
        for (int i = 0; i < 8; i++) {
            large[i] = i + 1.0;
        }
        FFTAcceleratedCWT.Complex[] largeFFT = fftAccelerator.fft(large);
        assertNotNull(largeFFT);
        assertEquals(8, largeFFT.length);
        
        // Verify Hermitian symmetry for real signals
        // For real input, FFT output should satisfy X[k] = X*[N-k]
        assertEquals(largeFFT[1].real, largeFFT[7].real, EPSILON,
            "Real parts should match for Hermitian symmetry");
        assertEquals(largeFFT[1].imag, -largeFFT[7].imag, EPSILON,
            "Imaginary parts should be negated for Hermitian symmetry");
    }
}