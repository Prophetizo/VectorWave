package ai.prophetizo.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for proper edge handling in CWT to verify no circular convolution artifacts.
 */
class CWTEdgeHandlingTest {
    
    private CWTTransform transform;
    private MorletWavelet wavelet;
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        wavelet = new MorletWavelet(6.0, 1.0);
        transform = new CWTTransform(wavelet);
    }
    
    @Test
    @DisplayName("Should handle signal edges without circular artifacts")
    void testEdgeHandlingNoCircularArtifacts() {
        // Create a signal with a sharp edge at the beginning
        int signalLength = 256;
        double[] signal = new double[signalLength];
        
        // Step function: zeros for first half, ones for second half
        for (int i = signalLength / 2; i < signalLength; i++) {
            signal[i] = 1.0;
        }
        
        // Analyze with FFT
        double[] scales = {4.0, 8.0, 16.0};
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult result = fftTransform.analyze(signal, scales);
        
        double[][] coefficients = result.getCoefficients();
        
        // Check edges - there should be no significant response at the very beginning
        // if circular convolution is avoided
        for (int s = 0; s < scales.length; s++) {
            double edgeValue = Math.abs(coefficients[s][0]);
            double midValue = Math.abs(coefficients[s][signalLength / 2]);
            
            // Edge should have much smaller response than the actual step location
            assertTrue(edgeValue < midValue * 0.1, 
                String.format("Scale %.1f: Edge value %.6f should be much smaller than mid value %.6f", 
                    scales[s], edgeValue, midValue));
        }
    }
    
    @Test
    @DisplayName("Should produce identical results for delta function regardless of position")
    void testDeltaFunctionPositionInvariance() {
        int signalLength = 128;
        double[] scales = {8.0};
        
        // Test delta at different positions
        int[] positions = {10, 64, 118};
        double[][][] results = new double[positions.length][][];
        
        for (int p = 0; p < positions.length; p++) {
            double[] signal = new double[signalLength];
            signal[positions[p]] = 1.0; // Delta function
            
            CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
                .enableFFT(true)
                .build());
            CWTResult result = fftTransform.analyze(signal, scales);
            
            results[p] = result.getCoefficients();
        }
        
        // Compare wavelet shapes at different positions (shifted appropriately)
        double[] coeffs0 = results[0][0];
        double[] coeffs1 = results[1][0];
        double[] coeffs2 = results[2][0];
        
        // Extract wavelet response around each delta position
        int responseWidth = 20;
        for (int i = -responseWidth; i <= responseWidth; i++) {
            int idx0 = positions[0] + i;
            int idx1 = positions[1] + i;
            int idx2 = positions[2] + i;
            
            if (idx0 >= 0 && idx0 < signalLength && 
                idx1 >= 0 && idx1 < signalLength && 
                idx2 >= 0 && idx2 < signalLength) {
                
                // All three should have similar values (position invariance)
                // Use relative tolerance for numerical stability
                double avgValue = (Math.abs(coeffs0[idx0]) + Math.abs(coeffs1[idx1]) + Math.abs(coeffs2[idx2])) / 3.0;
                double tolerance = Math.max(0.01, avgValue * 0.01); // 1% relative tolerance
                assertEquals(coeffs0[idx0], coeffs1[idx1], tolerance, 
                    "Delta response should be position invariant");
                assertEquals(coeffs1[idx1], coeffs2[idx2], tolerance, 
                    "Delta response should be position invariant");
            }
        }
    }
    
    @Test
    @DisplayName("Should handle zero-padded signal correctly")
    void testZeroPaddedSignal() {
        // Short signal with explicit zero padding
        double[] shortSignal = {1.0, 2.0, 3.0, 2.0, 1.0};
        int paddedLength = 64;
        double[] paddedSignal = new double[paddedLength];
        System.arraycopy(shortSignal, 0, paddedSignal, 0, shortSignal.length);
        
        double[] scales = {2.0, 4.0};
        
        // Analyze with FFT
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult result = fftTransform.analyze(paddedSignal, scales);
        
        double[][] coefficients = result.getCoefficients();
        
        // Check that coefficients decay to near zero in the padded region
        for (int s = 0; s < scales.length; s++) {
            // Check far from the signal (should be near zero)
            for (int i = paddedLength - 10; i < paddedLength; i++) {
                assertTrue(Math.abs(coefficients[s][i]) < 0.01,
                    String.format("Coefficients in padded region should be near zero at scale %.1f, position %d", 
                        scales[s], i));
            }
        }
    }
    
    @Test
    @DisplayName("Should match direct convolution for small signals")
    void testFFTMatchesDirectConvolution() {
        // Small signal where we can compare FFT vs direct
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        double[] scales = {4.0, 8.0};
        
        // Analyze with FFT
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult fftResult = fftTransform.analyze(signal, scales);
        
        // Analyze with direct convolution
        CWTTransform directTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(false)
            .build());
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        // Compare results
        for (int s = 0; s < scales.length; s++) {
            for (int t = 10; t < signal.length - 10; t++) { // Skip edges where boundary handling differs
                assertEquals(directCoeffs[s][t], fftCoeffs[s][t], 0.01,
                    String.format("FFT and direct convolution should match at scale %.1f, time %d", 
                        scales[s], t));
            }
        }
    }
    
    @Test
    @DisplayName("Should handle complex wavelets without circular artifacts")
    void testComplexWaveletEdgeHandling() {
        // Use a complex Morlet wavelet
        ComplexMorletWavelet complexWavelet = new ComplexMorletWavelet(6.0, 1.0);
        CWTTransform complexTransform = new CWTTransform(complexWavelet);
        
        // Signal with sharp transition
        int signalLength = 128;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength / 4; i++) {
            signal[i] = 1.0;
        }
        
        double[] scales = {8.0};
        CWTTransform fftComplexTransform = new CWTTransform(complexWavelet, 
            CWTConfig.builder().enableFFT(true).build());
        ComplexCWTResult result = fftComplexTransform.analyzeComplex(signal, scales);
        
        ComplexNumber[][] coefficients = result.getCoefficients();
        
        // Check the magnitude at the transition point vs the end
        // The signal is 1.0 for first quarter, then 0
        int transitionPoint = signalLength / 4;
        double transitionMagnitude = coefficients[0][transitionPoint].magnitude();
        double endMagnitude = coefficients[0][signalLength - 1].magnitude();
        
        System.out.printf("Complex edge test - Transition magnitude: %.6f, End magnitude: %.6f%n", 
            transitionMagnitude, endMagnitude);
        
        // The end should have much lower magnitude than the transition point
        // This verifies no circular wrapping from the beginning
        assertTrue(endMagnitude < transitionMagnitude * 0.1,
            String.format("End magnitude %.6f should be much smaller than transition %.6f", 
                endMagnitude, transitionMagnitude));
    }
}