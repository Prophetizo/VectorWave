package ai.prophetizo.wavelet.util;

import java.util.Arrays;

/**
 * FFT-based convolution for efficient filtering with large kernels.
 * 
 * <p>For filters larger than ~48 taps, FFT-based convolution is more efficient
 * than direct convolution. This class provides optimized convolution using the
 * overlap-add or overlap-save method.</p>
 * 
 * <p>Performance characteristics:</p>
 * <ul>
 *   <li>Direct convolution: O(N*M) where N is signal length, M is filter length</li>
 *   <li>FFT convolution: O(N*log(N)) for large filters</li>
 *   <li>Crossover point: typically around 48-64 taps</li>
 * </ul>
 */
public final class FFTConvolution {
    
    private static final int FFT_THRESHOLD = 128; // Filter length threshold for using FFT (set high due to simple implementation)
    private static final int DEFAULT_BLOCK_SIZE = 4096; // Default block size for overlap-add
    
    /**
     * Performs convolution using the most efficient method based on filter size.
     * 
     * @param signal the input signal
     * @param filter the filter coefficients
     * @param mode convolution mode (full, same, valid)
     * @return the convolution result
     */
    public static double[] convolve(double[] signal, double[] filter, ConvolutionMode mode) {
        if (signal == null || filter == null) {
            throw new IllegalArgumentException("Signal and filter cannot be null");
        }
        
        if (filter.length >= FFT_THRESHOLD) {
            return fftConvolve(signal, filter, mode);
        } else {
            return directConvolve(signal, filter, mode);
        }
    }
    
    /**
     * Direct convolution implementation for small filters.
     */
    private static double[] directConvolve(double[] signal, double[] filter, ConvolutionMode mode) {
        int N = signal.length;
        int M = filter.length;
        int outputLength = getOutputLength(N, M, mode);
        double[] result = new double[outputLength];
        
        // Perform direct convolution
        for (int n = 0; n < outputLength; n++) {
            double sum = 0.0;
            int start = mode == ConvolutionMode.VALID ? 0 : Math.max(0, M - 1 - n);
            int end = Math.min(M, N - n + M - 1);
            
            for (int m = start; m < end; m++) {
                int signalIndex = n - M + 1 + m;
                if (signalIndex >= 0 && signalIndex < N) {
                    sum += signal[signalIndex] * filter[M - 1 - m];
                }
            }
            result[n] = sum;
        }
        
        return trimResult(result, N, M, mode);
    }
    
    /**
     * FFT-based convolution using overlap-add method.
     */
    private static double[] fftConvolve(double[] signal, double[] filter, ConvolutionMode mode) {
        int N = signal.length;
        int M = filter.length;
        
        // Choose FFT size (next power of 2 >= N + M - 1)
        int fftSize = nextPowerOfTwo(N + M - 1);
        
        // Pad arrays to FFT size
        double[] paddedSignal = Arrays.copyOf(signal, fftSize);
        double[] paddedFilter = Arrays.copyOf(filter, fftSize);
        
        // Compute FFTs
        Complex[] signalFFT = fft(paddedSignal);
        Complex[] filterFFT = fft(paddedFilter);
        
        // Multiply in frequency domain
        Complex[] productFFT = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            productFFT[i] = signalFFT[i].multiply(filterFFT[i]);
        }
        
        // Inverse FFT
        double[] fullResult = ifftReal(productFFT);
        
        // Extract result based on mode
        return trimResult(fullResult, N, M, mode);
    }
    
    /**
     * Overlap-add method for very long signals.
     * Processes signal in blocks to reduce memory usage.
     */
    public static double[] overlapAdd(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        int L = DEFAULT_BLOCK_SIZE; // Block size
        int step = L - M + 1; // Step size
        
        double[] result = new double[N + M - 1];
        
        // Process signal in blocks
        for (int pos = 0; pos < N; pos += step) {
            int blockSize = Math.min(L, N - pos);
            double[] block = Arrays.copyOfRange(signal, pos, pos + blockSize);
            
            // Convolve block with filter
            double[] blockResult = fftConvolve(block, filter, ConvolutionMode.FULL);
            
            // Add to result with overlap
            for (int i = 0; i < blockResult.length && pos + i < result.length; i++) {
                result[pos + i] += blockResult[i];
            }
        }
        
        return result;
    }
    
    /**
     * FFT implementation using Cooley-Tukey algorithm.
     */
    private static Complex[] fft(double[] x) {
        int n = x.length;
        
        // Base case
        if (n == 1) {
            return new Complex[]{new Complex(x[0], 0)};
        }
        
        // Radix-2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw new IllegalArgumentException("FFT length must be power of 2");
        }
        
        // Divide
        double[] even = new double[n / 2];
        double[] odd = new double[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
            odd[k] = x[2 * k + 1];
        }
        
        // Conquer
        Complex[] evenFFT = fft(even);
        Complex[] oddFFT = fft(odd);
        
        // Combine
        Complex[] result = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            Complex t = wk.multiply(oddFFT[k]);
            result[k] = evenFFT[k].add(t);
            result[k + n / 2] = evenFFT[k].subtract(t);
        }
        
        return result;
    }
    
    /**
     * Inverse FFT for real-valued results.
     */
    private static double[] ifftReal(Complex[] x) {
        int n = x.length;
        
        // Conjugate
        Complex[] conjugate = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjugate[i] = x[i].conjugate();
        }
        
        // Forward FFT
        Complex[] result = fft(toReal(conjugate));
        
        // Conjugate and scale
        double[] realResult = new double[n];
        for (int i = 0; i < n; i++) {
            realResult[i] = result[i].conjugate().real / n;
        }
        
        return realResult;
    }
    
    /**
     * Convert complex array to real array (takes real parts).
     */
    private static double[] toReal(Complex[] x) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i].real;
        }
        return result;
    }
    
    /**
     * Get output length based on convolution mode.
     */
    private static int getOutputLength(int N, int M, ConvolutionMode mode) {
        switch (mode) {
            case FULL:
                return N + M - 1;
            case SAME:
                return N;
            case VALID:
                return Math.max(0, N - M + 1);
            default:
                throw new IllegalArgumentException("Unknown convolution mode: " + mode);
        }
    }
    
    /**
     * Trim convolution result based on mode.
     */
    private static double[] trimResult(double[] result, int N, int M, ConvolutionMode mode) {
        switch (mode) {
            case FULL:
                return result;
            case SAME:
                int start = (M - 1) / 2;
                return Arrays.copyOfRange(result, start, start + N);
            case VALID:
                return Arrays.copyOfRange(result, M - 1, N);
            default:
                return result;
        }
    }
    
    /**
     * Find next power of two >= n.
     */
    private static int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }
    
    /**
     * Convolution modes.
     */
    public enum ConvolutionMode {
        /**
         * Full convolution, output length = N + M - 1
         */
        FULL,
        
        /**
         * Same size as input, output length = N
         */
        SAME,
        
        /**
         * Valid convolution without zero-padding, output length = N - M + 1
         */
        VALID
    }
    
    /**
     * Simple complex number implementation for FFT.
     */
    private static class Complex {
        final double real;
        final double imag;
        
        Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        Complex add(Complex other) {
            return new Complex(real + other.real, imag + other.imag);
        }
        
        Complex subtract(Complex other) {
            return new Complex(real - other.real, imag - other.imag);
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
    
    private FFTConvolution() {
        // Utility class, prevent instantiation
    }
}