package ai.prophetizo.wavelet.cwt.optimization;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.api.MorletWavelet;
import ai.prophetizo.wavelet.cwt.ComplexMatrix;
import jdk.incubator.vector.*;

import java.util.Arrays;

/**
 * Cache-aware optimizations for Continuous Wavelet Transform.
 * 
 * <p>Implements blocking, tiling, and prefetching strategies to maximize
 * cache utilization and minimize memory bandwidth requirements.</p>
 *
 * @since 1.0.0
 */
public final class CacheAwareOps {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    // Cache parameters (typical values for modern CPUs)
    private static final int L1_CACHE_SIZE = 32 * 1024; // 32KB L1 data cache
    private static final int L2_CACHE_SIZE = 256 * 1024; // 256KB L2 cache
    private static final int CACHE_LINE_SIZE = 64; // 64-byte cache lines
    
    // Optimal block sizes based on cache hierarchy
    private static final int L1_BLOCK_SIZE = 
        Math.min(512, L1_CACHE_SIZE / (3 * Double.BYTES)); // 3 arrays in L1
    private static final int L2_BLOCK_SIZE = 
        Math.min(4096, L2_CACHE_SIZE / (4 * Double.BYTES)); // 4 arrays in L2
    
    // Ensure block sizes are multiples of vector length and cache line
    private static final int OPTIMAL_BLOCK_SIZE = 
        (L1_BLOCK_SIZE / VECTOR_LENGTH) * VECTOR_LENGTH;
    private static final int TILE_SIZE = 
        (CACHE_LINE_SIZE / Double.BYTES);
    
    /**
     * Computes convolution using cache-friendly blocking.
     * 
     * @param signal input signal
     * @param wavelet wavelet coefficients
     * @param scale scale factor
     * @return convolution result
     */
    public double[] blockedConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        if (signalLen < OPTIMAL_BLOCK_SIZE) {
            // Small signals - use direct computation
            return directConvolve(signal, wavelet, scale);
        }
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelet
        double[] scaledWavelet = new double[waveletLen];
        for (int i = 0; i < waveletLen; i++) {
            scaledWavelet[i] = wavelet[i] / sqrtScale;
        }
        
        // Process signal in blocks to maximize cache reuse
        for (int blockStart = 0; blockStart < signalLen; blockStart += OPTIMAL_BLOCK_SIZE) {
            int blockEnd = Math.min(blockStart + OPTIMAL_BLOCK_SIZE, signalLen);
            
            // Process each output position in the block
            for (int tau = blockStart; tau < blockEnd; tau++) {
                double sum = 0.0;
                
                // Process wavelet coefficients
                for (int t = 0; t < waveletLen; t++) {
                    int idx = tau - halfWavelet + t;
                    if (idx >= 0 && idx < signalLen) {
                        sum += signal[idx] * scaledWavelet[t];
                    }
                }
                
                result[tau] = sum;
            }
        }
        
