package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;
import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import jdk.incubator.vector.*;

import java.util.Arrays;

/**
 * FFT-accelerated Continuous Wavelet Transform implementation.
 * 
 * <p>Uses Fast Fourier Transform to accelerate convolution operations,
 * providing O(N log N) complexity instead of O(N²) for each scale.</p>
 *
 * @since 1.0.0
 */
public final class FFTAcceleratedCWT {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final double TWO_PI = 2.0 * Math.PI;
    
    /**
     * Window types for edge effect reduction.
     */
    public enum WindowType {
        NONE, HAMMING, HANN, BLACKMAN, TUKEY
    }
    
    /**
     * Complex number representation.
     */
    public static class Complex {
        public final double real;
        public final double imag;
        
        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        public Complex multiply(Complex other) {
            double r = real * other.real - imag * other.imag;
            double i = real * other.imag + imag * other.real;
            return new Complex(r, i);
        }
        
        public Complex conjugate() {
            return new Complex(real, -imag);
        }
        
        public double magnitude() {
            return Math.sqrt(real * real + imag * imag);
        }
        
        public double phase() {
            return Math.atan2(imag, real);
        }
    }
    
    /**
     * Computes CWT for a single scale using FFT acceleration.
     * 
     * @param signal input signal
     * @param wavelet continuous wavelet
     * @param scale scale parameter
     * @param fftSize FFT size (must be power of 2)
     * @return CWT coefficients
     */
    public double[] computeScaleFFT(double[] signal, ContinuousWavelet wavelet, 
                                   double scale, int fftSize) {
        validateFFTSize(fftSize);
        
        if (signal.length > fftSize) {
            throw new IllegalArgumentException("Signal length exceeds FFT size");
        }
        
        // Pad signal to FFT size
        double[] paddedSignal = Arrays.copyOf(signal, fftSize);
        
        // Generate scaled wavelet
        double[] scaledWavelet = generateScaledWavelet(wavelet, scale, fftSize);
        
        // Compute FFTs
        Complex[] signalFFT = fft(paddedSignal);
        Complex[] waveletFFT = fft(scaledWavelet);
        
        // Multiply in frequency domain (correlation = conjugate multiplication)
        Complex[] product = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            product[i] = signalFFT[i].multiply(waveletFFT[i].conjugate());
        }
        
        // Inverse FFT
        double[] result = ifft(product);
        
        // Extract valid portion and normalize
        double[] coefficients = new double[signal.length];
        double normFactor = Math.sqrt(scale);
        for (int i = 0; i < signal.length; i++) {
            coefficients[i] = result[i] / normFactor;
        }
        
