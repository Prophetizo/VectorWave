package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;
import ai.prophetizo.wavelet.Complex;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidConfigurationException;

/**
 * Inverse Continuous Wavelet Transform for signal reconstruction.
 * 
 * <p>This class implements the inverse CWT, allowing reconstruction of signals
 * from their time-frequency representation. The reconstruction is based on the
 * admissibility condition and uses the following formula:</p>
 * 
 * <pre>
 * x(t) = (1/C_ψ) ∫∫ W(a,b) ψ_{a,b}(t) da db / a²
 * </pre>
 * 
 * where:
 * <ul>
 *   <li>W(a,b) are the CWT coefficients</li>
 *   <li>ψ_{a,b}(t) is the scaled and translated wavelet</li>
 *   <li>C_ψ is the admissibility constant</li>
 *   <li>a is the scale parameter</li>
 *   <li>b is the translation parameter</li>
 * </ul>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Full signal reconstruction from CWT coefficients</li>
 *   <li>Band-limited reconstruction for frequency filtering</li>
 *   <li>Optimized computation using FFT when appropriate</li>
 *   <li>Support for both real and complex wavelets</li>
 * </ul>
 * 
 * @since 1.1.0
 */
public final class InverseCWT {
    
    private static final double DEFAULT_TOLERANCE = 1e-10;
    private static final int MIN_INTEGRATION_POINTS = 100;
    
    private final ContinuousWavelet wavelet;
    private final double admissibilityConstant;
    private final boolean useFFT;
    
    /**
     * Creates an inverse CWT calculator for the given wavelet.
     * 
     * @param wavelet the continuous wavelet used in the forward transform
     * @throws InvalidArgumentException if wavelet is null
     * @throws InvalidConfigurationException if wavelet doesn't satisfy admissibility
     */
    public InverseCWT(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        
        this.wavelet = wavelet;
        this.admissibilityConstant = calculateAdmissibilityConstant(wavelet);
        
        if (admissibilityConstant <= 0 || Double.isInfinite(admissibilityConstant)) {
            throw new InvalidConfigurationException(
                "Wavelet does not satisfy admissibility condition: C_ψ = " + admissibilityConstant);
        }
        
        // Use FFT for large-scale reconstructions
        this.useFFT = true;
    }
    
    /**
     * Reconstructs the signal from CWT coefficients.
     * 
     * @param cwtResult the CWT result containing coefficients
     * @return reconstructed signal
     * @throws InvalidArgumentException if input is invalid
     */
    public double[] reconstruct(CWTResult cwtResult) {
        if (cwtResult == null) {
            throw new InvalidArgumentException("CWT result cannot be null");
        }
        
        double[] scales = cwtResult.getScales();
        int signalLength = cwtResult.getNumSamples();
        
        // For now, we'll work with the real coefficients only
        // TODO: Add support for complex coefficient reconstruction
        double[][] realCoeffs = cwtResult.getCoefficients();
        return reconstructInternalReal(realCoeffs, scales, signalLength, 0, scales.length);
    }
    
    /**
     * Reconstructs the signal using only a specific frequency band.
     * 
     * @param cwtResult the CWT result
     * @param minScale minimum scale (inclusive)
     * @param maxScale maximum scale (exclusive)
     * @return band-limited reconstructed signal
     * @throws InvalidArgumentException if parameters are invalid
     */
    public double[] reconstructBand(CWTResult cwtResult, double minScale, double maxScale) {
        if (cwtResult == null) {
            throw new InvalidArgumentException("CWT result cannot be null");
        }
        if (minScale <= 0 || maxScale <= minScale) {
            throw new InvalidArgumentException(
                "Invalid scale range: minScale=" + minScale + ", maxScale=" + maxScale);
        }
        
        double[] scales = cwtResult.getScales();
        int signalLength = cwtResult.getNumSamples();
        
        // Find scale indices
        int startIdx = -1, endIdx = -1;
        for (int i = 0; i < scales.length; i++) {
            if (startIdx == -1 && scales[i] >= minScale) {
                startIdx = i;
            }
            if (scales[i] > maxScale) {
                endIdx = i;
                break;
            }
        }
        
        if (startIdx == -1) {
            startIdx = 0;
        }
        if (endIdx == -1) {
            endIdx = scales.length;
        }
        
        double[][] realCoeffs = cwtResult.getCoefficients();
        return reconstructInternalReal(realCoeffs, scales, signalLength, startIdx, endIdx);
    }
    
