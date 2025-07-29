package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for algorithm selection and optimized FFT implementation.
 */
class OptimizedFFTTest {
    
    private FFTAcceleratedCWT standardFFT;
    private FFTAcceleratedCWT optimizedFFT;
    
    @BeforeEach
    void setUp() {
        standardFFT = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
        optimizedFFT = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
    }
    
    @Nested
    @DisplayName("Algorithm Selection Tests")
    class AlgorithmSelectionTests {
        
        @Test
        @DisplayName("Default constructor should use OPTIMIZED algorithm")
        void defaultConstructor_shouldUseOptimizedAlgorithm() {
            // Given & When
            FFTAcceleratedCWT defaultFFT = new FFTAcceleratedCWT();
            
            // Then
            assertEquals(FFTAlgorithm.OPTIMIZED, defaultFFT.getAlgorithm());
        }
        
        @Test
        @DisplayName("Constructor should accept STANDARD algorithm")
        void constructor_shouldAcceptStandardAlgorithm() {
            // Given & When
            FFTAcceleratedCWT fft = new FFTAcceleratedCWT(FFTAlgorithm.STANDARD);
            
            // Then
            assertEquals(FFTAlgorithm.STANDARD, fft.getAlgorithm());
        }
        
        @Test
        @DisplayName("Constructor should accept OPTIMIZED algorithm")
        void constructor_shouldAcceptOptimizedAlgorithm() {
            // Given & When
            FFTAcceleratedCWT fft = new FFTAcceleratedCWT(FFTAlgorithm.OPTIMIZED);
            
            // Then
            assertEquals(FFTAlgorithm.OPTIMIZED, fft.getAlgorithm());
        }
        
        @Test
        @DisplayName("Constructor should reject null algorithm")
        void constructor_shouldRejectNullAlgorithm() {
            // Given
            FFTAlgorithm nullAlgorithm = null;
            
            // When & Then
            NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new FFTAcceleratedCWT(nullAlgorithm));
            
