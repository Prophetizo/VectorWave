package ai.prophetizo.wavelet.cwt.util;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;

/**
 * Utility class demonstrating the use of the unified wavelet evaluation helper.
 * This shows how other parts of the codebase can leverage the helper method
 * to avoid duplicating instanceof checks for complex wavelets.
 * 
 * @since 1.0.0
 */
public class WaveletEvaluationHelper {
    
    /**
     * Computes the magnitude of a wavelet at multiple points.
     * Works with both real and complex wavelets.
     * 
     * @param wavelet the wavelet to evaluate
     * @param points array of points to evaluate at
     * @return array of magnitudes
     */
    public static double[] computeMagnitudes(ContinuousWavelet wavelet, double[] points) {
        double[] magnitudes = new double[points.length];
        
        for (int i = 0; i < points.length; i++) {
            // Use the helper method instead of instanceof checks
            FFTAcceleratedCWT.Complex value = FFTAcceleratedCWT.evaluateAsComplex(wavelet, points[i]);
            magnitudes[i] = value.magnitude();
        }
        
        return magnitudes;
    }
    
    /**
     * Computes the phase of a wavelet at multiple points.
     * Returns 0 for real wavelets, actual phase for complex wavelets.
     * 
     * @param wavelet the wavelet to evaluate
     * @param points array of points to evaluate at
     * @return array of phases
     */
    public static double[] computePhases(ContinuousWavelet wavelet, double[] points) {
        double[] phases = new double[points.length];
        
        for (int i = 0; i < points.length; i++) {
            // Use the helper method for unified handling
            FFTAcceleratedCWT.Complex value = FFTAcceleratedCWT.evaluateAsComplex(wavelet, points[i]);
            phases[i] = value.phase();
        }
        
        return phases;
    }
    
    /**
     * Example of checking if a wavelet is complex without instanceof.
     * 
     * @param wavelet the wavelet to check
     * @param t point to evaluate at
     * @return true if the wavelet has non-zero imaginary part at t
     */
    public static boolean hasImaginaryComponent(ContinuousWavelet wavelet, double t) {
        FFTAcceleratedCWT.Complex value = FFTAcceleratedCWT.evaluateAsComplex(wavelet, t);
        return Math.abs(value.imag) > 1e-10;
    }
}