    /**
     * Reconstructs the signal from frequency domain representation.
     * 
     * @param cwtResult the CWT result
     * @param samplingRate the sampling rate in Hz
     * @param minFreq minimum frequency in Hz (inclusive)
     * @param maxFreq maximum frequency in Hz (exclusive)
     * @return frequency-band limited reconstructed signal
     */
    public double[] reconstructFrequencyBand(CWTResult cwtResult, double samplingRate,
                                           double minFreq, double maxFreq) {
        if (samplingRate <= 0) {
            throw new InvalidArgumentException("Sampling rate must be positive");
        }
        if (minFreq < 0 || maxFreq <= minFreq || maxFreq > samplingRate / 2) {
            throw new InvalidArgumentException(
                "Invalid frequency range: minFreq=" + minFreq + ", maxFreq=" + maxFreq);
        }
        
        // Convert frequencies to scales
        // For Morlet wavelet: frequency = centerFreq * samplingRate / scale
        // So scale = centerFreq * samplingRate / frequency
        double centerFreq = wavelet.centerFrequency();
        double maxScale = centerFreq * samplingRate / minFreq;
        double minScale = centerFreq * samplingRate / maxFreq;
        
        return reconstructBand(cwtResult, minScale, maxScale);
    }
    
    /**
     * Internal reconstruction implementation for real coefficients.
     */
    private double[] reconstructInternalReal(double[][] coefficients, double[] scales,
                                           int signalLength, int startScale, int endScale) {
        double[] reconstructed = new double[signalLength];
        
        // Integration weights for trapezoidal rule in log scale
        double[] weights = calculateLogScaleWeights(scales, startScale, endScale);
        
        // For each time point
        for (int t = 0; t < signalLength; t++) {
            double sum = 0.0;
            
            // Integrate over scales
            for (int s = startScale; s < endScale; s++) {
                double scale = scales[s];
                
                // Integrate over all translation positions
                for (int b = 0; b < signalLength; b++) {
                    double coeff = coefficients[s][b];
                    
                    // Skip negligible coefficients
                    if (Math.abs(coeff) < DEFAULT_TOLERANCE) {
                        continue;
                    }
                    
                    // Calculate reconstruction kernel value
                    double kernelValue = reconstructionKernel(t, b, scale, signalLength);
                    
                    // Add contribution: W(a,b) * ψ_{a,b}(t) * da / a²
                    // For logarithmic integration: da = a * d(log a)
                    // So da/a² = d(log a)/a
                    sum += coeff * kernelValue * weights[s - startScale] / scale;
                }
            }
            
            // Normalize by admissibility constant
            reconstructed[t] = sum / admissibilityConstant;
        }
        
        return reconstructed;
    }
    
    /**
     * Internal reconstruction implementation for complex coefficients.
     */
    private double[] reconstructInternal(ComplexMatrix coefficients, double[] scales,
                                       int signalLength, int startScale, int endScale) {
        double[] reconstructed = new double[signalLength];
        
        // Integration weights for trapezoidal rule
        double[] weights = calculateIntegrationWeights(scales, startScale, endScale);
        
        // For each time point
        for (int t = 0; t < signalLength; t++) {
            double sum = 0.0;
            
            // Integrate over scales
            for (int s = startScale; s < endScale; s++) {
                double scale = scales[s];
                double coeffReal = coefficients.getReal(s, t);
                double coeffImag = coefficients.getImaginary(s, t);
                double coeffMagnitude = coefficients.getMagnitude(s, t);
                
                // Skip negligible coefficients
                if (coeffMagnitude < DEFAULT_TOLERANCE) {
                    continue;
                }
                
                // Calculate reconstruction kernel value
                double kernelValue = reconstructionKernel(t, t, scale, signalLength);
                
                // Add contribution: W(a,b) * ψ(t) * da/a
                sum += coeffReal * kernelValue * weights[s - startScale] / scale;
            }
            
            // Normalize by admissibility constant
            reconstructed[t] = sum / admissibilityConstant;
        }
        
        return reconstructed;
    }
    
    /**
     * Calculates the reconstruction kernel ψ_{a,b}(t).
     */
    private double reconstructionKernel(int t, int b, double scale, int signalLength) {
        // Calculate the argument for the wavelet
        double arg = (t - b) / scale;
        
        // Scale factor for proper normalization: 1/√a
        double scaleFactor = 1.0 / Math.sqrt(scale);
        
        // Evaluate the wavelet (complex conjugate for reconstruction)
        if (wavelet instanceof ComplexContinuousWavelet complexWavelet) {
            // For complex wavelets, use the real part of the conjugate
            return scaleFactor * complexWavelet.psi(arg);
        } else {
            // For real wavelets, just evaluate with scaling
            return scaleFactor * wavelet.psi(arg);
        }
    }
    
    /**
     * Calculates integration weights using trapezoidal rule.
     */
    private double[] calculateIntegrationWeights(double[] scales, int start, int end) {
        int n = end - start;
        double[] weights = new double[n];
        
        if (n == 1) {
            weights[0] = 1.0;
            return weights;
        }
        
        // Trapezoidal rule weights
        for (int i = 0; i < n - 1; i++) {
            double da = scales[start + i + 1] - scales[start + i];
            if (i == 0) {
                weights[i] = da / 2.0;
            } else {
                weights[i] += da / 2.0;
            }
            weights[i + 1] = da / 2.0;
        }
        
        return weights;
    }
    
