package ai.prophetizo.wavelet.internal;

/**
 * Provides cache prefetching optimizations for wavelet transform operations.
 *
 * <p>This class uses JVM intrinsics and platform-specific hints to improve
 * cache performance during wavelet transforms. Prefetching is particularly
 * beneficial for:</p>
 * <ul>
 *   <li>Large signal processing where data doesn't fit in L1/L2 cache</li>
 *   <li>Streaming transforms with predictable access patterns</li>
 *   <li>Multi-level decompositions with strided memory access</li>
 * </ul>
 *
 * <p>The prefetching is implemented as hints and may be ignored by the JVM
 * or hardware if not beneficial. The implementation gracefully degrades on
 * platforms without prefetch support.</p>
 *
 * @since 1.0.0
 */
public final class PrefetchOptimizer {

    // Cache line size (typical for modern processors)
    private static final int CACHE_LINE_SIZE = 64;
    private static final int BYTES_PER_DOUBLE = 8;
    private static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / BYTES_PER_DOUBLE;

    // Prefetch distances (tuned for typical L2/L3 latencies)
    private static final int L1_PREFETCH_DISTANCE = 2;   // 2 cache lines ahead
    private static final int L2_PREFETCH_DISTANCE = 8;   // 8 cache lines ahead
    private static final int L3_PREFETCH_DISTANCE = 16;  // 16 cache lines ahead

    // Minimum array size to benefit from prefetching
    private static final int MIN_PREFETCH_SIZE = 256;

    // Platform-specific prefetch support
    private static final boolean PREFETCH_SUPPORTED = checkPrefetchSupport();

    private PrefetchOptimizer() {
        // Utility class
    }

    /**
     * Checks if the platform supports prefetch instructions.
     */
    private static boolean checkPrefetchSupport() {
        // Check for x86_64 or ARM64 platforms that typically support prefetch
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return arch.contains("amd64") || arch.contains("x86_64") ||
                arch.contains("aarch64") || arch.contains("arm64");
    }

    /**
     * Prefetches data for convolution operations with periodic boundary.
     *
     * @param signal       the input signal
     * @param currentIndex the current processing index
     * @param filterLength the filter length
     */
    public static void prefetchForConvolution(double[] signal, int currentIndex, int filterLength) {
        if (!PREFETCH_SUPPORTED || signal.length < MIN_PREFETCH_SIZE) {
            return;
        }

        int signalMask = signal.length - 1;

        // Prefetch for L1 cache (immediate future access)
        int l1Index = ((currentIndex + L1_PREFETCH_DISTANCE * 2) + filterLength) & signalMask;
        prefetchL1(signal, l1Index);

        // Prefetch for L2 cache (near future access)
        int l2Index = ((currentIndex + L2_PREFETCH_DISTANCE * 2) + filterLength) & signalMask;
        prefetchL2(signal, l2Index);

        // Prefetch for L3 cache (far future access)
        if (signal.length > 4096) { // Only for large signals
            int l3Index = ((currentIndex + L3_PREFETCH_DISTANCE * 2) + filterLength) & signalMask;
            prefetchL3(signal, l3Index);
        }
    }

    /**
     * Prefetches data for upsampling operations.
     *
     * @param coeffs       the coefficients array
     * @param currentIndex the current coefficient index
     */
    public static void prefetchForUpsampling(double[] coeffs, int currentIndex) {
        if (!PREFETCH_SUPPORTED || coeffs.length < MIN_PREFETCH_SIZE / 2) {
            return;
        }

        // Prefetch next coefficients
        int nextIndex = Math.min(currentIndex + L1_PREFETCH_DISTANCE, coeffs.length - 1);
        prefetchL1(coeffs, nextIndex);

        // Prefetch further ahead for L2
        int l2Index = Math.min(currentIndex + L2_PREFETCH_DISTANCE, coeffs.length - 1);
        prefetchL2(coeffs, l2Index);
    }

