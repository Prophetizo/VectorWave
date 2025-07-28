package ai.prophetizo.wavelet.cwt.optimization;

/**
 * FFT-accelerated implementation for Continuous Wavelet Transform.
 * 
 * <p>This class provides optimized FFT and IFFT operations for CWT computation,
 * achieving O(N log N) complexity instead of O(N²) per scale through frequency
 * domain convolution.</p>
 * 
 * <p>The implementation uses the Cooley-Tukey algorithm with optimizations for
 * real-valued signals and efficient memory usage patterns.</p>
 * 
 * <p>For detailed mathematical background and implementation notes, see
 * {@code docs/FFT_MATHEMATICAL_DETAILS.md}</p>
 *
 * @since 1.0.0
 */
public final class FFTAcceleratedCWT {
    
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
        
        return fftComplex(X);
    }
    
    /**
     * Computes the Inverse Fast Fourier Transform of complex frequency-domain data.
     * 
     * <p>Transforms frequency-domain coefficients back to time domain using the
     * Cooley-Tukey algorithm. Returns only the real part, suitable for CWT applications.</p>
     * 
     * <p>Time complexity: O(N log N). Requires power-of-2 input length.</p>
     * 
     * <p>For detailed mathematical background and implementation notes, see
     * {@code docs/FFT_MATHEMATICAL_DETAILS.md}</p>
     * 
     * @param X the input complex frequency-domain coefficients (power-of-2 length)
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