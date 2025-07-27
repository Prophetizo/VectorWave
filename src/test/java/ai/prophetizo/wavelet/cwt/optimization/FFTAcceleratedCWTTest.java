package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT.Complex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FFTAcceleratedCWT, focusing on parameter validation for the ifft method.
 */
class FFTAcceleratedCWTTest {
    
    private FFTAcceleratedCWT fft;
    
    @BeforeEach
    void setUp() {
        fft = new FFTAcceleratedCWT();
    }
    
    @Nested
    @DisplayName("IFFT Parameter Validation Tests")
    class IFFTValidationTests {
        
        @Test
        @DisplayName("Should throw NullPointerException when input array is null")
        void ifft_shouldThrowNullPointerException_whenInputIsNull() {
            // Given
            Complex[] nullArray = null;
            
            // When & Then
            NullPointerException exception = assertThrows(NullPointerException.class,
                () -> fft.ifft(nullArray));
            
            assertEquals("Input array X must not be null.", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when input length is zero")
        void ifft_shouldThrowIllegalArgumentException_whenLengthIsZero() {
            // Given
            Complex[] emptyArray = new Complex[0];
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(emptyArray));
            
            assertEquals("Input array length must be a power of 2.", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when input length is not power of 2")
        void ifft_shouldThrowIllegalArgumentException_whenLengthIsNotPowerOfTwo() {
            // Test various non-power-of-2 lengths
            int[] invalidLengths = {3, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20};
            
            for (int length : invalidLengths) {
                // Given
                Complex[] invalidArray = new Complex[length];
                for (int i = 0; i < length; i++) {
                    invalidArray[i] = new Complex(1.0, 0.0);
                }
                
                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> fft.ifft(invalidArray),
                    "Length " + length + " should throw exception");
                
                assertEquals("Input array length must be a power of 2.", exception.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should accept valid power-of-2 lengths")
        void ifft_shouldAcceptValidPowerOfTwoLengths() {
            // Test various valid power-of-2 lengths
            int[] validLengths = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};
            
            for (int length : validLengths) {
                // Given
                Complex[] validArray = new Complex[length];
                for (int i = 0; i < length; i++) {
                    validArray[i] = new Complex(1.0, 0.0);
                }
                
                // When & Then - Should not throw exception
                assertDoesNotThrow(() -> fft.ifft(validArray),
                    "Length " + length + " should be accepted");
            }
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when complex coefficient is null")
        void ifft_shouldThrowIllegalArgumentException_whenComplexCoefficientIsNull() {
            // Given
            Complex[] arrayWithNull = new Complex[4];
            arrayWithNull[0] = new Complex(1.0, 0.0);
            arrayWithNull[1] = new Complex(2.0, 1.0);
            arrayWithNull[2] = null; // Null coefficient
            arrayWithNull[3] = new Complex(3.0, -1.0);
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(arrayWithNull));
            
            assertEquals("Complex coefficient at index 2 must not be null.", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when complex coefficient contains NaN")
        void ifft_shouldThrowIllegalArgumentException_whenCoefficientContainsNaN() {
            // Test NaN in real part
            Complex[] arrayWithNaNReal = {
                new Complex(1.0, 0.0),
                new Complex(Double.NaN, 1.0),
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(arrayWithNaNReal));
            assertEquals("Complex coefficient at index 1 must contain finite values.", exception.getMessage());
            
            // Test NaN in imaginary part
            Complex[] arrayWithNaNImag = {
                new Complex(1.0, 0.0),
                new Complex(2.0, Double.NaN),
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            
            exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(arrayWithNaNImag));
            assertEquals("Complex coefficient at index 1 must contain finite values.", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when complex coefficient contains infinity")
        void ifft_shouldThrowIllegalArgumentException_whenCoefficientContainsInfinity() {
            // Test positive infinity in real part
            Complex[] arrayWithPosInfReal = {
                new Complex(1.0, 0.0),
                new Complex(Double.POSITIVE_INFINITY, 1.0),
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(arrayWithPosInfReal));
            assertEquals("Complex coefficient at index 1 must contain finite values.", exception.getMessage());
            
            // Test negative infinity in imaginary part
            Complex[] arrayWithNegInfImag = {
                new Complex(1.0, 0.0),
                new Complex(2.0, Double.NEGATIVE_INFINITY),
                new Complex(2.0, 0.0),
                new Complex(3.0, 0.0)
            };
            
            exception = assertThrows(IllegalArgumentException.class,
                () -> fft.ifft(arrayWithNegInfImag));
            assertEquals("Complex coefficient at index 1 must contain finite values.", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("IFFT Functional Tests")
    class IFFTFunctionalTests {
        
        @Test
        @DisplayName("Should correctly perform IFFT on simple signal")
        void ifft_shouldCorrectlyTransformSimpleSignal() {
            // Given - DC signal (all ones)
            double[] originalSignal = {1.0, 1.0, 1.0, 1.0};
            Complex[] spectrum = fft.fft(originalSignal);
            
            // When
            double[] reconstructed = fft.ifft(spectrum);
            
            // Then - Should reconstruct original signal within numerical precision
            assertEquals(originalSignal.length, reconstructed.length);
            for (int i = 0; i < originalSignal.length; i++) {
                assertEquals(originalSignal[i], reconstructed[i], 1e-10,
                    "Mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Should correctly perform IFFT on impulse signal")
        void ifft_shouldCorrectlyTransformImpulseSignal() {
            // Given - Impulse signal
            double[] originalSignal = {1.0, 0.0, 0.0, 0.0};
            Complex[] spectrum = fft.fft(originalSignal);
            
            // When
            double[] reconstructed = fft.ifft(spectrum);
            
            // Then - Should reconstruct original signal
            assertEquals(originalSignal.length, reconstructed.length);
            for (int i = 0; i < originalSignal.length; i++) {
                assertEquals(originalSignal[i], reconstructed[i], 1e-10,
                    "Mismatch at index " + i);
            }
        }
        
        @Test
        @DisplayName("Should handle single element array")
        void ifft_shouldHandleSingleElement() {
            // Given
            Complex[] singleElement = {new Complex(5.0, 3.0)};
            
            // When
            double[] result = fft.ifft(singleElement);
            
            // Then
            assertEquals(1, result.length);
            assertEquals(5.0, result[0], 1e-10); // Real part should be extracted
        }
        
        @Test
        @DisplayName("Should correctly handle complex spectrum")
        void ifft_shouldHandleComplexSpectrum() {
            // Given - Complex spectrum
            Complex[] spectrum = {
                new Complex(4.0, 0.0),    // DC component
                new Complex(1.0, -1.0),   // Complex frequency component
                new Complex(0.0, 0.0),    // Zero component
                new Complex(1.0, 1.0)     // Complex conjugate
            };
            
            // When
            double[] result = fft.ifft(spectrum);
            
            // Then - Should produce real-valued output
            assertEquals(4, result.length);
            for (double value : result) {
                assertTrue(Double.isFinite(value), "Result should be finite");
            }
        }
    }
    
    @Nested
    @DisplayName("FFT Parameter Validation Tests")
    class FFTValidationTests {
        
        @Test
        @DisplayName("Should throw NullPointerException when FFT input is null")
        void fft_shouldThrowNullPointerException_whenInputIsNull() {
            // Given
            double[] nullArray = null;
            
            // When & Then
            NullPointerException exception = assertThrows(NullPointerException.class,
                () -> fft.fft(nullArray));
            
            assertEquals("Input array x must not be null.", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when FFT input contains NaN")
        void fft_shouldThrowIllegalArgumentException_whenInputContainsNaN() {
            // Given
            double[] arrayWithNaN = {1.0, 2.0, Double.NaN, 4.0};
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.fft(arrayWithNaN));
            
            assertEquals("Input array must contain only finite values at index 2", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should throw IllegalArgumentException when FFT input contains infinity")
        void fft_shouldThrowIllegalArgumentException_whenInputContainsInfinity() {
            // Given
            double[] arrayWithInf = {1.0, 2.0, Double.POSITIVE_INFINITY, 4.0};
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fft.fft(arrayWithInf));
            
            assertEquals("Input array must contain only finite values at index 2", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Complex Number Tests")
    class ComplexNumberTests {
        
        @Test
        @DisplayName("Should correctly create complex numbers")
        void complex_shouldCreateCorrectly() {
            // Given & When
            Complex c = new Complex(3.0, 4.0);
            
            // Then
            assertEquals(3.0, c.real);
            assertEquals(4.0, c.imag);
        }
        
        @Test
        @DisplayName("Should correctly multiply complex numbers")
        void complex_shouldMultiplyCorrectly() {
            // Given
            Complex c1 = new Complex(2.0, 3.0);
            Complex c2 = new Complex(1.0, -1.0);
            
            // When
            Complex result = c1.multiply(c2);
            
            // Then
            // (2 + 3i) * (1 - i) = 2 - 2i + 3i - 3i² = 2 + i + 3 = 5 + i
            assertEquals(5.0, result.real, 1e-10);
            assertEquals(1.0, result.imag, 1e-10);
        }
        
        @Test
        @DisplayName("Should correctly compute conjugate")
        void complex_shouldComputeConjugateCorrectly() {
            // Given
            Complex c = new Complex(3.0, -4.0);
            
            // When
            Complex conjugate = c.conjugate();
            
            // Then
            assertEquals(3.0, conjugate.real);
            assertEquals(4.0, conjugate.imag);
        }
    }
}