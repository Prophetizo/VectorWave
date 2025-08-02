package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import ai.prophetizo.wavelet.test.TestConstants;
class SoATransformTest {
    
    private static final double EPSILON = 1e-10;
    private double[] haarLowPass;
    private double[] haarHighPass;
    private double[] db4LowPass;
    private double[] db4HighPass;
    
    @BeforeEach
    void setUp() {
        Haar haar = new Haar();
        haarLowPass = haar.lowPassDecomposition();
        haarHighPass = haar.highPassDecomposition();
        
        Daubechies db4 = Daubechies.DB4;
        db4LowPass = db4.lowPassDecomposition();
        db4HighPass = db4.highPassDecomposition();
    }
    
    @Test
    void testAoSToSoAConversion() {
        int numSignals = 4;
        int signalLength = 8;
        
        // Create test signals with distinct patterns
        double[][] signals = new double[numSignals][signalLength];
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = i * 10 + j; // Distinct values
            }
        }
        
        double[] soa = SoATransform.convertAoSToSoA(signals);
        
        // Verify SoA layout
        assertEquals(numSignals * signalLength, soa.length);
        
        // Check that all sample 0s are together, then all sample 1s, etc
        for (int sample = 0; sample < signalLength; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                assertEquals(signals[signal][sample], soa[sample * numSignals + signal], EPSILON);
            }
        }
    }
    
    @Test
    void testSoAToAoSConversion() {
        int numSignals = 3;
        int signalLength = 6;
        
        // Create SoA data
        double[] soa = new double[numSignals * signalLength];
        for (int i = 0; i < soa.length; i++) {
            soa[i] = i + 1.0;
        }
        
        // Convert to AoS
        double[][] signals = new double[numSignals][signalLength];
        SoATransform.convertSoAToAoS(soa, signals);
        
        // Verify conversion
        for (int sample = 0; sample < signalLength; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                assertEquals(soa[sample * numSignals + signal], signals[signal][sample], EPSILON);
            }
        }
    }
    
    @Test
    void testRoundTripConversion() {
        double[][] original = createTestSignals(8, 64);
        
        // Convert AoS -> SoA -> AoS
        double[] soa = SoATransform.convertAoSToSoA(original);
        double[][] reconstructed = new double[original.length][original[0].length];
        SoATransform.convertSoAToAoS(soa, reconstructed);
        
        // Verify perfect reconstruction
        for (int i = 0; i < original.length; i++) {
            assertArrayEquals(original[i], reconstructed[i], EPSILON);
        }
    }
    
    @Test
    void testHaarTransformSoA() {
        int numSignals = 8;
        int signalLength = 64;
        
        // Create test signals
        double[][] signals = createTestSignals(numSignals, signalLength);
        
        // Convert to SoA
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        // Transform in SoA layout
        SoATransform.haarTransformSoA(soaInput, soaApprox, soaDetail, numSignals, signalLength);
        
        // Convert results back to AoS for verification
        double[][] approxResults = new double[numSignals][signalLength / 2];
        double[][] detailResults = new double[numSignals][signalLength / 2];
        
        for (int sample = 0; sample < signalLength / 2; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                approxResults[signal][sample] = soaApprox[sample * numSignals + signal];
                detailResults[signal][sample] = soaDetail[sample * numSignals + signal];
            }
        }
        
        // Verify against standard transform
        WaveletTransform transform = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        for (int i = 0; i < numSignals; i++) {
            TransformResult expected = transform.forward(signals[i]);
            assertArrayEquals(expected.approximationCoeffs(), approxResults[i], EPSILON);
            assertArrayEquals(expected.detailCoeffs(), detailResults[i], EPSILON);
        }
    }
    
    @Test
    void testGeneralTransformSoA() {
        int numSignals = 4;
        int signalLength = 32;
        
        double[][] signals = createTestSignals(numSignals, signalLength);
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        // Transform with DB4
        SoATransform.transformSoA(soaInput, soaApprox, soaDetail,
            db4LowPass, db4HighPass, numSignals, signalLength, db4LowPass.length);
        
        // Convert back and verify
        double[][] approxResults = new double[numSignals][signalLength / 2];
        double[][] detailResults = new double[numSignals][signalLength / 2];
        
        for (int sample = 0; sample < signalLength / 2; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                approxResults[signal][sample] = soaApprox[sample * numSignals + signal];
                detailResults[signal][sample] = soaDetail[sample * numSignals + signal];
            }
        }
        
        // Verify energy preservation for each signal
        for (int i = 0; i < numSignals; i++) {
            double inputEnergy = 0.0;
            double outputEnergy = 0.0;
            
            for (double val : signals[i]) {
                inputEnergy += val * val;
            }
            
            for (double val : approxResults[i]) {
                outputEnergy += val * val;
            }
            for (double val : detailResults[i]) {
                outputEnergy += val * val;
            }
            
            assertEquals(inputEnergy, outputEnergy, inputEnergy * 0.001);
        }
    }
    
    @Test
    void testInverseTransformSoA() {
        int numSignals = 4;
        int signalLength = 64;
        
        // Create and transform signals
        double[][] signals = createTestSignals(numSignals, signalLength);
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        SoATransform.haarTransformSoA(soaInput, soaApprox, soaDetail, numSignals, signalLength);
        
        // Inverse transform
        double[] soaOutput = new double[numSignals * signalLength];
        double[] lowPassRecon = new Haar().lowPassReconstruction();
        double[] highPassRecon = new Haar().highPassReconstruction();
        
        SoATransform.inverseTransformSoA(soaApprox, soaDetail, soaOutput,
            lowPassRecon, highPassRecon, numSignals, signalLength, lowPassRecon.length);
        
        // Convert back and verify reconstruction
        double[][] reconstructed = new double[numSignals][signalLength];
        SoATransform.convertSoAToAoS(soaOutput, reconstructed);
        
        for (int i = 0; i < numSignals; i++) {
            assertArrayEquals(signals[i], reconstructed[i], 1e-8);
        }
    }
    
    @Test
    void testComplexTransformSoA() {
        int numSignals = 4;
        int signalLength = 32;
        
        // Create complex signals
        double[] realInput = new double[numSignals * signalLength];
        double[] imagInput = new double[numSignals * signalLength];
        
        Random random = new Random(TestConstants.TEST_SEED);
        for (int i = 0; i < realInput.length; i++) {
            realInput[i] = random.nextGaussian();
            imagInput[i] = random.nextGaussian();
        }
        
        // Output arrays
        double[] realApprox = new double[numSignals * signalLength / 2];
        double[] imagApprox = new double[numSignals * signalLength / 2];
        double[] realDetail = new double[numSignals * signalLength / 2];
        double[] imagDetail = new double[numSignals * signalLength / 2];
        
        // Simple complex filter (real-valued for simplicity)
        double[] realFilter = haarLowPass;
        double[] imagFilter = new double[haarLowPass.length]; // Zero imaginary part
        
        SoATransform.transformComplexSoA(realInput, imagInput,
            realApprox, imagApprox, realDetail, imagDetail,
            realFilter, imagFilter, numSignals, signalLength, realFilter.length);
        
        // Verify that we got non-trivial results
        assertTrue(hasSignificantValues(realApprox));
        assertTrue(hasSignificantValues(realDetail));
        
        // The imaginary parts should also have values since we have imaginary input
        // even though the filter is real-only
        assertTrue(hasSignificantValues(imagApprox));
        assertTrue(hasSignificantValues(imagDetail));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    void testVariousSignalCounts(int numSignals) {
        int signalLength = 32;
        
        double[][] signals = createTestSignals(numSignals, signalLength);
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        // Should handle various signal counts, including non-multiples of vector length
        assertDoesNotThrow(() -> 
            SoATransform.haarTransformSoA(soaInput, soaApprox, soaDetail, numSignals, signalLength)
        );
        
        // Verify results are non-zero
        assertTrue(hasSignificantValues(soaApprox));
        assertTrue(hasSignificantValues(soaDetail));
    }
    
    @Test
    void testPartialVectorHandling() {
        // Test with 3 signals (not a power of 2, likely partial vector)
        int numSignals = 3;
        int signalLength = 16;
        
        double[][] signals = createTestSignals(numSignals, signalLength);
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        SoATransform.haarTransformSoA(soaInput, soaApprox, soaDetail, numSignals, signalLength);
        
        // Convert back and verify each signal
        double[][] approxResults = new double[numSignals][signalLength / 2];
        double[][] detailResults = new double[numSignals][signalLength / 2];
        
        for (int sample = 0; sample < signalLength / 2; sample++) {
            for (int signal = 0; signal < numSignals; signal++) {
                approxResults[signal][sample] = soaApprox[sample * numSignals + signal];
                detailResults[signal][sample] = soaDetail[sample * numSignals + signal];
            }
        }
        
        // All signals should be properly transformed
        for (int i = 0; i < numSignals; i++) {
            assertTrue(hasSignificantValues(approxResults[i]));
            assertTrue(hasSignificantValues(detailResults[i]));
        }
    }
    
    @Test
    void testGetSoAInfo() {
        String info = SoATransform.getSoAInfo();
        
        assertNotNull(info);
        assertTrue(info.contains("Structure-of-Arrays Configuration"));
        assertTrue(info.contains("Vector Length:"));
        assertTrue(info.contains("Optimal batch size:"));
        assertTrue(info.contains("Memory layout:"));
    }
    
    @Test
    void testLargeSignalBatch() {
        // Test with larger batch to verify memory efficiency
        int numSignals = 32;
        int signalLength = 128;
        
        double[][] signals = createTestSignals(numSignals, signalLength);
        double[] soaInput = SoATransform.convertAoSToSoA(signals);
        double[] soaApprox = new double[numSignals * signalLength / 2];
        double[] soaDetail = new double[numSignals * signalLength / 2];
        
        // Transform with general method
        SoATransform.transformSoA(soaInput, soaApprox, soaDetail,
            db4LowPass, db4HighPass, numSignals, signalLength, db4LowPass.length);
        
        // Spot check a few signals
        WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        for (int sigIdx : new int[]{0, numSignals/2, numSignals-1}) {
            double[] approxResult = new double[signalLength / 2];
            double[] detailResult = new double[signalLength / 2];
            
            for (int sample = 0; sample < signalLength / 2; sample++) {
                approxResult[sample] = soaApprox[sample * numSignals + sigIdx];
                detailResult[sample] = soaDetail[sample * numSignals + sigIdx];
            }
            
            TransformResult expected = transform.forward(signals[sigIdx]);
            assertArrayEquals(expected.approximationCoeffs(), approxResult, EPSILON);
            assertArrayEquals(expected.detailCoeffs(), detailResult, EPSILON);
        }
    }
    
    // Helper methods
    
    private double[][] createTestSignals(int numSignals, int signalLength) {
        Random random = new Random(12345);
        double[][] signals = new double[numSignals][signalLength];
        
        for (int i = 0; i < numSignals; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 16.0 + i * Math.PI / 4) + 
                                0.1 * random.nextGaussian();
            }
        }
        
        return signals;
    }
    
    private boolean hasSignificantValues(double[] array) {
        double sum = 0.0;
        for (double val : array) {
            sum += Math.abs(val);
        }
        return sum > EPSILON * array.length;
    }
}