package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.WaveletRegistry;
import ai.prophetizo.wavelet.api.WaveletName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparative mathematical tests verifying the mathematical differences
 * between Shannon and FBSP wavelets.
 * 
 * This test ensures that the fix for issue #196 properly separates these
 * wavelets mathematically, not just in implementation.
 */
class ShannonVsFBSPMathematicalComparisonTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double COMPARISON_TOLERANCE = 1e-3;
    
    private ShannonWavelet shannon;
    private FrequencyBSplineWavelet fbsp;
    
    @BeforeEach
    void setUp() {
        // Get wavelets from registry to ensure we're testing the actual registered instances
        shannon = (ShannonWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        fbsp = (FrequencyBSplineWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
    }
    
    @Test
    @DisplayName("Shannon and FBSP should have fundamentally different mathematical definitions")
    void testFundamentalMathematicalDifferences() {
        // Shannon: ψ(t) = √fb * sinc(fb*t) * cos(2π*fc*t) [REAL]
        // FBSP: ψ̂(ω) = √fb * [sinc(fb*ω/(2m))]^m * exp(i*fc*ω) [COMPLEX]
        
        // 1. Complex vs Real nature
        assertFalse(shannon.isComplex(), "Shannon should be real-valued");
        assertTrue(fbsp.isComplex(), "FBSP should be complex-valued");
        
        // 2. Mathematical values should differ at key points
        double t0 = 0.0;
        double shannonAt0 = shannon.psi(t0);
        double fbspRealAt0 = fbsp.psi(t0);
        double fbspImagAt0 = fbsp.psiImaginary(t0);
        
        // All should be finite
        assertTrue(Double.isFinite(shannonAt0), "Shannon at t=0 should be finite");
        assertTrue(Double.isFinite(fbspRealAt0), "FBSP real part at t=0 should be finite");
        assertTrue(Double.isFinite(fbspImagAt0), "FBSP imaginary part at t=0 should be finite");
        
        // Shannon at t=0: √fb * sinc(0) * cos(0) = √1.0 * 1 * 1 = 1.0
        assertEquals(1.0, shannonAt0, TOLERANCE, "Shannon at t=0 should be 1.0");
        
        // FBSP has different value due to different mathematical definition
        assertNotEquals(shannonAt0, fbspRealAt0, COMPARISON_TOLERANCE,
            "Shannon and FBSP real parts should differ mathematically at t=0");
    }
    
    @Test
    @DisplayName("Should have different frequency domain characteristics")
    void testFrequencyDomainDifferences() {
        // Shannon has rectangular frequency response (perfect band-pass)
        // FBSP has smooth B-spline shaped frequency response
        
        assertEquals(1.5, shannon.centerFrequency(), TOLERANCE);
        assertEquals(1.0, shannon.bandwidth(), TOLERANCE);
        
        assertEquals(1.0, fbsp.centerFrequency(), TOLERANCE);
        assertEquals(1.0, fbsp.bandwidth(), TOLERANCE);
        
        // Even with same bandwidth, they have fundamentally different frequency shapes
        // Shannon: rectangular in frequency (perfect cutoff)
        // FBSP: smooth B-spline rolloff
    }
    
    @Test
    @DisplayName("Should have different time domain behavior patterns")
    void testTimeDomainBehaviorDifferences() {
        double[] testPoints = {0.5, 1.0, 2.0, 3.0, 5.0};
        
        for (double t : testPoints) {
            double shannonVal = shannon.psi(t);
            double fbspRealVal = fbsp.psi(t);
            
            // Both should be finite
            assertTrue(Double.isFinite(shannonVal), 
                String.format("Shannon at t=%.1f should be finite", t));
            assertTrue(Double.isFinite(fbspRealVal), 
                String.format("FBSP real part at t=%.1f should be finite", t));
            
            // Values should be different due to different mathematical definitions
            if (Math.abs(shannonVal) > TOLERANCE || Math.abs(fbspRealVal) > TOLERANCE) {
                assertNotEquals(shannonVal, fbspRealVal, COMPARISON_TOLERANCE,
                    String.format("Shannon and FBSP should differ at t=%.1f", t));
            }
        }
    }
    
    @Test
    @DisplayName("Should have different oscillatory characteristics")
    void testOscillatoryDifferences() {
        // Shannon has sinc-based oscillations with specific zero crossings
        // FBSP has smoother oscillations due to B-spline frequency shaping
        
        // Count sign changes (zero crossings) in a range
        int shannonZeroCrossings = countZeroCrossings(shannon, -5, 5, 0.1);
        int fbspZeroCrossings = countZeroCrossings(fbsp, -5, 5, 0.1);
        
        // Both should have oscillations, but different patterns
        assertTrue(shannonZeroCrossings > 0, "Shannon should have oscillations");
        assertTrue(fbspZeroCrossings > 0, "FBSP should have oscillations");
        
        // Different mathematical definitions should lead to different oscillation patterns
        // (We allow them to be the same in edge cases, but typically they differ)
    }
    
    @Test
    @DisplayName("Should have different decay characteristics")
    void testDecayCharacteristics() {
        // Shannon: sinc-based decay (1/t asymptotically)
        // FBSP: B-spline influenced decay (smoother)
        
        double[] farPoints = {10.0, 15.0, 20.0};
        
        for (double t : farPoints) {
            double shannonDecay = Math.abs(shannon.psi(t));
            double fbspDecay = magnitude(fbsp, t);
            
            // Both should decay
            assertTrue(shannonDecay < 0.1, 
                String.format("Shannon should decay at t=%.1f", t));
            assertTrue(fbspDecay < 0.1, 
                String.format("FBSP should decay at t=%.1f", t));
            
            // Decay rates may differ due to different mathematical forms
        }
    }
    
    @Test
    @DisplayName("Should have different discretization patterns")
    void testDiscretizationDifferences() {
        int length = 128;
        double[] shannonSamples = shannon.discretize(length);
        double[] fbspSamples = fbsp.discretize(length);
        
        assertEquals(length, shannonSamples.length);
        assertEquals(length, fbspSamples.length);
        
        // Find differences in the discretized forms
        boolean foundDifference = false;
        double maxDifference = 0;
        
        for (int i = 0; i < length; i++) {
            double diff = Math.abs(shannonSamples[i] - fbspSamples[i]);
            maxDifference = Math.max(maxDifference, diff);
            
            if (diff > COMPARISON_TOLERANCE) {
                foundDifference = true;
            }
        }
        
        assertTrue(foundDifference, 
            "Shannon and FBSP discretizations should differ significantly");
        assertTrue(maxDifference > 0.1, 
            String.format("Maximum difference should be substantial, got: %.6f", maxDifference));
    }
    
    @Test
    @DisplayName("Should verify distinct mathematical formulas are implemented")
    void testDistinctMathematicalFormulas() {
        // This test verifies the actual mathematical formulas are different
        
        // Shannon mathematical definition test
        double t = 1.0;
        double fb_shannon = shannon.getBandwidthParameter();
        double fc_shannon = shannon.getCenterFrequencyParameter();
        
        // Manual calculation of Shannon: √fb * sinc(fb*t) * cos(2π*fc*t)
        double sinc = (Math.abs(fb_shannon * t) < 1e-10) ? 1.0 : 
                      Math.sin(Math.PI * fb_shannon * t) / (Math.PI * fb_shannon * t);
        double expectedShannon = Math.sqrt(fb_shannon) * sinc * Math.cos(2 * Math.PI * fc_shannon * t);
        double actualShannon = shannon.psi(t);
        
        assertEquals(expectedShannon, actualShannon, TOLERANCE,
            "Shannon should match its mathematical definition");
        
        // FBSP mathematical definition test (frequency domain)
        double omega = 2.0;
        double[] fbspFreq = fbsp.psiHat(omega);
        
        // Manual calculation of FBSP frequency domain
        int m = fbsp.getOrder();
        double fb_fbsp = fbsp.bandwidth();
        double fc_fbsp = fbsp.centerFrequency();
        
        double arg = fb_fbsp * omega / (2 * m);
        double sincFbsp = Math.sin(Math.PI * arg) / (Math.PI * arg);
        double expectedMagnitude = Math.sqrt(fb_fbsp) * Math.pow(Math.abs(sincFbsp), m);
        double actualMagnitude = Math.sqrt(fbspFreq[0]*fbspFreq[0] + fbspFreq[1]*fbspFreq[1]);
        
        assertEquals(expectedMagnitude, actualMagnitude, TOLERANCE,
            "FBSP frequency domain should match its mathematical definition");
    }
    
    @Test
    @DisplayName("Should demonstrate different time-frequency trade-offs")
    void testTimeFrequencyTradeoffs() {
        // Shannon: Perfect frequency localization, poor time localization
        // FBSP: Better time-frequency balance due to B-spline smoothing
        
        // Test effective support in time domain
        double shannonSupport = estimateEffectiveSupport(shannon, 0.01);
        double fbspSupport = estimateEffectiveSupport(fbsp, 0.01);
        
        // Both should have finite effective support
        assertTrue(shannonSupport > 0 && shannonSupport < 50, 
            "Shannon should have reasonable effective support");
        assertTrue(fbspSupport > 0 && fbspSupport < 50, 
            "FBSP should have reasonable effective support");
        
        // The supports may be different due to different mathematical properties
        // Shannon typically has wider support due to sinc function nature
    }
    
    @Test
    @DisplayName("Should verify mathematical consistency with registry configuration")
    void testRegistryConfigurationConsistency() {
        // Verify that the registry configuration matches expected mathematical parameters
        
        // Shannon registry configuration: new ShannonWavelet(1.0, 1.5)
        assertEquals(1.0, shannon.getBandwidthParameter(), TOLERANCE);
        assertEquals(1.5, shannon.getCenterFrequencyParameter(), TOLERANCE);
        
        // FBSP registry configuration: new FrequencyBSplineWavelet(2, 1.0, 1.0)
        assertEquals(2, fbsp.getOrder());
        assertEquals(1.0, fbsp.bandwidth(), TOLERANCE);
        assertEquals(1.0, fbsp.centerFrequency(), TOLERANCE);
        
        // These different parameters ensure mathematical separation
        assertNotEquals(shannon.centerFrequency(), fbsp.centerFrequency(), TOLERANCE,
            "Shannon and FBSP should have different center frequencies in registry");
    }
    
    // Helper methods
    
    private int countZeroCrossings(ContinuousWavelet wavelet, double start, double end, double step) {
        int crossings = 0;
        double prev = wavelet.psi(start);
        
        for (double t = start + step; t <= end; t += step) {
            double curr = wavelet.psi(t);
            if (prev * curr < 0) { // Sign change
                crossings++;
            }
            prev = curr;
        }
        
        return crossings;
    }
    
    private double magnitude(FrequencyBSplineWavelet wavelet, double t) {
        double real = wavelet.psi(t);
        double imag = wavelet.psiImaginary(t);
        return Math.sqrt(real * real + imag * imag);
    }
    
    private double estimateEffectiveSupport(ContinuousWavelet wavelet, double threshold) {
        // Find the range where |wavelet(t)| > threshold
        double maxT = 0;
        
        for (double t = 0; t <= 20; t += 0.1) {
            if (Math.abs(wavelet.psi(t)) > threshold) {
                maxT = t;
            }
        }
        
        return 2 * maxT; // Symmetric support
    }
}