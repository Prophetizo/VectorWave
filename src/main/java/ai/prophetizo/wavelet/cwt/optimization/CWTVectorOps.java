package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import jdk.incubator.vector.*;

/**
 * SIMD-optimized operations for Continuous Wavelet Transform.
 * 
 * <p>Leverages Java 23's Vector API for significant performance improvements
 * in CWT computations including convolution, multi-scale analysis, and
 * complex arithmetic.</p>
 *
 * @since 1.0.0
 */
public final class CWTVectorOps {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    // Platform detection
    private static final boolean IS_APPLE_SILICON = 
        System.getProperty("os.arch").equals("aarch64") && 
        System.getProperty("os.name").toLowerCase().contains("mac");
    
    private static final boolean HAS_AVX512 = SPECIES.length() >= 8;
    private static final boolean HAS_AVX2 = SPECIES.length() >= 4;
    
    // Optimization thresholds
    private static final int SIMD_THRESHOLD = IS_APPLE_SILICON ? 8 : 16;
    private static final int FFT_THRESHOLD = 256;
    private static final int BLOCK_SIZE = 64; // Cache-friendly block size
    
    /**
     * Padding modes for boundary handling.
     */
    public enum PaddingMode {
        ZERO, REFLECT, PERIODIC, SYMMETRIC
    }
    
    /**
     * Computes convolution of signal with scaled wavelet using SIMD.
     * 
     * @param signal input signal
     * @param wavelet wavelet coefficients
     * @param scale scale factor
     * @return convolution result
     */
    public double[] convolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        
        if (signalLen < SIMD_THRESHOLD || waveletLen < SIMD_THRESHOLD) {
            return scalarConvolve(signal, wavelet, scale);
        }
        
