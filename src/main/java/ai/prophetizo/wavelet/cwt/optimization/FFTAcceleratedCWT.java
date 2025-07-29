package ai.prophetizo.wavelet.cwt.optimization;

/**
 * FFT-accelerated implementation for Continuous Wavelet Transform.
 * 
 * <p>This class provides optimized FFT and IFFT operations for CWT computation,
 * achieving O(N log N) complexity instead of O(N²) per scale through frequency
 * domain convolution.</p>
 * 
 * <p>The implementation supports multiple algorithms:
 * <ul>
 *   <li><strong>STANDARD</strong>: Traditional recursive Cooley-Tukey with good stability</li>
 *   <li><strong>OPTIMIZED</strong>: In-place algorithm with pre-computed twiddle factors (30-50% faster)</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public final class FFTAcceleratedCWT {
    
    /**
     * The selected FFT algorithm for this instance.
     */
    private final FFTAlgorithm algorithm;
    
    /**
     * Creates a new FFTAcceleratedCWT instance with the default OPTIMIZED algorithm.
     */
    public FFTAcceleratedCWT() {
        this(FFTAlgorithm.OPTIMIZED);
    }
    
    /**
     * Creates a new FFTAcceleratedCWT instance with the specified algorithm.
     * 
     * @param algorithm the FFT algorithm to use
     * @throws NullPointerException if algorithm is null
     */
    public FFTAcceleratedCWT(FFTAlgorithm algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("FFT algorithm must not be null.");
        }
        this.algorithm = algorithm;
    }
    
    /**
     * Gets the FFT algorithm used by this instance.
     * 
     * @return the FFT algorithm
     */
    public FFTAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Complex number representation for FFT operations.
     * 
     * <p>Uses separate real and imaginary fields for better performance
     * and compatibility with SIMD operations.</p>
     */
    public static final class Complex {
        public final double real;
        public final double imag;
        
        /**
         * Creates a new complex number.
         * 
         * @param real the real part
         * @param imag the imaginary part
         */
        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        /**
         * Multiplies this complex number by another.
         * 
         * @param other the other complex number
         * @return the product
         */
        public Complex multiply(Complex other) {
            double r = real * other.real - imag * other.imag;
            double i = real * other.imag + imag * other.real;
            return new Complex(r, i);
        }
        
        /**
         * Returns the complex conjugate.
         * 
         * @return the complex conjugate
         */
        public Complex conjugate() {
            return new Complex(real, -imag);
        }
        
        /**
         * Adds another complex number to this one.
         * 
         * @param other the other complex number
         * @return the sum
         */
        public Complex add(Complex other) {
            return new Complex(real + other.real, imag + other.imag);
        }
        
        /**
         * Subtracts another complex number from this one.
         * 
         * @param other the other complex number
         * @return the difference
         */
        public Complex subtract(Complex other) {
            return new Complex(real - other.real, imag - other.imag);
        }
    }
    
    /**
     * Computes the Fast Fourier Transform of a real-valued signal.
     * 
     * <p>This method transforms a real-valued time-domain signal into its
     * frequency-domain representation using the Cooley-Tukey FFT algorithm.</p>
     * 
     * <p><strong>Requirements:</strong>
     * <ul>
     *   <li>Input array must not be null</li>
     *   <li>Input array length must be a power of 2</li>
     *   <li>Input array must contain finite values only</li>
     * </ul>
     * </p>
     * 
     * <p><strong>Performance:</strong> O(N log N) where N is the input length.</p>
     * 
     * @param x the input real-valued signal
     * @return the frequency-domain representation as complex numbers
     * @throws NullPointerException if x is null
     * @throws IllegalArgumentException if x length is not a power of 2 or contains invalid values
     */
    public Complex[] fft(double[] x) {
        // Validate input
        if (x == null) {
            throw new NullPointerException("Input array x must not be null.");
        }
        int n = x.length;
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Input array length must be a power of 2.");
        }
        
        // Check for finite values
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("Input array must contain only finite values at index " + i);
            }
        }
        
        // Convert to complex array
        Complex[] X = new Complex[n];
        for (int i = 0; i < n; i++) {
            X[i] = new Complex(x[i], 0.0);
        }
        
        // Use selected algorithm
        switch (algorithm) {
            case STANDARD:
                return fftComplex(X);
            case OPTIMIZED:
                OptimizedFFT.fftInPlace(X);
                return X;
            default:
                throw new IllegalStateException("Unknown FFT algorithm: " + algorithm);
        }
    }
    
    /**
     * Computes the Inverse Fast Fourier Transform of complex frequency-domain data.
     * 
     * <p>This method transforms complex frequency-domain coefficients back to the
     * time domain, extracting the real part of the result. This is particularly
     * useful for CWT applications where the result should be real-valued.</p>
     * 
     * <p>The extensive mathematical foundation of the IFFT ensures perfect reconstruction
     * of signals when combined with the forward FFT, maintaining numerical stability
     * through careful normalization and precision handling.</p>
     * 
     * <p><strong>Mathematical Context:</strong></p>
     * <p>The Inverse Discrete Fourier Transform is defined as:</p>
     * <pre>
     * x[n] = (1/N) * Σ(k=0 to N-1) X[k] * e^(j*2π*k*n/N)
     * </pre>
     * 
     * <p>Where:</p>
     * <ul>
     *   <li>N is the transform length (must be power of 2 for FFT efficiency)</li>
     *   <li>X[k] are the frequency-domain coefficients</li>
     *   <li>x[n] are the reconstructed time-domain samples</li>
     *   <li>j is the imaginary unit (√-1)</li>
     * </ul>
     * 
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Uses Cooley-Tukey algorithm with bit-reversal permutation</li>
     *   <li>Applies 1/N normalization for proper scaling</li>
     *   <li>Optimized for real-valued output extraction</li>
     *   <li>Maintains numerical precision through careful floating-point handling</li>
     * </ul>
     * 
     * <p><strong>Validation Requirements:</strong></p>
     * <ul>
     *   <li>Input array must not be null</li>
     *   <li>Input array length must be a power of 2 (2, 4, 8, 16, 32, ...)</li>
     *   <li>All complex coefficients must contain finite values</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li>Time complexity: O(N log N)</li>
     *   <li>Space complexity: O(N)</li>
     *   <li>Optimized for power-of-2 lengths</li>
     *   <li>Cache-friendly memory access patterns</li>
     * </ul>
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * // Forward transform
     * Complex[] spectrum = fft(timeSignal);
     * 
     * // Process in frequency domain
     * // ... apply filtering, convolution, etc.
     * 
     * // Inverse transform back to time domain
     * double[] reconstructed = ifft(spectrum);
     * }</pre>
     * 
     * @param X the input complex frequency-domain coefficients
     * @return the real-valued time-domain signal
     * @throws NullPointerException if X is null
     * @throws IllegalArgumentException if X length is not a power of 2 or contains invalid values
     * @see #fft(double[]) for the forward transformation
     */
    public double[] ifft(Complex[] X) {
        // Validate input
        if (X == null) {
            throw new NullPointerException("Input array X must not be null.");
        }
        int n = X.length;
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Input array length must be a power of 2.");
        }
        
        // Validate complex coefficients
        for (int i = 0; i < n; i++) {
            if (X[i] == null) {
                throw new IllegalArgumentException("Complex coefficient at index " + i + " must not be null.");
            }
            if (!Double.isFinite(X[i].real) || !Double.isFinite(X[i].imag)) {
                throw new IllegalArgumentException("Complex coefficient at index " + i + " must contain finite values.");
            }
        }
        
        // Use selected algorithm
        switch (algorithm) {
            case STANDARD:
                return ifftStandard(X);
            case OPTIMIZED:
                return ifftOptimized(X);
            default:
                throw new IllegalStateException("Unknown FFT algorithm: " + algorithm);
        }
    }
    
    /**
     * Standard IFFT implementation using conjugate method.
     * 
     * @param X the input complex frequency-domain coefficients
     * @return the real-valued time-domain signal
     */
    private double[] ifftStandard(Complex[] X) {
        int n = X.length;
        
        // Compute IFFT by taking conjugate, applying FFT, then conjugating and scaling
        Complex[] conjugated = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjugated[i] = X[i].conjugate();
        }
        
        Complex[] result = fftComplex(conjugated);
        
        // Extract real parts and apply normalization
        double[] output = new double[n];
        for (int i = 0; i < n; i++) {
            output[i] = result[i].real / n;
        }
        
        return output;
    }
    
    /**
     * Optimized IFFT implementation using in-place algorithm.
     * 
     * @param X the input complex frequency-domain coefficients
     * @return the real-valued time-domain signal
     */
    private double[] ifftOptimized(Complex[] X) {
        int n = X.length;
        
        // Create a copy for in-place processing
        Complex[] data = new Complex[n];
        System.arraycopy(X, 0, data, 0, n);
        
        // Perform in-place IFFT
        OptimizedFFT.ifftInPlace(data);
        
        // Extract real parts
        double[] output = new double[n];
        for (int i = 0; i < n; i++) {
            output[i] = data[i].real;
        }
        
        return output;
    }
    
    /**
     * Internal FFT implementation for complex arrays.
     * 
     * @param X the complex input array
     * @return the FFT result
     */
    private Complex[] fftComplex(Complex[] X) {
        int n = X.length;
        
        if (n == 1) {
            return new Complex[]{X[0]};
        }
        
        // Bit-reversal permutation
        Complex[] data = new Complex[n];
        for (int i = 0; i < n; i++) {
            data[i] = X[bitReverse(i, n)];
        }
        
        // Cooley-Tukey FFT
        for (int len = 2; len <= n; len *= 2) {
            double angle = -2.0 * Math.PI / len;
            Complex wlen = new Complex(Math.cos(angle), Math.sin(angle));
            
            for (int i = 0; i < n; i += len) {
                Complex w = new Complex(1, 0);
                for (int j = 0; j < len / 2; j++) {
                    Complex u = data[i + j];
                    Complex v = data[i + j + len / 2].multiply(w);
                    data[i + j] = u.add(v);
                    data[i + j + len / 2] = u.subtract(v);
                    w = w.multiply(wlen);
                }
            }
        }
        
        return data;
    }
    
    /**
     * Computes bit-reversed index for FFT algorithm.
     * 
     * @param index the original index
     * @param n the array length (must be power of 2)
     * @return the bit-reversed index
     */
    private int bitReverse(int index, int n) {
        int result = 0;
        int bits = Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < bits; i++) {
            result = (result << 1) | (index & 1);
            index >>= 1;
        }
        
        return result;
    }
}