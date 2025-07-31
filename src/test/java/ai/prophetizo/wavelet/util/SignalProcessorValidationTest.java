package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SignalProcessor input validation consistency.
 */
@DisplayName("SignalProcessor Validation Tests")
class SignalProcessorValidationTest {
    
    @Test
    @DisplayName("FFT should throw exception for null input")
    void testFFTNullInput() {
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fft(null),
            "FFT should throw exception for null input");
    }
    
    @Test
    @DisplayName("FFT should throw exception for empty input")
    void testFFTEmptyInput() {
        ComplexNumber[] empty = new ComplexNumber[0];
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fft(empty),
            "FFT should throw exception for empty input");
    }
    
    @Test
    @DisplayName("IFFT should throw exception for null input")
    void testIFFTNullInput() {
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.ifft(null),
            "IFFT should throw exception for null input");
    }
    
    @Test
    @DisplayName("IFFT should throw exception for empty input")
    void testIFFTEmptyInput() {
        ComplexNumber[] empty = new ComplexNumber[0];
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.ifft(empty),
            "IFFT should throw exception for empty input");
    }
    
    @Test
    @DisplayName("FFTReal should throw exception for null input")
    void testFFTRealNullInput() {
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fftReal(null),
            "FFTReal should throw exception for null input");
    }
    
    @Test
    @DisplayName("FFTReal should throw exception for empty input")
    void testFFTRealEmptyInput() {
        double[] empty = new double[0];
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fftReal(empty),
            "FFTReal should throw exception for empty input");
    }
    
    @Test
    @DisplayName("ConvolveFFT should throw exception for null inputs")
    void testConvolveFFTNullInputs() {
        double[] valid = {1.0, 2.0};
        
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(null, valid),
            "ConvolveFFT should throw exception for null first input");
        
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(valid, null),
            "ConvolveFFT should throw exception for null second input");
        
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(null, null),
            "ConvolveFFT should throw exception for both null inputs");
    }
    
    @Test
    @DisplayName("ConvolveFFT should throw exception for empty inputs")
    void testConvolveFFTEmptyInputs() {
        double[] valid = {1.0, 2.0};
        double[] empty = new double[0];
        
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(empty, valid),
            "ConvolveFFT should throw exception for empty first input");
        
        assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(valid, empty),
            "ConvolveFFT should throw exception for empty second input");
    }
    
    @Test
    @DisplayName("ApplyWindow should throw exception for null signal")
    void testApplyWindowNullSignal() {
        assertThrows(IllegalArgumentException.class, 
            () -> SignalProcessor.applyWindow(null, SignalProcessor.WindowType.HANN),
            "ApplyWindow should throw exception for null signal");
    }
    
    @Test
    @DisplayName("ApplyWindow should throw exception for empty signal")
    void testApplyWindowEmptySignal() {
        double[] empty = new double[0];
        assertThrows(IllegalArgumentException.class, 
            () -> SignalProcessor.applyWindow(empty, SignalProcessor.WindowType.HANN),
            "ApplyWindow should throw exception for empty signal");
    }
    
    @Test
    @DisplayName("ApplyWindow should throw exception for null window type")
    void testApplyWindowNullWindowType() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        assertThrows(IllegalArgumentException.class, 
            () -> SignalProcessor.applyWindow(signal, null),
            "ApplyWindow should throw exception for null window type");
    }
    
    @Test
    @DisplayName("Valid inputs should not throw exceptions")
    void testValidInputs() {
        // FFT with valid power-of-2 input
        ComplexNumber[] validComplex = new ComplexNumber[4];
        for (int i = 0; i < 4; i++) {
            validComplex[i] = new ComplexNumber(i, 0);
        }
        assertDoesNotThrow(() -> SignalProcessor.fft(validComplex));
        
        // FFTReal with valid input
        double[] validReal = {1.0, 2.0, 3.0, 4.0};
        assertDoesNotThrow(() -> SignalProcessor.fftReal(validReal));
        
        // ConvolveFFT with valid inputs
        assertDoesNotThrow(() -> SignalProcessor.convolveFFT(validReal, validReal));
        
        // ApplyWindow with valid inputs
        assertDoesNotThrow(() -> SignalProcessor.applyWindow(validReal, SignalProcessor.WindowType.HANN));
    }
    
    @Test
    @DisplayName("Exception messages should be consistent")
    void testExceptionMessageConsistency() {
        // Test null inputs
        Exception fftEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fft(null));
        assertTrue(fftEx.getMessage().contains("FFT") && fftEx.getMessage().contains("cannot be null"));
        
        Exception ifftEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.ifft(null));
        assertTrue(ifftEx.getMessage().contains("IFFT") && ifftEx.getMessage().contains("cannot be null"));
        
        Exception convEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.convolveFFT(null, null));
        assertTrue(convEx.getMessage().contains("Convolution") && convEx.getMessage().contains("cannot be null"));
        
        Exception winEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.applyWindow(null, SignalProcessor.WindowType.HANN));
        assertTrue(winEx.getMessage().contains("Window") && winEx.getMessage().contains("cannot be null"));
        
        // Test empty inputs
        ComplexNumber[] empty = new ComplexNumber[0];
        Exception fftEmptyEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fft(empty));
        assertTrue(fftEmptyEx.getMessage().contains("FFT") && fftEmptyEx.getMessage().contains("cannot be empty"));
        
        double[] emptyReal = new double[0];
        Exception realEmptyEx = assertThrows(IllegalArgumentException.class, () -> SignalProcessor.fftReal(emptyReal));
        assertTrue(realEmptyEx.getMessage().contains("FFT") && realEmptyEx.getMessage().contains("cannot be empty"));
    }
}