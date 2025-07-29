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
    
    @Nested
    @DisplayName("Linear Convolution Tests")
    class LinearConvolutionTests {
        
        @Test
        @DisplayName("Should perform linear convolution without circular artifacts")
        void convolveLinear_shouldAvoidCircularArtifacts() {
            // Given - simple test case where circular vs linear gives different results
            double[] signal = {1.0, 2.0, 3.0, 0.0}; // Note: trailing zero to highlight difference
            double[] kernel = {0.5, -0.5}; // Simple difference kernel
            
            // When
            double[] result = fft.convolveLinear(signal, kernel);
            
            // Then - result should have length signal.length + kernel.length - 1
            assertEquals(5, result.length, "Linear convolution result should have length N+M-1");
            
            // Expected linear convolution result: [0.5, 0.5, 0.5, -1.5, 0.0]
            double[] expected = {0.5, 0.5, 0.5, -1.5, 0.0};
            
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], result[i], 1e-12, 
                    "Linear convolution result at index " + i + " should match expected value");
            }
        }
        
        @Test
        @DisplayName("Should handle edge case with unit impulse")
        void convolveLinear_shouldHandleUnitImpulse() {
            // Given - unit impulse should return kernel unchanged
            double[] signal = {1.0, 0.0, 0.0, 0.0};
            double[] kernel = {1.0, -1.0, 0.5};
            
            // When
            double[] result = fft.convolveLinear(signal, kernel);
            
            // Then - first part should be kernel, rest zeros
            assertEquals(6, result.length, "Result length should be N+M-1");
            
            for (int i = 0; i < kernel.length; i++) {
                assertEquals(kernel[i], result[i], 1e-12, 
                    "Convolution with unit impulse should preserve kernel at index " + i);
            }
            
            for (int i = kernel.length; i < result.length; i++) {
                assertEquals(0.0, result[i], 1e-12, 
                    "Remaining values should be zero at index " + i);
            }
        }
        
        @Test
        @DisplayName("Should validate input parameters")
        void convolveLinear_shouldValidateInputs() {
            double[] validSignal = {1.0, 2.0};
            double[] validKernel = {0.5};
            
            // Test null signal
            assertThrows(NullPointerException.class, 
                () -> fft.convolveLinear(null, validKernel));
            
            // Test null kernel
            assertThrows(NullPointerException.class, 
                () -> fft.convolveLinear(validSignal, null));
            
            // Test empty signal
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveLinear(new double[0], validKernel));
            
            // Test empty kernel
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveLinear(validSignal, new double[0]));
            
            // Test signal with NaN
            double[] signalWithNaN = {1.0, Double.NaN};
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveLinear(signalWithNaN, validKernel));
            
            // Test kernel with infinity
            double[] kernelWithInf = {Double.POSITIVE_INFINITY};
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveLinear(validSignal, kernelWithInf));
        }
        
        @Test
        @DisplayName("Should produce same result as direct convolution for small inputs")
        void convolveLinear_shouldMatchDirectConvolution() {
            // Given
            double[] signal = {1.0, 2.0, 3.0};
            double[] kernel = {0.5, -0.25};
            
            // When - FFT convolution
            double[] fftResult = fft.convolveLinear(signal, kernel);
            
            // When - direct convolution (reference implementation)
            double[] directResult = directConvolve(signal, kernel);
            
            // Then
            assertEquals(directResult.length, fftResult.length, "Result lengths should match");
            
            for (int i = 0; i < directResult.length; i++) {
                assertEquals(directResult[i], fftResult[i], 1e-12, 
                    "FFT and direct convolution should match at index " + i);
            }
        }
        
        private double[] directConvolve(double[] signal, double[] kernel) {
            int outputLength = signal.length + kernel.length - 1;
            double[] result = new double[outputLength];
            
            for (int n = 0; n < outputLength; n++) {
                for (int k = 0; k < kernel.length; k++) {
                    int signalIndex = n - k;
                    if (signalIndex >= 0 && signalIndex < signal.length) {
                        result[n] += signal[signalIndex] * kernel[k];
                    }
                }
            }
            
            return result;
        }
    }
    
    @Nested
    @DisplayName("CWT Convolution Tests")
    class CWTConvolutionTests {
        
        @Test
        @DisplayName("Should perform CWT convolution with proper scaling")
        void convolveCWT_shouldApplyProperScaling() {
            // Given
            double[] signal = {1.0, 0.0, -1.0, 0.0};
            double[] wavelet = {1.0, -1.0}; // Simple difference wavelet
            double scale = 2.0;
            
            // When
            double[] result = fft.convolveCWT(signal, wavelet, scale);
            
            // Then - result should be scaled by 1/sqrt(scale)
            assertEquals(5, result.length, "CWT result should have proper length");
            
            double expectedScale = 1.0 / Math.sqrt(scale);
            assertNotEquals(0.0, result[0], "Result should not be zero");
            
            // Verify scaling is applied (compare with unscaled convolution)
            double[] unscaledWavelet = {wavelet[0] * expectedScale, wavelet[1] * expectedScale};
            double[] expected = fft.convolveLinear(signal, unscaledWavelet);
            
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], result[i], 1e-12, 
                    "CWT result should match scaled linear convolution at index " + i);
            }
        }
        
        @Test
        @DisplayName("Should validate CWT parameters")
        void convolveCWT_shouldValidateParameters() {
            double[] validSignal = {1.0, 2.0};
            double[] validWavelet = {0.5};
            double validScale = 1.0;
            
            // Test null signal
            assertThrows(NullPointerException.class, 
                () -> fft.convolveCWT(null, validWavelet, validScale));
            
            // Test null wavelet
            assertThrows(NullPointerException.class, 
                () -> fft.convolveCWT(validSignal, null, validScale));
            
            // Test zero scale
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveCWT(validSignal, validWavelet, 0.0));
            
            // Test negative scale
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveCWT(validSignal, validWavelet, -1.0));
            
            // Test infinite scale
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveCWT(validSignal, validWavelet, Double.POSITIVE_INFINITY));
            
            // Test NaN scale
            assertThrows(IllegalArgumentException.class, 
                () -> fft.convolveCWT(validSignal, validWavelet, Double.NaN));
        }
        
        @Test
        @DisplayName("Should handle different scale values correctly")
        void convolveCWT_shouldHandleDifferentScales() {
            // Given
            double[] signal = {1.0, 1.0, 1.0, 1.0};
            double[] wavelet = {1.0};
            
            // When - different scales
            double[] result1 = fft.convolveCWT(signal, wavelet, 1.0);
            double[] result4 = fft.convolveCWT(signal, wavelet, 4.0);
            
            // Then - larger scale should produce smaller magnitude results
            assertEquals(result1.length, result4.length, "Results should have same length");
            
            // Scale factor should be 1/sqrt(4) = 0.5 relative to scale 1
            double expectedRatio = 1.0 / Math.sqrt(4.0);
            
            for (int i = 0; i < result1.length; i++) {
                assertEquals(result1[i] * expectedRatio, result4[i], 1e-12, 
                    "Scale 4 result should be scaled version of scale 1 result at index " + i);
            }
        }
    }
}