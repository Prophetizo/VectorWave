package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.cwt.ComplexNumber;
import java.util.Arrays;

/**
 * Signal processing utilities for frequency domain operations.
 * 
 * <p>This class provides high-level signal processing operations including
 * FFT, convolution, windowing, and spectral analysis. It serves as a
 * user-friendly API that delegates complex computations to optimized
 * implementations.</p>
 * 
 * <p>Core FFT computations are performed by {@link OptimizedFFT}.</p>
 * 
 * @since 1.0.0
 */
public final class SignalProcessor {
    
    // Precomputed constants for common sizes
    private static final int MAX_CACHED_SIZE = 4096;
    private static final double[][] COS_TABLES = new double[13][]; // 2^0 to 2^12
    private static final double[][] SIN_TABLES = new double[13][];
    
    static {
        // Initialize twiddle factor tables for common sizes
        for (int i = 0; i < 13; i++) {
            int n = 1 << i;
            if (n <= MAX_CACHED_SIZE && n > 1) {  // Skip n=1 case
                COS_TABLES[i] = new double[n / 2];
                SIN_TABLES[i] = new double[n / 2];
                for (int k = 0; k < n / 2; k++) {
                    double angle = -2.0 * Math.PI * k / n;
                    COS_TABLES[i][k] = Math.cos(angle);
                    SIN_TABLES[i][k] = Math.sin(angle);
                }
            }
        }
    }
    
    /**
     * Performs forward FFT on complex data.
     * 
     * @param data complex input/output array
     */
    public static void fft(ComplexNumber[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("FFT input cannot be null or empty");
        }
        if ((data.length & (data.length - 1)) != 0) {
            throw new IllegalArgumentException("FFT input length must be a power of 2, got: " + data.length);
        }
        fftRadix2(data, false);
    }
    
    /**
     * Performs inverse FFT on complex data.
     * 
     * @param data complex input/output array
     */
    public static void ifft(ComplexNumber[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("IFFT input cannot be null or empty");
        }
        fftRadix2(data, true);
        // Normalize
        double norm = 1.0 / data.length;
        for (int i = 0; i < data.length; i++) {
            data[i] = new ComplexNumber(
                data[i].real() * norm,
                data[i].imag() * norm
            );
        }
    }
    
    /**
     * Performs FFT on real data, returning complex result.
     * 
     * @param real real input data
     * @return complex FFT result
     */
    public static ComplexNumber[] fftReal(double[] real) {
        if (real == null || real.length == 0) {
            throw new IllegalArgumentException("FFT input cannot be null or empty");
        }
        
        int n = nextPowerOf2(real.length);
        ComplexNumber[] complex = new ComplexNumber[n];
        
        // Copy real data to complex array
        for (int i = 0; i < real.length; i++) {
            complex[i] = new ComplexNumber(real[i], 0);
        }
        for (int i = real.length; i < n; i++) {
            complex[i] = new ComplexNumber(0, 0);
        }
        
        fft(complex);
        return complex;
    }
    
    /**
     * Performs FFT on real data, returning only magnitude spectrum.
     * 
     * @param real real input data
     * @return magnitude spectrum (first half due to symmetry)
     */
    public static double[] fftMagnitude(double[] real) {
        ComplexNumber[] fft = fftReal(real);
        double[] magnitude = new double[fft.length / 2 + 1];
        
        for (int i = 0; i < magnitude.length; i++) {
            magnitude[i] = fft[i].magnitude();
        }
        
        return magnitude;
    }
    
    /**
     * Computes convolution using FFT.
     * 
     * @param a first signal
     * @param b second signal
     * @return convolution result
     */
    public static double[] convolveFFT(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            throw new IllegalArgumentException("Convolution inputs cannot be null or empty");
        }
        
        int resultSize = a.length + b.length - 1;
        int n = nextPowerOf2(resultSize);
        
        // Pad signals to next power of 2
        double[] paddedA = Arrays.copyOf(a, n);
        double[] paddedB = Arrays.copyOf(b, n);
        
        // FFT of both signals
        ComplexNumber[] fftA = fftReal(paddedA);
        ComplexNumber[] fftB = fftReal(paddedB);
        
        // Multiply in frequency domain
        ComplexNumber[] product = new ComplexNumber[n];
        for (int i = 0; i < n; i++) {
            product[i] = fftA[i].multiply(fftB[i]);
        }
        
        // Inverse FFT
        ifft(product);
        
        // Extract real part of result
        double[] result = new double[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = product[i].real();
        }
        
        return result;
    }
    
    /**
     * Returns the next power of 2 greater than or equal to n.
     */
    public static int nextPowerOf2(int n) {
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }
    
    /**
     * Checks if n is a power of 2.
     */
    public static boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Applies a window function to the signal before FFT.
     */
    public static double[] applyWindow(double[] signal, WindowType window) {
        double[] windowed = new double[signal.length];
        int n = signal.length;
        
        switch (window) {
            case HANN -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
                    windowed[i] = signal[i] * w;
                }
            }
            case HAMMING -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
                    windowed[i] = signal[i] * w;
                }
            }
            case BLACKMAN -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)) 
                              + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
                    windowed[i] = signal[i] * w;
                }
            }
            case RECTANGULAR -> {
                System.arraycopy(signal, 0, windowed, 0, n);
            }
        }
        
        return windowed;
    }
    
    /**
     * Window function types.
     */
    public enum WindowType {
        RECTANGULAR, HANN, HAMMING, BLACKMAN
    }
    
    /**
     * Optimized radix-2 FFT implementation.
     */
    private static void fftRadix2(ComplexNumber[] data, boolean inverse) {
        int n = data.length;
        if (n <= 1) return;
        
        // Bit reversal
        bitReverse(data);
        
        // Cooley-Tukey FFT
        for (int len = 2; len <= n; len <<= 1) {
            int halfLen = len / 2;
            // tableIndex corresponds to log2(len), which is the index in our tables
            int tableIndex = Integer.numberOfTrailingZeros(len);
            
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    // Use precomputed twiddle factors if available
                    double cos, sin;
                    if (len <= MAX_CACHED_SIZE && tableIndex >= 0 && tableIndex < COS_TABLES.length 
                        && COS_TABLES[tableIndex] != null) {
                        cos = COS_TABLES[tableIndex][j];
                        sin = inverse ? -SIN_TABLES[tableIndex][j] : SIN_TABLES[tableIndex][j];
                    } else {
                        double angle = -2 * Math.PI * j / len;
                        if (inverse) angle = -angle;
                        cos = Math.cos(angle);
                        sin = Math.sin(angle);
                    }
                    
                    ComplexNumber t = new ComplexNumber(
                        data[i + j + halfLen].real() * cos - data[i + j + halfLen].imag() * sin,
                        data[i + j + halfLen].real() * sin + data[i + j + halfLen].imag() * cos
                    );
                    data[i + j + halfLen] = data[i + j].subtract(t);
                    data[i + j] = data[i + j].add(t);
                }
            }
        }
    }
    
    /**
     * Performs bit reversal permutation on the array.
     */
    private static void bitReverse(ComplexNumber[] data) {
        int n = data.length;
        int shift = 32 - Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> shift;
            if (i < j) {
                ComplexNumber temp = data[i];
                data[i] = data[j];
                data[j] = temp;
            }
        }
    }
    
    // Private constructor to prevent instantiation
    private SignalProcessor() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}