        return result;
    }
    
    /**
     * Direct convolution for small signals.
     */
    private double[] directConvolve(double[] signal, double[] wavelet, double scale) {
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
     * Computes complex convolution with cache optimization.
     */
    public ComplexMatrix blockedComplexConvolve(double[] signal, 
                                               ContinuousWavelet wavelet, 
                                               double scale) {
        int signalLen = signal.length;
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        // Generate wavelet samples
        int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
        int halfSupport = waveletSupport / 2;
        
        // For Morlet wavelet, compute complex values
        MorletWavelet morlet = wavelet instanceof MorletWavelet m ? m : null;
        
        // Process in blocks
        for (int blockStart = 0; blockStart < signalLen; blockStart += OPTIMAL_BLOCK_SIZE) {
            int blockEnd = Math.min(blockStart + OPTIMAL_BLOCK_SIZE, signalLen);
            
            for (int tau = blockStart; tau < blockEnd; tau++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int idx = tau + t;
                    if (idx >= 0 && idx < signalLen) {
                        double signalValue = signal[idx];
                        double waveletReal = wavelet.psi(-t / scale) / Math.sqrt(scale);
                        
                        sumReal += signalValue * waveletReal;
                        
                        if (morlet != null) {
                            double waveletImag = morlet.psiImaginary(-t / scale) / Math.sqrt(scale);
                            sumImag += signalValue * waveletImag;
                        }
                    }
                }
                
                result.set(0, tau, sumReal, sumImag);
            }
        }
        
        return result;
    }
    
    /**
     * Normalizes matrix rows using tiled access pattern.
     */
    public void tiledNormalize(double[][] matrix) {
        int rows = matrix.length;
        if (rows == 0) return;
        int cols = matrix[0].length;
        if (cols == 0) return;
        
        // Process in tiles for better cache locality
        for (int rowStart = 0; rowStart < rows; rowStart += TILE_SIZE) {
            int rowEnd = Math.min(rowStart + TILE_SIZE, rows);
            
            // First pass: compute norms
            double[] norms = new double[rowEnd - rowStart];
            
            for (int r = rowStart; r < rowEnd; r++) {
                double sum = 0.0;
                double[] row = matrix[r];
                
                // Process columns in blocks
                for (int colStart = 0; colStart < cols; colStart += OPTIMAL_BLOCK_SIZE) {
                    int colEnd = Math.min(colStart + OPTIMAL_BLOCK_SIZE, cols);
                    
                    // Vectorized sum of squares
                    int c = colStart;
                    if (colEnd - colStart >= VECTOR_LENGTH) {
                        DoubleVector vSum = DoubleVector.zero(SPECIES);
                        
                        for (; c <= colEnd - VECTOR_LENGTH; c += VECTOR_LENGTH) {
                            DoubleVector v = DoubleVector.fromArray(SPECIES, row, c);
                            vSum = vSum.add(v.mul(v));
                        }
                        
                        sum += vSum.reduceLanes(VectorOperators.ADD);
                    }
                    
                    // Handle remainder
                    for (; c < colEnd; c++) {
                        sum += row[c] * row[c];
                    }
                }
                
                norms[r - rowStart] = Math.sqrt(sum);
            }
            
            // Second pass: normalize
            for (int r = rowStart; r < rowEnd; r++) {
                double norm = norms[r - rowStart];
                if (norm > 1e-10) { // Avoid division by zero
                    double invNorm = 1.0 / norm;
                    double[] row = matrix[r];
                    
                    // Vectorized normalization
                    int c = 0;
                    for (; c <= cols - VECTOR_LENGTH; c += VECTOR_LENGTH) {
                        DoubleVector v = DoubleVector.fromArray(SPECIES, row, c);
                        v = v.mul(invNorm);
                        v.intoArray(row, c);
                    }
                    
                    // Handle remainder
                    for (; c < cols; c++) {
                        row[c] *= invNorm;
                    }
                } else {
                    // If norm is zero, set row to unit vector
                    double[] row = matrix[r];
                    if (cols > 0) {
                        row[0] = 1.0;
                        for (int c = 1; c < cols; c++) {
                            row[c] = 0.0;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Computes multi-scale CWT with cache-aware blocking.
     */
    public double[][] computeMultiScaleBlocked(double[] signal, double[] scales,
                                              ContinuousWavelet wavelet) {
        int numScales = scales.length;
        int signalLen = signal.length;
        double[][] coefficients = new double[numScales][signalLen];
        
        // Process scales in groups that fit in L2 cache
        int scalesPerBlock = L2_CACHE_SIZE / (signalLen * Double.BYTES);
        scalesPerBlock = Math.max(1, Math.min(scalesPerBlock, numScales));
        
        for (int scaleBlock = 0; scaleBlock < numScales; scaleBlock += scalesPerBlock) {
            int scaleEnd = Math.min(scaleBlock + scalesPerBlock, numScales);
            
            // Pre-generate wavelets for this block of scales
            double[][] wavelets = new double[scaleEnd - scaleBlock][];
            for (int s = scaleBlock; s < scaleEnd; s++) {
                double scale = scales[s];
                int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
                wavelets[s - scaleBlock] = new double[waveletSupport];
                
                for (int i = 0; i < waveletSupport; i++) {
                    double t = (i - waveletSupport / 2.0) / scale;
                    wavelets[s - scaleBlock][i] = wavelet.psi(t);
                }
            }
            
            // Process signal blocks for all scales in this group
            for (int s = scaleBlock; s < scaleEnd; s++) {
                coefficients[s] = blockedConvolve(signal, 
                    wavelets[s - scaleBlock], scales[s]);
            }
        }
        
        return coefficients;
    }
    
    /**
     * Convolution with explicit prefetching hints.
     */
    public double[] convolveWithPrefetch(double[] signal, double[] wavelet, double scale) {
        // Note: Java doesn't have explicit prefetch instructions,
        // but we can structure access patterns for hardware prefetcher
        return blockedConvolve(signal, wavelet, scale);
    }
    
    /**
     * Creates an aligned array (best effort in Java).
     */
    public double[] createAlignedArray(int size) {
        // In Java, we can't directly control memory alignment,
        // but we can allocate arrays that are likely to be aligned
        int alignedSize = ((size + TILE_SIZE - 1) / TILE_SIZE) * TILE_SIZE;
        return new double[alignedSize];
    }
    
    /**
     * Gets optimal block size for current platform.
     */
    public int getOptimalBlockSize() {
        return OPTIMAL_BLOCK_SIZE;
    }
    
    /**
     * Selects caching strategy based on problem size.
     */
    public CacheStrategy selectStrategy(int signalLength, int waveletLength) {
        long totalOps = (long)signalLength * waveletLength;
        
        if (totalOps < 10_000) {
            return new CacheStrategy(true, false, false);
        } else if (totalOps < 1_000_000) {
            return new CacheStrategy(false, true, false);
        } else {
            return new CacheStrategy(false, false, true);
        }
    }
    
    /**
     * Creates streaming cache for real-time processing.
     */
    public StreamingCache createStreamingCache(int windowSize, int overlap, double[] scales) {
        return new StreamingCache(windowSize, overlap, scales);
    }
    
    /**
     * Processes streaming chunk with cache optimization.
     */
    public double[][] processStreamingChunk(StreamingCache cache, double[] chunk,
                                           ContinuousWavelet wavelet) {
        cache.addChunk(chunk);
        
        if (cache.isReady()) {
            double[] window = cache.getWindow();
            double[][] result = computeMultiScaleBlocked(window, cache.scales, wavelet);
            cache.advance();
            return result;
        }
        
        return null;
    }
    
    // Inner classes
    
    public static class CacheStrategy {
        private final boolean directComputation;
        private final boolean blockedComputation;
        private final boolean tiledComputation;
        
        public CacheStrategy(boolean direct, boolean blocked, boolean tiled) {
            this.directComputation = direct;
            this.blockedComputation = blocked;
            this.tiledComputation = tiled;
        }
        
        public boolean useDirectComputation() { return directComputation; }
        public boolean useBlockedComputation() { return blockedComputation; }
        public boolean useTiledComputation() { return tiledComputation; }
    }
    
    public static class StreamingCache {
        private final int windowSize;
        private final int overlap;
        private final double[] scales;
        private final double[] buffer;
        private int bufferPos;
        private int processedChunks;
        
        public StreamingCache(int windowSize, int overlap, double[] scales) {
            this.windowSize = windowSize;
            this.overlap = overlap;
            this.scales = scales.clone();
            this.buffer = new double[windowSize];
            this.bufferPos = 0;
            this.processedChunks = 0;
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
            return Arrays.copyOf(buffer, windowSize);
        }
        
        public void advance() {
            // Shift buffer by (windowSize - overlap)
            int shift = windowSize - overlap;
            System.arraycopy(buffer, shift, buffer, 0, overlap);
            bufferPos = overlap;
            processedChunks++;
        }
        
        public int getProcessedChunks() {
            return processedChunks;
        }
    }
}