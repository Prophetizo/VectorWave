package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.optimization.CWTVectorOps;

/**
 * Main engine for Continuous Wavelet Transform computation.
 * 
 * <p>Provides methods to analyze signals using continuous wavelets,
 * supporting both direct convolution and FFT-accelerated computation.</p>
 *
 * @since 1.0.0
 */
public final class CWTTransform {
    
    private final ContinuousWavelet wavelet;
    private final CWTConfig config;
    private final CWTVectorOps vectorOps;
    
    /**
     * Creates a CWT transform with default configuration.
     * 
     * @param wavelet the continuous wavelet to use
     */
    public CWTTransform(ContinuousWavelet wavelet) {
        this(wavelet, CWTConfig.defaultConfig());
    }
    
    /**
     * Creates a CWT transform with custom configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @param config the configuration
     */
    public CWTTransform(ContinuousWavelet wavelet, CWTConfig config) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        
        this.wavelet = wavelet;
        this.config = config;
        this.vectorOps = new CWTVectorOps();
    }
    
    /**
     * Analyzes a signal using the specified scales.
     * 
     * @param signal the input signal
     * @param scales the scales to analyze
     * @return CWT result
     */
    public CWTResult analyze(double[] signal, double[] scales) {
        validateInputs(signal, scales);
        
        if (config.shouldUseFFT(signal.length) && !wavelet.isComplex()) {
            return analyzeFFT(signal, scales);
        } else {
            return analyzeDirect(signal, scales);
        }
    }
    
    /**
     * Analyzes a signal using a scale space.
     * 
     * @param signal the input signal
     * @param scaleSpace the scale space
     * @return CWT result
     */
    public CWTResult analyze(double[] signal, ScaleSpace scaleSpace) {
        if (scaleSpace == null) {
            throw new IllegalArgumentException("ScaleSpace cannot be null");
        }
        return analyze(signal, scaleSpace.getScales());
    }
    
    /**
     * Direct convolution implementation.
     */
    private CWTResult analyzeDirect(double[] signal, double[] scales) {
        if (wavelet.isComplex()) {
            return analyzeDirectComplex(signal, scales);
        }
        
        // Allocate coefficients array using memory pool if available
        double[][] coefficients;
        if (config.getMemoryPool() != null) {
            coefficients = config.getMemoryPool().allocateCoefficients(scales.length, signal.length);
        } else {
            coefficients = new double[scales.length][signal.length];
        }
        
        // Use optimized multi-scale computation
        double[][] computedCoeffs = vectorOps.computeMultiScale(signal, scales, wavelet);
        
        // Copy results to allocated array
        for (int i = 0; i < scales.length; i++) {
            System.arraycopy(computedCoeffs[i], 0, coefficients[i], 0, signal.length);
        }
        
        // Apply normalization if needed
        if (config.isNormalizeAcrossScales()) {
            vectorOps.normalizeByScale(coefficients, scales);
        }
        
        // Apply boundary handling if not periodic
        if (config.getBoundaryMode() != ai.prophetizo.wavelet.api.BoundaryMode.PERIODIC) {
            coefficients = applyBoundaryHandling(signal, scales, coefficients);
        }
        
        return new CWTResult(coefficients, scales, wavelet);
    }
    
    /**
     * Direct convolution for complex wavelets.
     */
    private CWTResult analyzeDirectComplex(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        ComplexMatrix complexCoeffs = new ComplexMatrix(numScales, signalLength);
        
        // For complex wavelets, we need both real and imaginary parts
        MorletWavelet morlet = wavelet instanceof MorletWavelet m ? m : null;
        
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            int halfSupport = (int)(4 * scale * wavelet.bandwidth());
            
            for (int tau = 0; tau < signalLength; tau++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int idx = tau + t;
                    double signalValue;
                    
                    if (idx >= 0 && idx < signalLength) {
                        signalValue = signal[idx];
                    } else {
                        signalValue = getBoundaryValue(signal, idx);
                    }
                    
                    // Real part
                    double waveletReal = wavelet.psi(-t / scale) / sqrtScale;
                    sumReal += signalValue * waveletReal;
                    
                    // Imaginary part (if Morlet)
                    if (morlet != null) {
                        double waveletImag = morlet.psiImaginary(-t / scale) / sqrtScale;
                        sumImag += signalValue * waveletImag;
                    }
                }
                
                complexCoeffs.set(s, tau, sumReal, sumImag);
            }
        }
        
        return new CWTResult(complexCoeffs, scales, wavelet);
    }
    
    /**
     * FFT-accelerated implementation.
     */
    private CWTResult analyzeFFT(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        
        // Determine FFT size
        int fftSize = config.getFFTSize() > 0 ? 
            config.getFFTSize() : config.getOptimalFFTSize(signalLength);
        
        // Pad signal to FFT size
        double[] paddedSignal = new double[fftSize];
        System.arraycopy(signal, 0, paddedSignal, 0, signalLength);
        
        // Apply FFT to signal
        Complex[] signalFFT = fft(paddedSignal);
        
        double[][] coefficients = new double[numScales][signalLength];
        
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            // Generate scaled wavelet
            double[] scaledWavelet = generateScaledWavelet(scale, fftSize);
            
            // FFT of wavelet
            Complex[] waveletFFT = fft(scaledWavelet);
            
            // Multiply in frequency domain (convolution theorem)
            Complex[] product = new Complex[fftSize];
            for (int i = 0; i < fftSize; i++) {
                // Conjugate of wavelet for correlation
                product[i] = signalFFT[i].multiply(waveletFFT[i].conjugate());
            }
            
            // Inverse FFT
            double[] convResult = ifft(product);
            
            // Extract valid portion and normalize
            for (int i = 0; i < signalLength; i++) {
                coefficients[s][i] = convResult[i] / sqrtScale;
            }
        }
        
        return new CWTResult(coefficients, scales, wavelet);
    }
    
    /**
     * Generates a scaled wavelet for FFT.
     */
    private double[] generateScaledWavelet(double scale, int length) {
        double[] waveletArray = new double[length];
        int halfSupport = (int)(4 * scale * wavelet.bandwidth());
        int center = length / 2;
        
        for (int i = -halfSupport; i <= halfSupport; i++) {
            int idx = (center + i + length) % length; // Circular wrap
            waveletArray[idx] = wavelet.psi(i / scale);
        }
        
        return waveletArray;
    }
    
    /**
     * Apply boundary handling to coefficients.
     */
    private double[][] applyBoundaryHandling(double[] signal, double[] scales, 
                                           double[][] coefficients) {
        // For non-periodic boundaries, recompute with proper padding
        CWTVectorOps.PaddingMode paddingMode = switch (config.getPaddingStrategy()) {
            case ZERO -> CWTVectorOps.PaddingMode.ZERO;
            case REFLECT -> CWTVectorOps.PaddingMode.REFLECT;
            case PERIODIC -> CWTVectorOps.PaddingMode.PERIODIC;
            case SYMMETRIC -> CWTVectorOps.PaddingMode.SYMMETRIC;
        };
        
        for (int s = 0; s < scales.length; s++) {
            double scale = scales[s];
            int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
            double[] scaledWavelet = new double[waveletSupport];
            
            // Sample wavelet at scale
            for (int i = 0; i < waveletSupport; i++) {
                double t = (i - waveletSupport / 2.0) / scale;
                scaledWavelet[i] = wavelet.psi(t);
            }
            
            coefficients[s] = vectorOps.convolveWithPadding(
                signal, scaledWavelet, scale, paddingMode);
        }
        
        return coefficients;
    }
    
    /**
     * Gets boundary value based on configuration.
     */
    private double getBoundaryValue(double[] signal, int index) {
        int length = signal.length;
        
        switch (config.getPaddingStrategy()) {
            case ZERO:
                return 0.0;
                
            case REFLECT:
                if (index < 0) {
                    index = -index;
                    if (index >= length) {
                        index = length - 1;
                    }
                    return signal[index];
                } else if (index >= length) {
                    index = 2 * length - index - 2;
                    if (index < 0) {
                        index = 0;
                    }
                    return signal[index];
                }
                break;
                
            case SYMMETRIC:
                if (index < 0) {
                    index = -index - 1;
                    if (index >= length) {
                        index = length - 1;
                    }
                    return signal[index];
                } else if (index >= length) {
                    index = 2 * length - index - 1;
                    if (index < 0) {
                        index = 0;
                    }
                    return signal[index];
                }
                break;
                
            case PERIODIC:
                return signal[(index % length + length) % length];
        }
        
        return 0.0;
    }
    
    /**
     * Simple FFT implementation (placeholder - in production would use optimized library).
     */
    private Complex[] fft(double[] x) {
        int n = x.length;
        Complex[] X = new Complex[n];
        
        // Simple DFT for now - would be replaced with proper FFT
        for (int k = 0; k < n; k++) {
            double sumReal = 0;
            double sumImag = 0;
            
            for (int t = 0; t < n; t++) {
                double angle = -2 * Math.PI * t * k / n;
                sumReal += x[t] * Math.cos(angle);
                sumImag += x[t] * Math.sin(angle);
            }
            
            X[k] = new Complex(sumReal, sumImag);
        }
        
        return X;
    }
    
    /**
     * Simple inverse FFT implementation.
     */
    private double[] ifft(Complex[] X) {
        int n = X.length;
        double[] x = new double[n];
        
        for (int t = 0; t < n; t++) {
            double sum = 0;
            
            for (int k = 0; k < n; k++) {
                double angle = 2 * Math.PI * t * k / n;
                sum += X[k].real * Math.cos(angle) - X[k].imag * Math.sin(angle);
            }
            
            x[t] = sum / n;
        }
        
        return x;
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInputs(double[] signal, double[] scales) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
        if (scales == null) {
            throw new IllegalArgumentException("Scales cannot be null");
        }
        if (scales.length == 0) {
            throw new IllegalArgumentException("Scales cannot be empty");
        }
        
        for (double scale : scales) {
            if (scale <= 0) {
                throw new IllegalArgumentException("All scales must be positive");
            }
        }
    }
    
    // Getters
    
    public ContinuousWavelet getWavelet() {
        return wavelet;
    }
    
    public CWTConfig getConfig() {
        return config;
    }
    
    /**
     * Simple complex number class.
     */
    private static class Complex {
        final double real;
        final double imag;
        
        Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        Complex multiply(Complex other) {
            double r = real * other.real - imag * other.imag;
            double i = real * other.imag + imag * other.real;
            return new Complex(r, i);
        }
        
        Complex conjugate() {
            return new Complex(real, -imag);
        }
    }
}