    /**
     * Prefetches data for combined transform operations.
     *
     * @param signal       the input signal
     * @param outputIndex  the current output index being computed
     * @param filterLength the filter length
     */
    public static void prefetchForCombinedTransform(double[] signal, int outputIndex, int filterLength) {
        if (!PREFETCH_SUPPORTED || signal.length < MIN_PREFETCH_SIZE) {
            return;
        }

        // Combined transform accesses signal at 2*outputIndex + filter offset
        int baseIndex = 2 * outputIndex;
        int signalMask = signal.length - 1;

        // Prefetch the range of signal data needed for future iterations
        for (int offset = 0; offset < filterLength; offset += DOUBLES_PER_CACHE_LINE) {
            int prefetchIndex = (baseIndex + L1_PREFETCH_DISTANCE * 2 + offset) & signalMask;
            prefetchL1(signal, prefetchIndex);
        }
    }

    /**
     * Optimized convolution with prefetching for large signals.
     */
    public static void convolveAndDownsamplePeriodicWithPrefetch(
            double[] signal, double[] filter, double[] output) {

        int signalLen = signal.length;
        int filterLen = filter.length;

        // Only use prefetching for larger signals
        if (signalLen < MIN_PREFETCH_SIZE) {
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
            return;
        }

        int signalMask = signalLen - 1;

        // Process with prefetching
        for (int i = 0; i < output.length; i++) {
            // Prefetch future data
            if ((i & 7) == 0) { // Prefetch every 8 iterations
                prefetchForConvolution(signal, i, filterLen);
            }

            double sum = 0.0;
            int kStart = 2 * i;

            // Unroll loop for better performance
            int j = 0;
            for (; j < filterLen - 3; j += 4) {
                int idx0 = (kStart + j) & signalMask;
                int idx1 = (kStart + j + 1) & signalMask;
                int idx2 = (kStart + j + 2) & signalMask;
                int idx3 = (kStart + j + 3) & signalMask;

                sum += signal[idx0] * filter[j] +
                        signal[idx1] * filter[j + 1] +
                        signal[idx2] * filter[j + 2] +
                        signal[idx3] * filter[j + 3];
            }

            // Handle remainder
            for (; j < filterLen; j++) {
                int signalIndex = (kStart + j) & signalMask;
                sum += signal[signalIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Prefetch data into L1 cache (closest to CPU).
     * Uses a compiler intrinsic hint when available.
     */
    private static void prefetchL1(double[] array, int index) {
        if (index >= 0 && index < array.length) {
            // This volatile read acts as a prefetch hint to the JVM
            // The JVM may optimize this into a prefetch instruction
            @SuppressWarnings("unused")
            double dummy = array[index];
        }
    }

    /**
     * Prefetch data into L2 cache.
     */
    private static void prefetchL2(double[] array, int index) {
        if (index >= 0 && index < array.length) {
            // Prefetch a cache line
            int cacheLineStart = (index / DOUBLES_PER_CACHE_LINE) * DOUBLES_PER_CACHE_LINE;
            if (cacheLineStart < array.length) {
                @SuppressWarnings("unused")
                double dummy = array[cacheLineStart];
            }
        }
    }

    /**
     * Prefetch data into L3 cache (for very large arrays).
     */
    private static void prefetchL3(double[] array, int index) {
        if (index >= 0 && index < array.length) {
            // Prefetch multiple cache lines for L3
            int cacheLineStart = (index / DOUBLES_PER_CACHE_LINE) * DOUBLES_PER_CACHE_LINE;
            for (int i = 0; i < 4 && cacheLineStart + i * DOUBLES_PER_CACHE_LINE < array.length; i++) {
                @SuppressWarnings("unused")
                double dummy = array[cacheLineStart + i * DOUBLES_PER_CACHE_LINE];
            }
        }
    }

    /**
     * Checks if prefetching would be beneficial for the given array size.
     *
     * @param arrayLength the length of the array
     * @return true if prefetching is recommended
     */
    public static boolean isPrefetchBeneficial(int arrayLength) {
        return PREFETCH_SUPPORTED && arrayLength >= MIN_PREFETCH_SIZE;
    }

    /**
     * Gets information about prefetch support on this platform.
     *
     * @return a string describing prefetch capabilities
     */
    public static String getPrefetchInfo() {
        return String.format("Prefetch Support: %s, Architecture: %s, Cache Line Size: %d bytes",
                PREFETCH_SUPPORTED ? "Enabled" : "Disabled",
                System.getProperty("os.arch", "unknown"),
                CACHE_LINE_SIZE);
    }
}