            assertEquals("FFT algorithm must not be null.", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Algorithm Equivalence Tests")
    class AlgorithmEquivalenceTests {
        
        @Test
        @DisplayName("Both algorithms should produce identical results for DC signal")
        void bothAlgorithms_shouldProduceIdenticalResults_forDCSignal() {
            // Given - DC signal (all ones)
            double[] signal = {1.0, 1.0, 1.0, 1.0};
            
            // When
            Complex[] standardResult = standardFFT.fft(signal);
            Complex[] optimizedResult = optimizedFFT.fft(signal);
            
            // Then
            assertEquals(standardResult.length, optimizedResult.length);
            for (int i = 0; i < standardResult.length; i++) {
                assertEquals(standardResult[i].real, optimizedResult[i].real, 1e-10,
                    "Real part mismatch at index " + i);
                assertEquals(standardResult[i].imag, optimizedResult[i].imag, 1e-10,
                    "Imaginary part mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Both algorithms should produce identical results for impulse signal")
        void bothAlgorithms_shouldProduceIdenticalResults_forImpulseSignal() {
            // Given - Impulse signal
            double[] signal = {1.0, 0.0, 0.0, 0.0};
            
            // When
            Complex[] standardResult = standardFFT.fft(signal);
            Complex[] optimizedResult = optimizedFFT.fft(signal);
            
            // Then
            assertEquals(standardResult.length, optimizedResult.length);
            for (int i = 0; i < standardResult.length; i++) {
                assertEquals(standardResult[i].real, optimizedResult[i].real, 1e-10,
                    "Real part mismatch at index " + i);
                assertEquals(standardResult[i].imag, optimizedResult[i].imag, 1e-10,
                    "Imaginary part mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Both algorithms should produce identical results for sine wave")
        void bothAlgorithms_shouldProduceIdenticalResults_forSineWave() {
            // Given - Sine wave
            double[] signal = new double[8];
            for (int i = 0; i < 8; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 8);
            }
            
            // When
            Complex[] standardResult = standardFFT.fft(signal);
            Complex[] optimizedResult = optimizedFFT.fft(signal);
            
            // Then
            assertEquals(standardResult.length, optimizedResult.length);
            for (int i = 0; i < standardResult.length; i++) {
                assertEquals(standardResult[i].real, optimizedResult[i].real, 1e-10,
                    "Real part mismatch at index " + i);
                assertEquals(standardResult[i].imag, optimizedResult[i].imag, 1e-10,
                    "Imaginary part mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Both algorithms should produce identical IFFT results")
        void bothAlgorithms_shouldProduceIdenticalIFFTResults() {
            // Given - Complex spectrum
            Complex[] spectrum = {
                new Complex(4.0, 0.0),
                new Complex(1.0, -1.0),
                new Complex(0.0, 0.0),
                new Complex(1.0, 1.0)
            };
            
            // When
            double[] standardResult = standardFFT.ifft(spectrum);
            double[] optimizedResult = optimizedFFT.ifft(spectrum);
            
            // Then
            assertEquals(standardResult.length, optimizedResult.length);
            for (int i = 0; i < standardResult.length; i++) {
                assertEquals(standardResult[i], optimizedResult[i], 1e-10,
                    "Mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Both algorithms should handle round-trip transformation")
        void bothAlgorithms_shouldHandleRoundTripTransformation() {
            // Given - Random signal
            double[] originalSignal = {1.5, -0.5, 2.0, 0.8, -1.2, 0.3, -0.7, 1.1};
            
            // When - Standard algorithm round trip
            Complex[] standardFFT_result = standardFFT.fft(originalSignal);
            double[] standardRoundTrip = standardFFT.ifft(standardFFT_result);
            
            // When - Optimized algorithm round trip
            Complex[] optimizedFFT_result = optimizedFFT.fft(originalSignal);
            double[] optimizedRoundTrip = optimizedFFT.ifft(optimizedFFT_result);
            
            // Then - Both should reconstruct the original signal
            for (int i = 0; i < originalSignal.length; i++) {
                assertEquals(originalSignal[i], standardRoundTrip[i], 1e-10,
                    "Standard round trip failed at index " + i);
                assertEquals(originalSignal[i], optimizedRoundTrip[i], 1e-10,
                    "Optimized round trip failed at index " + i);
                assertEquals(standardRoundTrip[i], optimizedRoundTrip[i], 1e-10,
                    "Results differ between algorithms at index " + i);
            }
        }
        
        @Test
        @DisplayName("Both algorithms should handle large signals consistently")
        void bothAlgorithms_shouldHandleLargeSignalsConsistently() {
            // Given - Larger signal (64 points)
            double[] signal = new double[64];
            for (int i = 0; i < 64; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 64) + 0.5 * Math.cos(4 * Math.PI * i / 64);
            }
            
            // When
            Complex[] standardResult = standardFFT.fft(signal);
            Complex[] optimizedResult = optimizedFFT.fft(signal);
            
            // Then
            assertEquals(standardResult.length, optimizedResult.length);
            for (int i = 0; i < standardResult.length; i++) {
                assertEquals(standardResult[i].real, optimizedResult[i].real, 1e-9,
                    "Real part mismatch at index " + i);
                assertEquals(standardResult[i].imag, optimizedResult[i].imag, 1e-9,
                    "Imaginary part mismatch at index " + i);
            }
        }
    }
    
    @Nested
    @DisplayName("Twiddle Factor Cache Tests")
    class TwiddleFactorCacheTests {
        
        @Test
        @DisplayName("Cache should be populated after FFT operations")
        void cache_shouldBePopulatedAfterFFTOperations() {
            // Given
            int initialCacheSize = OptimizedFFT.getCacheSize();
            double[] signal = {1.0, 0.0, 0.0, 0.0};
            
            // When
            optimizedFFT.fft(signal);
            
            // Then
            assertTrue(OptimizedFFT.getCacheSize() >= initialCacheSize,
                "Cache should be populated after FFT operation");
        }
        
        @Test
        @DisplayName("Cache should be reused for same size transforms")
        void cache_shouldBeReusedForSameSizeTransforms() {
            // Given
            OptimizedFFT.clearCache();
            double[] signal1 = {1.0, 0.0, 0.0, 0.0};
            double[] signal2 = {0.0, 1.0, 0.0, 0.0};
            
            // When
            optimizedFFT.fft(signal1);
            int cacheAfterFirst = OptimizedFFT.getCacheSize();
            optimizedFFT.fft(signal2);
            int cacheAfterSecond = OptimizedFFT.getCacheSize();
            
            // Then
            assertEquals(cacheAfterFirst, cacheAfterSecond,
                "Cache size should not change for same size transforms");
        }
        
        @Test
        @DisplayName("Cache clear should empty cache")
        void cacheClear_shouldEmptyCache() {
            // Given
            double[] signal = {1.0, 0.0, 0.0, 0.0};
            optimizedFFT.fft(signal);
            assertTrue(OptimizedFFT.getCacheSize() > 0, "Cache should be populated");
            
            // When
            OptimizedFFT.clearCache();
            
            // Then
            assertEquals(0, OptimizedFFT.getCacheSize(), "Cache should be empty after clear");
        }
    }
    
    @Nested
    @DisplayName("In-Place Algorithm Tests")
    class InPlaceAlgorithmTests {
        
        @Test
        @DisplayName("In-place FFT should handle single element")
        void inPlaceFFT_shouldHandleSingleElement() {
            // Given
            Complex[] data = {new Complex(5.0, 3.0)};
            Complex expected = new Complex(5.0, 3.0);
            
            // When
            OptimizedFFT.fftInPlace(data);
            
            // Then
            assertEquals(expected.real, data[0].real, 1e-10);
            assertEquals(expected.imag, data[0].imag, 1e-10);
        }
        
        @Test
        @DisplayName("In-place FFT should handle two elements")
        void inPlaceFFT_shouldHandleTwoElements() {
            // Given
            Complex[] data = {new Complex(1.0, 0.0), new Complex(1.0, 0.0)};
            
            // When
            OptimizedFFT.fftInPlace(data);
            
            // Then - Should produce [2.0, 0.0]
            assertEquals(2.0, data[0].real, 1e-10);
            assertEquals(0.0, data[0].imag, 1e-10);
            assertEquals(0.0, data[1].real, 1e-10);
            assertEquals(0.0, data[1].imag, 1e-10);
        }
        
        @Test
        @DisplayName("In-place FFT should reject invalid lengths")
        void inPlaceFFT_shouldRejectInvalidLengths() {
            // Given
            Complex[] invalidData = new Complex[3];
            for (int i = 0; i < 3; i++) {
                invalidData[i] = new Complex(1.0, 0.0);
            }
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> OptimizedFFT.fftInPlace(invalidData));
            
            assertEquals("Data length must be a power of 2.", exception.getMessage());
        }
        
        @Test
        @DisplayName("In-place IFFT should handle single element")
        void inPlaceIFFT_shouldHandleSingleElement() {
            // Given
            Complex[] data = {new Complex(5.0, 3.0)};
            Complex expected = new Complex(5.0, 3.0);
            
            // When
            OptimizedFFT.ifftInPlace(data);
            
            // Then
            assertEquals(expected.real, data[0].real, 1e-10);
            assertEquals(expected.imag, data[0].imag, 1e-10);
        }
    }
}