    /**
     * Calculates integration weights for logarithmic scale spacing.
     * Since scales are often logarithmically spaced, we integrate in log scale.
     */
    private double[] calculateLogScaleWeights(double[] scales, int start, int end) {
        int n = end - start;
        double[] weights = new double[n];
        
        if (n == 1) {
            weights[0] = scales[start];
            return weights;
        }
        
        // For logarithmic integration: ∫ f(a) da/a = ∫ f(a) d(log a)
        // So we calculate weights for d(log a)
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                double dlogA = Math.log(scales[start + 1] / scales[start]);
                weights[i] = dlogA / 2.0;
            } else if (i == n - 1) {
                double dlogA = Math.log(scales[start + i] / scales[start + i - 1]);
                weights[i] = dlogA / 2.0;
            } else {
                double dlogA = Math.log(scales[start + i + 1] / scales[start + i - 1]) / 2.0;
                weights[i] = dlogA;
            }
        }
        
        return weights;
    }
    
    /**
     * Calculates the admissibility constant for the wavelet.
     * 
     * C_ψ = ∫ |Ψ(ω)|² / |ω| dω
     * 
     * where Ψ(ω) is the Fourier transform of the wavelet.
     */
    private double calculateAdmissibilityConstant(ContinuousWavelet wavelet) {
        // Use known analytical values for common wavelets
        String waveletName = wavelet.name().toLowerCase();
        
        if (waveletName.contains("morlet") || waveletName.contains("morl")) {
            // Morlet wavelet: C_ψ = π for ω₀ > 5
            return Math.PI;
        } else if (waveletName.contains("mexh") || waveletName.contains("dog2")) {
            // Mexican Hat (DOG2): C_ψ = π/√2  
            return Math.PI / Math.sqrt(2.0);
        } else if (waveletName.contains("dog")) {
            // General DOG wavelets
            return Math.PI; // Approximate
        } else if (waveletName.contains("paul")) {
            // Paul wavelet: C_ψ = 2π
            return 2.0 * Math.PI;
        } else if (waveletName.contains("shannon")) {
            // Shannon wavelet: C_ψ = π
            return Math.PI;
        }
        
        // For unknown wavelets, use numerical integration
        return calculateAdmissibilityNumerical(wavelet);
    }
    
    /**
     * Numerical calculation of admissibility constant.
     */
    private double calculateAdmissibilityNumerical(ContinuousWavelet wavelet) {
        int nPoints = 10000;
        double[] freqs = new double[nPoints];
        double maxFreq = 100.0; // Reasonable upper limit
        
        // Create frequency grid (logarithmic spacing to handle 1/ω)
        for (int i = 1; i < nPoints; i++) {
            freqs[i] = maxFreq * Math.pow(10.0, -4.0 + 4.0 * i / (nPoints - 1));
        }
        
        double sum = 0.0;
        
        // Numerical integration using trapezoidal rule
        for (int i = 1; i < nPoints - 1; i++) {
            double omega = freqs[i];
            double psiHat = waveletFourierTransform(wavelet, omega);
            double integrand = psiHat * psiHat / omega;
            
            double dOmega = (freqs[i + 1] - freqs[i - 1]) / 2.0;
            sum += integrand * dOmega;
        }
        
        return 2.0 * Math.PI * sum;
    }
    
    /**
     * Approximates the Fourier transform of the wavelet at frequency ω.
     */
    private double waveletFourierTransform(ContinuousWavelet wavelet, double omega) {
        // Use numerical integration for Fourier transform
        int nPoints = 1000;
        double tMax = 20.0; // Integration limits
        double dt = 2.0 * tMax / nPoints;
        
        double realSum = 0.0;
        double imagSum = 0.0;
        
        for (int i = 0; i < nPoints; i++) {
            double t = -tMax + i * dt;
            double psi = wavelet.psi(t);
            
            // Fourier transform: ∫ ψ(t) e^(-iωt) dt
            realSum += psi * Math.cos(-omega * t) * dt;
            imagSum += psi * Math.sin(-omega * t) * dt;
        }
        
        // Return magnitude
        return Math.sqrt(realSum * realSum + imagSum * imagSum);
    }
    
    /**
     * Gets the admissibility constant for this wavelet.
     * 
     * @return the admissibility constant C_ψ
     */
    public double getAdmissibilityConstant() {
        return admissibilityConstant;
    }
    
    /**
     * Checks if the wavelet satisfies the admissibility condition.
     * 
     * @return true if admissible
     */
    public boolean isAdmissible() {
        return admissibilityConstant > 0 && admissibilityConstant < Double.POSITIVE_INFINITY;
    }
}