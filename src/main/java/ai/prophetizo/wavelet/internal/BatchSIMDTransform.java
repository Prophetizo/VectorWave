package ai.prophetizo.wavelet.internal;

import ai.prophetizo.wavelet.memory.AlignedMemoryPool;
import ai.prophetizo.wavelet.memory.AlignedMemoryPool.PooledArray;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorMask;

/**
 * True SIMD batch wavelet transform implementation that processes multiple signals
 * in parallel using vector instructions.
 *
 * <p>This implementation achieves true parallelization by:</p>
 * <ul>
 *   <li>Processing N signals simultaneously where N = SIMD vector width</li>
 *   <li>Using efficient memory layouts for coalesced vector loads</li>
 *   <li>Minimizing data movement and maximizing arithmetic intensity</li>
 *   <li>Optimizing for different batch sizes and signal lengths</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class BatchSIMDTransform {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    // Validate vector length is reasonable
    static {
        VectorLengthValidator.validateVectorLength(VECTOR_LENGTH, "BatchSIMDTransform");
    }
    
    // Cache line size for optimal memory access patterns
    private static final int CACHE_LINE_SIZE = 64;
    private static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / 8;
    
    private BatchSIMDTransform() {
        // Utility class
    }
    
    /**
     * Perform Haar wavelet transform on multiple signals using true SIMD parallelization.
     * Processes VECTOR_LENGTH signals simultaneously for maximum throughput.
     * 
     * @param signals input signals [signal_index][sample_index]
     * @param approxResults approximation coefficients output [signal_index][sample_index]
     * @param detailResults detail coefficients output [signal_index][sample_index]
     */
    public static void haarBatchTransformSIMD(double[][] signals, 
                                             double[][] approxResults, 
                                             double[][] detailResults) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        int halfLength = signalLength / 2;
        
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);
        DoubleVector sqrt2Vec = DoubleVector.broadcast(SPECIES, SQRT2_INV);
        
        // Process full vectors first (no branching in hot loop)
        int fullVectorGroups = numSignals / VECTOR_LENGTH;
        
        // Pre-allocate arrays to avoid GC pressure
        double[] evenSamples = new double[VECTOR_LENGTH];
        double[] oddSamples = new double[VECTOR_LENGTH];
        double[] approxTemp = new double[VECTOR_LENGTH];
        double[] detailTemp = new double[VECTOR_LENGTH];
        
        // Process all full vector groups
        for (int sigGroup = 0; sigGroup < fullVectorGroups * VECTOR_LENGTH; sigGroup += VECTOR_LENGTH) {
            // Process each output sample
            for (int outIdx = 0; outIdx < halfLength; outIdx++) {
                int evenIdx = 2 * outIdx;
                int oddIdx = evenIdx + 1;
                
                // Gather samples from different signals
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    evenSamples[v] = signals[sigGroup + v][evenIdx];
                    oddSamples[v] = signals[sigGroup + v][oddIdx];
                }
                
                // SIMD computation
                DoubleVector evenVec = DoubleVector.fromArray(SPECIES, evenSamples, 0);
                DoubleVector oddVec = DoubleVector.fromArray(SPECIES, oddSamples, 0);
                
                DoubleVector approxVec = evenVec.add(oddVec).mul(sqrt2Vec);
                DoubleVector detailVec = evenVec.sub(oddVec).mul(sqrt2Vec);
                
                // Scatter results back to different signals
                approxVec.intoArray(approxTemp, 0);
                detailVec.intoArray(detailTemp, 0);
                
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    approxResults[sigGroup + v][outIdx] = approxTemp[v];
                    detailResults[sigGroup + v][outIdx] = detailTemp[v];
                }
            }
        }
        
        // Handle remainder signals (if any)
        int remainderStart = fullVectorGroups * VECTOR_LENGTH;
        if (remainderStart < numSignals) {
            for (int sig = remainderStart; sig < numSignals; sig++) {
                for (int outIdx = 0; outIdx < halfLength; outIdx++) {
                    double even = signals[sig][2 * outIdx];
                    double odd = signals[sig][2 * outIdx + 1];
                    approxResults[sig][outIdx] = (even + odd) * SQRT2_INV;
                    detailResults[sig][outIdx] = (even - odd) * SQRT2_INV;
                }
            }
        }
    }
    
    /**
     * Optimized batch transform using blocked memory access patterns.
     * Processes multiple signals and multiple samples in blocks for better cache utilization.
     */
    public static void blockedBatchTransformSIMD(double[][] signals,
                                                 double[][] approxResults,
                                                 double[][] detailResults,
                                                 double[] lowPass,
                                                 double[] highPass) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        int filterLength = lowPass.length;
        int halfLength = signalLength / 2;
        
        // Block sizes for cache optimization
        int signalBlockSize = Math.min(VECTOR_LENGTH * 2, numSignals);
        int sampleBlockSize = Math.min(DOUBLES_PER_CACHE_LINE * 4, halfLength);
        
        // Process in blocks for better cache locality
        for (int sigBlock = 0; sigBlock < numSignals; sigBlock += signalBlockSize) {
            int sigBlockEnd = Math.min(sigBlock + signalBlockSize, numSignals);
            
            for (int sampleBlock = 0; sampleBlock < halfLength; sampleBlock += sampleBlockSize) {
                int sampleBlockEnd = Math.min(sampleBlock + sampleBlockSize, halfLength);
                
                // Process this block
                processBlock(signals, approxResults, detailResults,
                           lowPass, highPass,
                           sigBlock, sigBlockEnd,
                           sampleBlock, sampleBlockEnd,
                           signalLength, filterLength);
            }
        }
    }
    
    private static void processBlock(double[][] signals,
                                   double[][] approxResults,
                                   double[][] detailResults,
                                   double[] lowPass,
                                   double[] highPass,
                                   int sigStart, int sigEnd,
                                   int sampleStart, int sampleEnd,
                                   int signalLength, int filterLength) {
        // Clear output block
        for (int sig = sigStart; sig < sigEnd; sig++) {
            for (int sample = sampleStart; sample < sampleEnd; sample++) {
                approxResults[sig][sample] = 0.0;
                detailResults[sig][sample] = 0.0;
            }
        }
        
        // Pre-allocate arrays to avoid GC pressure
        double[] inputSamples = new double[VECTOR_LENGTH];
        double[] approxSums = new double[VECTOR_LENGTH];
        double[] detailSums = new double[VECTOR_LENGTH];
        
        // Convolution for this block
        for (int outSample = sampleStart; outSample < sampleEnd; outSample++) {
            for (int k = 0; k < filterLength; k++) {
                int inSample = (2 * outSample + k) % signalLength;
                
                // Process signals in groups of VECTOR_LENGTH
                for (int sig = sigStart; sig < sigEnd; sig += VECTOR_LENGTH) {
                    int remaining = Math.min(VECTOR_LENGTH, sigEnd - sig);
                    
                    if (remaining == VECTOR_LENGTH) {
                        // Gather input samples
                        for (int v = 0; v < VECTOR_LENGTH; v++) {
                            inputSamples[v] = signals[sig + v][inSample];
                        }
                        
                        DoubleVector inputVec = DoubleVector.fromArray(SPECIES, inputSamples, 0);
                        DoubleVector lowCoeff = DoubleVector.broadcast(SPECIES, lowPass[k]);
                        DoubleVector highCoeff = DoubleVector.broadcast(SPECIES, highPass[k]);
                        
                        // Gather current sums
                        for (int v = 0; v < VECTOR_LENGTH; v++) {
                            approxSums[v] = approxResults[sig + v][outSample];
                            detailSums[v] = detailResults[sig + v][outSample];
                        }
                        
                        DoubleVector approxVec = DoubleVector.fromArray(SPECIES, approxSums, 0);
                        DoubleVector detailVec = DoubleVector.fromArray(SPECIES, detailSums, 0);
                        
                        // Accumulate
                        approxVec = approxVec.add(inputVec.mul(lowCoeff));
                        detailVec = detailVec.add(inputVec.mul(highCoeff));
                        
                        // Scatter back
                        approxVec.intoArray(approxSums, 0);
                        detailVec.intoArray(detailSums, 0);
                        for (int v = 0; v < VECTOR_LENGTH; v++) {
                            approxResults[sig + v][outSample] = approxSums[v];
                            detailResults[sig + v][outSample] = detailSums[v];
                        }
                    } else {
                        // Scalar fallback for partial vectors
                        for (int v = 0; v < remaining; v++) {
                            double val = signals[sig + v][inSample];
                            approxResults[sig + v][outSample] += val * lowPass[k];
                            detailResults[sig + v][outSample] += val * highPass[k];
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Ultra-fast batch transform for aligned data using unrolled loops.
     * Requires signals to be properly aligned and padded.
     */
    public static void alignedBatchTransformSIMD(double[][] signals,
                                                double[][] approxResults,
                                                double[][] detailResults,
                                                double[] lowPass,
                                                double[] highPass) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        int filterLength = lowPass.length;
        int halfLength = signalLength / 2;
        
        // Ensure alignment for optimal performance
        if (numSignals % VECTOR_LENGTH != 0) {
            // Fall back to blocked version for non-aligned batch sizes
            blockedBatchTransformSIMD(signals, approxResults, detailResults, lowPass, highPass);
            return;
        }
        
        // Pre-allocate arrays for unrolled loop to avoid GC pressure
        double[] approx1 = new double[VECTOR_LENGTH];
        double[] detail1 = new double[VECTOR_LENGTH];
        double[] approx2 = new double[VECTOR_LENGTH];
        double[] detail2 = new double[VECTOR_LENGTH];
        double[] input1 = new double[VECTOR_LENGTH];
        double[] input2 = new double[VECTOR_LENGTH];
        
        // Process with perfect alignment - unroll by 2x for better pipeline utilization
        for (int outSample = 0; outSample < halfLength - 1; outSample += 2) {
            // Clear two output samples
            for (int sig = 0; sig < numSignals; sig += VECTOR_LENGTH) {
                DoubleVector zero = DoubleVector.zero(SPECIES);
                
                // Clear for sample 1
                zero.intoArray(approx1, 0);
                zero.intoArray(detail1, 0);
                
                // Clear for sample 2
                zero.intoArray(approx2, 0);
                zero.intoArray(detail2, 0);
                
                // Convolution for both samples
                for (int k = 0; k < filterLength; k++) {
                    int inSample1 = (2 * outSample + k) % signalLength;
                    int inSample2 = (2 * (outSample + 1) + k) % signalLength;
                    
                    // Gather input for both samples
                    for (int v = 0; v < VECTOR_LENGTH; v++) {
                        input1[v] = signals[sig + v][inSample1];
                        input2[v] = signals[sig + v][inSample2];
                    }
                    
                    DoubleVector inputVec1 = DoubleVector.fromArray(SPECIES, input1, 0);
                    DoubleVector inputVec2 = DoubleVector.fromArray(SPECIES, input2, 0);
                    DoubleVector lowCoeff = DoubleVector.broadcast(SPECIES, lowPass[k]);
                    DoubleVector highCoeff = DoubleVector.broadcast(SPECIES, highPass[k]);
                    
                    // Load accumulators
                    DoubleVector approxVec1 = DoubleVector.fromArray(SPECIES, approx1, 0);
                    DoubleVector detailVec1 = DoubleVector.fromArray(SPECIES, detail1, 0);
                    DoubleVector approxVec2 = DoubleVector.fromArray(SPECIES, approx2, 0);
                    DoubleVector detailVec2 = DoubleVector.fromArray(SPECIES, detail2, 0);
                    
                    // Accumulate for both samples
                    approxVec1 = approxVec1.add(inputVec1.mul(lowCoeff));
                    detailVec1 = detailVec1.add(inputVec1.mul(highCoeff));
                    approxVec2 = approxVec2.add(inputVec2.mul(lowCoeff));
                    detailVec2 = detailVec2.add(inputVec2.mul(highCoeff));
                    
                    // Store back
                    approxVec1.intoArray(approx1, 0);
                    detailVec1.intoArray(detail1, 0);
                    approxVec2.intoArray(approx2, 0);
                    detailVec2.intoArray(detail2, 0);
                }
                
                // Scatter results for both samples
                for (int v = 0; v < VECTOR_LENGTH; v++) {
                    approxResults[sig + v][outSample] = approx1[v];
                    detailResults[sig + v][outSample] = detail1[v];
                    approxResults[sig + v][outSample + 1] = approx2[v];
                    detailResults[sig + v][outSample + 1] = detail2[v];
                }
            }
        }
        
        // Handle last sample if odd number
        if (halfLength % 2 == 1) {
            int lastSample = halfLength - 1;
            for (int sig = 0; sig < numSignals; sig++) {
                approxResults[sig][lastSample] = 0.0;
                detailResults[sig][lastSample] = 0.0;
                
                for (int k = 0; k < filterLength; k++) {
                    int inSample = (2 * lastSample + k) % signalLength;
                    double val = signals[sig][inSample];
                    approxResults[sig][lastSample] += val * lowPass[k];
                    detailResults[sig][lastSample] += val * highPass[k];
                }
            }
        }
    }
    
    /**
     * Adaptive batch transform that selects the best algorithm based on input characteristics.
     */
    public static void adaptiveBatchTransform(double[][] signals,
                                            double[][] approxResults,
                                            double[][] detailResults,
                                            double[] lowPass,
                                            double[] highPass) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        
        // Select best algorithm based on characteristics
        if (lowPass.length == 2 && highPass.length == 2) {
            // Haar wavelet - use specialized implementation
            haarBatchTransformSIMD(signals, approxResults, detailResults);
        } else if (numSignals % VECTOR_LENGTH == 0 && numSignals >= VECTOR_LENGTH * 2) {
            // Well-aligned batch size - use unrolled version
            alignedBatchTransformSIMD(signals, approxResults, detailResults, lowPass, highPass);
        } else if (signalLength >= 512 && numSignals >= VECTOR_LENGTH) {
            // Large signals - use cache-blocked version
            blockedBatchTransformSIMD(signals, approxResults, detailResults, lowPass, highPass);
        } else {
            // Default - use basic blocked version
            blockedBatchTransformSIMD(signals, approxResults, detailResults, lowPass, highPass);
        }
    }
    
    /**
     * Get information about batch SIMD capabilities.
     */
    public static String getBatchSIMDInfo() {
        return String.format(
            "Batch SIMD Transform Configuration:%n" +
            "Platform: %s%n" +
            "Vector Species: %s%n" +
            "Vector Length: %d doubles (%d bits)%n" +
            "Optimal batch size: multiples of %d%n" +
            "Cache line size: %d bytes%n" +
            "Recommended signal block: %d samples%n" +
            "Processing mode: True parallel SIMD across signals",
            System.getProperty("os.arch"),
            SPECIES.toString(),
            VECTOR_LENGTH,
            VECTOR_LENGTH * 64,
            VECTOR_LENGTH,
            CACHE_LINE_SIZE,
            DOUBLES_PER_CACHE_LINE * 4
        );
    }
}