        return vectorConvolve(signal, wavelet, scale);
    }
    
    /**
     * Vectorized convolution implementation.
     */
    private double[] vectorConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelet
        double[] scaledWavelet = new double[waveletLen];
        for (int i = 0; i < waveletLen; i++) {
            scaledWavelet[i] = wavelet[i] / sqrtScale;
        }
        
        // Main convolution loop
        for (int tau = 0; tau < signalLen; tau++) {
            double scalarSum = 0.0;
            
            // Process in chunks when beneficial
            if (waveletLen >= VECTOR_LENGTH) {
                DoubleVector sum = DoubleVector.zero(SPECIES);
                
                // Find valid range for vectorization
                int startIdx = Math.max(0, tau - halfWavelet);
                int endIdx = Math.min(signalLen, tau - halfWavelet + waveletLen);
                int waveletOffset = Math.max(0, halfWavelet - tau);
                
                int validStart = startIdx;
                int validEnd = Math.min(endIdx, startIdx + (waveletLen - waveletOffset));
                
                // Vectorized processing of valid range
                int i = validStart;
                int w = waveletOffset + (validStart - startIdx);
                
                for (; i <= validEnd - VECTOR_LENGTH && w <= waveletLen - VECTOR_LENGTH; 
                     i += VECTOR_LENGTH, w += VECTOR_LENGTH) {
                    DoubleVector sig = DoubleVector.fromArray(SPECIES, signal, i);
                    DoubleVector wav = DoubleVector.fromArray(SPECIES, scaledWavelet, w);
                    sum = sum.add(sig.mul(wav));
                }
                
                scalarSum = sum.reduceLanes(VectorOperators.ADD);
                
                // Handle remaining elements
                for (; i < validEnd && w < waveletLen; i++, w++) {
                    scalarSum += signal[i] * scaledWavelet[w];
                }
            } else {
                // Scalar fallback for small wavelets
                for (int t = 0; t < waveletLen; t++) {
                    int idx = tau - halfWavelet + t;
                    if (idx >= 0 && idx < signalLen) {
                        scalarSum += signal[idx] * scaledWavelet[t];
                    }
                }
            }
            
            result[tau] = scalarSum;
        }
        
        return result;
    }
    
    /**
     * Scalar convolution fallback.
     */
    private double[] scalarConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        for (int tau = 0; tau < signalLen; tau++) {
            double sum = 0.0;
            
            for (int t = 0; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    sum += signal[idx] * wavelet[t] / sqrtScale;
                }
            }
            
            result[tau] = sum;
        }
        
        return result;
    }
    
    /**
     * Computes complex convolution for complex wavelets.
     */
    public ComplexMatrix complexConvolve(double[] realSignal, double[] imagSignal,
                                       double[] realWavelet, double[] imagWavelet,
                                       double scale) {
        int signalLen = realSignal.length;
        int waveletLen = realWavelet.length;
        
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        if (signalLen < SIMD_THRESHOLD) {
            return scalarComplexConvolve(realSignal, imagSignal, 
                                       realWavelet, imagWavelet, scale);
        }
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelets
        double[] scaledRealWav = new double[waveletLen];
        double[] scaledImagWav = new double[waveletLen];
        for (int i = 0; i < waveletLen; i++) {
            scaledRealWav[i] = realWavelet[i] / sqrtScale;
            scaledImagWav[i] = imagWavelet[i] / sqrtScale;
        }
        
        // Complex convolution: (a + bi) * (c + di) = (ac - bd) + (ad + bc)i
        for (int tau = 0; tau < signalLen; tau++) {
            DoubleVector realSum = DoubleVector.zero(SPECIES);
            DoubleVector imagSum = DoubleVector.zero(SPECIES);
            
            int t = 0;
            for (; t < waveletLen - VECTOR_LENGTH + 1; t += VECTOR_LENGTH) {
                int idx = tau - halfWavelet + t;
                
                if (idx >= 0 && idx + VECTOR_LENGTH <= signalLen) {
                    // Load signal values
                    DoubleVector sigReal = DoubleVector.fromArray(SPECIES, realSignal, idx);
                    DoubleVector sigImag = imagSignal != null ? 
                        DoubleVector.fromArray(SPECIES, imagSignal, idx) : 
                        DoubleVector.zero(SPECIES);
                    
                    // Load wavelet values
                    DoubleVector wavReal = DoubleVector.fromArray(SPECIES, scaledRealWav, t);
                    DoubleVector wavImag = DoubleVector.fromArray(SPECIES, scaledImagWav, t);
                    
                    // Complex multiplication
                    // real = sigReal * wavReal - sigImag * wavImag
                    // imag = sigReal * wavImag + sigImag * wavReal
                    realSum = realSum.add(sigReal.mul(wavReal).sub(sigImag.mul(wavImag)));
                    imagSum = imagSum.add(sigReal.mul(wavImag).add(sigImag.mul(wavReal)));
                }
            }
            
            // Reduce and handle remainder
            double realTotal = realSum.reduceLanes(VectorOperators.ADD);
            double imagTotal = imagSum.reduceLanes(VectorOperators.ADD);
            
            for (; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    double sigR = realSignal[idx];
                    double sigI = (imagSignal != null) ? imagSignal[idx] : 0.0;
                    
                    realTotal += sigR * scaledRealWav[t] - sigI * scaledImagWav[t];
                    imagTotal += sigR * scaledImagWav[t] + sigI * scaledRealWav[t];
                }
            }
            
            result.set(0, tau, realTotal, imagTotal);
        }
        
        return result;
    }
    
    /**
     * Scalar complex convolution fallback.
     */
    private ComplexMatrix scalarComplexConvolve(double[] realSignal, double[] imagSignal,
                                               double[] realWavelet, double[] imagWavelet,
                                               double scale) {
        int signalLen = realSignal.length;
        int waveletLen = realWavelet.length;
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        for (int tau = 0; tau < signalLen; tau++) {
            double realSum = 0.0;
            double imagSum = 0.0;
            
            for (int t = 0; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    double sigR = realSignal[idx];
                    double sigI = (imagSignal != null) ? imagSignal[idx] : 0.0;
                    double wavR = realWavelet[t] / sqrtScale;
                    double wavI = imagWavelet[t] / sqrtScale;
                    
                    realSum += sigR * wavR - sigI * wavI;
                    imagSum += sigR * wavI + sigI * wavR;
                }
            }
            
            result.set(0, tau, realSum, imagSum);
        }
        
        return result;
    }
    
    /**
     * Computes CWT for multiple scales in parallel.
     */
    public double[][] computeMultiScale(double[] signal, double[] scales, 
                                      ContinuousWavelet wavelet) {
        int numScales = scales.length;
        int signalLen = signal.length;
        double[][] coefficients = new double[numScales][signalLen];
        
        // Generate wavelets for each scale
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
            double[] scaledWavelet = new double[waveletSupport];
            
            // Sample wavelet at scale
            for (int i = 0; i < waveletSupport; i++) {
                double t = (i - waveletSupport / 2.0) / scale;
                scaledWavelet[i] = wavelet.psi(t);
            }
            
            // Convolve with signal
            coefficients[s] = convolve(signal, scaledWavelet, scale);
        }
        
        return coefficients;
    }
    
    /**
     * Normalizes coefficients by scale factor.
     */
    public void normalizeByScale(double[][] coefficients, double[] scales) {
        int numScales = coefficients.length;
        int signalLen = coefficients[0].length;
        
        for (int s = 0; s < numScales; s++) {
            double normFactor = 1.0 / Math.sqrt(scales[s]);
            double[] row = coefficients[s];
            
            // Vectorized normalization
            int t = 0;
            for (; t < signalLen - VECTOR_LENGTH + 1; t += VECTOR_LENGTH) {
                DoubleVector vec = DoubleVector.fromArray(SPECIES, row, t);
                vec = vec.mul(normFactor);
                vec.intoArray(row, t);
            }
            
            // Handle remainder
            for (; t < signalLen; t++) {
                row[t] *= normFactor;
            }
        }
    }
    
    /**
     * Computes magnitude from complex coefficients using SIMD.
     */
    public double[][] computeMagnitude(ComplexMatrix complex) {
        int rows = complex.getRows();
        int cols = complex.getCols();
        double[][] magnitude = new double[rows][cols];
        
        double[][] real = complex.getReal();
        double[][] imag = complex.getImaginary();
        
        for (int r = 0; r < rows; r++) {
            int c = 0;
            for (; c < cols - VECTOR_LENGTH + 1; c += VECTOR_LENGTH) {
                DoubleVector realVec = DoubleVector.fromArray(SPECIES, real[r], c);
                DoubleVector imagVec = DoubleVector.fromArray(SPECIES, imag[r], c);
                
                // magnitude = sqrt(real² + imag²)
                DoubleVector mag2 = realVec.mul(realVec).add(imagVec.mul(imagVec));
                DoubleVector mag = mag2.sqrt();
                
                mag.intoArray(magnitude[r], c);
            }
            
            // Handle remainder
            for (; c < cols; c++) {
                double re = real[r][c];
                double im = imag[r][c];
                magnitude[r][c] = Math.sqrt(re * re + im * im);
            }
        }
        
        return magnitude;
    }
    
    /**
     * Computes power spectrum (magnitude squared) using SIMD.
     */
    public double[][] computePowerSpectrum(double[][] coefficients) {
        int rows = coefficients.length;
        int cols = coefficients[0].length;
        double[][] power = new double[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            double[] row = coefficients[r];
            double[] powerRow = power[r];
            
            int c = 0;
            for (; c < cols - VECTOR_LENGTH + 1; c += VECTOR_LENGTH) {
                DoubleVector vec = DoubleVector.fromArray(SPECIES, row, c);
                DoubleVector pow = vec.mul(vec);
                pow.intoArray(powerRow, c);
            }
            
            for (; c < cols; c++) {
                powerRow[c] = row[c] * row[c];
            }
        }
        
        return power;
    }
    
    /**
     * Convolution with specified padding mode.
     */
    public double[] convolveWithPadding(double[] signal, double[] wavelet, 
                                      double scale, PaddingMode padding) {
        // Extend signal based on padding mode
        int padSize = wavelet.length;
        double[] paddedSignal = new double[signal.length + 2 * padSize];
        
        // Copy original signal to center
        System.arraycopy(signal, 0, paddedSignal, padSize, signal.length);
        
        // Apply padding
        switch (padding) {
            case ZERO:
                // Already zeros
                break;
                
            case REFLECT:
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[Math.min(padSize - i, signal.length - 1)];
                    paddedSignal[paddedSignal.length - 1 - i] = 
                        signal[Math.max(signal.length - padSize + i - 1, 0)];
                }
                break;
                
            case PERIODIC:
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[signal.length - padSize + i];
                    paddedSignal[paddedSignal.length - padSize + i] = signal[i];
                }
                break;
                
            case SYMMETRIC:
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[padSize - i - 1];
                    paddedSignal[paddedSignal.length - 1 - i] = 
                        signal[signal.length - padSize + i];
                }
                break;
        }
        
        // Convolve padded signal
        double[] fullResult = convolve(paddedSignal, wavelet, scale);
        
        // Extract valid portion
        double[] result = new double[signal.length];
        System.arraycopy(fullResult, padSize, result, 0, signal.length);
        
        return result;
    }
    
    /**
     * Optimization strategy selection.
     */
    public OptimizationStrategy selectStrategy(int signalLength, int waveletLength) {
        if (signalLength * waveletLength < SIMD_THRESHOLD * SIMD_THRESHOLD) {
            return new OptimizationStrategy(true, false, false);
        } else if (signalLength > FFT_THRESHOLD && waveletLength > 32) {
            return new OptimizationStrategy(false, false, true);
        } else {
            return new OptimizationStrategy(false, true, false);
        }
    }
    
    /**
     * Streaming context for processing signals in chunks.
     */
    public StreamingContext createStreamingContext(int windowSize, int hopSize, 
                                                  double[] scales) {
        return new StreamingContext(windowSize, hopSize, scales);
    }
    
    /**
     * Process a chunk in streaming mode.
     */
    public double[][] processStreamingChunk(StreamingContext context, 
                                          double[] chunk, double[] wavelet) {
        context.addChunk(chunk);
        
        if (context.isReady()) {
            double[] window = context.getWindow();
            double[][] result = new double[context.scales.length][];
            
            for (int s = 0; s < context.scales.length; s++) {
                result[s] = convolve(window, wavelet, context.scales[s]);
            }
            
            context.advance();
            return result;
        }
        
        return new double[0][0]; // Return empty array instead of null
    }
    
    /**
     * Get platform information.
     */
    public PlatformInfo getPlatformInfo() {
        return new PlatformInfo(
            SPECIES,
            SPECIES.length() * Double.BYTES,
            IS_APPLE_SILICON,
            HAS_AVX512,
            HAS_AVX2,
            true // SIMD support with Vector API
        );
    }
    
    // Inner classes
    
    public static class OptimizationStrategy {
        private final boolean directComputation;
        private final boolean blockedComputation;
        private final boolean useFFT;
        
        public OptimizationStrategy(boolean direct, boolean blocked, boolean fft) {
            this.directComputation = direct;
            this.blockedComputation = blocked;
            this.useFFT = fft;
        }
        
        public boolean useDirectComputation() { return directComputation; }
        public boolean useBlockedComputation() { return blockedComputation; }
        public boolean useFFT() { return useFFT; }
    }
    
    public static class StreamingContext {
        private final int windowSize;
        private final int hopSize;
        private final double[] scales;
        private final double[] buffer;
        private int bufferPos;
        
        public StreamingContext(int windowSize, int hopSize, double[] scales) {
            this.windowSize = windowSize;
            this.hopSize = hopSize;
            this.scales = scales;
            this.buffer = new double[windowSize];
            this.bufferPos = 0;
        }
        
        public void addChunk(double[] chunk) {
            int copySize = Math.min(chunk.length, buffer.length - bufferPos);
            System.arraycopy(chunk, 0, buffer, bufferPos, copySize);
            bufferPos += copySize;
        }
        
        public boolean isReady() {
            return bufferPos >= windowSize;
        }
        
        public double[] getWindow() {
            return buffer.clone();
        }
        
        public void advance() {
            // Shift buffer by hop size
            System.arraycopy(buffer, hopSize, buffer, 0, windowSize - hopSize);
            bufferPos = windowSize - hopSize;
        }
    }
    
    public record PlatformInfo(
        VectorSpecies<Double> vectorSpecies,
        int vectorLength,
        boolean isAppleSilicon,
        boolean hasAVX512,
        boolean hasAVX2,
        boolean supportsSIMD
    ) {}
}