        return coefficients;
    }
    
    /**
     * Computes CWT for complex wavelets using FFT.
     */
    public ComplexMatrix computeComplexScaleFFT(double[] signal, ContinuousWavelet wavelet,
                                               double scale, int fftSize) {
        validateFFTSize(fftSize);
        
        // For complex wavelets like Morlet
        double[] paddedSignal = Arrays.copyOf(signal, fftSize);
        
        // Generate complex scaled wavelet
        Complex[] complexWavelet = generateComplexScaledWavelet(wavelet, scale, fftSize);
        
        // FFT of signal (real -> complex)
        Complex[] signalFFT = fft(paddedSignal);
        
        // Multiply in frequency domain
        Complex[] product = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            product[i] = signalFFT[i].multiply(complexWavelet[i].conjugate());
        }
        
        // Inverse FFT to get complex result
        Complex[] complexResult = ifftComplex(product);
        
        // Extract valid portion
        ComplexMatrix result = new ComplexMatrix(1, signal.length);
        double normFactor = Math.sqrt(scale);
        
        for (int i = 0; i < signal.length; i++) {
            result.set(0, i, 
                complexResult[i].real / normFactor,
                complexResult[i].imag / normFactor);
        }
        
        return result;
    }
    
    /**
     * Computes CWT for multiple scales efficiently.
     */
    public double[][] computeMultiScaleFFT(double[] signal, double[] scales, 
                                          ContinuousWavelet wavelet) {
        int signalLen = signal.length;
        int numScales = scales.length;
        double[][] coefficients = new double[numScales][signalLen];
        
        // Select optimal FFT size
        int maxWaveletSupport = (int)(8 * scales[scales.length - 1] * wavelet.bandwidth());
        int fftSize = selectOptimalFFTSize(signalLen, maxWaveletSupport);
        
        // Pre-compute signal FFT
        double[] paddedSignal = Arrays.copyOf(signal, fftSize);
        Complex[] signalFFT = fft(paddedSignal);
        
        // Process each scale
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            
            // Generate and FFT the scaled wavelet
            double[] scaledWavelet = generateScaledWavelet(wavelet, scale, fftSize);
            Complex[] waveletFFT = fft(scaledWavelet);
            
            // Multiply in frequency domain
            Complex[] product = new Complex[fftSize];
            for (int i = 0; i < fftSize; i++) {
                product[i] = signalFFT[i].multiply(waveletFFT[i].conjugate());
            }
            
            // Inverse FFT
            double[] result = ifft(product);
            
            // Extract and normalize
            double normFactor = Math.sqrt(scale);
            for (int i = 0; i < signalLen; i++) {
                coefficients[s][i] = result[i] / normFactor;
            }
        }
        
        return coefficients;
    }
    
    /**
     * FFT implementation using Cooley-Tukey algorithm.
     */
    public Complex[] fft(double[] x) {
        int n = x.length;
        if (n == 1) {
            return new Complex[]{new Complex(x[0], 0)};
        }
        
        if (!isPowerOfTwo(n)) {
            throw new IllegalArgumentException("FFT size must be power of 2, got: " + n);
        }
        
        // Bit reversal
        double[] xReordered = new double[n];
        int[] reversed = computeBitReversal(n);
        for (int i = 0; i < n; i++) {
            xReordered[i] = x[reversed[i]];
        }
        
        // FFT computation
        Complex[] X = new Complex[n];
        for (int i = 0; i < n; i++) {
            X[i] = new Complex(xReordered[i], 0);
        }
        
        // Cooley-Tukey FFT
        for (int size = 2; size <= n; size *= 2) {
            double angle = -TWO_PI / size;
            Complex w = new Complex(Math.cos(angle), Math.sin(angle));
            
            for (int start = 0; start < n; start += size) {
                Complex wn = new Complex(1, 0);
                int halfSize = size / 2;
                
                for (int k = 0; k < halfSize; k++) {
                    Complex even = X[start + k];
                    Complex odd = X[start + k + halfSize];
                    
                    X[start + k] = new Complex(
                        even.real + wn.real * odd.real - wn.imag * odd.imag,
                        even.imag + wn.real * odd.imag + wn.imag * odd.real
                    );
                    
                    X[start + k + halfSize] = new Complex(
                        even.real - (wn.real * odd.real - wn.imag * odd.imag),
                        even.imag - (wn.real * odd.imag + wn.imag * odd.real)
                    );
                    
                    // Update twiddle factor
                    double wnReal = wn.real * w.real - wn.imag * w.imag;
                    double wnImag = wn.real * w.imag + wn.imag * w.real;
                    wn = new Complex(wnReal, wnImag);
                }
            }
        }
        
        return X;
    }
    
    /**
     * Inverse FFT for real result.
     */
    public double[] ifft(Complex[] X) {
        int n = X.length;
        
        // Conjugate
        Complex[] conjX = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjX[i] = X[i].conjugate();
        }
        
        // Forward FFT
        Complex[] result = fftComplex(conjX);
        
        // Conjugate and scale
        double[] realResult = new double[n];
        for (int i = 0; i < n; i++) {
            realResult[i] = result[i].real / n;
        }
        
        return realResult;
    }
    
    /**
     * Inverse FFT for complex result.
     */
    public Complex[] ifftComplex(Complex[] X) {
        int n = X.length;
        
        // Conjugate
        Complex[] conjX = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjX[i] = X[i].conjugate();
        }
        
        // Forward FFT
        Complex[] result = fftComplex(conjX);
        
        // Conjugate and scale
        Complex[] finalResult = new Complex[n];
        for (int i = 0; i < n; i++) {
            finalResult[i] = new Complex(result[i].real / n, -result[i].imag / n);
        }
        
        return finalResult;
    }
    
    /**
     * FFT for complex input.
     */
    private Complex[] fftComplex(Complex[] x) {
        int n = x.length;
        if (n == 1) return x;
        
        Complex[] X = Arrays.copyOf(x, n);
        
        // Bit reversal
        int[] reversed = computeBitReversal(n);
        for (int i = 0; i < n; i++) {
            if (i < reversed[i]) {
                Complex temp = X[i];
                X[i] = X[reversed[i]];
                X[reversed[i]] = temp;
            }
        }
        
        // Cooley-Tukey FFT
        for (int size = 2; size <= n; size *= 2) {
            double angle = -TWO_PI / size;
            Complex w = new Complex(Math.cos(angle), Math.sin(angle));
            
            for (int start = 0; start < n; start += size) {
                Complex wn = new Complex(1, 0);
                int halfSize = size / 2;
                
                for (int k = 0; k < halfSize; k++) {
                    Complex even = X[start + k];
                    Complex odd = X[start + k + halfSize].multiply(wn);
                    
                    X[start + k] = new Complex(
                        even.real + odd.real,
                        even.imag + odd.imag
                    );
                    
                    X[start + k + halfSize] = new Complex(
                        even.real - odd.real,
                        even.imag - odd.imag
                    );
                    
                    wn = wn.multiply(w);
                }
            }
        }
        
        return X;
    }
    
    /**
     * Generates scaled wavelet for FFT.
     */
    private double[] generateScaledWavelet(ContinuousWavelet wavelet, double scale, 
                                          int length) {
        double[] samples = new double[length];
        int halfSupport = (int)(4 * scale * wavelet.bandwidth());
        
        // Limit support to half the FFT size to avoid wraparound
        halfSupport = Math.min(halfSupport, length / 2 - 1);
        
        // Generate wavelet centered at origin
        for (int i = -halfSupport; i <= halfSupport; i++) {
            int idx = (i + length) % length; // Circular wrap
            double t = i / scale;
            samples[idx] = wavelet.psi(t);
        }
        
        return samples;
    }
    
    /**
     * Generates complex scaled wavelet.
     */
    private Complex[] generateComplexScaledWavelet(ContinuousWavelet wavelet, 
                                                  double scale, int length) {
        Complex[] samples = new Complex[length];
        Arrays.fill(samples, new Complex(0, 0));
        
        int halfSupport = (int)(4 * scale * wavelet.bandwidth());
        
        for (int i = -halfSupport; i <= halfSupport; i++) {
            int idx = (i + length) % length;
            double t = i / scale;
            samples[idx] = evaluateComplexWavelet(wavelet, t);
        }
        
        // FFT the wavelet
        return fftComplex(samples);
    }
    
    /**
     * Evaluates a wavelet at a given point, handling both real and complex wavelets.
     * This helper method provides a unified interface for evaluating any continuous wavelet
     * as a complex number, simplifying code that needs to handle both real and complex wavelets.
     * 
     * @param wavelet the wavelet to evaluate
     * @param t the time/position parameter
     * @return Complex value of the wavelet at point t
     */
    public static Complex evaluateAsComplex(ContinuousWavelet wavelet, double t) {
        if (wavelet instanceof ComplexContinuousWavelet complexWavelet) {
            double real = complexWavelet.psi(t);
            double imag = complexWavelet.psiImaginary(t);
            return new Complex(real, imag);
        } else {
            // For real wavelets, imaginary part is zero
            return new Complex(wavelet.psi(t), 0);
        }
    }
    
    /**
     * Helper method for internal use.
     */
    private Complex evaluateComplexWavelet(ContinuousWavelet wavelet, double t) {
        return evaluateAsComplex(wavelet, t);
    }
    
    /**
     * Selects optimal FFT size.
     */
    public int selectOptimalFFTSize(int signalSize, int waveletSize) {
        int minSize = signalSize + waveletSize;
        return nextPowerOfTwo(minSize);
    }
    
    /**
     * Creates window function.
     */
    public double[] createWindow(int length, WindowType type) {
        double[] window = new double[length];
        
        switch (type) {
            case NONE:
                Arrays.fill(window, 1.0);
                break;
                
            case HAMMING:
                for (int i = 0; i < length; i++) {
                    window[i] = 0.54 - 0.46 * Math.cos(TWO_PI * i / (length - 1));
                }
                break;
                
            case HANN:
                for (int i = 0; i < length; i++) {
                    window[i] = 0.5 * (1 - Math.cos(TWO_PI * i / (length - 1)));
                }
                break;
                
            case BLACKMAN:
                for (int i = 0; i < length; i++) {
                    window[i] = 0.42 - 0.5 * Math.cos(TWO_PI * i / (length - 1)) +
                               0.08 * Math.cos(4 * Math.PI * i / (length - 1));
                }
                break;
                
            case TUKEY:
                double alpha = 0.5; // Tukey parameter
                int taperLength = (int)(alpha * (length - 1) / 2);
                
                Arrays.fill(window, 1.0); // Initialize to 1
                
                // Left taper
                for (int i = 0; i < taperLength; i++) {
                    window[i] = 0.5 * (1 - Math.cos(Math.PI * i / taperLength));
                }
                
                // Right taper
                for (int i = length - taperLength; i < length; i++) {
                    window[i] = 0.5 * (1 - Math.cos(Math.PI * (length - 1 - i) / taperLength));
                }
                break;
        }
        
        return window;
    }
    
    /**
     * FFT cache for multiple scale computations.
     */
    public FFTCache createFFTCache(double[] signal) {
        int fftSize = nextPowerOfTwo(signal.length * 2);
        double[] paddedSignal = Arrays.copyOf(signal, fftSize);
        Complex[] signalFFT = fft(paddedSignal);
        return new FFTCache(signalFFT, fftSize);
    }
    
    /**
     * Compute with cached FFT.
     */
    public double[][] computeWithCache(FFTCache cache, double[] scales, 
                                      ContinuousWavelet wavelet) {
        int signalLen = cache.fftSize / 2; // Assuming padding
        double[][] coefficients = new double[scales.length][signalLen];
        
        for (int s = 0; s < scales.length; s++) {
            double scale = scales[s];
            
            // Generate wavelet FFT
            double[] scaledWavelet = generateScaledWavelet(wavelet, scale, cache.fftSize);
            Complex[] waveletFFT = fft(scaledWavelet);
            
            // Multiply cached signal FFT with wavelet FFT
            Complex[] product = new Complex[cache.fftSize];
            for (int i = 0; i < cache.fftSize; i++) {
                product[i] = cache.signalFFT[i].multiply(waveletFFT[i].conjugate());
            }
            
            // Inverse FFT
            double[] result = ifft(product);
            
            // Extract and normalize
            double normFactor = Math.sqrt(scale);
            for (int i = 0; i < signalLen && i < result.length; i++) {
                coefficients[s][i] = result[i] / normFactor;
            }
        }
        
        return coefficients;
    }
    
    // Utility methods
    
    private void validateFFTSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("FFT size must be positive");
        }
        if (!isPowerOfTwo(size)) {
            throw new IllegalArgumentException("FFT size must be power of 2, got: " + size);
        }
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    private int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    private int[] computeBitReversal(int n) {
        int[] reversed = new int[n];
        int bits = Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < n; i++) {
            reversed[i] = Integer.reverse(i) >>> (32 - bits);
        }
        
        return reversed;
    }
    
    public static class FFTCache {
        final Complex[] signalFFT;
        final int fftSize;
        
        FFTCache(Complex[] signalFFT, int fftSize) {
            this.signalFFT = signalFFT;
            this.fftSize = fftSize;
        }
    }
}